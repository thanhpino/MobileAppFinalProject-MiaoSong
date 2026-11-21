package vn.miaosong.feature.merchant

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
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
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.OrderStatus
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
    var hasAutoPromptedLogin by rememberSaveable { mutableStateOf(false) }
    var seenPlacedOrderIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val context = LocalContext.current
    val isMerchantAuthenticated = uiState.currentUser?.role == UserRole.MERCHANT

    // Báo chuông tại thiết bị quán ngay khi có đơn mới (PLACED)
    LaunchedEffect(uiState.orders) {
        val currentPlacedIds = uiState.orders
            .filter { it.status == OrderStatus.PLACED }
            .map { it.id }
            .toSet()
        val hasBrandNewOrder = currentPlacedIds.any { it !in seenPlacedOrderIds }
        if (hasBrandNewOrder) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, uri)?.play()
            }
            snackbarHostState.showSnackbar("🔔 Có đơn hàng mới! Vào tab Bếp (KDS) để xác nhận nhận đơn.")
        }
        seenPlacedOrderIds = currentPlacedIds
    }

    LaunchedEffect(uiState.isAuthResolved, isMerchantAuthenticated) {
        if (uiState.isAuthResolved && !isMerchantAuthenticated && !hasAutoPromptedLogin) {
            hasAutoPromptedLogin = true
            showLoginDialog = true
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

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
            when (selectedTab) {
                0 -> MerchantMenuTab(
                    categories = uiState.categories,
                    items = uiState.menuItems,
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onAddItemClick = { showAddItemDialog = true },
                    onToggleAvailability = viewModel::toggleItemAvailability,
                    onDeleteItem = viewModel::deleteMenuItem,
                    onUploadItemImage = viewModel::uploadMenuItemImage,
                )
                1 -> MerchantVoucherTab(
                    vouchers = uiState.vouchers,
                    onAddVoucherClick = { showAddVoucherDialog = true },
                    onToggleActive = viewModel::toggleVoucherActive,
                    onDelete = viewModel::deleteVoucher,
                )
                2 -> MerchantKitchenTab(
                    orders = uiState.orders,
                    onConfirmOrder = viewModel::confirmOrder,
                    onRejectOrder = viewModel::rejectOrder,
                    onMarkReady = viewModel::markOrderReady,
                    onDispatchDriver = viewModel::dispatchDriver,
                    onAdvanceMockDelivery = viewModel::advanceMockDelivery,
                )
                3 -> MerchantStoreProfileTab(
                    uiState = uiState,
                    onUpdateStatus = viewModel::updateStoreStatus,
                    onUpdateStoreInfo = viewModel::updateStoreInfo,
                    onUpdateDailyHours = viewModel::updateDailyHours,
                    onAddSpecialDay = viewModel::addSpecialDay,
                    onRemoveSpecialDay = viewModel::removeSpecialDay,
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
private fun MerchantMenuTab(
    categories: List<MenuCategory>,
    items: List<MenuItem>,
    onAddCategoryClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onToggleAvailability: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit,
    onUploadItemImage: (String, ByteArray) -> Unit,
) {
    val context = LocalContext.current
    var pendingImageItemId by remember { mutableStateOf<String?>(null) }
    val itemPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val targetId = pendingImageItemId
        if (uri != null && targetId != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                onUploadItemImage(targetId, bytes)
            }
        }
        pendingImageItemId = null
    }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val filteredItems = if (selectedCategoryId == null) {
        items
    } else {
        items.filter { it.categoryId == selectedCategoryId }
    }

    val categoryNameMap = remember(categories) { categories.associate { it.id to it.name } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nhóm thực đơn (${categories.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MiaoColors.OnSurfaceVariant,
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                    Surface(
                        color = MiaoColors.SurfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onAddCategoryClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Thêm nhóm",
                                tint = MiaoColors.BrandOrange,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Nhóm",
                                color = MiaoColors.BrandOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Danh sách món (${filteredItems.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { onAddItemClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Thêm món",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            val catName = categoryNameMap[item.categoryId] ?: ""
            val fallbackEmoji = when {
                catName.contains("uống", ignoreCase = true) || catName.contains("nước", ignoreCase = true) -> "🧋"
                catName.contains("phụ", ignoreCase = true) || catName.contains("kèm", ignoreCase = true) -> "🥢"
                else -> "🍜"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Ảnh món ăn (chạm để tải / đổi ảnh cho món chính, món phụ, nước uống)
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MiaoColors.SurfaceContainerLow)
                                .clickable {
                                    pendingImageItemId = item.id
                                    itemPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
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
                                Text(text = fallbackEmoji, fontSize = 28.sp)
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .size(22.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Đổi ảnh món",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.name,
                                color = MiaoColors.OnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = MoneyFormatter.formatVnd(item.basePrice),
                                color = MiaoColors.BrandOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (catName.isNotBlank()) {
                                Text(
                                    text = catName,
                                    color = MiaoColors.OnSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = item.isAvailable,
                                    onCheckedChange = { onToggleAvailability(item.id, it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = MiaoColors.SecondaryTealBright),
                                )
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa món",
                                        tint = MiaoColors.OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Text(
                                text = if (item.isAvailable) "Còn hàng" else "Hết hàng",
                                color = if (item.isAvailable) MiaoColors.SecondaryTeal else MiaoColors.Error,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp),
                            )
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
    onAddVoucherClick: () -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mã khuyến mãi (${vouchers.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { onAddVoucherClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Tạo mã",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    text = "Voucher chỉ giảm trên tiền món & món thêm, không trừ vào phí giao hàng.",
                    color = MiaoColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
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
private fun MerchantKitchenTab(
    orders: List<Order>,
    onConfirmOrder: (String) -> Unit,
    onRejectOrder: (String, String, String) -> Unit,
    onMarkReady: (String) -> Unit,
    onDispatchDriver: (String) -> Unit,
    onAdvanceMockDelivery: (String, OrderStatus) -> Unit,
) {
    var rejectingOrder by remember { mutableStateOf<Order?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Màn hình Bếp & Điều phối (${orders.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                )
                Text(
                    text = "Nhận đơn, xem ghi chú dị ứng và gọi tài xế Lalamove tự động.",
                    color = MiaoColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Mã đơn #MS-261121-0012",
                                color = MiaoColors.OnSurface,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                color = Color(0xFFE6FAF8),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "Đang chuẩn bị",
                                    color = MiaoColors.SecondaryTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Text(
                            text = "1x Bún Riêu Cua Đặc Biệt (Tô lớn · Thêm chả cá)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MiaoColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        KitchenNoteBadge(
                            note = "Không hành, để mắm tôm riêng",
                            isAllergy = false,
                        )
                        KitchenNoteBadge(
                            note = "Khách dị ứng với đậu phộng",
                            isAllergy = true,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                color = MiaoColors.SurfaceContainerLow,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Báo xong món",
                                        color = MiaoColors.BrandOrangeDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    )
                                }
                            }
                            Surface(
                                color = MiaoColors.BrandOrange,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1.2f),
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Gọi shipper Lalamove",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                MerchantOrderKdsCard(
                    order = order,
                    onConfirm = { onConfirmOrder(order.id) },
                    onRejectClick = { rejectingOrder = order },
                    onMarkReady = { onMarkReady(order.id) },
                    onDispatchDriver = { onDispatchDriver(order.id) },
                    onAdvanceMockDelivery = { nextStatus -> onAdvanceMockDelivery(order.id, nextStatus) },
                )
            }
        }
    }

    rejectingOrder?.let { target ->
        RejectOrderDialog(
            orderCode = target.code,
            onDismiss = { rejectingOrder = null },
            onConfirm = { code, text ->
                onRejectOrder(target.id, code, text)
                rejectingOrder = null
            },
        )
    }
}

@Composable
private fun MerchantOrderKdsCard(
    order: Order,
    onConfirm: () -> Unit,
    onRejectClick: () -> Unit,
    onMarkReady: () -> Unit,
    onDispatchDriver: () -> Unit,
    onAdvanceMockDelivery: (OrderStatus) -> Unit,
) {
    val (statusLabel, statusBg, statusFg) = when (order.status) {
        OrderStatus.PENDING_PAYMENT -> Triple("Chờ thanh toán", Color(0xFFFFF3CD), Color(0xFF785A00))
        OrderStatus.PLACED -> Triple("Đơn mới", Color(0xFFFFEADF), MiaoColors.BrandOrangeDark)
        OrderStatus.CONFIRMED -> Triple("Đang chuẩn bị", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        OrderStatus.READY -> Triple("Sẵn sàng giao", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        OrderStatus.FINDING_DRIVER -> Triple("Đang tìm tài xế", Color(0xFFFFF3CD), Color(0xFF785A00))
        OrderStatus.DRIVER_ASSIGNED, OrderStatus.ARRIVED_AT_STORE -> Triple("Tài xế đang đến lấy", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        OrderStatus.PICKED_UP, OrderStatus.DELIVERING -> Triple("Đang giao khách", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        OrderStatus.COMPLETED -> Triple("Hoàn thành", Color(0xFFE6FAF8), MiaoColors.SecondaryTeal)
        OrderStatus.REJECTED, OrderStatus.CANCELLED, OrderStatus.DELIVERY_FAILED -> Triple("Đã hủy / Từ chối", Color(0xFFFFDAD6), Color(0xFF93000A))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mã đơn #${order.code}",
                    color = MiaoColors.OnSurface,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = statusBg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Danh sách món & ghi chú / cảnh báo dị ứng
            order.items.forEach { item ->
                val details = buildList {
                    if (item.sizeName.isNotBlank()) add(item.sizeName)
                    if (item.toppingNames.isNotEmpty()) add(item.toppingNames.joinToString(", "))
                }.joinToString(" · ")
                Text(
                    text = "${item.quantity}x ${item.itemName}" + if (details.isNotBlank()) " ($details)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MiaoColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
                if (item.note.isNotBlank()) {
                    val isAllergy = item.note.contains("dị ứng", ignoreCase = true) ||
                        item.note.contains("allergy", ignoreCase = true) ||
                        item.note.contains("đậu phộng", ignoreCase = true)
                    KitchenNoteBadge(note = item.note, isAllergy = isAllergy)
                }
            }

            Text(
                text = "Tổng thu: ${MoneyFormatter.formatVnd(order.pricing.total)} · ${order.payment.method.name}",
                color = MiaoColors.BrandOrangeDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            // Thông tin tài xế Lalamove khi đã được gán + nút gọi điện
            order.delivery.driver?.let { driver ->
                val dialContext = LocalContext.current
                Surface(
                    color = Color(0xFFE6FAF8),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Tài xế Lalamove: ${driver.name}",
                                color = MiaoColors.SecondaryTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "SĐT: ${driver.phone} · Biển số: ${driver.plate}",
                                color = MiaoColors.OnSurface,
                                fontSize = 12.sp,
                            )
                        }
                        if (driver.phone.isNotBlank()) {
                            Surface(
                                color = MiaoColors.SecondaryTeal,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.clickable {
                                    dialContext.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driver.phone}")),
                                    )
                                },
                            ) {
                                Text(
                                    text = "Gọi tài xế",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Nút thao tác theo trạng thái đơn hàng
            when (order.status) {
                OrderStatus.PLACED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = Color(0xFFFFF1F0),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0xFFD92D20).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(0.9f)
                                .clickable { onRejectClick() },
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Từ chối",
                                    color = Color(0xFFD92D20),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        Surface(
                            color = MiaoColors.BrandOrange,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .clickable { onConfirm() },
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Xác nhận nhận đơn",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
                OrderStatus.CONFIRMED -> {
                    MiaoPrimaryButton(
                        text = "Báo xong món (Sẵn sàng giao)",
                        onClick = onMarkReady,
                    )
                }
                OrderStatus.READY -> {
                    MiaoPrimaryButton(
                        text = "Gọi shipper Lalamove",
                        onClick = onDispatchDriver,
                    )
                }
                OrderStatus.FINDING_DRIVER -> {
                    TextButton(onClick = { onAdvanceMockDelivery(OrderStatus.DRIVER_ASSIGNED) }) {
                        Text("Giả lập: Tài xế nhận chuyến (Demo)", color = MiaoColors.SecondaryTeal, fontWeight = FontWeight.Bold)
                    }
                }
                OrderStatus.DRIVER_ASSIGNED -> {
                    TextButton(onClick = { onAdvanceMockDelivery(OrderStatus.PICKED_UP) }) {
                        Text("Giả lập: Tài xế đã lấy món (Demo)", color = MiaoColors.SecondaryTeal, fontWeight = FontWeight.Bold)
                    }
                }
                OrderStatus.PICKED_UP, OrderStatus.DELIVERING -> {
                    TextButton(onClick = { onAdvanceMockDelivery(OrderStatus.COMPLETED) }) {
                        Text("Giả lập: Đã giao thành công (Demo)", color = MiaoColors.SecondaryTeal, fontWeight = FontWeight.Bold)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun RejectOrderDialog(
    orderCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val reasons = listOf(
        "OUT_OF_STOCK" to "Hết nguyên liệu / Hết món",
        "STORE_BUSY" to "Quán đang quá tải đơn",
        "CLOSING_SOON" to "Quán sắp đóng cửa",
    )
    var selectedCode by remember { mutableStateOf(reasons.first().first) }
    var customReason by remember { mutableStateOf(reasons.first().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Từ chối Mã đơn #$orderCode",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { (code, label) ->
                    FilterChip(
                        selected = selectedCode == code,
                        onClick = {
                            selectedCode = code
                            customReason = label
                        },
                        label = { Text(label) },
                    )
                }
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    label = { Text("Lý do gửi khách") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = customReason.isNotBlank(),
                onClick = { onConfirm(selectedCode, customReason) },
            ) { Text("Xác nhận từ chối", color = Color(0xFFD92D20), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun MerchantStoreProfileTab(
    uiState: MerchantUiState,
    onUpdateStatus: (ManualStoreStatus, Int?) -> Unit,
    onUpdateStoreInfo: (String, String, String) -> Unit,
    onUpdateDailyHours: (String, String) -> Unit,
    onAddSpecialDay: (String, Boolean, String, String) -> Unit,
    onRemoveSpecialDay: (String) -> Unit,
    onAvatarPicked: (ByteArray) -> Unit,
    onSignInClick: () -> Unit,
    onSwitchToCustomer: () -> Unit,
) {
    val context = LocalContext.current
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showDailyHoursDialog by remember { mutableStateOf(false) }
    var showSpecialDayDialog by remember { mutableStateOf(false) }
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
    val firstSlot = store?.schedule?.weeklySchedule?.values?.firstOrNull()?.firstOrNull()
    val dailyHoursText = if (firstSlot != null) "${firstSlot.open} – ${firstSlot.close}" else "07:00 – 21:00"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Thẻ Hồ sơ Quán — tiêu đề + nút Chỉnh sửa nằm ngay trên đầu
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Thông tin & địa chỉ quán",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Text(
                        text = "Chỉnh sửa",
                        color = MiaoColors.BrandOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showEditInfoDialog = true },
                    )
                }

                Row(
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MiaoColors.OnSurface,
                        )
                        Text(
                            text = store?.address ?: "420 Nguyễn Thị Thập, Q.7",
                            color = MiaoColors.OnSurfaceVariant,
                            fontSize = 12.5.sp,
                        )
                        if (!store?.phone.isNullOrBlank()) {
                            Text(
                                text = "SĐT quán: ${store?.phone}",
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        // 2. Trạng thái hoạt động của quán
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trạng thái hoạt động",
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

        // 3. Lịch hoạt động thường ngày & Lịch mở/đóng cửa tùy chỉnh (Mục 3 đề bài)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Lịch mở cửa & ngày lễ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Text(
                        text = "Chỉnh giờ",
                        color = MiaoColors.BrandOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showDailyHoursDialog = true },
                    )
                }

                Surface(
                    color = MiaoColors.SurfaceCanvas,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Thứ 2 – Chủ nhật",
                            color = MiaoColors.OnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = dailyHoursText,
                            color = MiaoColors.SecondaryTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Lịch mở/đóng cửa tùy chỉnh",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "+ Thêm ngày",
                        color = MiaoColors.BrandOrange,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showSpecialDayDialog = true },
                    )
                }

                val specialDays = store?.schedule?.specialDays.orEmpty()
                if (specialDays.isEmpty()) {
                    Text(
                        text = "Chưa có ngày nghỉ lễ hoặc giờ mở cửa đặc biệt.",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                } else {
                    specialDays.forEach { day ->
                        val desc = if (day.isClosed) {
                            "Nghỉ cả ngày"
                        } else {
                            val s = day.slots.firstOrNull()
                            if (s != null) "Mở ${s.open} – ${s.close}" else "Giờ tùy chỉnh"
                        }
                        Surface(
                            color = MiaoColors.SurfaceCanvas,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${day.date} · $desc",
                                    color = MiaoColors.OnSurface,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                IconButton(
                                    onClick = { onRemoveSpecialDay(day.date) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa ngày",
                                        tint = MiaoColors.OnSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Lịch sử đánh giá từ khách hàng (Mục 5 đề bài)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Đánh giá từ khách hàng",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                    )
                    Surface(
                        color = Color(0xFFFFF8E6),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        val avgText = String.format("%.1f", store?.ratingAvg ?: 4.9)
                        val count = store?.ratingCount ?: uiState.reviews.size
                        Text(
                            text = "★ $avgText ($count)",
                            color = Color(0xFFB45309),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                if (uiState.reviews.isEmpty()) {
                    Text(
                        text = "Quán bạn hiện chưa có đánh giá nào từ khách hàng.",
                        color = MiaoColors.OnSurfaceVariant,
                        fontSize = 12.5.sp,
                    )
                } else {
                    uiState.reviews.take(5).forEach { review ->
                        Surface(
                            color = MiaoColors.SurfaceCanvas,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "★".repeat(review.stars.coerceIn(1, 5)) +
                                        "☆".repeat((5 - review.stars).coerceIn(0, 5)),
                                    color = Color(0xFFD97706),
                                    fontSize = 13.sp,
                                )
                                if (review.comment.isNotBlank()) {
                                    Text(
                                        text = review.comment,
                                        color = MiaoColors.OnSurface,
                                        fontSize = 13.sp,
                                    )
                                }
                                if (review.tags.isNotEmpty()) {
                                    Text(
                                        text = review.tags.joinToString(" · "),
                                        color = MiaoColors.SecondaryTeal,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
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

    if (showEditInfoDialog) {
        EditStoreInfoDialog(
            initialName = store?.name.orEmpty(),
            initialAddress = store?.address.orEmpty(),
            initialPhone = store?.phone.orEmpty(),
            onDismiss = { showEditInfoDialog = false },
            onConfirm = { name, address, phone ->
                onUpdateStoreInfo(name, address, phone)
                showEditInfoDialog = false
            },
        )
    }

    if (showDailyHoursDialog) {
        EditDailyHoursDialog(
            initialOpen = firstSlot?.open ?: "07:00",
            initialClose = firstSlot?.close ?: "21:00",
            onDismiss = { showDailyHoursDialog = false },
            onConfirm = { open, close ->
                onUpdateDailyHours(open, close)
                showDailyHoursDialog = false
            },
        )
    }

    if (showSpecialDayDialog) {
        AddSpecialDayDialog(
            onDismiss = { showSpecialDayDialog = false },
            onConfirm = { date, isClosed, open, close ->
                onAddSpecialDay(date, isClosed, open, close)
                showSpecialDayDialog = false
            },
        )
    }
}

@Composable
private fun EditDailyHoursDialog(
    initialOpen: String,
    initialClose: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var openTime by remember { mutableStateOf(initialOpen) }
    var closeTime by remember { mutableStateOf(initialClose) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Giờ hoạt động hàng ngày", fontWeight = FontWeight.Bold, color = MiaoColors.OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = openTime,
                    onValueChange = { openTime = it },
                    label = { Text("Giờ mở cửa (VD: 07:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = closeTime,
                    onValueChange = { closeTime = it },
                    label = { Text("Giờ đóng cửa (VD: 21:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = openTime.isNotBlank() && closeTime.isNotBlank(),
                onClick = { onConfirm(openTime, closeTime) },
            ) { Text("Lưu lịch", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AddSpecialDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String, String) -> Unit,
) {
    var date by remember { mutableStateOf("2026-11-30") }
    var isClosed by remember { mutableStateOf(true) }
    var openTime by remember { mutableStateOf("08:00") }
    var closeTime by remember { mutableStateOf("18:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Thêm lịch mở/đóng tùy chỉnh", fontWeight = FontWeight.Bold, color = MiaoColors.OnSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Ngày áp dụng (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Đóng cửa cả ngày", fontSize = 13.5.sp, color = MiaoColors.OnSurface)
                    Switch(
                        checked = isClosed,
                        onCheckedChange = { isClosed = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MiaoColors.BrandOrange),
                    )
                }
                if (!isClosed) {
                    OutlinedTextField(
                        value = openTime,
                        onValueChange = { openTime = it },
                        label = { Text("Giờ mở cửa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = closeTime,
                        onValueChange = { closeTime = it },
                        label = { Text("Giờ đóng cửa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = date.isNotBlank(),
                onClick = { onConfirm(date, isClosed, openTime, closeTime) },
            ) { Text("Lưu ngày", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun EditStoreInfoDialog(
    initialName: String,
    initialAddress: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var phone by remember { mutableStateOf(initialPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Chỉnh sửa thông tin quán",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên quán") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ quán (điểm lấy hàng Lalamove)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại liên hệ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && address.isNotBlank(),
                onClick = { onConfirm(name, address, phone) },
            ) { Text("Lưu thay đổi", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Thêm nhóm thực đơn",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên nhóm (VD: Món chính, Thức uống)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Thêm món mới",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (categories.isNotEmpty()) {
                    Text(
                        text = "Chọn nhóm thực đơn",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiaoColors.OnSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MiaoColors.BrandOrange,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên món ăn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter(Char::isDigit) },
                    label = { Text("Giá cơ bản (VNĐ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Gắn sẵn Size & Topping",
                        fontSize = 13.sp,
                        color = MiaoColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = includeDefaultOptions,
                        onCheckedChange = { includeDefaultOptions = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MiaoColors.BrandOrange),
                    )
                }
                Text(
                    text = "💡 Sau khi lưu món, chạm vào ảnh món ở danh sách để tải hình thật từ thư viện.",
                    fontSize = 11.5.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Tạo mã khuyến mãi",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Mã voucher (VD: MIAO15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPercent,
                        onClick = { isPercent = true },
                        label = { Text("Giảm theo %", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiaoColors.SecondaryContainer,
                            selectedLabelColor = MiaoColors.OnSecondaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = !isPercent,
                        onClick = { isPercent = false },
                        label = { Text("Giảm tiền (đ)", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiaoColors.SecondaryContainer,
                            selectedLabelColor = MiaoColors.OnSecondaryContainer,
                        ),
                    )
                }
                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it.filter(Char::isDigit) },
                    label = { Text(if (isPercent) "Phần trăm giảm (%)" else "Số tiền giảm (VNĐ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isPercent) {
                    OutlinedTextField(
                        value = maxDiscountStr,
                        onValueChange = { maxDiscountStr = it.filter(Char::isDigit) },
                        label = { Text("Giảm tối đa (VNĐ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = minOrderStr,
                    onValueChange = { minOrderStr = it.filter(Char::isDigit) },
                    label = { Text("Đơn tối thiểu (VNĐ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = usageLimitStr,
                    onValueChange = { usageLimitStr = it.filter(Char::isDigit) },
                    label = { Text("Tổng số lượng mã tối đa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Đăng nhập tài khoản Quán",
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Đăng nhập để quản lý thực đơn, tạo voucher và tải ảnh quán / ảnh món ăn lên hệ thống.",
                    fontSize = 12.5.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email quán") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = email.isNotBlank() && password.isNotBlank(),
                onClick = { onLogin(email, password) },
            ) { Text("Đăng nhập", color = MiaoColors.BrandOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để sau") } },
    )
}
