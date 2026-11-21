package vn.miaosong.app

import android.Manifest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import vn.miaosong.core.designsystem.components.MiaoLoadingOverlay
import vn.miaosong.core.designsystem.theme.MiaoSongTheme
import vn.miaosong.core.domain.AuthRepository
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Kết quả cấp quyền thông báo Android 13+ */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                var showOrderTracking by rememberSaveable { mutableStateOf(false) }
                var trackingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                var showAuthScreen by rememberSaveable { mutableStateOf(!hasLoggedInUser) }
                var showProfileScreen by rememberSaveable { mutableStateOf(false) }
                var showCartScreen by rememberSaveable { mutableStateOf(false) }
                var selectedStoreId by rememberSaveable { mutableStateOf<String?>(null) }
                var homeInitialTab by rememberSaveable { mutableStateOf(0) }
                var isLoading by rememberSaveable { mutableStateOf(false) }
                var loadingMessage by rememberSaveable {
                    mutableStateOf("MiaoSong đang chuẩn bị món ngon cho bạn...")
                }
                var loadingDurationMs by rememberSaveable { mutableStateOf(2200L) }

                LaunchedEffect(Unit) {
                    authRepository.observeCurrentUser().collect { user ->
                        if (user != null) {
                            activeRole = user.role
                            showAuthScreen = false
                        } else {
                            showAuthScreen = true
                            showProfileScreen = false
                        }
                    }
                }

                LaunchedEffect(activeRole) {
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
                            onSwitchToMerchantPreview = {
                                loadingMessage = "Đang mở không gian quản lý Quán..."
                                loadingDurationMs = 900L
                                isLoading = true
                                activeRole = UserRole.MERCHANT
                            },
                            onTriggerLoadingPreview = {
                                loadingMessage = "MiaoSong đang chuẩn bị món ngon cho bạn..."
                                loadingDurationMs = 2000L
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
