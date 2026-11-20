package vn.miaosong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import vn.miaosong.feature.merchant.MerchantDashboardScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiaoSongTheme {
                var activeRole by rememberSaveable { mutableStateOf(UserRole.CUSTOMER) }
                var isLoading by rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        delay(2400L)
                        isLoading = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeRole) {
                        UserRole.CUSTOMER -> CustomerHomeScreen(
                            onSwitchToMerchantPreview = { activeRole = UserRole.MERCHANT },
                            onTriggerLoadingPreview = { isLoading = true },
                        )
                        UserRole.MERCHANT -> MerchantDashboardScreen(
                            onSwitchToCustomerPreview = { activeRole = UserRole.CUSTOMER },
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
