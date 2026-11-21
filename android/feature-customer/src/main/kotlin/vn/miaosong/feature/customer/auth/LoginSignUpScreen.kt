package vn.miaosong.feature.customer.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.miaosong.core.designsystem.components.FacebookSignInButton
import vn.miaosong.core.designsystem.components.GoogleSignInButton
import vn.miaosong.core.designsystem.components.MiaoAnimatedCatLogo
import vn.miaosong.core.designsystem.components.MiaoCulinaryDivider
import vn.miaosong.core.designsystem.components.MiaoPrimaryButton
import vn.miaosong.core.designsystem.components.MiaoSegmentedSwitcher
import vn.miaosong.core.designsystem.components.MiaoTextField
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.designsystem.theme.MiaoSongTheme
import vn.miaosong.core.model.UserProfile

@Composable
fun LoginSignUpScreen(
    onAuthSuccess: (UserProfile) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess, uiState.currentUser) {
        val user = uiState.currentUser
        if (uiState.isSuccess && user != null) {
            viewModel.resetAuthState()
            onAuthSuccess(user)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LoginSignUpContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSwitchMode = viewModel::switchMode,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onNameChange = viewModel::onNameChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
        onRememberMeChange = viewModel::onRememberMeChange,
        onSubmit = viewModel::submit,
        onGoogleClick = viewModel::signInWithGoogle,
        onFacebookClick = viewModel::signInWithFacebook,
        onForgotPasswordClick = viewModel::resetPassword,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}

@Composable
fun LoginSignUpContent(
    uiState: AuthUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSwitchMode: (AuthMode) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onToggleConfirmPasswordVisibility: () -> Unit = {},
    onRememberMeChange: (Boolean) -> Unit = {},
    onSubmit: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onFacebookClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MiaoColors.SurfaceCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 🐾 Header Brand Showcase: Mascot Mèo Thần Tài với Ambient Glow
            MascotBrandHeader()

            Spacer(modifier = Modifier.height(20.dp))

            // 🔀 Segmented Tab Switcher: Đăng nhập / Đăng ký
            val tabs = listOf(
                "Đăng nhập" to Icons.AutoMirrored.Filled.Login,
                "Đăng ký" to Icons.Default.PersonAdd,
            )
            MiaoSegmentedSwitcher(
                selectedTabIndex = if (uiState.mode == AuthMode.LOGIN) 0 else 1,
                tabs = tabs,
                onTabSelected = { index ->
                    onSwitchMode(if (index == 0) AuthMode.LOGIN else AuthMode.REGISTER)
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 📋 Card Form Xác thực chính (Nền trắng, bo góc 20dp, shadow nhẹ)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Họ và tên (Chỉ hiển thị khi Đăng ký)
                    AnimatedVisibility(
                        visible = uiState.mode == AuthMode.REGISTER,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        MiaoTextField(
                            value = uiState.name,
                            onValueChange = onNameChange,
                            label = "Họ và tên",
                            placeholder = "Nguyễn Văn A",
                            leadingIcon = Icons.Default.Person,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                        )
                    }

                    // Email đăng nhập
                    MiaoTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = "Địa chỉ Email",
                        placeholder = "miao.song@delivery.vn",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    // Mật khẩu
                    MiaoTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "Mật khẩu",
                        placeholder = "Nhập ít nhất 6 ký tự...",
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Ẩn/hiện mật khẩu",
                                    tint = MiaoColors.Outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done,
                        ),
                    )

                    // Xác nhận mật khẩu (Chỉ hiển thị khi Đăng ký)
                    AnimatedVisibility(
                        visible = uiState.mode == AuthMode.REGISTER,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        MiaoTextField(
                            value = uiState.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = "Xác nhận mật khẩu",
                            placeholder = "Nhập lại mật khẩu trên...",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = {
                                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Ẩn/hiện mật khẩu xác nhận",
                                        tint = MiaoColors.Outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }

                    // Hàng phụ: Ghi nhớ đăng nhập & Quên mật khẩu (Chỉ hiện khi Đăng nhập)
                    if (uiState.mode == AuthMode.LOGIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Checkbox(
                                    checked = uiState.rememberMe,
                                    onCheckedChange = onRememberMeChange,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MiaoColors.BrandOrange,
                                        checkmarkColor = Color.White,
                                    ),
                                )
                                Text(
                                    text = "Ghi nhớ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiaoColors.OnSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = onForgotPasswordClick,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                            ) {
                                Text(
                                    text = "Quên mật khẩu?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MiaoColors.BrandOrange,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Thông báo lỗi nếu có
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MiaoColors.ErrorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiaoColors.OnErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // Nút CTA chính: "Đăng nhập ngay" hoặc "Tạo tài khoản mới"
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MiaoColors.BrandOrange,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    } else {
                        val buttonText = if (uiState.mode == AuthMode.LOGIN) "Đăng nhập ngay" else "Tạo tài khoản mới"
                        val buttonIcon = if (uiState.mode == AuthMode.LOGIN) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.RocketLaunch
                        MiaoPrimaryButton(
                            text = buttonText,
                            trailingIcon = buttonIcon,
                            onClick = onSubmit,
                        )
                    }
                }
            }

            // 🍜 Divider phân cách ẩm thực
            MiaoCulinaryDivider()

            // 🌐 Hệ sinh thái Đăng nhập Mạng xã hội: Google & Facebook
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GoogleSignInButton(onClick = onGoogleClick)
                FacebookSignInButton(onClick = onFacebookClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 📜 Footer tuân thủ & Điều khoản dịch vụ
            Text(
                text = "Bằng việc tiếp tục, bạn đồng ý với Điều khoản dịch vụ & Chính sách bảo mật của MiaoSong.",
                style = MaterialTheme.typography.bodySmall,
                color = MiaoColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MascotBrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient Glow đằng sau mascot
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MiaoColors.BrandOrangeLight.copy(alpha = 0.35f),
                                MiaoColors.SecondaryContainer.copy(alpha = 0.45f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .blur(18.dp),
            )

            // Vòng tròn chứa Mascot Mèo Thần Tài (phóng to nổi bật)
            Surface(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape),
                shape = CircleShape,
                color = Color.White,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    MiaoAnimatedCatLogo(
                        size = 86.dp,
                        showSquircleBackground = false,
                        animated = true,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Chào mừng đến MiaoSong! 🐾",
            style = MaterialTheme.typography.headlineMedium,
            color = MiaoColors.OnSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Giao đồ ăn nóng hổi & trà sữa mát lạnh thơm ngon trong nháy mắt",
            style = MaterialTheme.typography.bodyMedium,
            color = MiaoColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Preview(name = "MiaoSong Auth - Login Mode", showBackground = true, widthDp = 380)
@Composable
private fun LoginModePreview() {
    MiaoSongTheme {
        LoginSignUpContent(
            uiState = AuthUiState(
                mode = AuthMode.LOGIN,
                email = "customer@miaosong.vn",
                password = "Password123!",
            ),
        )
    }
}

@Preview(name = "MiaoSong Auth - Register Mode", showBackground = true, widthDp = 380)
@Composable
private fun RegisterModePreview() {
    MiaoSongTheme {
        LoginSignUpContent(
            uiState = AuthUiState(
                mode = AuthMode.REGISTER,
                name = "Huỳnh Trần Thiên Phú",
                email = "customer@miaosong.vn",
                password = "Password123!",
                confirmPassword = "Password123!",
            ),
        )
    }
}
