package vn.miaosong.feature.customer.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.Store

private const val DEFAULT_HERO_IMAGE =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuA8HcjR4YESHf09oz8JajH3H0Q4PT5dHYWuHyj-vq6lt4IltzZCt8rjKPCPf47LUM5XUZifmL6VSFrHuBAOAAPjsNhKu-pho2vJEfER8lp3NV5nOO9SyGwVpZ4P9WKogjifPA3Ict7wmkildcHEVzeuSRvMdqzGYBnoXwxcUwUyaD2anJdwBEbKWWYw73cliKrZOZ-fd5da9PinCvDK0Nqu8EPM3UhdAnz1P9ZGd9A"

private const val DEFAULT_STORE_AVATAR =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDLEGTUDj1U_Yg11MfTv01vY_3AF_TF1ilrOTa0QYJDXkEjB2s9pLTEReU_XCfyVoTyXJo0-kGTiFUK-NolEEMFXJUUkBnNP63g97dPZEVofOtqWwywcKNYST6_QkTJ9nZoAxUuCD7YrBZLfgU4NZVVeXQnRpx6EdyIbVSI4sxXHPTmWpJlM_5zc3rC4k10r3U8w22Gw-jC7ahY7tLQ_DZMT3xl8an6mM7CBAleZ5Q"

/**
 * P2-02: Màn hình Chi tiết Quán & Thực đơn theo nhóm
 * Chuẩn Warm Savory Delivery (Stitch):
 * - Top Sub-header mờ có Logo Mèo + badge "Quán Tuyển Chọn" + actions (Search, Favorite, Share)
 * - Hero banner cao 208dp + nhãn "Đối tác MiaoSong"
 * - Master profile card gối lên banner (-48dp) + chấm pulse xanh "Đang mở cửa"
 * - Special Voucher Banner Card ("COBA25")
 * - Sticky category navigation rail (Bán chạy, Món chính, Món thêm, Thức uống)
 * - Danh sách món ăn realtime với giá, badges, ảnh món
 * - Persistent Floating Bottom Bar giỏ hàng
 */
@Composable
fun StoreDetailScreen(
    storeId: String,
    onBack: () -> Unit,
    onOpenCart: () -> Unit = {},
    onSelectItem: (MenuItem) -> Unit = {},
    viewModel: StoreDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(storeId) {
        viewModel.initStoreId(storeId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var itemToCustomize by remember { mutableStateOf<MenuItem?>(null) }

    // Tự động tắt Toast thông báo sau 2.5s
    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2500)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        topBar = {
            StoreDetailTopHeader(
                isFavorited = uiState.isFavorited,
                isSearchVisible = uiState.isSearchVisible,
                searchQuery = uiState.searchQuery,
                onBack = onBack,
                onToggleSearch = viewModel::toggleSearchVisibility,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onToggleFavorite = viewModel::toggleFavorite,
                onShare = {
                    val slug = uiState.store?.slug?.takeIf { it.isNotBlank() } ?: storeId
                    val shareUrl = "https://miaosong-pro.web.app/s/$slug"
                    clipboardManager.setText(AnnotatedString(shareUrl))
                    viewModel.showToast("Đã sao chép link quán: $shareUrl")
                },
            )
        },
        bottomBar = {
            // Thanh nổi giỏ hàng cố định đáy khi có món
            if (uiState.cartItemCount > 0) {
                FloatingCartBar(
                    itemCount = uiState.cartItemCount,
                    totalVnd = uiState.cartTotalVnd,
                    previewSummary = uiState.cartSummaryPreview,
                    onOpenCart = onOpenCart,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 90.dp else 24.dp),
            ) {
                // 1. Hero banner ảnh quán
                item {
                    StoreHeroBanner(
                        bannerUrl = uiState.store?.avatarUrl?.ifBlank { DEFAULT_HERO_IMAGE } ?: DEFAULT_HERO_IMAGE,
                    )
                }

                // 2. Master Profile Card gối lên banner (-48dp)
                item {
                    StoreProfileMasterCard(
                        store = uiState.store,
                        modifier = Modifier.offset(y = (-48).dp),
                    )
                }

                // 3. Special Voucher Banner Card
                item {
                    Box(modifier = Modifier.offset(y = (-36).dp)) {
                        StoreVoucherBannerCard(
                            isClaimed = uiState.isVoucherClaimed,
                            onClaim = { viewModel.claimVoucher("COBA25") },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }

                // 4. Sticky Category Navigation Rail
                item {
                    Box(modifier = Modifier.offset(y = (-24).dp)) {
                        CategoryNavigationRail(
                            selectedPill = uiState.selectedCategoryPill,
                            onSelectPill = viewModel::selectCategoryPill,
                        )
                    }
                }

                // 5. Header tiêu đề danh sách món
                item {
                    Box(modifier = Modifier.offset(y = (-16).dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Món Ngon Nổi Bật",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.OnSurface,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MiaoColors.BrandOrange),
                                )
                            }
                            Text(
                                text = "${uiState.filteredMenuItems.size} món chọn nhanh",
                                fontSize = 12.sp,
                                color = MiaoColors.OnSurfaceVariant,
                            )
                        }
                    }
                }

                // 6. Danh sách các món ăn
                if (uiState.filteredMenuItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MiaoColors.OutlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "🥣", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Chưa có món phù hợp với mục này",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MiaoColors.OnSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hãy chọn mục khác hoặc đổi từ khóa tìm kiếm nhé!",
                                    fontSize = 12.sp,
                                    color = MiaoColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredMenuItems, key = { it.id }) { menuItem ->
                        Box(
                            modifier = Modifier
                                .offset(y = (-12).dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            MenuItemCard(
                                item = menuItem,
                                onClick = {
                                    itemToCustomize = menuItem
                                    onSelectItem(menuItem)
                                },
                                onQuickAdd = {
                                    itemToCustomize = menuItem
                                },
                            )
                        }
                    }
                }
            }

            // Bottom Sheet tùy chỉnh món ăn (P2-03)
            itemToCustomize?.let { currentItem ->
                ItemCustomizerBottomSheet(
                    item = currentItem,
                    storeId = storeId,
                    onDismiss = { itemToCustomize = null },
                    onAddToCart = { cartLine ->
                        viewModel.addToCart(cartLine)
                        itemToCustomize = null
                    },
                )
            }

            // Toast notification dạng viên thuốc nổi ở trên
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            ) {
                Surface(
                    color = MiaoColors.OnSurface,
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MiaoColors.SecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = uiState.toastMessage.orEmpty(),
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreDetailTopHeader(
    isFavorited: Boolean,
    isSearchVisible: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Bên trái: Nút Back + Mascot MiaoSong + Badge "Quán Tuyển Chọn"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MiaoColors.OnSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    MiaoAnimatedCatLogo(
                        size = 32.dp,
                        animated = false,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                    )

                    Surface(
                        color = Color(0xFFFFEADF),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "Quán Tuyển Chọn",
                            color = MiaoColors.BrandOrange,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }

                // Bên phải: Nút Tìm kiếm món · Yêu thích · Chia sẻ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = MiaoColors.SurfaceContainerLow,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onToggleSearch() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm món",
                                tint = if (isSearchVisible) MiaoColors.BrandOrange else MiaoColors.OnSurface,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    Surface(
                        color = MiaoColors.SurfaceContainerLow,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onToggleFavorite() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (isFavorited) Color(0xFFE53935) else MiaoColors.BrandOrange,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    Surface(
                        color = MiaoColors.SurfaceContainerLow,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onShare() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Chia sẻ",
                                tint = MiaoColors.OnSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // Thanh tìm kiếm mở rộng khi bấm nút Search
            AnimatedVisibility(visible = isSearchVisible) {
                Surface(
                    color = MiaoColors.SurfaceContainerLow,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm món ăn trong quán...",
                                    fontSize = 12.5.sp,
                                    color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MiaoColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(MiaoColors.BrandOrange),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa",
                                tint = MiaoColors.OnSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSearchQueryChange("") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreHeroBanner(
    bannerUrl: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
            .background(MiaoColors.SurfaceContainerHighest),
    ) {
        AsyncImage(
            model = bannerUrl,
            contentDescription = "Ảnh đại diện quán",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient đen mờ từ dưới lên để tạo chiều sâu thị giác
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x55000000),
                            Color(0xCC27180F),
                        ),
                    ),
                ),
        )

        // Badge góc trên phải: Đối tác MiaoSong
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MiaoColors.SecondaryTeal,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Đối tác MiaoSong",
                    color = MiaoColors.SecondaryTeal,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StoreProfileMasterCard(
    store: Store?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Avatar quán bo góc 14dp + chấm xanh online
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MiaoColors.SurfaceContainerHigh,
                        modifier = Modifier.size(62.dp),
                    ) {
                        AsyncImage(
                            model = store?.avatarUrl?.ifBlank { DEFAULT_STORE_AVATAR } ?: DEFAULT_STORE_AVATAR,
                            contentDescription = store?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(MiaoColors.SecondaryTeal)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = store?.name ?: "Bún Bò Huế & Cơm Tấm Cô Ba",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MiaoColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = store?.address?.ifBlank { "Hương vị truyền thống 25 năm • Nước dùng hầm xương bò 12 tiếng" }
                            ?: "Hương vị truyền thống 25 năm • Nước dùng hầm xương bò 12 tiếng",
                        fontSize = 12.sp,
                        color = MiaoColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Hàng tags rating & logistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Đánh giá
                Surface(
                    color = MiaoColors.SurfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MiaoColors.TertiaryAmber,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if ((store?.ratingAvg ?: 0.0) > 0) "%.1f".format(store?.ratingAvg) else "4.9",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MiaoColors.OnSurface,
                        )
                        Text(
                            text = if ((store?.ratingCount ?: 0) > 0) "(${store?.ratingCount})" else "(1.2k+)",
                            fontSize = 11.sp,
                            color = MiaoColors.OnSurfaceVariant,
                        )
                    }
                }

                // ETA & Khoảng cách
                Surface(
                    color = Color(0xFFE6FAF8),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MiaoColors.SecondaryTeal,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "15 phút • 850m",
                            color = MiaoColors.SecondaryTeal,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Freeship / Giao hàng
                Surface(
                    color = Color(0xFFFFF2E6),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Freeship đơn 50k",
                            color = MiaoColors.BrandOrange,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Trạng thái mở cửa
            val isOpen = store?.schedule?.manualStatus != ManualStoreStatus.CLOSED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOpen) MiaoColors.SecondaryTeal else MiaoColors.Error),
                )
                Text(
                    text = if (isOpen) "Đang mở cửa (06:00 - 22:00)" else "Tạm ngưng phục vụ",
                    color = if (isOpen) MiaoColors.SecondaryTeal else MiaoColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StoreVoucherBannerCard(
    isClaimed: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFDBCD),
                            Color(0xFFFFE3D4),
                            Color(0xFFFFDBCD),
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GIẢM 25.000đ",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MiaoColors.BrandOrangeDark,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MiaoColors.BrandOrange,
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = "COBA25",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Text(
                            text = "Áp dụng cho đơn từ 60k • HSD hôm nay",
                            fontSize = 11.5.sp,
                            color = MiaoColors.OnSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onClaim,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isClaimed) MiaoColors.SecondaryTeal else MiaoColors.BrandOrange,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    if (isClaimed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Đã lưu", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "Lưu mã", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryNavigationRail(
    selectedPill: String,
    onSelectPill: (String) -> Unit,
) {
    val pills = listOf(
        "bestseller" to "🔥 Bán chạy nhất",
        "main" to "🍲 Món chính",
        "side" to "🥢 Món thêm & Topping",
        "drinks" to "🥤 Thức uống",
    )

    Surface(
        color = MiaoColors.SurfaceCanvas,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pills.forEach { (id, label) ->
                val isSelected = selectedPill == id
                Surface(
                    color = if (isSelected) MiaoColors.BrandOrange else MiaoColors.SurfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, MiaoColors.OutlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier.clickable { onSelectPill(id) },
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MiaoColors.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cột thông tin món ăn bên trái
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.name.contains("Đặc Biệt", ignoreCase = true) || item.name.contains("Cô Ba", ignoreCase = true)) {
                        Surface(
                            color = Color(0xFFFFF2E6),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Best Seller",
                                color = MiaoColors.BrandOrange,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (item.name.contains("Cơm", ignoreCase = true) || item.name.contains("Sườn", ignoreCase = true)) {
                        Surface(
                            color = Color(0xFFE6FAF8),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Yêu thích",
                                color = MiaoColors.SecondaryTeal,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (!item.isAvailable) {
                        Surface(
                            color = Color(0xFFFFDAD6),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Tạm hết",
                                color = MiaoColors.Error,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Tên món
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiaoColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Mô tả món
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = MiaoColors.OnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Hàng giá & nút Thêm/Tùy chọn
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = formatVnd(item.basePrice),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MiaoColors.BrandOrange,
                        )
                        if (item.name.contains("Đặc Biệt")) {
                            Text(
                                text = formatVnd(item.basePrice + 10_000L),
                                fontSize = 11.5.sp,
                                color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }

                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.clickable { onQuickAdd() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (item.optionGroups.isNotEmpty()) "Tùy chọn" else "Thêm",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Ảnh món bên phải
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiaoColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(text = "🍜", fontSize = 34.sp)
                }

                if (item.name.contains("Đặc Biệt")) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                    ) {
                        Text(
                            text = "-15%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingCartBar(
    itemCount: Int,
    totalVnd: Long,
    previewSummary: String,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MiaoColors.OnSurface,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Bên trái: Icon giỏ hàng + badge số lượng + tiền + tên món
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Giỏ hàng",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Badge số món
                    Surface(
                        color = MiaoColors.SecondaryContainer,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, MiaoColors.OnSurface),
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(19.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$itemCount",
                                color = MiaoColors.OnSecondaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = formatVnd(totalVnd),
                            color = Color(0xFFFFB597),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Đã giảm 25k",
                            color = MiaoColors.SecondaryContainer,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = previewSummary.ifBlank { "Xem đơn hàng của bạn" },
                        color = Color(0xFFF1D4C5),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bên phải: Nút Xem giỏ hàng
            Button(
                onClick = onOpenCart,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiaoColors.BrandOrange,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Xem ngay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun formatVnd(amount: Long): String {
    return "%,dđ".format(amount).replace(',', '.')
}
