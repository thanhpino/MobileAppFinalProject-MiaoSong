package vn.miaosong.feature.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.MerchantCatalogRepository
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.OrderStatus
import vn.miaosong.core.model.Review
import vn.miaosong.core.model.SpecialDay
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.TimeSlot
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.UserRole
import vn.miaosong.core.model.Voucher
import javax.inject.Inject

data class MerchantUiState(
    val storeId: String = DEFAULT_MERCHANT_STORE_ID,
    val isAuthResolved: Boolean = false,
    val currentUser: UserProfile? = null,
    val store: Store? = null,
    val categories: List<MenuCategory> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val vouchers: List<Voucher> = emptyList(),
    val orders: List<Order> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val isBusyAction: Boolean = false,
    val feedbackMessage: String? = null,
)

const val DEFAULT_MERCHANT_STORE_ID = "store_bun_rieu_co_ba"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MerchantViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    storeRepository: StoreRepository,
    private val catalogRepository: MerchantCatalogRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val busyState = MutableStateFlow(false)
    private val feedbackState = MutableStateFlow<String?>(null)

    private val storeId = DEFAULT_MERCHANT_STORE_ID

    private val currentUserFlow = authRepository.observeCurrentUser()

    // Tự động đăng ký lại listener `orders` ngay khi tài khoản Quán (MERCHANT) đăng nhập thành công
    private val liveOrdersFlow = currentUserFlow.flatMapLatest { user ->
        if (user?.role == UserRole.MERCHANT) {
            orderRepository.observeStoreOrders(user.storeId?.takeIf { it.isNotBlank() } ?: storeId)
        } else {
            flowOf(emptyList())
        }
    }

    val uiState: StateFlow<MerchantUiState> = combine(
        currentUserFlow,
        storeRepository.observeStore(storeId),
        storeRepository.observeCategories(storeId),
        storeRepository.observeMenuItems(storeId),
        storeRepository.observeVouchers(storeId),
    ) { user, store, categories, items, vouchers ->
        MerchantUiState(
            storeId = user?.storeId ?: storeId,
            isAuthResolved = true,
            currentUser = user,
            store = store,
            categories = categories,
            menuItems = items,
            vouchers = vouchers,
        )
    }.combine(liveOrdersFlow) { state, orders ->
        state.copy(orders = orders)
    }.combine(storeRepository.observeStoreReviews(storeId)) { state, reviews ->
        state.copy(reviews = reviews)
    }.combine(busyState) { state, busy ->
        state.copy(isBusyAction = busy)
    }.combine(feedbackState) { state, msg ->
        state.copy(feedbackMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MerchantUiState(),
    )

    fun clearFeedback() {
        feedbackState.value = null
    }

    fun showFeedback(message: String) {
        feedbackState.value = message
    }

    fun signInMerchant(email: String, password: String) {
        runAction("Đăng nhập tài khoản Quán thành công!") {
            when (val res = authRepository.signInWithEmail(email, password)) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Error -> res
            }
        }
    }

    fun addCategory(name: String) {
        val nextSort = (uiState.value.categories.maxOfOrNull { it.sortOrder } ?: 0) + 1
        runAction("Đã thêm nhóm món \"$name\"") {
            catalogRepository.upsertCategory(
                uiState.value.storeId,
                MenuCategory(id = "", name = name, sortOrder = nextSort),
            )
        }
    }

    fun saveMenuItem(item: MenuItem) {
        runAction("Đã lưu món \"${item.name}\"") {
            catalogRepository.upsertMenuItem(uiState.value.storeId, item)
        }
    }

    fun toggleItemAvailability(itemId: String, isAvailable: Boolean) {
        val label = if (isAvailable) "Đã bật Còn hàng" else "Đã đánh dấu Hết hàng"
        runAction(label) {
            catalogRepository.toggleMenuItemAvailability(uiState.value.storeId, itemId, isAvailable)
        }
    }

    fun deleteMenuItem(itemId: String) {
        runAction("Đã xóa món ăn") {
            catalogRepository.deleteMenuItem(uiState.value.storeId, itemId)
        }
    }

    fun saveVoucher(voucher: Voucher) {
        runAction("Đã tạo mã ${voucher.code.uppercase()}") {
            catalogRepository.upsertVoucher(uiState.value.storeId, voucher)
        }
    }

    fun toggleVoucherActive(voucherId: String, isActive: Boolean) {
        runAction(if (isActive) "Đã kích hoạt voucher" else "Đã tạm tắt voucher") {
            catalogRepository.toggleVoucherActive(uiState.value.storeId, voucherId, isActive)
        }
    }

    fun deleteVoucher(voucherId: String) {
        runAction("Đã xóa voucher") {
            catalogRepository.deleteVoucher(uiState.value.storeId, voucherId)
        }
    }

    fun updateStoreStatus(status: ManualStoreStatus, busyMinutes: Int? = null) {
        val until = busyMinutes?.let { System.currentTimeMillis() + it * 60_000L }
        val msg = when (status) {
            ManualStoreStatus.OPEN -> "Quán đã chuyển sang Đang mở cửa"
            ManualStoreStatus.BUSY -> "Quán tạm bận trong ${busyMinutes ?: 30} phút"
            ManualStoreStatus.CLOSED -> "Quán đã đóng cửa"
        }
        runAction(msg) {
            catalogRepository.updateStoreStatus(uiState.value.storeId, status, until)
        }
    }

    fun updateStoreInfo(name: String, address: String, phone: String) {
        runAction("Đã cập nhật thông tin quán!") {
            catalogRepository.updateStoreInfo(uiState.value.storeId, name, address, phone)
        }
    }

    fun updateDailyHours(openTime: String, closeTime: String) {
        val days = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        val weekly = days.associateWith { listOf(TimeSlot(open = openTime.trim(), close = closeTime.trim())) }
        val existingSpecials = uiState.value.store?.schedule?.specialDays.orEmpty()
        runAction("Đã cập nhật lịch hoạt động ($openTime – $closeTime)") {
            catalogRepository.updateStoreSchedule(uiState.value.storeId, weekly, existingSpecials)
        }
    }

    fun addSpecialDay(date: String, isClosed: Boolean, openTime: String = "08:00", closeTime: String = "20:00") {
        val current = uiState.value.store?.schedule
        val weekly = current?.weeklySchedule.orEmpty()
        val updated = current?.specialDays.orEmpty().filterNot { it.date == date.trim() } + SpecialDay(
            date = date.trim(),
            isClosed = isClosed,
            slots = if (isClosed) emptyList() else listOf(TimeSlot(openTime.trim(), closeTime.trim())),
        )
        runAction("Đã lưu lịch tùy chỉnh ngày $date") {
            catalogRepository.updateStoreSchedule(uiState.value.storeId, weekly, updated)
        }
    }

    fun removeSpecialDay(date: String) {
        val current = uiState.value.store?.schedule
        val weekly = current?.weeklySchedule.orEmpty()
        val updated = current?.specialDays.orEmpty().filterNot { it.date == date }
        runAction("Đã xóa lịch tùy chỉnh ngày $date") {
            catalogRepository.updateStoreSchedule(uiState.value.storeId, weekly, updated)
        }
    }

    fun uploadStoreAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            busyState.value = true
            when (val res = catalogRepository.uploadStoreAvatar(uiState.value.storeId, bytes)) {
                is AppResult.Success -> feedbackState.value = "Đã cập nhật ảnh đại diện quán!"
                is AppResult.Error -> feedbackState.value = res.message
            }
            busyState.value = false
        }
    }

    fun uploadMenuItemImage(itemId: String, bytes: ByteArray) {
        viewModelScope.launch {
            busyState.value = true
            when (val res = catalogRepository.uploadMenuItemImage(uiState.value.storeId, itemId, bytes)) {
                is AppResult.Success -> feedbackState.value = "Đã cập nhật ảnh món ăn!"
                is AppResult.Error -> feedbackState.value = res.message
            }
            busyState.value = false
        }
    }

    fun confirmOrder(orderId: String) {
        runAction("Đã xác nhận nhận đơn — Khách nhận thông báo 'Quán đang chuẩn bị món'") {
            orderRepository.confirmOrder(orderId)
        }
    }

    fun rejectOrder(orderId: String, reasonCode: String, reasonText: String) {
        runAction("Đã từ chối đơn hàng") {
            orderRepository.rejectOrder(orderId, reasonCode, reasonText)
        }
    }

    fun markOrderReady(orderId: String) {
        runAction("Đơn đã làm xong — Sẵn sàng gọi tài xế!") {
            orderRepository.markReady(orderId)
        }
    }

    fun dispatchDriver(orderId: String) {
        runAction("Đã gửi yêu cầu điều phối tài xế Lalamove!") {
            orderRepository.dispatchDriver(orderId)
        }
    }

    fun advanceMockDelivery(orderId: String, targetStatus: OrderStatus) {
        runAction("Đã cập nhật trạng thái giao hàng: ${targetStatus.name}") {
            orderRepository.mockDeliveryAdvance(orderId, targetStatus)
        }
    }

    private fun runAction(successMsg: String, block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            busyState.value = true
            when (val res = block()) {
                is AppResult.Success -> feedbackState.value = successMsg
                is AppResult.Error -> feedbackState.value = res.message
            }
            busyState.value = false
        }
    }
}
