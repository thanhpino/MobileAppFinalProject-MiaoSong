package vn.miaosong.feature.customer.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.domain.CartLineKey
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.MenuOption
import vn.miaosong.core.model.OptionGroup
import vn.miaosong.core.model.OptionGroupType

private val DEFAULT_SIZES = listOf(
    MenuOption(id = "size_m", name = "Size Vừa (M)", priceDelta = 0L),
    MenuOption(id = "size_l", name = "Size Lớn (L)", priceDelta = 12_000L),
    MenuOption(id = "size_special", name = "Size Đặc Biệt Siêu Thịt", priceDelta = 22_000L),
)

private val DEFAULT_TOPPINGS = listOf(
    MenuOption(id = "top_cha_cua", name = "Thêm 1 Chả Cua Huế", priceDelta = 10_000L),
    MenuOption(id = "top_tiet", name = "Thêm Tiết Luộc Mềm", priceDelta = 5_000L),
    MenuOption(id = "top_bap_bo", name = "Thêm Bắp Bò Hoa", priceDelta = 15_000L),
    MenuOption(id = "top_rau_trung", name = "Đĩa Rau Trụng Thêm", priceDelta = 5_000L),
)

/**
 * P2-03: Modal Bottom Sheet tùy chỉnh món ăn
 * Chuẩn Warm Savory Delivery (Stitch):
 * - Header: thumbnail món, badge BÁN CHẠY, tên món, mô tả, giá chuẩn, nút close
 * - Nhóm 1 (SINGLE - Bắt buộc): Chọn kích cỡ (Radio indicator)
 * - Nhóm 2 (MULTI - Tùy chọn): Món ăn kèm & Topping (Checkbox)
 * - Nhóm 3: Ghi chú đặc biệt (max 150 ký tự) kèm Quick chips (+ Không hành, + Nhiều hành,...)
 * - Bottom Bar cố định: Stepper số lượng + Nút CTA cam "Thêm vào giỏ [Tổng tiền]"
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemCustomizerBottomSheet(
    item: MenuItem,
    storeId: String,
    onDismiss: () -> Unit,
    onAddToCart: (CartLine) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Khởi tạo nhóm Size và Topping từ item hoặc fallback nếu quán chưa định nghĩa
    val sizeGroup = item.optionGroups.firstOrNull { it.type == OptionGroupType.SINGLE }
    val availableSizes = sizeGroup?.options?.ifEmpty { DEFAULT_SIZES } ?: DEFAULT_SIZES

    val toppingGroup = item.optionGroups.firstOrNull { it.type == OptionGroupType.MULTI }
    val availableToppings = toppingGroup?.options?.ifEmpty { DEFAULT_TOPPINGS } ?: DEFAULT_TOPPINGS

    // Trạng thái chọn
    var selectedSize by remember { mutableStateOf(availableSizes.firstOrNull()) }
    var selectedToppings by remember { mutableStateOf<Set<MenuOption>>(emptySet()) }
    var specialNote by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    // Tính đơn giá và thành tiền realtime
    val unitPrice = item.basePrice + (selectedSize?.priceDelta ?: 0L) + selectedToppings.sumOf { it.priceDelta }
    val lineTotalPrice = unitPrice * quantity
    val canAddToCart = selectedSize != null && quantity >= 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MiaoColors.OutlineVariant.copy(alpha = 0.7f)),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header món ăn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Thumbnail món có badge BÁN CHẠY
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
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
                        Text(text = "🍜", fontSize = 36.sp)
                    }

                    Surface(
                        color = MiaoColors.BrandOrange,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "BÁN CHẠY",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                // Tên món, mô tả, giá chuẩn
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = MiaoColors.OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = formatVnd(item.basePrice),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MiaoColors.BrandOrange,
                        )
                        Surface(
                            color = MiaoColors.SurfaceContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Giá chuẩn",
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Nút đóng
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MiaoColors.SurfaceContainer, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MiaoColors.SurfaceContainerLow),
            )

            // Danh sách các nhóm tùy chọn (Cuộn dọc)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Nhóm SINGLE: Kích cỡ (Bắt buộc)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Chọn kích cỡ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiaoColors.OnSurface,
                            )
                            Surface(
                                color = MiaoColors.ErrorContainer,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "BẮT BUỘC",
                                    color = MiaoColors.Error,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Surface(
                            color = MiaoColors.SurfaceContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Chọn 1",
                                color = MiaoColors.OnSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    availableSizes.forEach { sizeOption ->
                        val isSelected = selectedSize?.id == sizeOption.id
                        Surface(
                            color = if (isSelected) MiaoColors.SurfaceContainerLow else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MiaoColors.BrandOrange else MiaoColors.OutlineVariant.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSize = sizeOption },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    // Custom Radio Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MiaoColors.BrandOrange else MiaoColors.SurfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                            )
                                        }
                                    }
                                    Text(
                                        text = sizeOption.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MiaoColors.OnSurface,
                                    )
                                }
                                Text(
                                    text = if (sizeOption.priceDelta > 0) "+${formatVnd(sizeOption.priceDelta)}" else "+0đ",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (sizeOption.priceDelta > 0) MiaoColors.BrandOrange else MiaoColors.OnSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MiaoColors.OnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Chưa chọn mục bắt buộc sẽ khóa nút Thêm",
                            fontSize = 11.sp,
                            color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                HorizontalDivider(color = MiaoColors.SurfaceContainerLow, thickness = 1.dp)

                // 2. Nhóm MULTI: Món ăn kèm & Topping (Tùy chọn)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Món ăn kèm & Topping",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiaoColors.OnSurface,
                            )
                            Surface(
                                color = MiaoColors.SecondaryContainer,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "TÙY CHỌN",
                                    color = MiaoColors.OnSecondaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFE6FAF8),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Đã chọn ${selectedToppings.size} / tối đa 4",
                                color = MiaoColors.SecondaryTeal,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    availableToppings.forEach { topping ->
                        val isChecked = selectedToppings.contains(topping)
                        Surface(
                            color = if (isChecked) MiaoColors.SurfaceContainerLow else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) MiaoColors.SecondaryTealBright else MiaoColors.OutlineVariant.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedToppings = if (isChecked) {
                                        selectedToppings - topping
                                    } else {
                                        if (selectedToppings.size < 4) selectedToppings + topping else selectedToppings
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    // Custom Checkbox Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isChecked) MiaoColors.SecondaryTeal else MiaoColors.SurfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = topping.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = MiaoColors.OnSurface,
                                    )
                                }
                                Text(
                                    text = "+${formatVnd(topping.priceDelta)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.BrandOrange,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MiaoColors.SurfaceContainerLow, thickness = 1.dp)

                // 3. Ghi chú cho quán (max 150 ký tự + Quick chips)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Ghi chú cho quán",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiaoColors.OnSurface,
                            )
                            Text(
                                text = "(Không bắt buộc)",
                                fontSize = 11.sp,
                                color = MiaoColors.OnSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${specialNote.length} / 150 ký tự",
                            fontSize = 11.sp,
                            color = MiaoColors.OnSurfaceVariant,
                        )
                    }

                    // Ô nhập ghi chú
                    Surface(
                        color = MiaoColors.SurfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (specialNote.isEmpty()) {
                                Text(
                                    text = "Ví dụ: Ít cay, không lấy tiết, xin thêm nước béo...",
                                    fontSize = 12.5.sp,
                                    color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            BasicTextField(
                                value = specialNote,
                                onValueChange = {
                                    if (it.length <= 150) specialNote = it
                                },
                                textStyle = TextStyle(
                                    color = MiaoColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(MiaoColors.BrandOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            )
                        }
                    }

                    // Quick Chips chọn nhanh 1 chạm
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            "Không hành",
                            "Nhiều hành",
                            "Dị ứng đậu phộng",
                            "Để nước lèo riêng",
                            "Ít cay",
                        ).forEach { chipText ->
                            val isAppended = specialNote.contains(chipText)
                            Surface(
                                color = if (isAppended) Color(0xFFFFEDD5) else MiaoColors.SurfaceContainer,
                                shape = RoundedCornerShape(999.dp),
                                border = if (isAppended) BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.4f)) else null,
                                modifier = Modifier.clickable {
                                    specialNote = if (isAppended) {
                                        specialNote.replace(chipText, "").replace(", ,", ",").trim(' ', ',')
                                    } else {
                                        if (specialNote.isBlank()) chipText else "$specialNote, $chipText"
                                    }
                                },
                            ) {
                                Text(
                                    text = "+ $chipText",
                                    color = if (isAppended) MiaoColors.BrandOrangeDark else MiaoColors.OnSurface,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isAppended) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 4. Thanh cố định đáy (Bottom Action Bar)
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MiaoColors.SurfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Stepper số lượng (+ / -)
                    Surface(
                        color = MiaoColors.SurfaceContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(3.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (quantity > 1) Color.White else Color.White.copy(alpha = 0.5f))
                                    .clickable(enabled = quantity > 1) { if (quantity > 1) quantity-- },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Giảm",
                                    tint = if (quantity > 1) MiaoColors.BrandOrange else MiaoColors.OnSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            Text(
                                text = "$quantity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiaoColors.OnSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 32.dp),
                            )

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MiaoColors.BrandOrange)
                                    .clickable { quantity++ },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tăng",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Nút Thêm vào giỏ
                    Button(
                        onClick = {
                            if (canAddToCart) {
                                val toppingIds = selectedToppings.map { it.id }
                                val toppingNames = selectedToppings.map { it.name }
                                val toppingsDelta = selectedToppings.map { it.priceDelta }

                                val lineKey = CartLineKey.compute(
                                    menuItemId = item.id,
                                    sizeOptionId = selectedSize?.id,
                                    toppingOptionIds = toppingIds,
                                    note = specialNote,
                                )

                                val cartLine = CartLine(
                                    lineKey = lineKey,
                                    storeId = storeId,
                                    menuItemId = item.id,
                                    itemName = item.name,
                                    imageUrl = item.imageUrl,
                                    basePrice = item.basePrice,
                                    sizeOptionId = selectedSize?.id,
                                    sizeName = selectedSize?.name.orEmpty(),
                                    sizeDelta = selectedSize?.priceDelta ?: 0L,
                                    toppingOptionIds = toppingIds,
                                    toppingNames = toppingNames,
                                    toppingsDelta = toppingsDelta,
                                    note = specialNote.trim(),
                                    quantity = quantity,
                                )
                                onAddToCart(cartLine)
                            }
                        },
                        enabled = canAddToCart,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiaoColors.BrandOrange,
                            contentColor = Color.White,
                            disabledContainerColor = MiaoColors.BrandOrange.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = "Thêm vào giỏ • ${formatVnd(lineTotalPrice)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun formatVnd(amount: Long): String {
    return "%,dđ".format(amount).replace(',', '.')
}
