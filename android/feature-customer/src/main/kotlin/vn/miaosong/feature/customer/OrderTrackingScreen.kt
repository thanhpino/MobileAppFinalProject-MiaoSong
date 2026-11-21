package vn.miaosong.feature.customer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.MotionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.common.MoneyFormatter
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.domain.StoreRepository
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
import vn.miaosong.core.model.Store
import java.io.File
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class OrderTrackingUiState(
    val order: Order = buildFallbackTrackingOrder(),
    val store: Store? = null,
    val submittedStars: Int? = null,
    val feedbackMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    storeRepository: StoreRepository,
) : ViewModel() {

    private val requestedOrderId = MutableStateFlow<String?>(null)
    private val submittedStarsState = MutableStateFlow<Int?>(null)
    private val feedbackState = MutableStateFlow<String?>(null)

    private val liveOrderFlow = requestedOrderId.flatMapLatest { id ->
        if (!id.isNullOrBlank() && !id.startsWith("demo_")) {
            orderRepository.observeOrder(id)
        } else {
            authRepository.observeCurrentUser().flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    orderRepository.observeCustomerOrders(user.uid).map { orders ->
                        orders.firstOrNull()
                    }
                }
            }
        }
    }

    private val liveStoreFlow = liveOrderFlow.flatMapLatest { order ->
        val storeId = order?.storeId?.takeIf { it.isNotBlank() } ?: "store_bun_rieu_co_ba"
        storeRepository.observeStore(storeId)
    }

    val uiState: StateFlow<OrderTrackingUiState> = combine(
        liveOrderFlow,
        liveStoreFlow,
        submittedStarsState,
        feedbackState,
    ) { order, store, stars, msg ->
        OrderTrackingUiState(
            order = order ?: buildFallbackTrackingOrder(),
            store = store,
            submittedStars = stars,
            feedbackMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrderTrackingUiState(),
    )

    fun bindOrderId(orderId: String?) {
        requestedOrderId.value = orderId
    }

    fun clearFeedback() {
        feedbackState.value = null
    }

    fun submitReview(stars: Int, tags: List<String>, comment: String) {
        val currentOrder = uiState.value.order
        viewModelScope.launch {
            if (currentOrder.id.startsWith("demo_")) {
                submittedStarsState.value = stars
                feedbackState.value = "Cảm ơn bạn đã đánh giá $stars★ cho ${currentOrder.storeName}!"
                return@launch
            }
            when (val res = orderRepository.submitReview(currentOrder.id, stars, tags, comment)) {
                is AppResult.Success -> {
                    submittedStarsState.value = stars
                    feedbackState.value = "Đã gửi đánh giá $stars★ thành công!"
                }
                is AppResult.Error -> {
                    feedbackState.value = res.message
                }
            }
        }
    }
}

private fun buildFallbackTrackingOrder(): Order = Order(
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
            sizeName = "Tô lớn",
            toppingNames = listOf("Thêm chả cá"),
            note = "Không hành, để mắm tôm riêng",
            unitPrice = 63_000L,
            quantity = 1,
        ),
    ),
    pricing = PriceBreakdown(63_000L, 0L, 63_000L, 6_300L, 18_000L, 74_700L),
    address = DeliveryAddress(
        label = "ĐH Tôn Đức Thắng",
        fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
        lat = 10.7326,
        lng = 106.6998,
    ),
    payment = OrderPayment(PaymentMethod.ZALOPAY, PaymentStatus.PAID, 74_700L),
    status = OrderStatus.DELIVERING,
    delivery = OrderDelivery(
        driver = DriverInfo(
            name = "Nguyễn Văn Minh",
            phone = "0909888777",
            plate = "59X1-123.45",
        ),
    ),
    createdAtEpochMs = System.currentTimeMillis() - 14 * 60_000L,
)

/**
 * Màn hình Theo dõi Đơn hàng & Bản đồ OpenStreetMap (OSMDroid) realtime — Mục 4 & 5 đề bài.
 * - Hiển thị ghim Quán (Pickup), ghim Khách (Dropoff), ghim Tài xế Lalamove & tuyến đường nối.
 * - Đồng bộ trạng thái realtime qua `OrderRepository.observeOrder(orderId)`.
 * - Nút gọi điện trực tiếp cho tài xế (`ACTION_DIAL`).
 * - Tự động gợi ý pop-up Đánh giá 1–5 sao ngay khi đơn chuyển sang `Giao thành công` (`COMPLETED`).
 */
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    orderId: String? = null,
    viewModel: OrderTrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewInitialStars by remember { mutableIntStateOf(5) }
    var hasAutoPromptedReview by rememberSaveable(orderId) { mutableStateOf(false) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(orderId) {
        viewModel.bindOrderId(orderId)
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val order = uiState.order
    val store = uiState.store

    // Tự động gợi ý Đánh giá 1–5 sao ngay khi đơn chuyển sang Giao thành công (Mục 5 đề bài)
    LaunchedEffect(order.status, uiState.submittedStars) {
        if (order.status == OrderStatus.COMPLETED && uiState.submittedStars == null && !hasAutoPromptedReview) {
            hasAutoPromptedReview = true
            reviewInitialStars = 5
            showReviewDialog = true
        }
    }

    // Tọa độ Quán (Pickup) & Khách (Dropoff)
    val pickupLat = store?.lat?.takeIf { it != 0.0 } ?: 10.7418
    val pickupLng = store?.lng?.takeIf { it != 0.0 } ?: 106.7052
    val dropoffLat = order.address.lat.takeIf { it != 0.0 } ?: 10.7326
    val dropoffLng = order.address.lng.takeIf { it != 0.0 } ?: 106.6998

    // Nội suy vị trí tài xế theo trạng thái đơn hàng
    val driverProgress = when (order.status) {
        OrderStatus.PENDING_PAYMENT, OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.READY, OrderStatus.FINDING_DRIVER -> 0.0
        OrderStatus.DRIVER_ASSIGNED -> 0.18
        OrderStatus.ARRIVED_AT_STORE -> 0.05
        OrderStatus.PICKED_UP -> 0.45
        OrderStatus.DELIVERING -> 0.78
        OrderStatus.COMPLETED -> 1.0
        else -> 0.0
    }
    // Cache tuyến đường bộ ngắn nhất trong RAM để thoát ra vào lại hiển thị ngay 0ms (không bị nháy qua sông)
    val routeCacheKey = remember(pickupLat, pickupLng, dropoffLat, dropoffLng) {
        String.format("%.4f,%.4f->%.4f,%.4f", pickupLat, pickupLng, dropoffLat, dropoffLng)
    }
    val cachedEntry = remember(routeCacheKey) { osrmRouteCache[routeCacheKey] }
    var roadRoutePoints by remember(routeCacheKey) { mutableStateOf(cachedEntry?.first.orEmpty()) }
    var roadTotalMeters by remember(routeCacheKey) { mutableStateOf(cachedEntry?.second) }
    var isFetchingRoute by remember(routeCacheKey) { mutableStateOf(cachedEntry == null) }

    LaunchedEffect(routeCacheKey) {
        if (osrmRouteCache.containsKey(routeCacheKey)) {
            isFetchingRoute = false
            return@LaunchedEffect
        }
        isFetchingRoute = true
        val fetched = fetchOsrmShortestRoute(pickupLat, pickupLng, dropoffLat, dropoffLng)
        if (fetched != null && fetched.first.size >= 2) {
            osrmRouteCache[routeCacheKey] = fetched
            roadRoutePoints = fetched.first
            roadTotalMeters = fetched.second
        }
        isFetchingRoute = false
    }

    // Đặt vị trí tài xế bám chính xác trên tuyến đường bộ thực tế theo tiến độ đơn hàng
    val driverIndex = remember(roadRoutePoints, driverProgress) {
        if (roadRoutePoints.size >= 2) {
            ((roadRoutePoints.size - 1) * driverProgress).toInt()
                .coerceIn(0, roadRoutePoints.size - 1)
        } else {
            -1
        }
    }
    val driverGeoPoint = remember(roadRoutePoints, driverIndex, pickupLat, pickupLng, dropoffLat, dropoffLng) {
        if (driverIndex in roadRoutePoints.indices) {
            roadRoutePoints[driverIndex]
        } else {
            GeoPoint(pickupLat, pickupLng)
        }
    }

    val remainingKm = if (roadTotalMeters != null) {
        ((roadTotalMeters!! / 1000.0) * (1.0 - driverProgress)).coerceAtLeast(0.0)
    } else {
        haversineKm(driverGeoPoint.latitude, driverGeoPoint.longitude, dropoffLat, dropoffLng)
    }
    val etaMinutes = ((remainingKm / 0.35).toInt()).coerceAtLeast(if (order.status == OrderStatus.COMPLETED) 0 else 2)
    val elapsedMinutes = if (order.createdAtEpochMs > 0L) {
        ((System.currentTimeMillis() - order.createdAtEpochMs) / 60_000L).coerceAtLeast(0L)
    } else {
        0L
    }
    val isLateWarning = order.status != OrderStatus.COMPLETED &&
        order.status != OrderStatus.REJECTED &&
        order.status != OrderStatus.CANCELLED &&
        elapsedMinutes >= 30L

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MiaoColors.SurfaceContainerLow,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onBack() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = MiaoColors.OnSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Theo dõi đơn hàng",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MiaoColors.OnSurface,
                            )
                            Text(
                                text = "Mã đơn #${order.code}",
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    MiaoAnimatedCatLogo(
                        size = 34.dp,
                        animated = false,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Bản đồ OpenStreetMap (OSMDroid) thật hiển thị Quán -> Tài xế -> Khách
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.SecondaryTealBright.copy(alpha = 0.3f)),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    ) {
                        OsmDeliveryMapView(
                            pickup = GeoPoint(pickupLat, pickupLng),
                            dropoff = GeoPoint(dropoffLat, dropoffLng),
                            driver = driverGeoPoint,
                            driverIndex = driverIndex,
                            routePoints = roadRoutePoints,
                            storeName = order.storeName.ifBlank { "Quán MiaoSong" },
                            dropoffLabel = order.address.fullText.ifBlank { "Điểm giao hàng" },
                            driverName = order.delivery.driver?.name,
                            recenterTrigger = recenterTrigger,
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (isFetchingRoute) {
                            Surface(
                                color = Color.White.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(999.dp),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = "Đang tìm đường ngắn nhất...",
                                    color = MiaoColors.OnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }

                        // Overlay badges trên bản đồ
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MiaoColors.SecondaryTeal,
                                shape = RoundedCornerShape(999.dp),
                                shadowElevation = 2.dp,
                            ) {
                                Text(
                                    text = "MIAOSONG × LALAMOVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Surface(
                                color = if (isLateWarning) Color(0xFFFFF1F0) else Color(0xFFFFF2E6),
                                shape = RoundedCornerShape(999.dp),
                                shadowElevation = 2.dp,
                            ) {
                                Text(
                                    text = when {
                                        order.status == OrderStatus.COMPLETED -> "Đã giao thành công"
                                        isLateWarning -> "Đang trễ (${elapsedMinutes}p)"
                                        else -> "Đang đúng giờ"
                                    },
                                    color = if (isLateWarning) Color(0xFFD92D20) else MiaoColors.BrandOrangeDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        // Nút căn giữa lại bản đồ
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(38.dp)
                                .clickable { recenterTrigger++ },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Căn giữa bản đồ",
                                    tint = MiaoColors.BrandOrange,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }

                    // Thanh thông tin ETA & khoảng cách dưới bản đồ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = MiaoColors.SurfaceContainerLow,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MiaoColors.BrandOrange,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (order.status) {
                                    OrderStatus.COMPLETED -> "Đơn hàng đã giao thành công!"
                                    OrderStatus.REJECTED, OrderStatus.CANCELLED -> "Đơn hàng đã bị hủy / từ chối"
                                    else -> "Dự kiến còn khoảng $etaMinutes phút"
                                },
                                color = MiaoColors.OnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = buildString {
                                    if (order.status != OrderStatus.COMPLETED) {
                                        val distStr = if (remainingKm < 1.0) {
                                            "~${(remainingKm * 1000).toInt()}m"
                                        } else {
                                            String.format("~%.1f km", remainingKm)
                                        }
                                        append("Cách bạn $distStr · ")
                                    }
                                    append(order.address.fullText.ifBlank { "TP. Hồ Chí Minh" })
                                },
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // 2. Thẻ thông tin tài xế Lalamove (Tên, Biển số, SĐT + nút Gọi điện)
            val driver = order.delivery.driver
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.15f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            color = MiaoColors.BrandOrange,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = driver?.name ?: "Đang điều phối tài xế Lalamove...",
                                color = MiaoColors.OnSurface,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = if (driver != null) {
                                    "Biển số: ${driver.plate} · SĐT: ${driver.phone}"
                                } else {
                                    "Hệ thống đang tìm tài xế gần ${order.storeName}"
                                },
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Surface(
                                color = Color(0xFFE6FAF8),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Đối tác Lalamove · ${orderStatusLabel(order.status)}",
                                    color = MiaoColors.SecondaryTeal,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    if (driver != null && driver.phone.isNotBlank()) {
                        Surface(
                            color = MiaoColors.BrandOrange,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driver.phone}")),
                                    )
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Gọi tài xế",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 3. Thẻ Đánh giá 1–5 sao ngay khi đơn đã Giao thành công
            if (order.status == OrderStatus.COMPLETED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.3f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (uiState.submittedStars != null) {
                                "Cảm ơn bạn đã đánh giá ${uiState.submittedStars}/5 sao!"
                            } else {
                                "Bạn thấy món ăn & dịch vụ giao hàng thế nào?"
                            },
                            color = MiaoColors.OnSurface,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                val filled = uiState.submittedStars != null && star <= uiState.submittedStars!!
                                Icon(
                                    imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star sao",
                                    tint = if (filled) Color(0xFFD97706) else MiaoColors.OutlineVariant,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable(enabled = uiState.submittedStars == null) {
                                            reviewInitialStars = star
                                            showReviewDialog = true
                                        },
                                )
                            }
                        }
                    }
                }
            }

            // 4. Timeline tiến độ đơn hàng đồng bộ realtime theo OrderStatus
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Tiến độ đơn hàng",
                            color = MiaoColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = MoneyFormatter.formatVnd(order.pricing.total),
                            color = MiaoColors.BrandOrange,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    val stepIndex = when (order.status) {
                        OrderStatus.PENDING_PAYMENT -> 0
                        OrderStatus.PLACED -> 1
                        OrderStatus.CONFIRMED, OrderStatus.READY, OrderStatus.FINDING_DRIVER -> 2
                        OrderStatus.DRIVER_ASSIGNED, OrderStatus.ARRIVED_AT_STORE -> 3
                        OrderStatus.PICKED_UP, OrderStatus.DELIVERING -> 4
                        OrderStatus.COMPLETED -> 5
                        else -> 1
                    }

                    val steps = listOf(
                        "Đã đặt đơn tại ${order.storeName.ifBlank { "quán" }}" to (stepIndex >= 1),
                        "Quán đã xác nhận & đang chuẩn bị món" to (stepIndex >= 2),
                        "Tài xế Lalamove đã nhận chuyến & đến quán" to (stepIndex >= 3),
                        "Tài xế đã lấy món & đang giao đến bạn" to (stepIndex >= 4),
                        "Giao hàng thành công" to (stepIndex >= 5),
                    )
                    steps.forEach { (title, done) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (done) MiaoColors.SecondaryTealBright else MiaoColors.OutlineVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = title,
                                color = if (done) MiaoColors.OnSurface else MiaoColors.OnSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        TrackingReviewDialog(
            order = order,
            initialStars = reviewInitialStars,
            onDismiss = { showReviewDialog = false },
            onSubmit = { stars, tags, comment ->
                viewModel.submitReview(stars, tags, comment)
                showReviewDialog = false
            },
        )
    }
}

private val osrmRouteCache = ConcurrentHashMap<String, Pair<List<GeoPoint>, Double>>()

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun OsmDeliveryMapView(
    pickup: GeoPoint,
    dropoff: GeoPoint,
    driver: GeoPoint,
    driverIndex: Int,
    routePoints: List<GeoPoint>,
    storeName: String,
    dropoffLabel: String,
    driverName: String?,
    recenterTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastRecenterCount by remember { mutableIntStateOf(-1) }
    var hasFittedRoute by remember { mutableStateOf(false) }

    // Cấu hình OSMDroid dùng cache nội bộ app (không cần quyền WRITE_EXTERNAL_STORAGE trên Android 11+)
    remember {
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = context.packageName
        val osmBase = File(context.cacheDir, "osmdroid")
        cfg.osmdroidBasePath = osmBase
        cfg.osmdroidTileCache = File(osmBase, "tiles")
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.5)
            // Cho phép kéo/lướt bản đồ tự do cả lên-xuống & trái-phải dù nằm trong Column có verticalScroll
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()

            // 1. Chỉ vẽ tuyến đường khi đã có đường bộ thật từ OSRM (KHÔNG bao giờ vẽ đường thẳng bay qua sông!)
            if (routePoints.size >= 2) {
                val splitIdx = driverIndex.coerceIn(0, routePoints.lastIndex)

                // Đoạn đã đi (Quán -> Tài xế): màu xám xanh dịu
                if (splitIdx >= 1) {
                    val traveledLine = Polyline(map).apply {
                        setPoints(routePoints.subList(0, splitIdx + 1))
                        outlinePaint.color = Color(0xFF94A3B8).toArgb()
                        outlinePaint.strokeWidth = 7f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    map.overlays.add(traveledLine)
                }

                // Đoạn sắp tới (Tài xế -> Khách): viền trắng + lõi Cam MiaoSong nổi bật
                if (splitIdx < routePoints.lastIndex) {
                    val remainingPts = routePoints.subList(splitIdx, routePoints.size)
                    val casingLine = Polyline(map).apply {
                        setPoints(remainingPts)
                        outlinePaint.color = android.graphics.Color.WHITE
                        outlinePaint.strokeWidth = 13f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    val activeLine = Polyline(map).apply {
                        setPoints(remainingPts)
                        outlinePaint.color = MiaoColors.BrandOrange.toArgb()
                        outlinePaint.strokeWidth = 8.5f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    map.overlays.add(casingLine)
                    map.overlays.add(activeLine)
                }
            }

            // 2. Ghim Quán có nhãn rõ ràng
            val pickupMarker = Marker(map).apply {
                position = pickup
                title = "Quán: $storeName"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createLabeledPinDrawable(
                    context = context,
                    circleColor = MiaoColors.BrandOrange.toArgb(),
                    iconGlyph = "🍜",
                    tagText = "Quán",
                    withHalo = false,
                )
            }
            map.overlays.add(pickupMarker)

            // 3. Ghim Khách có nhãn rõ ràng
            val dropoffMarker = Marker(map).apply {
                position = dropoff
                title = "Giao tới: $dropoffLabel"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createLabeledPinDrawable(
                    context = context,
                    circleColor = MiaoColors.SecondaryTeal.toArgb(),
                    iconGlyph = "📍",
                    tagText = "Bạn",
                    withHalo = false,
                )
            }
            map.overlays.add(dropoffMarker)

            // 4. Ghim Tài xế Lalamove (chỉ hiện khi đã bám đúng đường bộ thật)
            if (routePoints.size >= 2) {
                val driverMarker = Marker(map).apply {
                    position = driver
                    title = driverName?.let { "Tài xế Lalamove: $it" } ?: "Tài xế Lalamove"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createLabeledPinDrawable(
                        context = context,
                        circleColor = Color(0xFF27180F).toArgb(),
                        iconGlyph = "🛵",
                        tagText = "Tài xế",
                        withHalo = true,
                    )
                }
                map.overlays.add(driverMarker)
            }

            // Căn khung hình vừa khít lần đầu (hoặc khi có tuyến đường mới / user bấm căn giữa)
            val shouldFit = (lastRecenterCount != recenterTrigger) || (!hasFittedRoute && routePoints.size >= 2)
            if (shouldFit) {
                lastRecenterCount = recenterTrigger
                if (routePoints.size >= 2) hasFittedRoute = true
                val allPts = (routePoints + listOf(pickup, dropoff))
                val north = allPts.maxOf { it.latitude }
                val south = allPts.minOf { it.latitude }
                val east = allPts.maxOf { it.longitude }
                val west = allPts.minOf { it.longitude }
                val latPad = ((north - south) * 0.30).coerceAtLeast(0.0022)
                val lngPad = ((east - west) * 0.30).coerceAtLeast(0.0022)
                val box = BoundingBox(north + latPad, east + lngPad, south - latPad, west - lngPad)
                map.post {
                    runCatching { map.zoomToBoundingBox(box, true, 56) }
                }
            }
            map.invalidate()
        },
    )
}

/** Gọi OSRM OpenStreetMap Routing API (alternatives=true) và chọn nhánh đường bộ NGẮN NHẤT */
private suspend fun fetchOsrmShortestRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
): Pair<List<GeoPoint>, Double>? = withContext(Dispatchers.IO) {
    runCatching {
        val urlStr = "https://router.project-osrm.org/route/v1/driving/" +
            "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson&alternatives=true"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            setRequestProperty("User-Agent", "MiaoSongAndroid/1.0")
        }
        try {
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null

            // Chọn tuyến có tổng chiều dài (distance) ngắn nhất trong các phương án OSRM trả về
            var bestRoute = routes.getJSONObject(0)
            var bestDistance = bestRoute.optDouble("distance", Double.MAX_VALUE)
            for (r in 1 until routes.length()) {
                val candidate = routes.getJSONObject(r)
                val dist = candidate.optDouble("distance", Double.MAX_VALUE)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestRoute = candidate
                }
            }

            val coords = bestRoute.getJSONObject("geometry").getJSONArray("coordinates")
            val pts = ArrayList<GeoPoint>(coords.length())
            for (i in 0 until coords.length()) {
                val c = coords.getJSONArray(i)
                pts.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
            }
            pts to bestDistance
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

private fun createLabeledPinDrawable(
    context: Context,
    circleColor: Int,
    iconGlyph: String,
    tagText: String,
    withHalo: Boolean,
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (64 * density).toInt().coerceAtLeast(120)
    val heightPx = (56 * density).toInt().coerceAtLeast(108)
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = widthPx / 2f
    val circleRadius = 15f * density
    val cy = circleRadius + 4f * density

    if (withHalo) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MiaoColors.BrandOrange.copy(alpha = 0.28f).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, circleRadius + 4.5f * density, haloPaint)
    }

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = circleColor
        style = Paint.Style.FILL
    }
    val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
    }

    canvas.drawCircle(cx, cy, circleRadius, whitePaint)
    canvas.drawCircle(cx, cy, circleRadius - 2.2f * density, fillPaint)
    val glyphY = cy - (glyphPaint.descent() + glyphPaint.ascent()) / 2f
    canvas.drawText(iconGlyph, cx, glyphY, glyphPaint)

    // Nhãn pill gọn ngay dưới biểu tượng ("Quán", "Tài xế", "Bạn")
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 9.5f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textWidth = tagPaint.measureText(tagText)
    val pillW = (textWidth + 12f * density).coerceAtLeast(34f * density)
    val pillH = 15f * density
    val pillTop = cy + circleRadius + 2f * density
    val pillRect = RectF(cx - pillW / 2f, pillTop, cx + pillW / 2f, pillTop + pillH)

    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = circleColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)
    val tagY = pillRect.centerY() - (tagPaint.descent() + tagPaint.ascent()) / 2f
    canvas.drawText(tagText, cx, tagY, tagPaint)

    return BitmapDrawable(context.resources, bmp)
}

private fun orderStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING_PAYMENT -> "Chờ thanh toán"
    OrderStatus.PLACED -> "Chờ quán nhận"
    OrderStatus.CONFIRMED -> "Quán đang chuẩn bị món"
    OrderStatus.READY -> "Đã làm xong"
    OrderStatus.FINDING_DRIVER -> "Đang tìm tài xế"
    OrderStatus.DRIVER_ASSIGNED, OrderStatus.ARRIVED_AT_STORE -> "Tài xế đang đến quán"
    OrderStatus.PICKED_UP, OrderStatus.DELIVERING -> "Đang giao"
    OrderStatus.COMPLETED -> "Giao thành công"
    OrderStatus.REJECTED, OrderStatus.CANCELLED, OrderStatus.DELIVERY_FAILED -> "Đã hủy"
}

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@Composable
private fun TrackingReviewDialog(
    order: Order,
    initialStars: Int,
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
