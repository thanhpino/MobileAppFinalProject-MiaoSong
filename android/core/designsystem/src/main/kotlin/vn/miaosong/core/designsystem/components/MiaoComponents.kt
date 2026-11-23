package vn.miaosong.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.miaosong.core.designsystem.theme.MiaoColors

/** Nút CTA chính dạng pill cam Warm Savory Delivery (touch target >= 52dp) */
@Composable
fun MiaoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MiaoColors.BrandOrange,
            contentColor = Color.White,
            disabledContainerColor = MiaoColors.BrandOrange.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Thẻ ghi chú & cảnh báo dị ứng đồng nhất UI Warm Savory Delivery cho Màn hình Bếp (KDS) */
@Composable
fun KitchenNoteBadge(
    note: String,
    isAllergy: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (isAllergy) Color(0xFFFFF1F0) else Color(0xFFFFF8E6)
    val accent = if (isAllergy) Color(0xFFD92D20) else Color(0xFFD97706)
    val label = if (isAllergy) "DỊ ỨNG" else "GHI CHÚ"
    val textColor = if (isAllergy) Color(0xFF7A1C16) else Color(0xFF5C3A00)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = label,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = note,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Thông báo nổi (Toast Card) hiện đại chuẩn Warm Savory Delivery thay thế Snackbar đen mặc định.
 * Tự nhận diện trạng thái Thành công (xanh Teal), Lỗi (đỏ) hoặc Đơn mới (cam).
 */
@Composable
fun MiaoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) { data ->
        val msg = data.visuals.message
        val lower = msg.lowercase()
        val isError = lower.contains("thất bại") ||
            lower.contains("lỗi") ||
            lower.contains("permission") ||
            lower.contains("incorrect") ||
            lower.contains("failed") ||
            lower.contains("không thể")
        val isBell = msg.contains("🔔") || lower.contains("đơn hàng mới")

        val accent = when {
            isError -> Color(0xFFD92D20)
            isBell -> MiaoColors.BrandOrange
            else -> MiaoColors.SecondaryTeal
        }
        val iconBg = when {
            isError -> Color(0xFFFFF1F0)
            isBell -> Color(0xFFFFF1EA)
            else -> Color(0xFFE6FAF8)
        }
        val icon = when {
            isError -> Icons.Default.ErrorOutline
            isBell -> Icons.Default.NotificationsActive
            else -> Icons.Default.CheckCircle
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = iconBg,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = msg.replace("🔔 ", ""),
                    color = MiaoColors.OnSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Trường nhập liệu chuẩn Warm Savory Delivery với nền SurfaceContainerLow */
@Composable
fun MiaoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MiaoColors.OnSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiaoColors.Outline.copy(alpha = 0.7f),
                )
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isError) MiaoColors.Error else MiaoColors.Outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else null,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = MiaoColors.SurfaceContainerLow,
                errorContainerColor = MiaoColors.SurfaceContainerLow,
                focusedBorderColor = MiaoColors.BrandOrange,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MiaoColors.Error,
                focusedTextColor = MiaoColors.OnSurface,
                unfocusedTextColor = MiaoColors.OnSurface,
                cursorColor = MiaoColors.BrandOrange,
            ),
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MiaoColors.Error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

/** Thanh chuyển đổi Tab Segmented Switcher (Đăng nhập / Đăng ký) */
@Composable
fun MiaoSegmentedSwitcher(
    selectedTabIndex: Int,
    tabs: List<Pair<String, ImageVector>>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(999.dp),
        color = MiaoColors.SurfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTabIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MiaoColors.BrandOrange else MiaoColors.OnSurfaceVariant,
                    animationSpec = tween(200),
                    label = "tab_text_color",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(999.dp))
                                    .background(Color.White)
                            } else {
                                Modifier.clickable { onTabSelected(index) }
                            }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/** Đường phân cách ẩm thực với huy hiệu trung tâm */
@Composable
fun MiaoCulinaryDivider(
    badgeText: String = "🍜 Hoặc tiếp tục với",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            color = MiaoColors.SurfaceContainerHighest,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            color = MiaoColors.SurfaceCanvas,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = MiaoColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

/** Nút Social OAuth cho Google với chuẩn vector 4 màu */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            GoogleLogoIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tiếp tục với Google",
                style = MaterialTheme.typography.labelLarge,
                color = MiaoColors.OnSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Nút Social OAuth cho Facebook với màu xanh thương hiệu #1877F2 */
@Composable
fun FacebookSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            FacebookLogoIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tiếp tục với Facebook",
                style = MaterialTheme.typography.labelLarge,
                color = MiaoColors.OnSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = w / 24f

        // Blue path
        val bluePath = Path().apply {
            moveTo(22.56f * scale, 12.25f * scale)
            cubicTo(22.56f * scale, 11.47f * scale, 22.49f * scale, 10.72f * scale, 22.36f * scale, 10.0f * scale)
            lineTo(12.0f * scale, 10.0f * scale)
            lineTo(12.0f * scale, 14.26f * scale)
            lineTo(17.92f * scale, 14.26f * scale)
            cubicTo(17.66f * scale, 15.63f * scale, 16.88f * scale, 16.79f * scale, 15.71f * scale, 17.57f * scale)
            lineTo(15.71f * scale, 20.34f * scale)
            lineTo(19.28f * scale, 20.34f * scale)
            cubicTo(21.36f * scale, 18.42f * scale, 22.56f * scale, 15.6f * scale, 22.56f * scale, 12.25f * scale)
            close()
        }
        drawPath(bluePath, Color(0xFF4285F4))

        // Green path
        val greenPath = Path().apply {
            moveTo(12.0f * scale, 23.0f * scale)
            cubicTo(14.97f * scale, 23.0f * scale, 17.46f * scale, 22.02f * scale, 19.28f * scale, 20.34f * scale)
            lineTo(15.71f * scale, 17.57f * scale)
            cubicTo(14.73f * scale, 18.23f * scale, 13.48f * scale, 18.63f * scale, 12.0f * scale, 18.63f * scale)
            cubicTo(9.14f * scale, 18.63f * scale, 6.71f * scale, 16.7f * scale, 5.84f * scale, 14.09f * scale)
            lineTo(2.18f * scale, 14.09f * scale)
            lineTo(2.18f * scale, 16.93f * scale)
            cubicTo(3.99f * scale, 20.53f * scale, 7.7f * scale, 23.0f * scale, 12.0f * scale, 23.0f * scale)
            close()
        }
        drawPath(greenPath, Color(0xFF34A853))

        // Yellow path
        val yellowPath = Path().apply {
            moveTo(5.84f * scale, 14.09f * scale)
            cubicTo(5.62f * scale, 13.43f * scale, 5.49f * scale, 12.73f * scale, 5.49f * scale, 12.0f * scale)
            cubicTo(5.49f * scale, 11.27f * scale, 5.62f * scale, 10.57f * scale, 5.84f * scale, 9.91f * scale)
            lineTo(5.84f * scale, 7.06f * scale)
            lineTo(2.18f * scale, 7.06f * scale)
            cubicTo(1.43f * scale, 8.55f * scale, 1.0f * scale, 10.22f * scale, 1.0f * scale, 12.0f * scale)
            cubicTo(1.0f * scale, 13.78f * scale, 1.43f * scale, 15.45f * scale, 2.18f * scale, 16.93f * scale)
            lineTo(5.03f * scale, 14.71f * scale)
            lineTo(5.84f * scale, 14.09f * scale)
            close()
        }
        drawPath(yellowPath, Color(0xFFFBBC05))

        // Red path
        val redPath = Path().apply {
            moveTo(12.0f * scale, 5.38f * scale)
            cubicTo(13.62f * scale, 5.38f * scale, 15.06f * scale, 5.94f * scale, 16.21f * scale, 7.02f * scale)
            lineTo(19.36f * scale, 3.87f * scale)
            cubicTo(17.45f * scale, 2.09f * scale, 14.97f * scale, 1.0f * scale, 12.0f * scale, 1.0f * scale)
            cubicTo(7.7f * scale, 1.0f * scale, 3.99f * scale, 3.47f * scale, 2.18f * scale, 7.06f * scale)
            lineTo(5.84f * scale, 9.91f * scale)
            cubicTo(6.71f * scale, 7.31f * scale, 9.14f * scale, 5.38f * scale, 12.0f * scale, 5.38f * scale)
            close()
        }
        drawPath(redPath, Color(0xFFEA4335))
    }
}

@Composable
fun FacebookLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val scale = w / 24f

        val fbPath = Path().apply {
            moveTo(24f * scale, 12.073f * scale)
            cubicTo(24f * scale, 5.446f * scale, 18.627f * scale, 0.073f * scale, 12f * scale, 0.073f * scale)
            cubicTo(5.373f * scale, 0.073f * scale, 0f * scale, 5.446f * scale, 0f * scale, 12.073f * scale)
            cubicTo(0f * scale, 18.063f * scale, 4.388f * scale, 23.027f * scale, 10.125f * scale, 23.927f * scale)
            lineTo(10.125f * scale, 15.542f * scale)
            lineTo(7.078f * scale, 15.542f * scale)
            lineTo(7.078f * scale, 12.073f * scale)
            lineTo(10.125f * scale, 12.073f * scale)
            lineTo(10.125f * scale, 9.43f * scale)
            cubicTo(10.125f * scale, 6.423f * scale, 11.917f * scale, 4.761f * scale, 14.658f * scale, 4.761f * scale)
            cubicTo(15.97f * scale, 4.761f * scale, 17.344f * scale, 4.996f * scale, 17.344f * scale, 4.996f * scale)
            lineTo(17.344f * scale, 7.949f * scale)
            lineTo(15.83f * scale, 7.949f * scale)
            cubicTo(14.339f * scale, 7.949f * scale, 13.874f * scale, 8.874f * scale, 13.874f * scale, 9.823f * scale)
            lineTo(13.874f * scale, 12.073f * scale)
            lineTo(17.202f * scale, 12.073f * scale)
            lineTo(16.67f * scale, 15.542f * scale)
            lineTo(13.874f * scale, 15.542f * scale)
            lineTo(13.874f * scale, 23.927f * scale)
            cubicTo(19.612f * scale, 23.027f * scale, 24f * scale, 18.063f * scale, 24f * scale, 12.073f * scale)
            close()
        }
        drawPath(fbPath, Color(0xFF1877F2))
    }
}
