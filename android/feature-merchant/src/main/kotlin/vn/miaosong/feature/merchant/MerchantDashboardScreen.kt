package vn.miaosong.feature.merchant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import vn.miaosong.core.common.MoneyFormatter
import vn.miaosong.core.designsystem.components.KitchenNoteBadge
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.components.MiaoPrimaryButton
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.MenuOption
import vn.miaosong.core.model.OptionGroup
import vn.miaosong.core.model.OptionGroupType
import vn.miaosong.core.model.UserRole
import vn.miaosong.core.model.Voucher
import vn.miaosong.core.model.VoucherType

/**
 * Chế độ Quán (MERCHANT) — áp dụng bộ giao diện Stitch Warm Savory Delivery.
 * Đáp ứng đầy đủ Mục 1 (Quản lý Thực đơn, Nhóm món, Option SINGLE/MULTI, Bật/Tắt Hết hàng, Quản lý Voucher)
 * + Mục 3 (Màn hình Bếp KDS, Đổi trạng thái quán) + Upload Avatar quán lên Firebase Storage.
 */
@Composable
fun MerchantDashboardScreen(
    onSwitchToCustomerPreview: () -> Unit,
    viewModel: MerchantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddVoucherDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val isMerchantAuthenticated = uiState.currentUser?.role == UserRole.MERCHANT

    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MerchantTopHeader(
                storeName = uiState.store?.name ?: "Bún Riêu Cô Ba",
                status = uiState.store?.schedule?.manualStatus ?: ManualStoreStatus.OPEN,
                isSignedIn = isMerchantAuthenticated,
                onSignInClick = { showLoginDialog = true },
                onSwitchToCustomer = onSwitchToCustomerPreview,
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = MiaoColors.BrandOrange,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm món")
                }
                1 -> FloatingActionButton(
                    onClick = { showAddVoucherDialog = true },
                    containerColor = MiaoColors.BrandOrange,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tạo voucher")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val tabs = listOf(
                    "Thực đơn" to Icons.Default.RestaurantMenu,
                    "Voucher" to Icons.Default.ConfirmationNumber,
                    "Bếp (KDS)" to Icons.Default.SoupKitchen,
                    "Quán" to Icons.Default.Storefront,
                )
                tabs.forEachIndexed { idx, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
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
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!isMerchantAuthenticated) {
                MerchantAuthBanner(onClick = { showLoginDialog = true })
            }

            when (selectedTab) {
                0 -> MerchantMenuTab(
                    categories = uiState.categories,
                    items = uiState.menuItems,
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onToggleAvailability = viewModel::toggleItemAvailability,
                    onDeleteItem = viewModel::deleteMenuItem,
                )
                1 -> MerchantVoucherTab(
                    vouchers = uiState.vouchers,
                    onToggleActive = viewModel::toggleVoucherActive,
                    onDelete = viewModel::deleteVoucher,
                )
                2 -> MerchantKitchenTab()
                3 -> MerchantStoreProfileTab(
                    uiState = uiState,
                    onUpdateStatus = viewModel::updateStoreStatus,
                    onAvatarPicked = viewModel::uploadStoreAvatar,
                    onSignInClick = { showLoginDialog = true },
                    onSwitchToCustomer = onSwitchToCustomerPreview,
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
            },
        )
    }

    if (showAddItemDialog) {
        AddMenuItemDialog(
            categories = uiState.categories,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { item ->
                viewModel.saveMenuItem(item)
                showAddItemDialog = false
            },
        )
    }

    if (showAddVoucherDialog) {
        AddVoucherDialog(
            onDismiss = { showAddVoucherDialog = false },
            onConfirm = { voucher ->
                viewModel.saveVoucher(voucher)
                showAddVoucherDialog = false
            },
        )
    }

    if (showLoginDialog) {
        MerchantLoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { email, pwd ->
                viewModel.signInMerchant(email, pwd)
                showLoginDialog = false
            },
        )
    }
}

@Composable
private fun MerchantTopHeader(
    storeName: String,
    status: ManualStoreStatus,
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    onSwitchToCustomer: () -> Unit,
) {
    val (statusLabel, statusBg, statusFg) = when (status) {
        ManualStoreStatus.OPEN -> Triple("Đang mở", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        ManualStoreStatus.BUSY -> Triple("Tạm bận", Color(0xFFFFF3CD), Color(0xFF785A00))
        ManualStoreStatus.CLOSED -> Triple("Đóng cửa", Color(0xFFFFDAD6), Color(0xFF93000A))
    }

    Surface(color = Color.White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                MiaoAnimatedCatLogo(
                    size = 38.dp,
                    animated = false,
                    modifier = Modifier.clip(RoundedCornerShape(11.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = storeName,
                        color = MiaoColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(color = statusBg, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                text = "● $statusLabel",
                                color = statusFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                            )
                        }
                        if (!isSignedIn) {
                            Text(
                                text = "Chưa đăng nhập",
                                color = MiaoColors.Error,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onSignInClick() },
                            )
                        }
                    }
                }
            }

            Surface(
                color = MiaoColors.SurfaceContainerLow,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.25f)),
                modifier = Modifier.clickable { onSwitchToCustomer() },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Chế độ Khách",
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Khách",
                        color = MiaoColors.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantAuthBanner(onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF3CD),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Text(
            text = "🔑 Chạm vào đây để đăng nhập tài khoản Quán (tt3145539@gmail.com) để lưu Thực đơn, Voucher & tải Avatar lên Firebase!",
            color = MiaoColors.OnSurface,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MerchantMenuTab(
    categories: List<MenuCategory>,
    items: List<MenuItem>,
    onAddCategoryClick: () -> Unit,
    onToggleAvailability: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val filteredItems = if (selectedCategoryId == null) {
        items
    } else {
        items.filter { it.categoryId == selectedCategoryId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nhóm thực đơn (${categories.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
                TextButton(onClick = onAddCategoryClick) {
                    Text("+ Thêm nhóm", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("Tất cả (${items.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiaoColors.BrandOrange,
                        selectedLabelColor = Color.White,
                    ),
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiaoColors.BrandOrange,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = MiaoColors.OnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = MoneyFormatter.formatVnd(item.basePrice),
                                color = MiaoColors.BrandOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (item.isAvailable) "Còn hàng" else "Hết hàng",
                                color = if (item.isAvailable) MiaoColors.SecondaryTeal else MiaoColors.Error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = item.isAvailable,
                                onCheckedChange = { onToggleAvailability(item.id, it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = MiaoColors.SecondaryTealBright),
                            )
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa món",
                                    tint = MiaoColors.OnSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            color = MiaoColors.OnSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }

                    // Hiển thị tóm tắt Option Groups (SINGLE bắt buộc / MULTI tùy chọn)
                    if (item.optionGroups.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            item.optionGroups.forEach { grp ->
                                val badge = if (grp.type == OptionGroupType.SINGLE) {
                                    "Chọn 1 (Bắt buộc): ${grp.options.size} cỡ"
                                } else {
                                    "Chọn nhiều: ${grp.options.size} topping"
                                }
                                Surface(
                                    color = MiaoColors.SurfaceContainerLow,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = badge,
                                        color = MiaoColors.BrandOrangeDark,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantVoucherTab(
    vouchers: List<Voucher>,
    onToggleActive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Mã khuyến mãi của quán (${vouchers.size})",
                style = MaterialTheme.typography.headlineSmall,
                color = MiaoColors.OnSurface,
            )
            Text(
                text = "Voucher chỉ giảm trên tiền món & món thêm, không trừ vào phí giao hàng.",
                color = MiaoColors.OnSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        items(vouchers, key = { it.id }) { v ->
            val benefit = if (v.type == VoucherType.PERCENT) {
                val cap = v.maxDiscount?.let { " (tối đa ${MoneyFormatter.formatVnd(it)})" }.orEmpty()
                "Giảm ${v.value}%$cap"
            } else {
                "Giảm ${MoneyFormatter.formatVnd(v.value)}"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.18f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(color = MiaoColors.BrandOrange, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = v.code,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Text(
                                text = benefit,
                                color = MiaoColors.OnSurface,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "Đơn tối thiểu ${MoneyFormatter.formatVnd(v.minOrder)} · Đã dùng ${v.usedCount}/${v.usageLimit}",
                            color = MiaoColors.OnSurfaceVariant,
                            fontSize = 11.5.sp,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = v.isActive,
                            onCheckedChange = { onToggleActive(v.id, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MiaoColors.SecondaryTealBright),
                        )
                        IconButton(onClick = { onDelete(v.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = MiaoColors.OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantKitchenTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Màn hình Bếp (KDS)",
            style = MaterialTheme.typography.headlineSmall,
            color = MiaoColors.OnSurface,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ticket #MS-261121-0012 · Đang chuẩn bị",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
                Text(
                    text = "1x Bún Riêu Cua Đặc Biệt (Tô lớn · Thêm chả cá)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MiaoColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
                KitchenNoteBadge(note = "Không hành, để mắm tôm riêng", isAllergy = false)
                KitchenNoteBadge(note = "Dị ứng đậu phộng — tuyệt đối không rắc", isAllergy = true)
            }
        }
    }
}

@Composable
private fun MerchantStoreProfileTab(
    uiState: MerchantUiState,
    onUpdateStatus: (ManualStoreStatus, Int?) -> Unit,
    onAvatarPicked: (ByteArray) -> Unit,
    onSignInClick: () -> Unit,
    onSwitchToCustomer: () -> Unit,
) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                onAvatarPicked(bytes)
            }
        }
    }

    val store = uiState.store
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Thẻ Avatar Quán + Upload lên Firebase Storage
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiaoColors.SurfaceContainerLow)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!store?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = store?.avatarUrl,
                            contentDescription = "Avatar quán",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(text = "🍜", fontSize = 36.sp)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Đổi ảnh quán",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = store?.name ?: "Bún Riêu Cô Ba",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Text(
                        text = store?.address ?: "420 Nguyễn Thị Thập, Q.7",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "Chạm vào ảnh để tải Avatar quán lên Firebase Storage",
                        color = MiaoColors.BrandOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // 2. Đổi trạng thái hoạt động 1 chạm (Mục 3 đề bài)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trạng thái hoạt động (1 chạm)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = store?.schedule?.manualStatus == ManualStoreStatus.OPEN,
                        onClick = { onUpdateStatus(ManualStoreStatus.OPEN, null) },
                        label = { Text("🟢 Đang mở") },
                    )
                    FilterChip(
                        selected = store?.schedule?.manualStatus == ManualStoreStatus.BUSY,
                        onClick = { onUpdateStatus(ManualStoreStatus.BUSY, 15) },
                        label = { Text("🟡 Bận 15p") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onUpdateStatus(ManualStoreStatus.BUSY, 30) },
                        label = { Text("🟡 Bận 30p") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onUpdateStatus(ManualStoreStatus.BUSY, 60) },
                        label = { Text("🟡 Bận 1h") },
                    )
                    FilterChip(
                        selected = store?.schedule?.manualStatus == ManualStoreStatus.CLOSED,
                        onClick = { onUpdateStatus(ManualStoreStatus.CLOSED, null) },
                        label = { Text("🔴 Đóng cửa") },
                    )
                }
            }
        }

        if (uiState.currentUser?.role != UserRole.MERCHANT) {
            MiaoPrimaryButton(
                text = "Đăng nhập tài khoản Quán (tt3145539@gmail.com)",
                onClick = onSignInClick,
            )
        }

        MiaoPrimaryButton(
            text = "Quay lại Chế độ Khách (CUSTOMER)",
            onClick = onSwitchToCustomer,
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm nhóm thực đơn") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên nhóm (VD: Món chính, Thức uống)") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text("Lưu", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AddMenuItemDialog(
    categories: List<MenuCategory>,
    onDismiss: () -> Unit,
    onConfirm: (MenuItem) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("45000") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "cat_main") }
    var includeDefaultOptions by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm món mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên món ăn") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter(Char::isDigit) },
                    label = { Text("Giá cơ bản (VNĐ)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    maxLines = 2,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Gắn sẵn Size (SINGLE) & Topping (MULTI)", fontSize = 12.sp)
                    Switch(checked = includeDefaultOptions, onCheckedChange = { includeDefaultOptions = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (priceStr.toLongOrNull() ?: 0L) > 0L,
                onClick = {
                    val groups = if (includeDefaultOptions) {
                        listOf(
                            OptionGroup(
                                id = "grp_size",
                                name = "Chọn kích cỡ",
                                type = OptionGroupType.SINGLE,
                                required = true,
                                min = 1,
                                max = 1,
                                options = listOf(
                                    MenuOption("opt_size_kid", "Tô trẻ em", 0L, true),
                                    MenuOption("opt_size_regular", "Tô thường", 0L, true),
                                    MenuOption("opt_size_large", "Tô lớn", 10_000L, true),
                                ),
                            ),
                            OptionGroup(
                                id = "grp_topping",
                                name = "Thêm món ăn kèm",
                                type = OptionGroupType.MULTI,
                                required = false,
                                min = 0,
                                max = 4,
                                options = listOf(
                                    MenuOption("opt_top_cha", "Thêm chả", 8_000L, true),
                                    MenuOption("opt_top_gio", "Thêm giò", 15_000L, true),
                                    MenuOption("opt_top_mam_tom", "Mắm tôm riêng", 0L, true),
                                ),
                            ),
                        )
                    } else {
                        emptyList()
                    }
                    onConfirm(
                        MenuItem(
                            id = "",
                            categoryId = selectedCatId,
                            name = name,
                            description = description,
                            basePrice = priceStr.toLongOrNull() ?: 45_000L,
                            isAvailable = true,
                            optionGroups = groups,
                        ),
                    )
                },
            ) { Text("Lưu món", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AddVoucherDialog(
    onDismiss: () -> Unit,
    onConfirm: (Voucher) -> Unit,
) {
    var code by remember { mutableStateOf("GIAM15") }
    var isPercent by remember { mutableStateOf(true) }
    var valueStr by remember { mutableStateOf("15") }
    var maxDiscountStr by remember { mutableStateOf("25000") }
    var minOrderStr by remember { mutableStateOf("100000") }
    var usageLimitStr by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo mã khuyến mãi (Voucher)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Mã voucher (VD: MIAO15)") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPercent,
                        onClick = { isPercent = true },
                        label = { Text("Theo phần trăm (%)") },
                    )
                    FilterChip(
                        selected = !isPercent,
                        onClick = { isPercent = false },
                        label = { Text("Tiền cố định (đ)") },
                    )
                }
                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it.filter(Char::isDigit) },
                    label = { Text(if (isPercent) "Phần trăm giảm (%)" else "Số tiền giảm (VNĐ)") },
                    singleLine = true,
                )
                if (isPercent) {
                    OutlinedTextField(
                        value = maxDiscountStr,
                        onValueChange = { maxDiscountStr = it.filter(Char::isDigit) },
                        label = { Text("Giảm tối đa (VNĐ)") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = minOrderStr,
                    onValueChange = { minOrderStr = it.filter(Char::isDigit) },
                    label = { Text("Đơn tối thiểu (VNĐ)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = usageLimitStr,
                    onValueChange = { usageLimitStr = it.filter(Char::isDigit) },
                    label = { Text("Tổng số lượng mã tối đa") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && (valueStr.toLongOrNull() ?: 0L) > 0L,
                onClick = {
                    val now = System.currentTimeMillis()
                    onConfirm(
                        Voucher(
                            id = "",
                            code = code.trim().uppercase(),
                            type = if (isPercent) VoucherType.PERCENT else VoucherType.FIXED,
                            value = valueStr.toLongOrNull() ?: 10L,
                            maxDiscount = if (isPercent) maxDiscountStr.toLongOrNull() else null,
                            minOrder = minOrderStr.toLongOrNull() ?: 0L,
                            startAtEpochMs = now,
                            endAtEpochMs = now + 30L * 24 * 60 * 60 * 1000,
                            usageLimit = usageLimitStr.toIntOrNull() ?: 50,
                            usedCount = 0,
                            perUserLimit = 2,
                            isActive = true,
                        ),
                    )
                },
            ) { Text("Tạo mã", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun MerchantLoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("tt3145539@gmail.com") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng nhập tài khoản Quán") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email quán") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = email.isNotBlank() && password.isNotBlank(),
                onClick = { onLogin(email, password) },
            ) { Text("Đăng nhập", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
