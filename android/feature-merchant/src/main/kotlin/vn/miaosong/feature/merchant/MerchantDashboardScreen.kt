package vn.miaosong.feature.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.miaosong.core.designsystem.components.KitchenNoteBadge
import vn.miaosong.core.designsystem.components.MiaoPrimaryButton
import vn.miaosong.core.designsystem.theme.MiaoColors

/**
 * Màn hình khởi đầu của Chế độ Quán (feature-merchant).
 * Thành sẽ phát triển tiếp Inbox chuông đơn mới, Màn hình Bếp (KDS) và Gọi Shipper Lalamove tại đây.
 */
@Composable
fun MerchantDashboardScreen(
    onSwitchToCustomerPreview: () -> Unit,
) {
    Scaffold(containerColor = MiaoColors.SurfaceCanvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "🏪 MiaoSong — Chế độ Quán",
                style = MaterialTheme.typography.headlineLarge,
                color = MiaoColors.OnSurface,
            )
            Text(
                text = "Bún Riêu Cô Ba · Trạng thái: Đang mở cửa",
                style = MaterialTheme.typography.bodyLarge,
                color = MiaoColors.SecondaryTeal,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MiaoColors.SurfaceCard),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ticket Bếp (KDS) #MS-261120-0012",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Text(
                        text = "1x Bún Riêu Cua Đặc Biệt (Tô lớn · Thêm chả cá)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MiaoColors.OnSurface,
                    )
                    KitchenNoteBadge(note = "Không hành, để mắm tôm riêng", isAllergy = false)
                    KitchenNoteBadge(note = "Dị ứng đậu phộng — tuyệt đối không rắc", isAllergy = true)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            MiaoPrimaryButton(
                text = "Quay lại Chế độ Khách (CUSTOMER)",
                onClick = onSwitchToCustomerPreview,
            )
        }
    }
}
