package vn.miaosong.feature.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.theme.MiaoColors

/**
 * Màn hình Theo dõi Đơn hàng & Tài xế realtime (mở khi bấm "Xem ngay >" trên trang Home).
 * Dựng theo mockup Stitch `miaosong_theo_d_i_n_h_ng_warm_palette` — chứa vị trí tài xế,
 * tiến độ giao hàng và đầy đủ thông tin tài xế (Tên, SĐT, Biển số xe) theo Mục 4 & 5 đề bài.
 */
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
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
                        Text(
                            text = "Theo dõi đơn hàng",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MiaoColors.OnSurface,
                        )
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
            // 1. Khung bản đồ vị trí tài xế realtime (OpenStreetMap placeholder + ETA overlay)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7F5)),
                border = BorderStroke(1.dp, MiaoColors.SecondaryTealBright.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = MiaoColors.SecondaryTealBright,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "⚡ MIAOSONG × LALAMOVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Surface(
                            color = Color(0xFFFFF2E6),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "⏱ Đang đúng giờ",
                                color = MiaoColors.BrandOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Thông tin khoảng cách & thời gian dự kiến
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                            Column {
                                Text(
                                    text = "Còn 4 phút (Dự kiến đến 10:22)",
                                    color = MiaoColors.OnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = "Tài xế còn cách bạn ~300m · 19 Nguyễn Hữu Thọ, Q.7",
                                    color = MiaoColors.OnSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Thẻ thông tin tài xế đầy đủ (Tên, SĐT, Biển số xe + nút Gọi điện)
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
                                text = "Nguyễn Văn Minh",
                                color = MiaoColors.OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "Biển số: 59X1-123.45 · SĐT: 0909 888 777",
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Surface(
                                color = Color(0xFFE6FAF8),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Đối tác Lalamove · Đang giao",
                                    color = MiaoColors.SecondaryTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    // Nút gọi điện trực tiếp cho tài xế
                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0909888777"))
                                context.startActivity(dialIntent)
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

            // 3. Timeline tiến độ giao hàng realtime
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tiến độ đơn hàng #MS-261121-0012",
                        color = MiaoColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val steps = listOf(
                        Triple("Đã đặt đơn tại Bún Riêu Cô Ba", "10:02", true),
                        Triple("Quán đã xác nhận & chuẩn bị món", "10:04", true),
                        Triple("Tài xế đã lấy món tại quán", "10:12", true),
                        Triple("Đang giao đến bạn (~300m)", "10:18", true),
                        Triple("Giao hàng thành công", "Dự kiến 10:22", false),
                    )
                    steps.forEach { (title, time, done) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
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
                            Text(
                                text = time,
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 11.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
