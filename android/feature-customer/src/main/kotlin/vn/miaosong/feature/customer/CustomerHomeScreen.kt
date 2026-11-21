package vn.miaosong.feature.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.Store

private data class StoreTag(
    val label: String,
    val bgColor: Color = Color(0xFFFFF2E6),
    val textColor: Color = MiaoColors.BrandOrange,
)

private data class DemoStoreCard(
    val id: String,
    val name: String,
    val etaBadge: String,
    val rating: String,
    val reviewCount: String,
    val distance: String,
    val shipNote: String,
    val tags: List<StoreTag>,
    val emojiThumb: String,
    val avatarUrl: String = "",
    val isDimmed: Boolean = false,
)

private val DEMO_STORES = listOf(
    DemoStoreCard(
        id = "store_bun_rieu_co_ba",
        name = "Bún Riêu Cô Ba — Nguyễn Thị Thập",
        etaBadge = "15p",
        rating = "4.9",
        reviewCount = "(128)",
        distance = "Quận 7",
        shipNote = "Mã MIAO10",
        tags = listOf(
            StoreTag("Đang mở", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal),
            StoreTag("Quán quen"),
            StoreTag("Giảm 10%"),
        ),
        emojiThumb = "🍜",
    ),
)

/**
 * Trang chủ Chế độ Khách — chuẩn Warm Savory Delivery (Stitch), đã tinh chỉnh:
 * 1. Top Nav dùng để điều hướng nhanh (Logo + Giỏ hàng + Chuông + Chế độ Quán), dời địa chỉ xuống dòng thoáng riêng.
 * 2. Badge sấm sét `⚡15p` nhô ra góc trên trái ngoài khung ảnh giống lưới danh mục.
 * 3. Toàn bộ tag pill `maxLines = 1` trong FlowRow, không bẻ đôi chữ.
 * 4. Thẻ quán gọn gàng, chỉ gắn tag (không lặp thanh món bán chạy).
 * 5. Thanh nổi đơn hàng gọn gàng; bấm "Xem ngay >" mở màn Theo dõi đơn & Tài xế realtime.
 */
@Composable
fun CustomerHomeScreen(
    onSwitchToMerchantPreview: () -> Unit = {},
    onTriggerLoadingPreview: () -> Unit = {},
    onOpenOrderTracking: (String?) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onSelectStore: (String) -> Unit = {},
    initialTab: Int = 0,
    viewModel: CustomerHomeViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Map dữ liệu thật từ Cloud Firestore `miaosong-pro` sang StoreCard
    val storeCards = if (uiState.stores.isNotEmpty()) {
        uiState.stores.map { it.toStoreCard() }
    } else if (uiState.isLoading) {
        DEMO_STORES
    } else if (uiState.allStoresCount > 0 || uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
        emptyList()
    } else {
        DEMO_STORES.filter { demo ->
            val matchQuery = if (uiState.searchQuery.isBlank()) true else {
                demo.name.contains(uiState.searchQuery, ignoreCase = true) || demo.distance.contains(uiState.searchQuery, ignoreCase = true)
            }
            val matchCat = if (uiState.selectedCategory.isNullOrBlank()) true else {
                demo.name.contains(uiState.selectedCategory!!, ignoreCase = true) || demo.tags.any { it.label.contains(uiState.selectedCategory!!, ignoreCase = true) }
            }
            matchQuery && matchCat
        }
    }

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        topBar = {
            StitchTopNavHeader(
                onLogoClick = {
                    selectedTab = 0
                    onTriggerLoadingPreview()
                },
                onOpenCart = onOpenCart,
            )
        },
        bottomBar = {
            Column {
                // Thanh nổi gọn gàng — ẩn khi đang ở tab Đơn hàng (index 1)
                if (selectedTab != 1) {
                    ActiveOrderFloatingPill(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { onOpenOrderTracking(null) },
                    )
                }
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                ) {
                    val navItems = listOf(
                        "Trang chủ" to Icons.Default.Home,
                        "Đơn hàng" to Icons.AutoMirrored.Filled.ReceiptLong,
                        "Tài khoản" to Icons.Default.Person,
                    )
                    navItems.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (index == 2) {
                                    onOpenProfile()
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
        if (selectedTab == 1) {
            CustomerOrdersTab(
                onOpenOrderDetail = { order -> onOpenOrderTracking(order.id) },
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Thẻ mèo MiaoSong chào khách
                item {
                    MiaoGreetingBanner(onClick = {})
                }

                // 2. Thanh chọn địa chỉ giao hàng thoáng rộng (không bị cắt trên Top Bar)
                item {
                    DeliveryAddressRow(
                        address = uiState.selectedAddress,
                        onClick = viewModel::openAddressPicker,
                    )
                }

                // 3. Thanh tìm kiếm thật + Trending tags
                item {
                    SearchAndTrendingSection(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onClearQuery = viewModel::clearSearchQuery,
                        onTagClick = viewModel::onSearchQueryChange,
                    )
                }

                // 4. Lưới danh mục món ăn (2 hàng x 4 cột)
                item {
                    QuickCategoryGrid(
                        selectedCategory = uiState.selectedCategory,
                        onCategoryClick = viewModel::selectCategory,
                    )
                }

                // 5. Banner cam khuyến mãi
                item {
                    PromoBannerCard(onClaimClick = {
                        viewModel.selectCategory("Deal 50%")
                    })
                }

                // 6. Thanh lọc quán
                item {
                    FilterSortPillsRow(
                        activeFilter = uiState.activeFilter,
                        onFilterSelect = viewModel::selectFilter,
                    )
                }

                // 7. Danh sách quán ăn D2C realtime từ Cloud Firestore hoặc Empty State
                if (storeCards.isEmpty() && !uiState.isLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.15f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "🐾", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Không tìm thấy quán phù hợp",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.OnSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) {
                                        "Không có kết quả cho \"${uiState.searchQuery}\". Hãy thử tìm tên món hoặc đổi từ khóa nhé!"
                                    } else {
                                        "Hiện chưa có quán nào trong mục này. Vui lòng thử chọn danh mục khác nhé!"
                                    },
                                    fontSize = 12.sp,
                                    color = MiaoColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        viewModel.clearSearchQuery()
                                        if (uiState.selectedCategory != null) {
                                            viewModel.selectCategory(uiState.selectedCategory!!)
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MiaoColors.BrandOrange,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text(
                                        text = "Xem tất cả quán",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(storeCards, key = { it.id }) { store ->
                        StoreFeedCard(
                            store = store,
                            onClick = { onSelectStore(store.id) },
                        )
                    }
                }
            }
        }
    }

    // Modal chọn & thêm địa chỉ giao hàng (Step 5)
    if (uiState.isAddressPickerVisible) {
        AddressPickerBottomSheet(
            addresses = uiState.addresses,
            selectedAddress = uiState.selectedAddress,
            onSelectAddress = viewModel::selectAddress,
            onAddNewAddress = viewModel::addNewAddress,
            onDismiss = viewModel::closeAddressPicker,
        )
    }
}

private fun Store.toStoreCard(): DemoStoreCard {
    val lower = "$name $slug".lowercase()
    val emoji = when {
        lower.contains("tra-sua") || lower.contains("trà sữa") || lower.contains("phuc-long") -> "🧋"
        lower.contains("ca-phe") || lower.contains("cà phê") || lower.contains("coffee") -> "☕"
        lower.contains("com") || lower.contains("cơm") -> "🍛"
        lower.contains("banh-mi") || lower.contains("bánh mì") -> "🥖"
        lower.contains("ga-ran") || lower.contains("gà") -> "🍗"
        lower.contains("banh-ngot") || lower.contains("che") || lower.contains("chè") || lower.contains("kem") -> "🍰"
        lower.contains("pizza") || lower.contains("mi-y") -> "🍕"
        lower.contains("dimsum") || lower.contains("ha-cao") -> "🥟"
        lower.contains("cha-ca") || lower.contains("chả cá") || lower.contains("lau") -> "🍲"
        else -> "🍜"
    }

    val statusTag = when (schedule.manualStatus) {
        ManualStoreStatus.OPEN -> StoreTag("Đang mở", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        ManualStoreStatus.BUSY -> StoreTag("Tạm bận", Color(0xFFFFF3CD), Color(0xFF785A00))
        ManualStoreStatus.CLOSED -> StoreTag("Đóng cửa", Color(0xFFFFDAD6), Color(0xFF93000A))
    }

    val parts = address.split(",").map { it.trim() }
    val district = parts.firstOrNull { it.startsWith("Quận") || it.startsWith("Q.") || it.contains("Phú Nhuận") || it.contains("Bình Thạnh") || it.contains("Thủ Đức") }
        ?: parts.lastOrNull().orEmpty().ifEmpty { "TP.HCM" }

    val eta = "${12 + (id.hashCode().let { kotlin.math.abs(it) } % 11)}p"

    return DemoStoreCard(
        id = id,
        name = name,
        etaBadge = eta,
        rating = "%.1f".format(ratingAvg),
        reviewCount = "($ratingCount)",
        distance = district,
        shipNote = if (deliveryProvider.name == "LALAMOVE") "Giao Lalamove" else "Freeship 0Đ",
        tags = listOf(
            statusTag,
            StoreTag(if (ratingAvg >= 4.8) "Quán quen" else "Gợi ý"),
            StoreTag(if (slug.contains("rieu")) "Giảm 10%" else "Ưu đãi"),
        ),
        emojiThumb = emoji,
        avatarUrl = avatarUrl,
        isDimmed = schedule.manualStatus != ManualStoreStatus.OPEN,
    )
}

@Composable
private fun StitchTopNavHeader(
    onLogoClick: () -> Unit = {},
    onOpenCart: () -> Unit = {},
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
            // Bên trái: Logo mèo tĩnh + Brand Name (chạm để loading về trang chủ)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onLogoClick() },
            ) {
                MiaoAnimatedCatLogo(
                    size = 40.dp,
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

            // Bên phải: Các nút điều hướng nhanh (Giỏ hàng · Thông báo)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Nút Giỏ hàng
                Surface(
                    color = MiaoColors.SurfaceContainerLow,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onOpenCart() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Giỏ hàng",
                            tint = MiaoColors.OnSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                // Nút Thông báo
                Surface(
                    color = MiaoColors.SurfaceContainerLow,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = MiaoColors.OnSurface,
                            modifier = Modifier.size(19.dp),
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
}

@Composable
private fun DeliveryAddressRow(
    address: DeliveryAddress,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MiaoColors.BrandOrange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Giao đến: ",
                    color = MiaoColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = address.fullText,
                    color = MiaoColors.OnSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Đổi địa chỉ",
                tint = MiaoColors.BrandOrange,
                modifier = Modifier.size(18.dp),
            )
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
private fun SearchAndTrendingSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(
                1.dp,
                if (searchQuery.isNotBlank()) MiaoColors.BrandOrange else MiaoColors.BrandOrange.copy(alpha = 0.18f),
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (searchQuery.isNotBlank()) MiaoColors.BrandOrange else MiaoColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Tìm bún riêu, bún chả cá, cơm tấm, trà sữa...",
                            color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MiaoColors.OnSurface,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(MiaoColors.BrandOrange),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa tìm kiếm",
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClearQuery() },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val isRieuActive = searchQuery.contains("riêu", ignoreCase = true)
            Surface(
                color = if (isRieuActive) MiaoColors.BrandOrange else Color(0xFFFFEDD5),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.35f)),
                modifier = Modifier.clickable {
                    if (isRieuActive) onClearQuery() else onTagClick("Bún riêu")
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isRieuActive) Color.White else MiaoColors.BrandOrange,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Bún riêu giảm 20%",
                        color = if (isRieuActive) Color.White else MiaoColors.BrandOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            listOf(
                "Bún chả cá" to "Bún chả cá Nha Trang",
                "Cơm tấm" to "Cơm tấm sườn bì",
                "Trà sữa" to "Trà sữa 15p",
            ).forEach { (keyword, displayTag) ->
                val isTagActive = searchQuery.contains(keyword, ignoreCase = true)
                Surface(
                    color = if (isTagActive) MiaoColors.SurfaceContainerHigh else Color.White,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isTagActive) MiaoColors.BrandOrange else MiaoColors.OutlineVariant,
                    ),
                    modifier = Modifier.clickable {
                        if (isTagActive) onClearQuery() else onTagClick(keyword)
                    },
                ) {
                    Text(
                        text = displayTag,
                        color = if (isTagActive) MiaoColors.BrandOrange else MiaoColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.SemiBold,
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
private fun QuickCategoryGrid(
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit,
) {
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
                    val isSelected = selectedCategory == item.title
                    val accentColor = if (item.isTealBadge) MiaoColors.SecondaryTealBright else MiaoColors.BrandOrange
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { onCategoryClick(item.title) },
                    ) {
                        Box(
                            modifier = Modifier.padding(top = 6.dp, end = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                color = if (isSelected) Color(0xFFFFEADF) else Color.White,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MiaoColors.BrandOrange else accentColor.copy(alpha = 0.18f),
                                ),
                                shadowElevation = if (isSelected) 3.dp else 1.dp,
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
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = item.title,
                            color = if (isSelected) MiaoColors.BrandOrange else MiaoColors.OnSurface,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
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
private fun FilterSortPillsRow(
    activeFilter: StoreSortFilter,
    onFilterSelect: (StoreSortFilter) -> Unit,
) {
    var showSortDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val isNearby = activeFilter == StoreSortFilter.NEARBY
        val dropdownLabel = when (activeFilter) {
            StoreSortFilter.NEARBY -> "Gần tôi nhất"
            StoreSortFilter.FASTEST -> "Giao nhanh"
            StoreSortFilter.TOP_RATED -> "Đánh giá 4.8★+"
        }

        Box {
            Surface(
                color = if (isNearby) MiaoColors.OnSurface else Color.White,
                shape = RoundedCornerShape(999.dp),
                border = if (isNearby) null else BorderStroke(1.dp, MiaoColors.OutlineVariant),
                modifier = Modifier.clickable { showSortDropdown = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dropdownLabel,
                        color = if (isNearby) MiaoColors.SurfaceCanvas else MiaoColors.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Chọn kiểu sắp xếp",
                        tint = if (isNearby) MiaoColors.SurfaceCanvas else MiaoColors.OnSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = showSortDropdown,
                onDismissRequest = { showSortDropdown = false },
                containerColor = Color.White,
            ) {
                DropdownMenuItem(
                    text = { Text("Gần tôi nhất", fontWeight = if (activeFilter == StoreSortFilter.NEARBY) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        showSortDropdown = false
                        onFilterSelect(StoreSortFilter.NEARBY)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Giao nhanh ⚡", fontWeight = if (activeFilter == StoreSortFilter.FASTEST) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        showSortDropdown = false
                        onFilterSelect(StoreSortFilter.FASTEST)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Đánh giá 4.8★+", fontWeight = if (activeFilter == StoreSortFilter.TOP_RATED) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        showSortDropdown = false
                        onFilterSelect(StoreSortFilter.TOP_RATED)
                    },
                )
            }
        }

        val isFastest = activeFilter == StoreSortFilter.FASTEST
        Surface(
            color = if (isFastest) MiaoColors.SecondaryTeal else Color(0xFFE6FAF8),
            shape = RoundedCornerShape(999.dp),
            border = if (isFastest) null else BorderStroke(1.dp, MiaoColors.SecondaryTealBright.copy(alpha = 0.35f)),
            modifier = Modifier.clickable { onFilterSelect(StoreSortFilter.FASTEST) },
        ) {
            Text(
                text = "Giao nhanh ⚡",
                color = if (isFastest) Color.White else MiaoColors.SecondaryTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }

        val isTopRated = activeFilter == StoreSortFilter.TOP_RATED
        Surface(
            color = if (isTopRated) MiaoColors.BrandOrange else Color.White,
            shape = RoundedCornerShape(999.dp),
            border = if (isTopRated) null else BorderStroke(1.dp, MiaoColors.OutlineVariant),
            modifier = Modifier.clickable { onFilterSelect(StoreSortFilter.TOP_RATED) },
        ) {
            Text(
                text = "Đánh giá 4.8★+",
                color = if (isTopRated) Color.White else MiaoColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = if (isTopRated) FontWeight.Bold else FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun StoreFeedCard(
    store: DemoStoreCard,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (store.isDimmed) 0.72f else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Khung ảnh đại diện với badge `⚡15p` nhô ra góc trên trái ngoài khung
            Box(
                modifier = Modifier.padding(top = 6.dp, start = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MiaoColors.SurfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (store.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = store.avatarUrl,
                                contentDescription = store.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(text = store.emojiThumb, fontSize = 36.sp)
                        }
                    }
                }
                Surface(
                    color = MiaoColors.SecondaryTealBright,
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-7).dp, y = (-7).dp),
                ) {
                    Text(
                        text = "⚡${store.etaBadge}",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
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
                        maxLines = 1,
                    )
                    Text(
                        text = store.shipNote,
                        color = MiaoColors.SecondaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Luôn giữ đúng 1 hàng ngang duy nhất (cuộn ngang nếu nhiều tag), không bao giờ rớt dòng
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    store.tags.forEach { tag ->
                        Surface(
                            color = tag.bgColor,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, tag.textColor.copy(alpha = 0.22f)),
                        ) {
                            Text(
                                text = tag.label,
                                color = tag.textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thanh nổi gọn gàng: chỉ báo trạng thái đơn đang giao + nút "Xem ngay >".
 * Chi tiết vị trí realtime & thông tin tài xế (Tên, SĐT, Biển số) hiển thị trong OrderTrackingScreen.
 */
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Đơn hàng đang giao đến bạn",
                        color = MiaoColors.OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        color = Color(0xFFFFE4CC),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "● Đang đến",
                            color = MiaoColors.BrandOrangeDark,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Xem ngay >",
                color = MiaoColors.BrandOrange,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
