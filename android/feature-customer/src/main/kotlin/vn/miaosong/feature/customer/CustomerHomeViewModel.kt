package vn.miaosong.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.NotificationItem
import vn.miaosong.core.model.NotificationType
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.OrderStatus
import vn.miaosong.core.model.RefundStatus
import vn.miaosong.core.model.Store
import java.text.Normalizer
import java.util.regex.Pattern
import javax.inject.Inject

enum class StoreSortFilter {
    NEARBY,
    FASTEST,
    TOP_RATED,
}

val DEFAULT_CUSTOMER_ADDRESS = DeliveryAddress(
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

data class CustomerHomeUiState(
    val isLoading: Boolean = true,
    val stores: List<Store> = emptyList(),
    val allStoresCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val activeFilter: StoreSortFilter = StoreSortFilter.NEARBY,
    val addresses: List<DeliveryAddress> = listOf(DEFAULT_CUSTOMER_ADDRESS),
    val selectedAddress: DeliveryAddress = DEFAULT_CUSTOMER_ADDRESS,
    val isAddressPickerVisible: Boolean = false,
    val activeOngoingOrder: Order? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val hasClaimedPromoBanner: Boolean = false,
    val toastFeedback: String? = null,
    val errorMessage: String? = null,
)

private data class OrderNotifPromoState(
    val activeOngoingOrder: Order? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val hasClaimedPromo: Boolean = false,
    val toastFeedback: String? = null,
)

private data class HomeFilterState(
    val query: String = "",
    val category: String? = null,
    val filter: StoreSortFilter = StoreSortFilter.NEARBY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val addressRepository: AddressRepository,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _activeFilter = MutableStateFlow(StoreSortFilter.NEARBY)
    private val _selectedAddressId = MutableStateFlow<String?>(null)
    private val _isAddressPickerVisible = MutableStateFlow(false)
    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    private val _hasClaimedPromo = MutableStateFlow(false)
    private val _toastFeedback = MutableStateFlow<String?>(null)

    private val activeOrderStatuses = setOf(
        OrderStatus.PENDING_PAYMENT,
        OrderStatus.PLACED,
        OrderStatus.CONFIRMED,
        OrderStatus.READY,
        OrderStatus.FINDING_DRIVER,
        OrderStatus.DRIVER_ASSIGNED,
        OrderStatus.ARRIVED_AT_STORE,
        OrderStatus.PICKED_UP,
        OrderStatus.DELIVERING,
    )

    // Quan sát đơn hàng của khách hàng realtime
    private val _customerOrdersFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null && user.uid.isNotBlank()) {
            orderRepository.observeCustomerOrders(user.uid)
        } else {
            flowOf(emptyList())
        }
    }

    private val _notifAndPromoState = combine(
        _customerOrdersFlow,
        _readNotificationIds,
        _hasClaimedPromo,
        _toastFeedback,
    ) { orders, readIds, hasClaimed, toast ->
        val activeOrder = orders.firstOrNull { it.status in activeOrderStatuses }

        val orderNotifs = orders.take(5).map { order ->
            val title = when (order.status) {
                OrderStatus.CONFIRMED -> "Quán đã nhận đơn #${order.code}"
                OrderStatus.FINDING_DRIVER -> "Đang tìm tài xế · #${order.code}"
                OrderStatus.DRIVER_ASSIGNED -> "Đã có tài xế nhận đơn · #${order.code}"
                OrderStatus.ARRIVED_AT_STORE -> "Tài xế đã tới quán · #${order.code}"
                OrderStatus.PICKED_UP -> "Tài xế đã lấy món · #${order.code}"
                OrderStatus.DELIVERING -> "Đang giao tới bạn · #${order.code}"
                OrderStatus.COMPLETED -> "Giao hàng thành công! · #${order.code}"
                OrderStatus.REJECTED -> "Đơn #${order.code} bị từ chối"
                else -> "Cập nhật đơn hàng #${order.code}"
            }
            val body = when (order.status) {
                OrderStatus.CONFIRMED -> "Quán đang chuẩn bị món cho bạn nè 🍲"
                OrderStatus.FINDING_DRIVER -> "Quán đã đóng gói xong và đang gọi tài xế Lalamove 🛵"
                OrderStatus.DRIVER_ASSIGNED -> {
                    val d = order.delivery.driver
                    if (d != null) "Tài xế ${d.name} (${d.plate}) đang tới quán lấy món."
                    else "Tài xế đang trên đường tới quán lấy món cho bạn."
                }
                OrderStatus.ARRIVED_AT_STORE -> "Tài xế đang nhận món tại quán."
                OrderStatus.PICKED_UP -> "Món nóng hổi đang trên đường tới bạn 🛵"
                OrderStatus.DELIVERING -> "Chú ý điện thoại nhé, tài xế sắp tới rồi!"
                OrderStatus.COMPLETED -> "Chúc bạn ngon miệng! Đánh giá cho quán nhé ⭐"
                OrderStatus.REJECTED -> {
                    val reason = order.rejectReason?.takeIf { it.isNotBlank() } ?: "hết nguyên liệu"
                    val refundNote = when (order.refundStatus) {
                        RefundStatus.REFUNDED -> " Tiền thanh toán ZaloPay đã được hoàn về tài khoản của bạn."
                        RefundStatus.PENDING -> " Hệ thống đang xử lý hoàn tiền ZaloPay cho bạn."
                        RefundStatus.REFUND_FAILED -> " Hoàn tiền ZaloPay gặp sự cố, vui lòng liên hệ hỗ trợ."
                        RefundStatus.NONE -> ""
                    }
                    "Quán xin lỗi, đơn bị từ chối vì $reason.$refundNote"
                }
                else -> "Đơn hàng của bạn đang được xử lý."
            }
            val notifId = if (order.status == OrderStatus.REJECTED) {
                "order_${order.id}_${order.status.name}_${order.refundStatus.name}"
            } else {
                "order_${order.id}_${order.status.name}"
            }
            NotificationItem(
                id = notifId,
                title = title,
                body = body,
                timestampEpochMs = order.createdAtEpochMs,
                type = NotificationType.ORDER_STATUS,
                orderId = order.id,
                isRead = readIds.contains(notifId),
            )
        }

        val promoNotifs = listOf(
            NotificationItem(
                id = "promo_coba25",
                title = "🎁 Ưu đãi bạn mới COBA25",
                body = "Tặng mã giảm 25.000đ cho đơn hàng từ 60.000đ khi đặt món tại MiaoSong!",
                timestampEpochMs = System.currentTimeMillis() - 3600_000L,
                type = NotificationType.PROMOTION,
                isRead = readIds.contains("promo_coba25"),
            ),
            NotificationItem(
                id = "promo_lalamove",
                title = "⚡ Cam kết giao nhanh 20 phút",
                body = "Giao hàng thần tốc cùng đối tác Lalamove. Trễ giờ đền ngay 50.000đ!",
                timestampEpochMs = System.currentTimeMillis() - 7200_000L,
                type = NotificationType.SYSTEM,
                isRead = readIds.contains("promo_lalamove"),
            ),
        )

        val allNotifs = (orderNotifs + promoNotifs).sortedByDescending { it.timestampEpochMs }
        val unreadCount = allNotifs.count { !it.isRead }

        OrderNotifPromoState(
            activeOngoingOrder = activeOrder,
            notifications = allNotifs,
            unreadCount = unreadCount,
            hasClaimedPromo = hasClaimed,
            toastFeedback = toast,
        )
    }

    // Quan sát luồng địa chỉ người dùng realtime
    private val _userAddressesFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null && user.uid.isNotBlank()) {
            addressRepository.observeAddresses(user.uid).flatMapLatest { list ->
                if (list.isNotEmpty()) {
                    flowOf(list)
                } else {
                    val defaultAddr = DEFAULT_CUSTOMER_ADDRESS.copy(
                        recipientName = user.name.ifBlank { "Khách hàng" },
                        recipientPhone = user.phone,
                    )
                    flowOf(listOf(defaultAddr))
                }
            }
        } else {
            flowOf(listOf(DEFAULT_CUSTOMER_ADDRESS))
        }
    }

    private val _addressStateFlow = combine(
        _userAddressesFlow,
        _selectedAddressId,
        _isAddressPickerVisible,
    ) { addresses, selectedId, isVisible ->
        Triple(addresses, selectedId, isVisible)
    }

    private val _filterStateFlow = combine(
        _searchQuery,
        _selectedCategory,
        _activeFilter,
    ) { query, category, filter ->
        HomeFilterState(query, category, filter)
    }

    val uiState: StateFlow<CustomerHomeUiState> = combine(
        storeRepository.observeStores(),
        _filterStateFlow,
        _addressStateFlow,
        _notifAndPromoState,
    ) { allStores, filterState, (addresses, selectedId, isPickerVisible), notifPromoState ->
        var list = allStores
        val query = filterState.query
        val category = filterState.category
        val filter = filterState.filter

        // 1. Lọc theo từ khóa tìm kiếm (Tên quán, món ăn, địa chỉ)
        if (query.isNotBlank()) {
            val normalizedQuery = query.trim().unaccent()
            list = list.filter { store ->
                store.name.unaccent().contains(normalizedQuery, ignoreCase = true) ||
                    store.slug.unaccent().contains(normalizedQuery, ignoreCase = true) ||
                    store.address.unaccent().contains(normalizedQuery, ignoreCase = true)
            }
        }

        // 2. Lọc theo danh mục được chọn
        if (!category.isNullOrBlank()) {
            list = list.filter { store ->
                store.matchesCategory(category)
            }
        }

        // 3. Sắp xếp theo bộ lọc
        list = when (filter) {
            StoreSortFilter.FASTEST -> list.sortedBy { store ->
                12 + (store.id.hashCode().let { kotlin.math.abs(it) } % 11)
            }
            StoreSortFilter.TOP_RATED -> list.sortedByDescending { it.ratingAvg }
            StoreSortFilter.NEARBY -> list
        }

        val effectiveAddresses = if (addresses.isNotEmpty()) addresses else listOf(DEFAULT_CUSTOMER_ADDRESS)
        val selectedAddress = effectiveAddresses.firstOrNull { it.id == selectedId }
            ?: effectiveAddresses.firstOrNull { it.isDefault }
            ?: effectiveAddresses.firstOrNull()
            ?: DEFAULT_CUSTOMER_ADDRESS

        CustomerHomeUiState(
            isLoading = false,
            stores = list,
            allStoresCount = allStores.size,
            searchQuery = query,
            selectedCategory = category,
            activeFilter = filter,
            addresses = effectiveAddresses,
            selectedAddress = selectedAddress,
            isAddressPickerVisible = isPickerVisible,
            activeOngoingOrder = notifPromoState.activeOngoingOrder,
            notifications = notifPromoState.notifications,
            unreadNotificationCount = notifPromoState.unreadCount,
            hasClaimedPromoBanner = notifPromoState.hasClaimedPromo,
            toastFeedback = notifPromoState.toastFeedback,
            errorMessage = null,
        )
    }.catch { e ->
        emit(
            CustomerHomeUiState(
                isLoading = false,
                stores = emptyList(),
                allStoresCount = 0,
                errorMessage = e.localizedMessage ?: "Không tải được danh sách quán.",
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerHomeUiState(isLoading = true),
    )

    fun markNotificationAsRead(id: String) {
        _readNotificationIds.update { it + id }
    }

    fun markAllNotificationsAsRead() {
        val currentIds = uiState.value.notifications.map { it.id }.toSet()
        _readNotificationIds.update { it + currentIds }
    }

    fun claimPromoVoucher() {
        _hasClaimedPromo.value = true
        _toastFeedback.value = "🎉 Đã nhận mã ưu đãi MIAOSONG50 (Trễ đền 50.000đ)! Đã lưu vào ví voucher"
    }

    fun dismissToastFeedback() {
        _toastFeedback.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun selectCategory(categoryTitle: String) {
        _selectedCategory.update { current ->
            if (current == categoryTitle) null else categoryTitle
        }
    }

    fun selectFilter(filter: StoreSortFilter) {
        _activeFilter.value = filter
    }

    fun openAddressPicker() {
        _isAddressPickerVisible.value = true
    }

    fun closeAddressPicker() {
        _isAddressPickerVisible.value = false
    }

    fun selectAddress(address: DeliveryAddress) {
        _selectedAddressId.value = address.id
        _isAddressPickerVisible.value = false
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
                    _selectedAddressId.value = res.data
                }
            } else {
                _selectedAddressId.value = newAddr.id
            }
            _isAddressPickerVisible.value = false
        }
    }

    private fun Store.matchesCategory(category: String): Boolean {
        val lower = "$name $slug $address".unaccent()
        return when (category) {
            "Bún & Phở" -> lower.contains("bun") || lower.contains("pho") || lower.contains("rieu") || lower.contains("hue")
            "Cơm tấm" -> lower.contains("com") || lower.contains("tam") || lower.contains("suon")
            "Trà sữa" -> lower.contains("tra sua") || lower.contains("phuc long") || lower.contains("tea") || lower.contains("boba")
            "Ăn vặt" -> lower.contains("banh mi") || lower.contains("an vat") || lower.contains("snack")
            "Tráng miệng" -> lower.contains("che") || lower.contains("kem") || lower.contains("cake") || lower.contains("banh")
            "Cà phê" -> lower.contains("ca phe") || lower.contains("coffee") || lower.contains("espresso")
            "Giao nhanh" -> true
            "Deal 50%" -> ratingAvg >= 4.8 || slug.contains("rieu") || slug.contains("ba")
            else -> true
        }
    }

    private fun String.unaccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D').lowercase()
    }
}
