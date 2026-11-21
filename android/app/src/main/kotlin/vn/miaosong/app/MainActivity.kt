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
                var isLoading by rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        delay(2400L)
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
                                showOrderTracking = true
                            },
                        )
                        activeRole == UserRole.MERCHANT -> MerchantDashboardScreen(
                            onSwitchToCustomerPreview = { activeRole = UserRole.CUSTOMER },
                        )
                        showOrderTracking -> OrderTrackingScreen(
                            onBack = { showOrderTracking = false },
                        )
                        else -> CustomerHomeScreen(
                            onSwitchToMerchantPreview = { activeRole = UserRole.MERCHANT },
                            onTriggerLoadingPreview = { isLoading = true },
                            onOpenOrderTracking = { showOrderTracking = true },
                            onOpenProfile = { showProfileScreen = true },
                        )
                    }

                    // Màn hình Loading nền đen mờ + mèo thần tài vẫy tay cỡ lớn (ANIMATION_35 từ Stitch)
                    MiaoLoadingOverlay(
                        visible = isLoading,
                        message = "MiaoSong đang chuẩn bị món ngon cho bạn...",
                        onDismiss = { isLoading = false },
                    )
                }
            }
        }
    }
}
