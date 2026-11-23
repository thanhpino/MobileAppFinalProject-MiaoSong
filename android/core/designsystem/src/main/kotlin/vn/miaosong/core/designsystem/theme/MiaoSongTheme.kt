package vn.miaosong.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bảng màu Warm Savory Delivery trích xuất trực tiếp từ Stitch DESIGN.md
 * (docs/ui_design/stitch_miaosong_delivery_app_ui/warm_savory_delivery/DESIGN.md).
 */
object MiaoColors {
    val BrandOrange = Color(0xFFFF6A1A)          // Primary CTA, cart badges, highlights
    val BrandOrangeDark = Color(0xFFA43D00)      // Accessible text/icon on light surface
    val BrandOrangeHover = Color(0xFFE55A0F)
    val BrandOrangeLight = Color(0xFFFF9457)

    val SecondaryTeal = Color(0xFF006B5F)        // Verified store, ETA tags, success
    val SecondaryTealBright = Color(0xFF14B8A6)
    val SecondaryContainer = Color(0xFF6DF5E1)
    val OnSecondaryContainer = Color(0xFF006F64)

    val TertiaryGold = Color(0xFF785A00)         // Ratings, promo ribbons, kitchen note highlight
    val TertiaryAmber = Color(0xFFFFC83D)
    val KitchenNoteBg = Color(0xFFFFF3CD)        // Ghi chú đặc biệt tại màn hình bếp
    val KitchenAllergyBg = Color(0xFFFFDAD6)     // Cảnh báo dị ứng tại màn hình bếp
    val KitchenAllergyText = Color(0xFF93000A)

    val SurfaceCanvas = Color(0xFFFFF8F5)        // Butter-cream base (không dùng trắng tinh)
    val SurfaceCard = Color(0xFFFFFFFF)          // Elevated dish / store cards
    val SurfaceContainerLow = Color(0xFFFFF1EA)
    val SurfaceContainer = Color(0xFFFFEADF)
    val SurfaceContainerHigh = Color(0xFFFFE3D4)
    val SurfaceContainerHighest = Color(0xFFFADDCD)

    val OnSurface = Color(0xFF27180F)            // Roasted cocoa (không dùng đen tuyền)
    val OnSurfaceVariant = Color(0xFF5A4137)
    val Outline = Color(0xFF8E7165)
    val OutlineVariant = Color(0xFFE2BFB2)

    val Error = Color(0xFFBA1A1A)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
}

private val WarmSavoryLightColorScheme = lightColorScheme(
    primary = MiaoColors.BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF591E00),
    secondary = MiaoColors.SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = MiaoColors.SecondaryContainer,
    onSecondaryContainer = MiaoColors.OnSecondaryContainer,
    tertiary = MiaoColors.TertiaryGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF9C),
    onTertiaryContainer = Color(0xFF3F2E00),
    error = MiaoColors.Error,
    onError = Color.White,
    errorContainer = MiaoColors.ErrorContainer,
    onErrorContainer = MiaoColors.OnErrorContainer,
    background = MiaoColors.SurfaceCanvas,
    onBackground = MiaoColors.OnSurface,
    surface = MiaoColors.SurfaceCanvas,
    onSurface = MiaoColors.OnSurface,
    surfaceVariant = MiaoColors.SurfaceContainerHighest,
    onSurfaceVariant = MiaoColors.OnSurfaceVariant,
    surfaceContainerLowest = MiaoColors.SurfaceCard,
    surfaceContainerLow = MiaoColors.SurfaceContainerLow,
    surfaceContainer = MiaoColors.SurfaceContainer,
    surfaceContainerHigh = MiaoColors.SurfaceContainerHigh,
    surfaceContainerHighest = MiaoColors.SurfaceContainerHighest,
    outline = MiaoColors.Outline,
    outlineVariant = MiaoColors.OutlineVariant,
)

private val WarmSavoryDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB597),
    onPrimary = Color(0xFF591E00),
    primaryContainer = MiaoColors.BrandOrangeDark,
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFF4FDBC8),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF1A110C),
    onBackground = Color(0xFFFFEDE5),
    surface = Color(0xFF1A110C),
    onSurface = Color(0xFFFFEDE5),
    surfaceVariant = Color(0xFF3E2D23),
    onSurfaceVariant = Color(0xFFE2BFB2),
)

val MiaoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val MiaoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun MiaoSongTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WarmSavoryDarkColorScheme else WarmSavoryLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiaoTypography,
        shapes = MiaoShapes,
        content = content,
    )
}
