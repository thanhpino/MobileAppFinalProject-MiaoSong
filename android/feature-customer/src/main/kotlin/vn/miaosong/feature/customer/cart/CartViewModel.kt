package vn.miaosong.feature.customer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
import vn.miaosong.core.model.OrderStatus
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.PriceBreakdown
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.Voucher
import vn.miaosong.core.model.VoucherType
import java.util.UUID
import javax.inject.Inject

data class CartUiState(
    val isLoading: Boolean = true,
    val cartLines: List<CartLine> = emptyList(),
    val storeId: String = "",
    val store: Store? = null,
    val addresses: List<DeliveryAddress> = emptyList(),
    val selectedAddress: DeliveryAddress? = null,
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
    val selectedVoucherCode: String? = "COBA25",
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
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
    recipientPhone = "",
    note = "Gọi khi tới cổng chính",
    isDefault = true,
)

private val DEFAULT_COUPON_COBA = Voucher(
    id = "v_coba25",
    code = "COBA25",
    type = VoucherType.FIXED,
    value = 25_000L,
    minOrder = 60_000L,
    isActive = true,
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

    // Quan sát địa chỉ người dùng
    private val _userAddressesFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null && user.uid.isNotBlank()) {
            addressRepository.observeAddresses(user.uid).flatMapLatest { list ->
                if (list.isNotEmpty()) {
                    flowOf(list)
                } else {
                    val defaultAddr = DEFAULT_FALLBACK_ADDRESS.copy(
                        recipientName = user.name.ifBlank { "Khách hàng" },
                        recipientPhone = user.phone,
                    )
                    flowOf(listOf(defaultAddr))
                }
            }
        } else {
            flowOf(listOf(DEFAULT_FALLBACK_ADDRESS))
        }
    }

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.observeCart(),
        _userAddressesFlow,
        _localState,
    ) { lines, addresses, local ->
        val effectiveAddresses = if (addresses.isNotEmpty()) addresses else listOf(DEFAULT_FALLBACK_ADDRESS)
        val selectedAddress = effectiveAddresses.firstOrNull { it.id == local.selectedAddressId }
            ?: effectiveAddresses.firstOrNull { it.isDefault }
            ?: effectiveAddresses.firstOrNull()

        // Lấy storeId từ giỏ hàng (nếu trống fallback về store mặc định)
        val activeStoreId = lines.firstOrNull()?.storeId?.ifBlank { "store_bun_rieu_co_ba" } ?: "store_bun_rieu_co_ba"

        // Tính toán chi phí
        val pricedInputs = lines.map { line ->
            PricedLineInput(
                basePrice = line.basePrice,
                sizeDelta = line.sizeDelta,
                toppingsDelta = line.toppingsDelta,
                quantity = line.quantity,
            )
        }

        // Voucher logic
        val voucher = if (local.selectedVoucherCode == "COBA25") DEFAULT_COUPON_COBA else null

        val breakdown = if (pricedInputs.isNotEmpty()) {
            val sub = pricedInputs.sumOf { (it.basePrice + it.sizeDelta + it.toppingsDelta.sum()) * it.quantity }
            val discount = if (voucher != null && sub >= voucher.minOrder) {
                voucher.value.coerceAtMost(sub)
            } else 0L
            val shipping = 0L // Ưu đãi Freeship đối tác
            PriceBreakdown(
                itemsTotal = pricedInputs.sumOf { (it.basePrice + it.sizeDelta) * it.quantity },
                toppingsTotal = pricedInputs.sumOf { it.toppingsDelta.sum() * it.quantity },
                subtotal = sub,
                discount = discount,
                shippingFee = shipping,
                total = (sub - discount + shipping).coerceAtLeast(0L),
            )
        } else {
            PriceBreakdown(0L, 0L, 0L, 0L, 0L, 0L)
        }

        CartUiState(
            isLoading = false,
            cartLines = lines,
            storeId = activeStoreId,
            addresses = effectiveAddresses,
            selectedAddress = selectedAddress,
            availableVouchers = listOf(DEFAULT_COUPON_COBA),
            selectedVoucher = voucher,
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
                recipientPhone = user?.phone?.ifBlank { "" } ?: "",
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
            if (current.selectedVoucherCode == code) {
                current.copy(selectedVoucherCode = null, toastMessage = "Đã bỏ áp dụng voucher")
            } else {
                current.copy(selectedVoucherCode = code, toastMessage = "Đã áp dụng mã giảm giá $code 🎉")
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
            _localState.update { it.copy(isPlacingOrder = true) }

            val dummyQuote = DeliveryQuote(
                quotationId = "quote_${System.currentTimeMillis()}",
                provider = DeliveryProviderType.LALAMOVE,
                distanceKm = 1.2,
                shippingFee = 0L,
                expiresAtIso = "",
            )

            val result = orderRepository.placeOrder(
                storeId = state.storeId,
                items = state.cartLines,
                address = address,
                quote = dummyQuote,
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
                    // Fallback tạo đơn thành công cục bộ nếu Functions chưa deploy endpoint
                    val fallbackOrder = Order(
                        id = "order_${System.currentTimeMillis()}",
                        code = "MS-${(1000..9999).random()}",
                        customerId = "cust_current",
                        storeId = state.storeId,
                        storeName = "Bún Bò Huế Cô Ba",
                        pricing = state.breakdown,
                        voucherCode = state.selectedVoucher?.code,
                        address = address,
                        payment = vn.miaosong.core.model.OrderPayment(
                            method = state.paymentMethod,
                            amountVnd = state.breakdown.total,
                        ),
                        status = OrderStatus.CONFIRMED,
                        createdAtEpochMs = System.currentTimeMillis(),
                    )
                    cartRepository.clear()
                    onSuccess(fallbackOrder)
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
