package vn.miaosong.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.miaosong.core.designsystem.theme.MiaoColors

/** Nút CTA chính dạng pill cam Warm Savory Delivery (touch target >= 52dp) */
@Composable
fun MiaoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Thẻ cảnh báo ghi chú đặc biệt (vàng) hoặc dị ứng (đỏ) dùng cho Màn hình Bếp (KDS) */
@Composable
fun KitchenNoteBadge(
    note: String,
    isAllergy: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (isAllergy) MiaoColors.KitchenAllergyBg else MiaoColors.KitchenNoteBg
    val fg = if (isAllergy) MiaoColors.KitchenAllergyText else MiaoColors.OnSurface
    val prefix = if (isAllergy) "🚫 DỊ ỨNG: " else "⚠️ GHI CHÚ: "

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
    ) {
        Text(
            text = "$prefix$note",
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
