package vn.miaosong.feature.customer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AddressRepository
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.CartRepository
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.domain.PricedLineInput
import vn.miaosong.core.domain.PricingCalculator
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.DeliveryProviderType
import vn.miaosong.core.model.DeliveryQuote
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.PriceBreakdown
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.Voucher
import java.util.UUID
import javax.inject.Inject

data class CartUiState(
    val isLoading: Boolean = true,
    val cartLines: List<CartLine> = emptyList(),
    val storeId: String = "",
    val store: Store? = null,
    val addresses: List<DeliveryAddress> = emptyList(),
    val selectedAddress: DeliveryAddress? = null,
    val deliveryQuote: DeliveryQuote? = null,
    val isQuotingDelivery: Boolean = false,
    val availableVouchers: List<Voucher> = emptyList(),
    val selectedVoucher: Voucher? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val breakdown: PriceBreakdown = PriceBreakdown(
        itemsTotal = 0L,
        toppingsTotal = 0L,
        subtotal = 0L,
        discount = 0L,
        shippingFee = 0L,
        total = 0L,
    ),
    val isPlacingOrder: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
)

private data class LocalCartState(
    val selectedAddressId: String? = null,
    val selectedVoucherCode: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val deliveryQuote: DeliveryQuote? = null,
    val isQuotingDelivery: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
)

private val DEFAULT_FALLBACK_ADDRESS = DeliveryAddress(
    id = "addr_default",
    label = "Trường ĐH Tôn Đức Thắng",
    fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
    lat = 10.7327,
    lng = 106.6992,
    recipientName = "Khách hàng",
    recipientPhone = "0901234567",
    note = "Gọi khi tới cổng chính",
    isDefault = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val storeRepository: StoreRepository,
    private val addressRepository: AddressRepository,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalCartState())

    private val _cartFlow = cartRepository.observeCart()

    private val _activeStoreIdFlow = _cartFlow
        .map { lines -> lines.firstOrNull()?.storeId?.takeIf { it.isNotBlank() } ?: "store_bun_rieu_co_ba" }
        .distinctUntilChanged()

    private val _storeFlow = _activeStoreIdFlow.flatMapLatest { storeId ->
        storeRepository.observeStore(storeId)
    }

    private val _vouchersFlow = _activeStoreIdFlow.flatMapLatest { storeId ->
        storeRepository.observeVouchers(storeId)
    }

    private val _userAddressesFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null && user.uid.isNotBlank()) {
            addressRepository.observeAddresses(user.uid).flatMapLatest { list ->
                if (list.isNotEmpty()) {
                    flowOf(list)
                } else {
                    val defaultAddr = DEFAULT_FALLBACK_ADDRESS.copy(
                        recipientName = user.name.ifBlank { "Khách hàng" },
                        recipientPhone = user.phone.ifBlank { "0901234567" },
                    )
                    flowOf(listOf(defaultAddr))
                }
            }
        } else {
            flowOf(listOf(DEFAULT_FALLBACK_ADDRESS))
        }
    }

    val uiState: StateFlow<CartUiState> = combine(
        _cartFlow,
        _storeFlow,
        _vouchersFlow,
        _userAddressesFlow,
        _localState,
    ) { lines, store, vouchers, addresses, local ->
        val effectiveAddresses = if (addresses.isNotEmpty()) addresses else listOf(DEFAULT_FALLBACK_ADDRESS)
        val selectedAddress = effectiveAddresses.firstOrNull { it.id == local.selectedAddressId }
            ?: effectiveAddresses.firstOrNull { it.isDefault }
            ?: effectiveAddresses.firstOrNull()

        val activeStoreId = lines.firstOrNull()?.storeId?.takeIf { it.isNotBlank() }
            ?: store?.id
            ?: "store_bun_rieu_co_ba"

        val activeVouchers = vouchers.filter { it.isActive }
        val selectedVoucher = activeVouchers.firstOrNull {
            it.code.equals(local.selectedVoucherCode, ignoreCase = true)
        }

        val pricedInputs = lines.map { line ->
            PricedLineInput(
                basePrice = line.basePrice,
                sizeDelta = line.sizeDelta,
                toppingsDelta = line.toppingsDelta,
                quantity = line.quantity,
            )
        }

        val shippingFee = if (pricedInputs.isEmpty()) 0L else (local.deliveryQuote?.shippingFee ?: 15_000L)
        val breakdown = if (pricedInputs.isNotEmpty()) {
            PricingCalculator.calculate(
                lines = pricedInputs,
                voucher = selectedVoucher,
                shippingFee = shippingFee,
            )
        } else {
            PriceBreakdown(0L, 0L, 0L, 0L, 0L, 0L)
        }

        CartUiState(
            isLoading = false,
            cartLines = lines,
            storeId = activeStoreId,
            store = store,
            addresses = effectiveAddresses,
            selectedAddress = selectedAddress,
            deliveryQuote = local.deliveryQuote,
            isQuotingDelivery = local.isQuotingDelivery,
            availableVouchers = activeVouchers,
            selectedVoucher = selectedVoucher,
            paymentMethod = local.paymentMethod,
            breakdown = breakdown,
            isPlacingOrder = local.isPlacingOrder,
            toastMessage = local.toastMessage,
            errorMessage = local.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState(isLoading = true),
    )

    init {
        // Tự động gọi Cloud Function `getDeliveryQuote` mỗi khi đổi quán hoặc địa chỉ giao hàng
        viewModelScope.launch {
            combine(
                _activeStoreIdFlow,
                _userAddressesFlow,
                _localState.map { it.selectedAddressId }.distinctUntilChanged(),
            ) { storeId, addresses, selectedId ->
                val addr = addresses.firstOrNull { it.id == selectedId }
                    ?: addresses.firstOrNull { it.isDefault }
                    ?: addresses.firstOrNull()
                    ?: DEFAULT_FALLBACK_ADDRESS
                storeId to addr
            }.distinctUntilChanged().collect { (storeId, addr) ->
                refreshDeliveryQuote(storeId, addr)
            }
        }
    }

    private suspend fun refreshDeliveryQuote(storeId: String, address: DeliveryAddress) {
        _localState.update { it.copy(isQuotingDelivery = true) }
        when (val res = orderRepository.getDeliveryQuote(storeId, address)) {
            is AppResult.Success -> {
                _localState.update {
                    it.copy(
                        deliveryQuote = res.data,
                        isQuotingDelivery = false,
                    )
                }
            }
            is AppResult.Error -> {
                // Giữ báo giá dự phòng nhẹ nếu chưa đăng nhập hoặc mạng yếu
                _localState.update {
                    it.copy(
                        deliveryQuote = it.deliveryQuote ?: DeliveryQuote(
                            quotationId = "quote_est_${System.currentTimeMillis()}",
                            provider = DeliveryProviderType.LALAMOVE,
                            distanceKm = 2.0,
                            shippingFee = 15_000L,
                            expiresAtIso = "",
                        ),
                        isQuotingDelivery = false,
                    )
                }
            }
        }
    }

    fun incrementQuantity(lineKey: String, currentQty: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(lineKey, currentQty + 1)
        }
    }

    fun decrementQuantity(lineKey: String, currentQty: Int) {
        viewModelScope.launch {
            if (currentQty <= 1) {
                cartRepository.removeLine(lineKey)
                showToast("Đã xóa món khỏi giỏ hàng")
            } else {
                cartRepository.updateQuantity(lineKey, currentQty - 1)
            }
        }
    }

    fun removeLine(lineKey: String) {
        viewModelScope.launch {
            cartRepository.removeLine(lineKey)
            showToast("Đã xóa món khỏi giỏ hàng")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clear()
            showToast("Đã dọn sạch giỏ hàng")
        }
    }

    fun selectAddress(address: DeliveryAddress) {
        _localState.update { it.copy(selectedAddressId = address.id) }
    }

    fun addNewAddress(label: String, fullText: String, note: String) {
        viewModelScope.launch {
            val user = authRepository.observeCurrentUser().first()
            val newAddr = DeliveryAddress(
                id = "",
                label = label,
                fullText = fullText,
                lat = 10.7327,
                lng = 106.6992,
                recipientName = user?.name?.ifBlank { "Khách hàng" } ?: "Khách hàng",
                recipientPhone = user?.phone?.ifBlank { "0901234567" } ?: "0901234567",
                note = note,
                isDefault = false,
            )
            if (user != null && user.uid.isNotBlank()) {
                val res = addressRepository.addAddress(user.uid, newAddr)
                if (res is AppResult.Success) {
                    _localState.update { it.copy(selectedAddressId = res.data) }
                }
            } else {
                _localState.update { it.copy(selectedAddressId = newAddr.id) }
            }
        }
    }

    fun toggleVoucher(code: String) {
        _localState.update { current ->
            if (current.selectedVoucherCode.equals(code, ignoreCase = true)) {
                current.copy(selectedVoucherCode = null, toastMessage = "Đã bỏ áp dụng voucher")
            } else {
                current.copy(selectedVoucherCode = code, toastMessage = "Đã áp dụng mã giảm giá $code")
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _localState.update { it.copy(paymentMethod = method) }
    }

    fun placeOrder(onSuccess: (Order) -> Unit) {
        val state = uiState.value
        if (state.cartLines.isEmpty()) {
            showToast("Giỏ hàng đang trống!")
            return
        }
        val address = state.selectedAddress ?: DEFAULT_FALLBACK_ADDRESS

        viewModelScope.launch {
            _localState.update { it.copy(isPlacingOrder = true, errorMessage = null) }

            // Đảm bảo có báo giá thật từ Lalamove / MockProvider trước khi tạo đơn
            val quote = state.deliveryQuote ?: when (val qRes = orderRepository.getDeliveryQuote(state.storeId, address)) {
                is AppResult.Success -> qRes.data
                is AppResult.Error -> {
                    _localState.update { it.copy(isPlacingOrder = false) }
                    showToast(qRes.message)
                    return@launch
                }
            }

            val result = orderRepository.placeOrder(
                storeId = state.storeId,
                items = state.cartLines,
                address = address,
                quote = quote,
                voucherCode = state.selectedVoucher?.code,
                paymentMethod = state.paymentMethod,
                idempotencyKey = UUID.randomUUID().toString(),
            )

            _localState.update { it.copy(isPlacingOrder = false) }

            when (result) {
                is AppResult.Success -> {
                    cartRepository.clear()
                    onSuccess(result.data)
                }
                is AppResult.Error -> {
                    // Báo lỗi thật từ Cloud Functions (quán đóng cửa, hết món, sai voucher, chưa đăng nhập...)
                    showToast(result.message)
                }
            }
        }
    }

    fun showToast(msg: String) {
        _localState.update { it.copy(toastMessage = msg) }
    }

    fun dismissToast() {
        _localState.update { it.copy(toastMessage = null) }
    }
}
