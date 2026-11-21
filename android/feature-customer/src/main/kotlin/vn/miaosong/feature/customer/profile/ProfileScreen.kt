package vn.miaosong.feature.customer.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.components.MiaoPrimaryButton
import vn.miaosong.core.designsystem.components.MiaoTextField
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.designsystem.theme.MiaoSongTheme
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.UserRole

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onOpenOrderHistory: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher mở thư viện ảnh của Android
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatarFromUri(uri.toString())
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            viewModel.resetLogoutState()
            onLoggedOut()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    ProfileContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPickAvatar = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onEditNameChange = viewModel::onEditNameChange,
        onEditPhoneChange = viewModel::onEditPhoneChange,
        onSaveProfile = viewModel::saveProfile,
        onStartChangingPassword = viewModel::startChangingPassword,
        onCancelChangingPassword = viewModel::cancelChangingPassword,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmNewPasswordChange = viewModel::onConfirmNewPasswordChange,
        onToggleNewPasswordVisibility = viewModel::toggleNewPasswordVisibility,
        onToggleConfirmNewPasswordVisibility = viewModel::toggleConfirmNewPasswordVisibility,
        onSubmitPasswordChange = viewModel::submitPasswordChange,
        onSendPasswordReset = viewModel::sendPasswordReset,
        onAddNewAddress = viewModel::openAddAddressDialog,
        onCloseAddAddressDialog = viewModel::closeAddAddressDialog,
        onNewAddressLabelChange = viewModel::onNewAddressLabelChange,
        onNewAddressFullTextChange = viewModel::onNewAddressFullTextChange,
        onNewAddressNoteChange = viewModel::onNewAddressNoteChange,
        onNewAddressIsDefaultChange = viewModel::onNewAddressIsDefaultChange,
        onSubmitAddAddress = viewModel::submitAddAddress,
        onSetDefaultAddress = viewModel::setDefaultAddress,
        onDeleteAddress = viewModel::deleteAddress,
        onSignOut = viewModel::signOut,
        onOpenOrderHistory = onOpenOrderHistory,
        modifier = modifier,
    )
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onPickAvatar: () -> Unit = {},
    onStartEditing: () -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onEditNameChange: (String) -> Unit = {},
    onEditPhoneChange: (String) -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onStartChangingPassword: () -> Unit = {},
    onCancelChangingPassword: () -> Unit = {},
    onNewPasswordChange: (String) -> Unit = {},
    onConfirmNewPasswordChange: (String) -> Unit = {},
    onToggleNewPasswordVisibility: () -> Unit = {},
    onToggleConfirmNewPasswordVisibility: () -> Unit = {},
    onSubmitPasswordChange: () -> Unit = {},
    onSendPasswordReset: () -> Unit = {},
    onAddNewAddress: () -> Unit = {},
    onCloseAddAddressDialog: () -> Unit = {},
    onNewAddressLabelChange: (String) -> Unit = {},
    onNewAddressFullTextChange: (String) -> Unit = {},
    onNewAddressNoteChange: (String) -> Unit = {},
    onNewAddressIsDefaultChange: (Boolean) -> Unit = {},
    onSubmitAddAddress: () -> Unit = {},
    onSetDefaultAddress: (String) -> Unit = {},
    onDeleteAddress: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenOrderHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfileTopAppBar(onBack = onBack)
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 🌟 Profile Hero Section (Avatar chọn từ máy, Ambient Glow, Name, Role Badge)
            ProfileHeroSection(
                user = uiState.user,
                avatarUri = uiState.avatarUri,
                isUploadingAvatar = uiState.isUploadingAvatar,
                onPickAvatar = onPickAvatar,
            )

            // 👤 Section 1: Thông tin cá nhân
            PersonalInfoSection(
                user = uiState.user,
                onEditClick = onStartEditing,
            )

            // 📍 Section 2: Địa chỉ nhận món (OpenStreetMap / Firestore)
            SavedAddressesSection(
                addresses = uiState.addresses,
                onAddNewAddress = onAddNewAddress,
                onSetDefaultAddress = onSetDefaultAddress,
                onDeleteAddress = onDeleteAddress,
            )

            // 🔒 Section 3: Bảo mật & Cài đặt
            SecuritySection(
                onChangePasswordClick = onStartChangingPassword,
                onOrderHistoryClick = onOpenOrderHistory,
            )

            // 🚪 Section 4: Nút Đăng xuất
            LogoutButton(onClick = onSignOut)

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ✏️ Dialog Chỉnh sửa Thông tin Cá nhân
        if (uiState.isEditing) {
            EditProfileDialog(
                editName = uiState.editName,
                editPhone = uiState.editPhone,
                onNameChange = onEditNameChange,
                onPhoneChange = onEditPhoneChange,
                onDismiss = onCancelEditing,
                onSave = onSaveProfile,
            )
        }

        // 🔑 Dialog Đổi Mật Khẩu Trực Tiếp Trong App
        if (uiState.isChangingPassword) {
            ChangePasswordDialog(
                newPassword = uiState.newPassword,
                confirmNewPassword = uiState.confirmNewPassword,
                isNewPasswordVisible = uiState.isNewPasswordVisible,
                isConfirmNewPasswordVisible = uiState.isConfirmNewPasswordVisible,
                onNewPasswordChange = onNewPasswordChange,
                onConfirmNewPasswordChange = onConfirmNewPasswordChange,
                onToggleNewPasswordVisibility = onToggleNewPasswordVisibility,
                onToggleConfirmNewPasswordVisibility = onToggleConfirmNewPasswordVisibility,
                onSendResetEmail = onSendPasswordReset,
                onDismiss = onCancelChangingPassword,
                onConfirm = onSubmitPasswordChange,
            )
        }

        // 🏡 Dialog Thêm Địa Chỉ Giao Hàng Mới
        if (uiState.isAddingAddress) {
            AddAddressDialog(
                label = uiState.newAddressLabel,
                fullText = uiState.newAddressFullText,
                note = uiState.newAddressNote,
                isDefault = uiState.newAddressIsDefault,
                isLoading = uiState.isLoading,
                onLabelChange = onNewAddressLabelChange,
                onFullTextChange = onNewAddressFullTextChange,
                onNoteChange = onNewAddressNoteChange,
                onDefaultChange = onNewAddressIsDefaultChange,
                onDismiss = onCloseAddAddressDialog,
                onConfirm = onSubmitAddAddress,
            )
        }
    }
}

@Composable
private fun ProfileTopAppBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MiaoColors.OnSurface,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                MiaoAnimatedCatLogo(
                    size = 32.dp,
                    animated = false,
                    showSquircleBackground = false,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thông Tin Tài Khoản",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeroSection(
    user: UserProfile,
    avatarUri: String?,
    isUploadingAvatar: Boolean = false,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveAvatar = avatarUri.takeIf { !it.isNullOrBlank() } ?: user.avatarUrl.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient Warm Glow
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MiaoColors.BrandOrangeLight.copy(alpha = 0.35f),
                                MiaoColors.SecondaryContainer.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .blur(16.dp),
            )

            // Vòng tròn chứa Avatar
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape),
                shape = CircleShape,
                color = Color.White,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!effectiveAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveAvatar,
                            contentDescription = "Ảnh đại diện",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
            }

            // Nút Camera nhỏ mở Photo Picker
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isUploadingAvatar) { onPickAvatar() }
                    .shadow(elevation = 3.dp, shape = CircleShape),
                shape = CircleShape,
                color = MiaoColors.BrandOrange,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Chọn ảnh đại diện từ máy",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tên người dùng
        Text(
            text = user.name.ifBlank { "Khách hàng" },
            style = MaterialTheme.typography.headlineMedium,
            color = MiaoColors.OnSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PersonalInfoSection(
    user: UserProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MiaoColors.SurfaceContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MiaoColors.BrandOrange,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thông tin cá nhân",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(
                    onClick = onEditClick,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chỉnh sửa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MiaoColors.BrandOrange,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Trường 1: Họ và tên
            Column {
                Text(
                    text = "Họ và tên",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Text(
                    text = user.name.ifBlank { "Chưa cập nhật" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MiaoColors.OnSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Trường 2: Số điện thoại
            Column {
                Text(
                    text = "Số điện thoại",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Text(
                    text = user.phone.ifBlank { "Chưa cập nhật (Nhấn 'Chỉnh sửa' để thêm)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user.phone.isNotBlank()) MiaoColors.OnSurface else MiaoColors.Outline,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Trường 3: Email liên hệ
            Column {
                Text(
                    text = "Email liên hệ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Text(
                    text = user.email.ifBlank { "Chưa có email" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user.email.isNotBlank()) MiaoColors.OnSurface else MiaoColors.Outline,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SavedAddressesSection(
    addresses: List<DeliveryAddress>,
    onAddNewAddress: () -> Unit,
    onSetDefaultAddress: (String) -> Unit = {},
    onDeleteAddress: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MiaoColors.SurfaceContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MiaoColors.BrandOrange,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Địa chỉ nhận món",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(
                    onClick = onAddNewAddress,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thêm mới",
                        style = MaterialTheme.typography.labelMedium,
                        color = MiaoColors.BrandOrange,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (addresses.isEmpty()) {
                Text(
                    text = "Chưa có địa chỉ nào được lưu. Bấm \"+ Thêm mới\" để thêm địa chỉ giao hàng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiaoColors.OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                addresses.forEach { address ->
                    AddressItemCard(
                        address = address,
                        onSetDefault = { onSetDefaultAddress(address.id) },
                        onDelete = { onDeleteAddress(address.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressItemCard(
    address: DeliveryAddress,
    onSetDefault: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (address.isDefault) MiaoColors.SurfaceContainerLow else MiaoColors.SurfaceCanvas,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = if (address.isDefault) MiaoColors.BrandOrange else MiaoColors.SurfaceContainer,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (address.isDefault) Icons.Default.Domain else Icons.Default.Home,
                        contentDescription = null,
                        tint = if (address.isDefault) Color.White else MiaoColors.OnSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = address.label.ifBlank { "Địa chỉ" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MiaoColors.BrandOrangeLight.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.75.dp, MiaoColors.BrandOrange.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = "MẶC ĐỊNH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MiaoColors.BrandOrangeDark,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = address.fullText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiaoColors.OnSurfaceVariant,
                    lineHeight = 16.sp,
                )
                if (address.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ghi chú: ${address.note}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiaoColors.Outline,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = MiaoColors.Outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (!address.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Đặt làm mặc định") },
                            onClick = {
                                showMenu = false
                                onSetDefault()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MiaoColors.BrandOrange,
                                )
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Xóa địa chỉ", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SecuritySection(
    onChangePasswordClick: () -> Unit,
    onOrderHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MiaoColors.SurfaceContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bảo mật & Cài đặt",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MiaoColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Đổi mật khẩu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onChangePasswordClick() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Password,
                        contentDescription = null,
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đổi mật khẩu tài khoản",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MiaoColors.Outline,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Lịch sử đơn hàng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOrderHistoryClick() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Xem lịch sử đơn đặt hàng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MiaoColors.Outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() },
        color = MiaoColors.SurfaceContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MiaoColors.BrandOrangeDark,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đăng xuất tài khoản",
                style = MaterialTheme.typography.labelLarge,
                color = MiaoColors.BrandOrangeDark,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    editName: String,
    editPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chỉnh sửa thông tin",
                style = MaterialTheme.typography.headlineSmall,
                color = MiaoColors.OnSurface,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiaoTextField(
                    value = editName,
                    onValueChange = onNameChange,
                    label = "Họ và tên",
                    placeholder = "Nhập họ và tên...",
                    leadingIcon = Icons.Default.Person,
                )
                MiaoTextField(
                    value = editPhone,
                    onValueChange = onPhoneChange,
                    label = "Số điện thoại liên hệ",
                    placeholder = "Ví dụ: 0901234567",
                    leadingIcon = Icons.Default.Person,
                )
            }
        },
        confirmButton = {
            MiaoPrimaryButton(
                text = "Lưu thay đổi",
                onClick = onSave,
                modifier = Modifier.width(130.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Hủy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MiaoColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun ChangePasswordDialog(
    newPassword: String,
    confirmNewPassword: String,
    isNewPasswordVisible: Boolean,
    isConfirmNewPasswordVisible: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onToggleNewPasswordVisibility: () -> Unit,
    onToggleConfirmNewPasswordVisibility: () -> Unit,
    onSendResetEmail: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Đổi Mật Khẩu",
                style = MaterialTheme.typography.headlineSmall,
                color = MiaoColors.OnSurface,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiaoTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "Mật khẩu mới",
                    placeholder = "Tối thiểu 6 ký tự...",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = onToggleNewPasswordVisibility) {
                            Icon(
                                imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Ẩn/hiện mật khẩu",
                                tint = MiaoColors.Outline,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                )

                MiaoTextField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChange,
                    label = "Xác nhận mật khẩu mới",
                    placeholder = "Nhập lại mật khẩu mới...",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = onToggleConfirmNewPasswordVisibility) {
                            Icon(
                                imageVector = if (isConfirmNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Ẩn/hiện mật khẩu",
                                tint = MiaoColors.Outline,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    visualTransformation = if (isConfirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )

                TextButton(
                    onClick = onSendResetEmail,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = "📧 Hoặc gửi email khôi phục mật khẩu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiaoColors.BrandOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            MiaoPrimaryButton(
                text = "Đổi mật khẩu",
                onClick = onConfirm,
                modifier = Modifier.width(140.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Hủy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MiaoColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun AddAddressDialog(
    label: String,
    fullText: String,
    note: String,
    isDefault: Boolean,
    isLoading: Boolean,
    onLabelChange: (String) -> Unit,
    onFullTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDefaultChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm địa chỉ nhận món",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiaoTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = "Tên gợi nhớ",
                    placeholder = "Vd: Nhà riêng, Cơ quan, KTX...",
                    leadingIcon = Icons.Default.Home,
                )
                MiaoTextField(
                    value = fullText,
                    onValueChange = onFullTextChange,
                    label = "Địa chỉ chi tiết",
                    placeholder = "Số nhà, tên đường, phường, quận/huyện...",
                    leadingIcon = Icons.Default.LocationOn,
                )
                MiaoTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = "Ghi chú cho tài xế",
                    placeholder = "Vd: Gọi khi đến cổng, gửi bàn lễ tân...",
                    leadingIcon = Icons.Default.Edit,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDefaultChange(!isDefault) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = isDefault,
                        onCheckedChange = onDefaultChange,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đặt làm địa chỉ mặc định",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiaoColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            MiaoPrimaryButton(
                text = if (isLoading) "Đang lưu..." else "Lưu địa chỉ",
                onClick = onConfirm,
                enabled = !isLoading && label.isNotBlank() && fullText.isNotBlank(),
                modifier = Modifier.width(130.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(
                    text = "Hủy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MiaoColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}

@Preview(name = "MiaoSong Profile Screen", showBackground = true, widthDp = 380)
@Composable
private fun ProfileScreenPreview() {
    MiaoSongTheme {
        ProfileContent(
            uiState = ProfileUiState(
                user = UserProfile(
                    uid = "demo_customer_01",
                    name = "Huỳnh Trần Thiên Phú",
                    email = "customer@miaosong.vn",
                    phone = "0901234567",
                    role = UserRole.CUSTOMER,
                ),
                addresses = listOf(
                    DeliveryAddress(
                        id = "addr_tdtu",
                        label = "Trường ĐH Tôn Đức Thắng",
                        fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
                        lat = 10.7326,
                        lng = 106.6998,
                        note = "Gọi khi tới cổng chính",
                        isDefault = true,
                    ),
                    DeliveryAddress(
                        id = "addr_bitexco",
                        label = "Văn phòng / Bitexco",
                        fullText = "2 Hải Triều, Bến Nghé, Quận 1, TP. HCM",
                        lat = 10.7716,
                        lng = 106.7044,
                        note = "Tầng 18, gửi tại bàn lễ tân",
                        isDefault = false,
                    ),
                ),
            ),
        )
    }
}
