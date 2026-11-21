package vn.miaosong.feature.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import vn.miaosong.core.common.MoneyFormatter
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.CartLineKey
import vn.miaosong.core.domain.CartRepository
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.DriverInfo
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.OrderDelivery
import vn.miaosong.core.model.OrderItemSnapshot
import vn.miaosong.core.model.OrderPayment
import vn.miaosong.core.model.OrderStatus
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.PaymentStatus
import vn.miaosong.core.model.PriceBreakdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CustomerOrdersUiState(
    val orders: List<Order> = emptyList(),
    val reviewedStarsByOrderId: Map<String, Int> = emptyMap(),
    val feedbackMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerOrdersViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val reviewedMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val feedbackState = MutableStateFlow<String?>(null)

    private val ordersFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else orderRepository.observeCustomerOrders(user.uid)
    }

    val uiState: StateFlow<CustomerOrdersUiState> = combine(
        ordersFlow,
        reviewedMap,
        feedbackState,
    ) { orders, reviewed, msg ->
        CustomerOrdersUiState(
            orders = orders,
            reviewedStarsByOrderId = reviewed,
            feedbackMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerOrdersUiState(),
    )

    fun clearFeedback() {
        feedbackState.value = null
    }

    /** Tái mua hàng 1 chạm: đổ lại toàn bộ món + Size + Topping + ghi chú vào giỏ hàng Room */
    fun reorder(order: Order) {
        viewModelScope.launch {
            cartRepository.clear()
            order.items.forEach { snap ->
                val key = snap.lineKey.ifBlank {
                    CartLineKey.compute(
                        menuItemId = snap.menuItemId,
                        sizeOptionId = snap.sizeOptionId,
                        toppingOptionIds = snap.toppingOptionIds,
                        note = snap.note,
                    )
                }
                cartRepository.upsertLine(
                    CartLine(
                        lineKey = key,
                        storeId = order.storeId,
                        menuItemId = snap.menuItemId,
                        itemName = snap.itemName,
                        basePrice = snap.unitPrice,
                        sizeOptionId = snap.sizeOptionId,
                        sizeName = snap.sizeName,
                        toppingOptionIds = snap.toppingOptionIds,
                        toppingNames = snap.toppingNames,
                        note = snap.note,
                        quantity = snap.quantity,
                    ),
                )
            }
            feedbackState.value = "Đã thêm lại ${order.items.sumOf { it.quantity }} món từ đơn #${order.code} vào giỏ hàng!"
        }
    }

    fun submitReview(order: Order, stars: Int, tags: List<String>, comment: String) {
        viewModelScope.launch {
            if (order.id.startsWith("demo_")) {
                reviewedMap.value = reviewedMap.value + (order.id to stars)
                feedbackState.value = "Cảm ơn bạn đã đánh giá $stars★ cho ${order.storeName}!"
                return@launch
            }
            when (val res = orderRepository.submitReview(order.id, stars, tags, comment)) {
                is AppResult.Success -> {
                    reviewedMap.value = reviewedMap.value + (order.id to stars)
                    feedbackState.value = "Đã gửi đánh giá $stars★ thành công!"
                }
                is AppResult.Error -> {
                    feedbackState.value = res.message
                }
            }
        }
    }
}

private val ACTIVE_ORDER_STATUSES = setOf(
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

private fun buildSampleCustomerOrders(): List<Order> {
    val now = System.currentTimeMillis()
    return listOf(
        Order(
            id = "demo_active_1",
            code = "MS-261121-0012",
            customerId = "demo",
            storeId = "store_bun_rieu_co_ba",
            storeName = "Bún Riêu Cô Ba — Nguyễn Thị Thập",
            items = listOf(
                OrderItemSnapshot(
                    lineKey = "k1",
                    menuItemId = "item_bun_rieu_db",
                    itemName = "Bún Riêu Cua Đặc Biệt",
                    sizeOptionId = "opt_size_large",
                    sizeName = "Tô lớn",
                    toppingOptionIds = listOf("opt_top_cha"),
                    toppingNames = listOf("Thêm chả"),
                    note = "Không hành, để mắm tôm riêng",
                    unitPrice = 63_000L,
                    quantity = 1,
                ),
            ),
            pricing = PriceBreakdown(63_000L, 0L, 63_000L, 6_300L, 18_000L, 74_700L),
            voucherCode = "MIAO10",
            address = DeliveryAddress(fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Q.7", lat = 10.732, lng = 106.699),
            payment = OrderPayment(PaymentMethod.ZALOPAY, PaymentStatus.PAID, 74_700L),
            status = OrderStatus.DELIVERING,
            delivery = OrderDelivery(
                driver = DriverInfo("Nguyễn Văn Minh", "0909888777", "59X1-123.45"),
            ),
            createdAtEpochMs = now - 14 * 60_000L,
        ),
        Order(
            id = "demo_past_1",
            code = "MS-261120-0088",
            customerId = "demo",
            storeId = "store_bun_rieu_co_ba",
            storeName = "Bún Riêu Cô Ba — Nguyễn Thị Thập",
            items = listOf(
                OrderItemSnapshot(
                    lineKey = "k2",
                    menuItemId = "item_bun_rieu_db",
                    itemName = "Bún Riêu Cua Đặc Biệt",
                    sizeOptionId = "opt_size_regular",
                    sizeName = "Tô thường",
                    toppingOptionIds = listOf("opt_top_cha", "opt_top_gio"),
                    toppingNames = listOf("Thêm chả", "Thêm giò"),
                    note = "Ít cay",
                    unitPrice = 68_000L,
                    quantity = 2,
                ),
            ),
            pricing = PriceBreakdown(136_000L, 0L, 136_000L, 13_600L, 15_000L, 137_400L),
            voucherCode = "MIAO10",
            address = DeliveryAddress(fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Q.7", lat = 10.732, lng = 106.699),
            payment = OrderPayment(PaymentMethod.COD, PaymentStatus.PAID, 137_400L),
            status = OrderStatus.COMPLETED,
            createdAtEpochMs = now - 26 * 60 * 60_000L,
        ),
        Order(
            id = "demo_past_2",
            code = "MS-261119-0041",
            customerId = "demo",
            storeId = "store_com_tam_ba_ghien",
            storeName = "Cơm Tấm Sườn Bì Chả Ba Ghiền",
            items = listOf(
                OrderItemSnapshot(
                    lineKey = "k3",
                    menuItemId = "item_com_suon",
                    itemName = "Cơm Tấm Sườn Bì Chả Trứng",
                    sizeOptionId = "opt_large",
                    sizeName = "Phần lớn",
                    toppingOptionIds = listOf("opt_trung_op_la"),
                    toppingNames = listOf("Trứng ốp la"),
                    note = "Nước mắm ngọt riêng",
                    unitPrice = 65_000L,
                    quantity = 1,
                ),
            ),
            pricing = PriceBreakdown(65_000L, 0L, 65_000L, 0L, 16_000L, 81_000L),
            address = DeliveryAddress(fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Q.7", lat = 10.732, lng = 106.699),
            payment = OrderPayment(PaymentMethod.ZALOPAY, PaymentStatus.PAID, 81_000L),
            status = OrderStatus.COMPLETED,
            createdAtEpochMs = now - 52 * 60 * 60_000L,
        ),
    )
}

/**
 * Tab "Đơn hàng" tích hợp ngay trong thanh điều hướng dưới của Khách:
 * - Trên cùng: Các đơn đang đặt / đang giao (bấm "Xem chi tiết đơn" mở màn Theo dõi realtime).
 * - Ở dưới: Lịch sử đơn hàng (Mã đơn, ngày giờ đặt, đánh giá 1-5 sao & nút "Đặt lại đơn này").
 */
@Composable
fun CustomerOrdersTab(
    onOpenOrderDetail: (Order) -> Unit,
    onNavigateToCheckout: (Order) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CustomerOrdersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var reviewingTarget by remember { mutableStateOf<Pair<Order, Int>?>(null) }
    var reorderingOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val allOrders = remember(uiState.orders) {
        if (uiState.orders.isNotEmpty()) uiState.orders else buildSampleCustomerOrders()
    }
    val activeOrders = remember(allOrders) {
        allOrders.filter { it.status in ACTIVE_ORDER_STATUSES }
            .sortedByDescending { it.createdAtEpochMs }
    }
    val pastOrders = remember(allOrders) {
        allOrders.filterNot { it.status in ACTIVE_ORDER_STATUSES }
            .sortedByDescending { it.createdAtEpochMs }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. PHẦN TRÊN CÙNG: ĐƠN ĐANG ĐẶT / ĐANG GIAO
            item {
                Text(
                    text = "Đơn đang thực hiện (${activeOrders.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
            }

            if (activeOrders.isEmpty()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Hiện bạn không có đơn hàng nào đang giao.",
                            color = MiaoColors.OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(activeOrders, key = { it.id }) { order ->
                    ActiveCustomerOrderCard(
                        order = order,
                        onViewDetail = { onOpenOrderDetail(order) },
                    )
                }
            }

            // 2. PHẦN DƯỚI: LỊCH SỬ ĐƠN HÀNG (ĐÃ ĐẶT)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lịch sử đơn hàng (${pastOrders.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
            }

            items(pastOrders, key = { it.id }) { order ->
                PastCustomerOrderCard(
                    order = order,
                    votedStars = uiState.reviewedStarsByOrderId[order.id],
                    onSelectStar = { star -> reviewingTarget = order to star },
                    onReorderClick = {
                        viewModel.reorder(order)
                        reorderingOrder = order
                        onNavigateToCheckout(order)
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    reviewingTarget?.let { (target, initialStars) ->
        ReviewOrderDialog(
            order = target,
            initialStars = initialStars,
            onDismiss = { reviewingTarget = null },
            onSubmit = { stars, tags, comment ->
                viewModel.submitReview(target, stars, tags, comment)
                reviewingTarget = null
            },
        )
    }

    reorderingOrder?.let { target ->
        ReorderQuickReviewDialog(
            order = target,
            onDismiss = { reorderingOrder = null },
            onProceed = { updatedMethod ->
                reorderingOrder = null
                onOpenOrderDetail(target.copy(payment = target.payment.copy(method = updatedMethod)))
            },
        )
    }
}

@Composable
private fun ActiveCustomerOrderCard(
    order: Order,
    onViewDetail: () -> Unit,
) {
    val statusText = when (order.status) {
        OrderStatus.PENDING_PAYMENT -> "Chờ thanh toán"
        OrderStatus.PLACED -> "Chờ quán nhận"
        OrderStatus.CONFIRMED -> "Đang chuẩn bị"
        OrderStatus.READY, OrderStatus.FINDING_DRIVER -> "Đang tìm tài xế"
        OrderStatus.DRIVER_ASSIGNED, OrderStatus.ARRIVED_AT_STORE -> "Tài xế đến quán"
        else -> "Đang giao"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, MiaoColors.BrandOrange.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mã đơn #${order.code}",
                    color = MiaoColors.OnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = Color(0xFFE6FAF8), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = statusText,
                        color = MiaoColors.SecondaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = order.storeName.ifBlank { "Quán MiaoSong" },
                color = MiaoColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Đặt lúc ${formatOrderDateTime(order.createdAtEpochMs)}",
                color = MiaoColors.OnSurfaceVariant,
                fontSize = 11.5.sp,
            )

            order.items.forEach { item ->
                val opts = buildList {
                    if (item.sizeName.isNotBlank()) add(item.sizeName)
                    if (item.toppingNames.isNotEmpty()) add(item.toppingNames.joinToString(", "))
                }.joinToString(" · ")
                Text(
                    text = "${item.quantity}x ${item.itemName}" + if (opts.isNotBlank()) " ($opts)" else "",
                    color = MiaoColors.OnSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = MoneyFormatter.formatVnd(order.pricing.total),
                    color = MiaoColors.BrandOrange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Surface(
                    color = MiaoColors.BrandOrange,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable { onViewDetail() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Xem chi tiết đơn",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastCustomerOrderCard(
    order: Order,
    votedStars: Int?,
    onSelectStar: (Int) -> Unit,
    onReorderClick: () -> Unit,
) {
    val isCompleted = order.status == OrderStatus.COMPLETED
    val (statusLabel, statusBg, statusFg) = if (isCompleted) {
        Triple("Giao thành công", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
    } else {
        Triple("Đã hủy / Từ chối", Color(0xFFFFF1F0), Color(0xFFD92D20))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.14f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mã đơn #${order.code}",
                    color = MiaoColors.OnSurface,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = statusBg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = order.storeName.ifBlank { "Quán MiaoSong" },
                color = MiaoColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ngày đặt: ${formatOrderDateTime(order.createdAtEpochMs)}",
                color = MiaoColors.OnSurfaceVariant,
                fontSize = 11.5.sp,
            )

            order.items.forEach { item ->
                val opts = buildList {
                    if (item.sizeName.isNotBlank()) add(item.sizeName)
                    if (item.toppingNames.isNotEmpty()) add(item.toppingNames.joinToString(", "))
                }.joinToString(" · ")
                Text(
                    text = "${item.quantity}x ${item.itemName}" + if (opts.isNotBlank()) " ($opts)" else "",
                    color = MiaoColors.OnSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "Tổng tiền: ${MoneyFormatter.formatVnd(order.pricing.total)} · ${order.payment.method.name}",
                color = MiaoColors.BrandOrangeDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            if (isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Hàng 5 ngôi sao trực tiếp: chưa đánh giá -> 5 sao trắng viền; đã đánh giá -> tô màu đúng số sao
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        (1..5).forEach { star ->
                            val isFilled = votedStars != null && star <= votedStars
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star sao",
                                tint = if (isFilled) Color(0xFFD97706) else MiaoColors.OutlineVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(enabled = votedStars == null) { onSelectStar(star) },
                            )
                        }
                        if (votedStars != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$votedStars/5",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { onReorderClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Đặt lại đơn này",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewOrderDialog(
    order: Order,
    initialStars: Int = 5,
    onDismiss: () -> Unit,
    onSubmit: (Int, List<String>, String) -> Unit,
) {
    var stars by remember(initialStars) { mutableIntStateOf(initialStars) }
    var comment by remember { mutableStateOf("") }
    val allTags = listOf("Món ngon", "Đóng gói kỹ", "Giao nhanh", "Đúng ghi chú")
    var selectedTags by remember { mutableStateOf(setOf("Món ngon")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Đánh giá đơn #${order.code}",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = order.storeName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    (1..5).forEach { value ->
                        Icon(
                            imageVector = if (value <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$value sao",
                            tint = Color(0xFFD97706),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { stars = value }
                                .padding(2.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag in selectedTags
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MiaoColors.BrandOrange,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét của bạn (tùy chọn)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(stars, selectedTags.toList(), comment) },
            ) { Text("Gửi đánh giá", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để sau") } },
    )
}

@Composable
private fun ReorderQuickReviewDialog(
    order: Order,
    onDismiss: () -> Unit,
    onProceed: (PaymentMethod) -> Unit,
) {
    var selectedMethod by remember { mutableStateOf(order.payment.method) }
    var quantities by remember { mutableStateOf(order.items.associate { it.lineKey to it.quantity }) }

    val updatedSubtotal = order.items.sumOf { snap ->
        val qty = quantities[snap.lineKey] ?: 0
        snap.unitPrice * qty
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Đặt lại từ ${order.storeName}",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
                fontSize = 17.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Đã giữ nguyên món, Size & Topping cũ — bạn có thể chỉnh số lượng hoặc đổi phương thức thanh toán trước khi đặt:",
                    fontSize = 12.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )

                order.items.forEach { snap ->
                    val qty = quantities[snap.lineKey] ?: 0
                    val opts = buildList {
                        if (snap.sizeName.isNotBlank()) add(snap.sizeName)
                        if (snap.toppingNames.isNotEmpty()) add(snap.toppingNames.joinToString(", "))
                    }.joinToString(" · ")

                    Surface(
                        color = MiaoColors.SurfaceCanvas,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snap.itemName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MiaoColors.OnSurface,
                                )
                                if (opts.isNotBlank()) {
                                    Text(
                                        text = opts,
                                        fontSize = 11.5.sp,
                                        color = MiaoColors.OnSurfaceVariant,
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MiaoColors.OutlineVariant),
                                    modifier = Modifier.clickable {
                                        quantities = quantities + (snap.lineKey to (qty - 1).coerceAtLeast(0))
                                    },
                                ) {
                                    Text("−", modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), fontWeight = FontWeight.Bold)
                                }
                                Text("$qty", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                                Surface(
                                    color = MiaoColors.BrandOrange,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable {
                                        quantities = quantities + (snap.lineKey to (qty + 1))
                                    },
                                ) {
                                    Text("+", color = Color.White, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Phương thức thanh toán",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiaoColors.OnSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.COD,
                        onClick = { selectedMethod = PaymentMethod.COD },
                        label = { Text("Tiền mặt (COD)", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiaoColors.BrandOrange,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.ZALOPAY,
                        onClick = { selectedMethod = PaymentMethod.ZALOPAY },
                        label = { Text("ZaloPay", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiaoColors.BrandOrange,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }

                Text(
                    text = "Tạm tính món: ${MoneyFormatter.formatVnd(updatedSubtotal)}",
                    color = MiaoColors.BrandOrangeDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = updatedSubtotal > 0L,
                onClick = { onProceed(selectedMethod) },
            ) { Text("Tiếp tục đặt hàng", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}

private fun formatOrderDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return "Hôm nay"
    val fmt = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN"))
    return fmt.format(Date(epochMs))
}
