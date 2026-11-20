package vn.miaosong.feature.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.theme.MiaoColors

private data class DemoStoreCard(
    val id: String,
    val name: String,
    val etaBadge: String,
    val rating: String,
    val reviewCount: String,
    val distance: String,
    val shipNote: String,
    val tags: List<String>,
    val featuredTag: String,
    val featuredDish: String,
    val featuredPrice: String,
    val emojiThumb: String,
)

private val DEMO_STORES = listOf(
    DemoStoreCard(
        id = "store_bun_rieu_co_ba",
        name = "Bún Riêu Cô Ba — Nguyễn Thị Thập",
        etaBadge = "15p",
        rating = "4.9",
        reviewCount = "(1.2k+)",
        distance = "850m",
        shipNote = "Freeship đơn 50k",
        tags = listOf("⚡ Quán Quen Chọn Lọc", "Mã MIAO10 giảm 10%"),
        featuredTag = "Món bán chạy",
        featuredDish = "Bún Riêu Cua Đặc Biệt (Tô lớn · Chả cá)",
        featuredPrice = "55.000đ",
        emojiThumb = "🍜",
    ),
    DemoStoreCard(
        id = "store_bun_cha_ca_nt",
        name = "Bún Chả Cá Nha Trang — Lê Văn Lương",
        etaBadge = "18p",
        rating = "4.8",
        reviewCount = "(860+)",
        distance = "1.2km",
        shipNote = "Phí giao 14k",
        tags = listOf("Mã GIAM20K", "Chả cá thu tươi mỗi ngày"),
        featuredTag = "Gợi ý",
        featuredDish = "Bún Chả Cá Sứa Nha Trang Đặc Biệt",
        featuredPrice = "48.000đ",
        emojiThumb = "🍲",
    ),
    DemoStoreCard(
        id = "store_com_tam",
        name = "Cơm Tấm Ba Ghiền — Đặng Văn Ngữ",
        etaBadge = "20p",
        rating = "4.8",
        reviewCount = "(3.8k+)",
        distance = "1.5km",
        shipNote = "Phí giao 16k",
        tags = listOf("Mã giảm 25k", "Tặng canh rong biển"),
        featuredTag = "Món bán chạy",
        featuredDish = "Cơm sườn nướng mỡ hành đặc biệt",
        featuredPrice = "65.000đ",
        emojiThumb = "🍛",
    ),
)

/**
 * Trang chủ Chế độ Khách dựng theo đúng thiết kế Stitch
 * (`docs/ui_design/stitch_miaosong_delivery_app_ui/miaosong_trang_ch_giao_h_ng/code.html`),
 * sử dụng trực tiếp logo mèo thần tài vẫy tay `MiaoAnimatedCatLogo`.
 */
@Composable
fun CustomerHomeScreen(
    onSwitchToMerchantPreview: () -> Unit,
    onTriggerLoadingPreview: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        topBar = {
            StitchTopHeader(
                onSwitchToMerchantPreview = onSwitchToMerchantPreview,
                onTriggerLoadingPreview = onTriggerLoadingPreview,
            )
        },
        bottomBar = {
            Column {
                // Thanh nổi theo dõi đơn hàng đang giao (Floating Active Order Status Pill)
                ActiveOrderFloatingPill(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = onTriggerLoadingPreview,
                )
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                ) {
                    val navItems = listOf(
                        "Trang chủ" to Icons.Default.Home,
                        "Ưu đãi / Quán" to Icons.Default.Storefront,
                        "Đơn hàng" to Icons.Default.ReceiptLong,
                        "Chế độ Quán" to Icons.Default.Person,
                    )
                    navItems.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (index == 3) {
                                    onSwitchToMerchantPreview()
                                } else {
                                    selectedTab = index
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MiaoColors.BrandOrange,
                                selectedTextColor = MiaoColors.BrandOrange,
                                indicatorColor = MiaoColors.SurfaceContainerLow,
                                unselectedIconColor = MiaoColors.OnSurfaceVariant,
                                unselectedTextColor = MiaoColors.OnSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Thẻ mèo MiaoSong chào khách
            item {
                MiaoGreetingBanner(onClick = onTriggerLoadingPreview)
            }

            // 2. Thanh tìm kiếm + Trending tags
            item {
                SearchAndTrendingSection()
            }

            // 3. Lưới danh mục món ăn (2 hàng x 4 cột)
            item {
                QuickCategoryGrid()
            }

            // 4. Banner cam khuyến mãi
            item {
                PromoBannerCard(onClaimClick = onTriggerLoadingPreview)
            }

            // 5. Thanh lọc quán
            item {
                FilterSortPillsRow()
            }

            // 6. Danh sách quán ăn D2C
            items(DEMO_STORES, key = { it.id }) { store ->
                StoreFeedCard(
                    store = store,
                    onAddClick = onTriggerLoadingPreview,
                )
            }
        }
    }
}

@Composable
private fun StitchTopHeader(
    onSwitchToMerchantPreview: () -> Unit,
    onTriggerLoadingPreview: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Logo mèo thần tài tĩnh (static) + Brand Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTriggerLoadingPreview() },
            ) {
                MiaoAnimatedCatLogo(
                    size = 42.dp,
                    animated = false,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "MiaoSong",
                        color = MiaoColors.BrandOrange,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "GIAO HÀNG NHANH",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Pill địa chỉ giao hàng
            Surface(
                color = MiaoColors.SurfaceContainerLow,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "19 Nguyễn Hữu Thọ, Q.7",
                        color = MiaoColors.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Nút thông báo
            Surface(
                color = MiaoColors.SurfaceContainerLow,
                shape = CircleShape,
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onSwitchToMerchantPreview() },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Thông báo",
                        tint = MiaoColors.OnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MiaoColors.BrandOrange),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiaoGreetingBanner(onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF2E6),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.5.dp, MiaoColors.BrandOrange.copy(alpha = 0.28f)),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MiaoAnimatedCatLogo(
                            size = 34.dp,
                            animated = false,
                            modifier = Modifier.clip(CircleShape),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "MiaoSong chào bạn! 🐾",
                        color = MiaoColors.OnSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "Hôm nay bạn muốn ăn gì nào?",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MiaoColors.BrandOrange,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 1.dp,
            ) {
                Text(
                    text = "Freeship 0Đ",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchAndTrendingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.18f)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MiaoColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tìm bún riêu, bún chả cá, cơm tấm, trà sữa...",
                    color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = Color(0xFFFFEDD5),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Bún riêu giảm 20%",
                        color = MiaoColors.BrandOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            listOf("Bún chả cá Nha Trang", "Cơm tấm sườn bì", "Trà sữa 15p").forEach { tag ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MiaoColors.OutlineVariant),
                ) {
                    Text(
                        text = tag,
                        color = MiaoColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

private data class CategoryTile(
    val emoji: String,
    val title: String,
    val badge: String? = null,
    val isTealBadge: Boolean = false,
)

@Composable
private fun QuickCategoryGrid() {
    val row1 = listOf(
        CategoryTile("🍜", "Bún & Phở", "15-20p", isTealBadge = false),
        CategoryTile("🍛", "Cơm tấm", "Nóng hổi", isTealBadge = true),
        CategoryTile("🧋", "Trà sữa", "Mua 1+1", isTealBadge = false),
        CategoryTile("🥖", "Ăn vặt", "Siêu tốc", isTealBadge = true),
    )
    val row2 = listOf(
        CategoryTile("🍰", "Tráng miệng", null),
        CategoryTile("☕", "Cà phê", null),
        CategoryTile("⚡", "Giao nhanh", "Lấy ngay", isTealBadge = true),
        CategoryTile("🏷️", "Deal 50%", "-50%", isTealBadge = false),
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(row1, row2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                rowItems.forEach { item ->
                    val accentColor = if (item.isTealBadge) MiaoColors.SecondaryTealBright else MiaoColors.BrandOrange
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(top = 6.dp, end = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.size(52.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = item.emoji, fontSize = 25.sp)
                                }
                            }
                            if (item.badge != null) {
                                Surface(
                                    color = accentColor,
                                    shape = RoundedCornerShape(999.dp),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-7).dp),
                                ) {
                                    Text(
                                        text = item.badge,
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = item.title,
                            color = MiaoColors.OnSurface,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoBannerCard(onClaimClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(MiaoColors.BrandOrange, MiaoColors.BrandOrangeLight),
                ),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MiaoColors.TertiaryAmber,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "CAM KẾT ĐÚNG GIỜ",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "GIAO NHANH 20 PHÚT – TRỄ ĐỀN 50.000Đ",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Đặt trực tiếp từ quán quen, điều phối Lalamove tự động",
                    color = Color(0xFFFFEDD5),
                    fontSize = 11.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onClaimClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MiaoColors.BrandOrange,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Lấy mã ngay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun FilterSortPillsRow() {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = MiaoColors.OnSurface,
            shape = RoundedCornerShape(999.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Gần tôi nhất",
                    color = MiaoColors.SurfaceCanvas,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MiaoColors.SurfaceCanvas,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Surface(
            color = Color(0xFFE6FAF8),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MiaoColors.SecondaryTealBright.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "Giao nhanh nhất ⚡",
                color = MiaoColors.SecondaryTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MiaoColors.OutlineVariant),
        ) {
            Text(
                text = "Đánh giá tốt 4.8★+",
                color = MiaoColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun StoreFeedCard(
    store: DemoStoreCard,
    onAddClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Ảnh đại diện món + badge thời gian góc trái
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiaoColors.SurfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = store.emojiThumb, fontSize = 36.sp)
                    Surface(
                        color = MiaoColors.SecondaryTealBright,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    ) {
                        Text(
                            text = "⚡${store.etaBadge}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = store.name,
                        color = MiaoColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MiaoColors.TertiaryAmber,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = store.rating,
                            color = MiaoColors.OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${store.reviewCount} • ${store.distance} • ",
                            color = MiaoColors.OnSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = store.shipNote,
                            color = MiaoColors.SecondaryTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        store.tags.forEach { t ->
                            Surface(
                                color = Color(0xFFFFF2E6),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                            ) {
                                Text(
                                    text = t,
                                    color = MiaoColors.BrandOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Thanh mini-bar giới thiệu món tiêu biểu + nút [+]
            Surface(
                color = MiaoColors.SurfaceCanvas,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            color = Color(0xFFE6FAF8),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = store.featuredTag,
                                color = MiaoColors.SecondaryTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = store.featuredDish,
                            color = MiaoColors.OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = store.featuredPrice,
                            color = MiaoColors.BrandOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MiaoColors.BrandOrange,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onAddClick() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Thêm vào giỏ",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderFloatingPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.25f)),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    color = MiaoColors.BrandOrange,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Đơn hàng đang giao đến bạn",
                            color = MiaoColors.OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFFEDD5),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Đang đến",
                                color = MiaoColors.BrandOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(
                        text = "Tài xế Minh (59X1-123.45) còn cách 300m (~4 phút)",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "Xem ngay >",
                color = MiaoColors.BrandOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
