package vn.miaosong.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import vn.miaosong.core.designsystem.components.MiaoLoadingOverlay
import vn.miaosong.core.designsystem.theme.MiaoSongTheme
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.UserRole
import vn.miaosong.feature.customer.CustomerHomeScreen
import vn.miaosong.feature.customer.OrderTrackingScreen
import vn.miaosong.feature.customer.auth.LoginSignUpScreen
import vn.miaosong.feature.customer.cart.CartScreen
import vn.miaosong.feature.customer.profile.ProfileScreen
import vn.miaosong.feature.customer.store.StoreDetailScreen
import vn.miaosong.feature.merchant.MerchantDashboardScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var storeRepository: StoreRepository

    private val openMerchantKdsSignal = MutableStateFlow(0)
    private val deepLinkStoreSlug = MutableStateFlow<String?>(null)
    private val deepLinkTrackingOrderId = MutableStateFlow<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Kết quả cấp quyền thông báo Android 13+ */ }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("open_merchant_kds", false)) {
            openMerchantKdsSignal.value += 1
        } else {
            // Chạm vào thông báo trạng thái đơn / hoàn tiền / hủy đơn phía Khách -> mở thẳng Theo dõi đơn
            intent.getStringExtra("orderId")?.takeIf { it.isNotBlank() }?.let {
                deepLinkTrackingOrderId.value = it
            }
        }
        val uri = intent.data ?: return
        val segments = uri.pathSegments
        when {
            // https://miaosong-pro.web.app/s/{slug}
            uri.scheme == "https" && segments.size >= 2 && segments[0] == "s" -> {
                deepLinkStoreSlug.value = segments[1].trim().takeIf { it.isNotEmpty() }
            }
            // miaosong://s/{slug}
            uri.scheme == "miaosong" && uri.host == "s" -> {
                deepLinkStoreSlug.value = segments.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            }
            // https://miaosong-pro.web.app/pay/return?orderId=... hoặc miaosong://pay/return?orderId=...
            (uri.scheme == "https" && uri.path?.startsWith("/pay/return") == true) ||
                (uri.scheme == "miaosong" && uri.host == "pay") -> {
                uri.getQueryParameter("orderId")?.takeIf { it.isNotBlank() }?.let {
                    deepLinkTrackingOrderId.value = it
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        requestNotificationPermissionIfNeeded()
        MiaoMessagingService.syncCurrentToken()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        setContent {
            MiaoSongTheme {
                val hasLoggedInUser = remember { FirebaseAuth.getInstance().currentUser != null }
                var activeRole by rememberSaveable { mutableStateOf(UserRole.CUSTOMER) }
                var accountRole by rememberSaveable { mutableStateOf(UserRole.CUSTOMER) }
                var showOrderTracking by rememberSaveable { mutableStateOf(false) }
                var trackingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                var showAuthScreen by rememberSaveable { mutableStateOf(!hasLoggedInUser) }
                var showProfileScreen by rememberSaveable { mutableStateOf(false) }
                var showCartScreen by rememberSaveable { mutableStateOf(false) }
                var selectedStoreId by rememberSaveable { mutableStateOf<String?>(null) }
                var homeInitialTab by rememberSaveable { mutableStateOf(0) }
                var merchantInitialTab by rememberSaveable { mutableStateOf(0) }
                var isLoading by rememberSaveable { mutableStateOf(false) }
                val kdsSignal by openMerchantKdsSignal.collectAsState()
                val pendingStoreSlug by deepLinkStoreSlug.collectAsState()
                val pendingOrderId by deepLinkTrackingOrderId.collectAsState()

                // Khi chạm vào thông báo "Có đơn hàng mới" -> mở thẳng Tab Bếp (KDS) ở Chế độ Quán
                LaunchedEffect(kdsSignal) {
                    if (kdsSignal > 0) {
                        activeRole = UserRole.MERCHANT
                        merchantInitialTab = 2
                        showAuthScreen = false
                        showProfileScreen = false
                        showCartScreen = false
                        showOrderTracking = false
                        selectedStoreId = null
                    }
                }

                // Deep Link mở thẳng trang quán: https://miaosong-pro.web.app/s/{slug} hoặc miaosong://s/{slug}
                LaunchedEffect(pendingStoreSlug) {
                    val slug = pendingStoreSlug ?: return@LaunchedEffect
                    val matchedStoreId = runCatching {
                        storeRepository.observeStores().first().firstOrNull { store ->
                            store.slug.equals(slug, ignoreCase = true) ||
                                store.id.equals(slug, ignoreCase = true)
                        }?.id
                    }.getOrNull() ?: slug
                    activeRole = UserRole.CUSTOMER
                    showProfileScreen = false
                    showCartScreen = false
                    showOrderTracking = false
                    selectedStoreId = matchedStoreId
                    deepLinkStoreSlug.value = null
                }

                // Deep Link quay về từ ZaloPay: miaosong://pay/return?orderId=...
                LaunchedEffect(pendingOrderId) {
                    val orderId = pendingOrderId ?: return@LaunchedEffect
                    activeRole = UserRole.CUSTOMER
                    showProfileScreen = false
                    showCartScreen = false
                    trackingOrderId = orderId
                    showOrderTracking = true
                    deepLinkTrackingOrderId.value = null
                }
                var loadingMessage by rememberSaveable {
                    mutableStateOf("🍜 Tinh hoa ẩm thực Việt ✨")
                }
                var loadingDurationMs by rememberSaveable { mutableStateOf(2200L) }

                LaunchedEffect(Unit) {
                    authRepository.observeCurrentUser().collect { user ->
                        if (user != null) {
                            accountRole = user.role
                            activeRole = user.role
                            showAuthScreen = false
                            MiaoMessagingService.updateActiveSession(this@MainActivity, user.uid, user.role.name)
                            MiaoMessagingService.syncCurrentToken()
                        } else {
                            accountRole = UserRole.CUSTOMER
                            showAuthScreen = true
                            showProfileScreen = false
                            MiaoMessagingService.clearActiveSession(this@MainActivity)
                        }
                    }
                }

                LaunchedEffect(activeRole) {
                    MiaoMessagingService.updateActiveRole(this@MainActivity, activeRole.name)
                    if (activeRole == UserRole.MERCHANT) {
                        MerchantOrderMonitorService.start(this@MainActivity)
                    } else {
                        MerchantOrderMonitorService.stop(this@MainActivity)
                    }
                }

                LaunchedEffect(isLoading, loadingMessage) {
                    if (isLoading) {
                        delay(loadingDurationMs)
                        isLoading = false
                    }
                }

                // Xử lý nút Back phần cứng / vuốt Back hệ thống Android
                BackHandler(enabled = showCartScreen || showProfileScreen || showOrderTracking || selectedStoreId != null) {
                    when {
                        showCartScreen -> showCartScreen = false
                        showOrderTracking -> showOrderTracking = false
                        selectedStoreId != null -> selectedStoreId = null
                        showProfileScreen -> showProfileScreen = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        showAuthScreen -> LoginSignUpScreen(
                            onAuthSuccess = { user ->
                                accountRole = user.role
                                activeRole = user.role
                                showAuthScreen = false
                                MiaoMessagingService.syncCurrentToken()
                            },
                        )
                        showProfileScreen -> ProfileScreen(
                            onBack = { showProfileScreen = false },
                            onLoggedOut = {
                                showProfileScreen = false
                                showAuthScreen = true
                            },
                            onOpenOrderHistory = {
                                showProfileScreen = false
                                homeInitialTab = 1
                            },
                            onOpenFavoriteStore = { storeId ->
                                showProfileScreen = false
                                selectedStoreId = storeId
                            },
                        )
                        activeRole == UserRole.MERCHANT -> MerchantDashboardScreen(
                            initialTab = merchantInitialTab,
                            kdsNavigationNonce = kdsSignal,
                            onSwitchToCustomerPreview = {
                                loadingMessage = "Đang quay lại trang chủ MiaoSong..."
                                loadingDurationMs = 700L
                                isLoading = true
                                activeRole = UserRole.CUSTOMER
                            },
                        )
                        showOrderTracking -> OrderTrackingScreen(
                            orderId = trackingOrderId,
                            onBack = {
                                showOrderTracking = false
                                trackingOrderId = null
                            },
                        )
                        showCartScreen -> CartScreen(
                            onBack = { showCartScreen = false },
                            onAddMore = { showCartScreen = false },
                            onOrderSuccess = { order ->
                                showCartScreen = false
                                selectedStoreId = null
                                trackingOrderId = order.id
                                showOrderTracking = true
                            },
                        )
                        selectedStoreId != null -> StoreDetailScreen(
                            storeId = selectedStoreId!!,
                            onBack = { selectedStoreId = null },
                            onOpenCart = { showCartScreen = true },
                        )
                        else -> CustomerHomeScreen(
                            showMerchantSwitch = accountRole == UserRole.MERCHANT,
                            onSwitchToMerchantPreview = {
                                loadingMessage = "Đang mở không gian quản lý Quán..."
                                loadingDurationMs = 900L
                                isLoading = true
                                activeRole = UserRole.MERCHANT
                            },
                            onTriggerLoadingPreview = {
                                loadingMessage = "🍜 Tinh hoa ẩm thực Việt ✨"
                                loadingDurationMs = 800L
                                isLoading = true
                            },
                            onOpenOrderTracking = { id ->
                                trackingOrderId = id
                                showOrderTracking = true
                            },
                            onOpenProfile = { showProfileScreen = true },
                            onOpenCart = { showCartScreen = true },
                            onSelectStore = { storeId ->
                                selectedStoreId = storeId
                            },
                            initialTab = homeInitialTab,
                        )
                    }

                    // Màn hình Loading nền đen mờ + mèo thần tài vẫy tay cỡ lớn (ANIMATION_35 từ Stitch)
                    MiaoLoadingOverlay(
                        visible = isLoading,
                        message = loadingMessage,
                        onDismiss = { isLoading = false },
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
