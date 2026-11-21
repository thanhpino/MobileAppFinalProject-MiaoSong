package vn.miaosong.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import vn.miaosong.core.designsystem.components.MiaoLoadingOverlay
import vn.miaosong.core.designsystem.theme.MiaoSongTheme
import vn.miaosong.core.model.UserRole
import vn.miaosong.feature.customer.CustomerHomeScreen
import vn.miaosong.feature.customer.OrderTrackingScreen
import vn.miaosong.feature.customer.auth.LoginSignUpScreen
import vn.miaosong.feature.customer.profile.ProfileScreen
import vn.miaosong.feature.merchant.MerchantDashboardScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        setContent {
            MiaoSongTheme {
                var activeRole by rememberSaveable { mutableStateOf(UserRole.CUSTOMER) }
                var showOrderTracking by rememberSaveable { mutableStateOf(false) }
                var showAuthScreen by rememberSaveable { mutableStateOf(true) }
                var showProfileScreen by rememberSaveable { mutableStateOf(false) }
                var homeInitialTab by rememberSaveable { mutableStateOf(0) }
                var isLoading by rememberSaveable { mutableStateOf(true) }
                var loadingMessage by rememberSaveable {
                    mutableStateOf("MiaoSong đang chuẩn bị món ngon cho bạn...")
                }
                var loadingDurationMs by rememberSaveable { mutableStateOf(2200L) }

                LaunchedEffect(isLoading, loadingMessage) {
                    if (isLoading) {
                        delay(loadingDurationMs)
                        isLoading = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        showAuthScreen -> LoginSignUpScreen(
                            onAuthSuccess = { user ->
                                activeRole = user.role
                                showAuthScreen = false
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
                                homeInitialTab = 2
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
                            onBack = { showOrderTracking = false },
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
                            onOpenOrderTracking = { showOrderTracking = true },
                            onOpenProfile = { showProfileScreen = true },
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
}
