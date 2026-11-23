package vn.miaosong.feature.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.miaosong.core.designsystem.theme.MiaoColors
import vn.miaosong.core.model.DeliveryAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPickerBottomSheet(
    addresses: List<DeliveryAddress>,
    selectedAddress: DeliveryAddress?,
    onSelectAddress: (DeliveryAddress) -> Unit,
    onAddNewAddress: (label: String, fullText: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAddingNew by remember { mutableStateOf(false) }

    // Form thêm địa chỉ mới
    var newLabel by remember { mutableStateOf("Nhà riêng") }
    var newFullText by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MiaoColors.BrandOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = if (isAddingNew) "Thêm địa chỉ mới" else "Địa chỉ giao hàng",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.OnSurface,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(
                color = MiaoColors.OutlineVariant.copy(alpha = 0.5f),
                thickness = 0.75.dp,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (isAddingNew) {
                // Form thêm địa chỉ mới
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Chọn nhãn gợi nhớ (Nhà riêng / Công ty / Khác)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Tên gợi nhớ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.OnSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Nhà riêng", "Công ty", "Ký túc xá", "Khác").forEach { tag ->
                                val isSelected = newLabel == tag
                                Surface(
                                    color = if (isSelected) MiaoColors.BrandOrange else MiaoColors.SurfaceContainerLow,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MiaoColors.BrandOrange else MiaoColors.OutlineVariant,
                                    ),
                                    modifier = Modifier.clickable { newLabel = tag },
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) Color.White else MiaoColors.OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Địa chỉ chi tiết
                    OutlinedTextField(
                        value = newFullText,
                        onValueChange = {
                            newFullText = it
                            formError = null
                        },
                        label = { Text("Số nhà, tên đường, phường, quận...") },
                        placeholder = { Text("Ví dụ: 19 Nguyễn Hữu Thọ, Tân Phong, Q.7") },
                        isError = formError != null,
                        supportingText = formError?.let { { Text(it, color = MiaoColors.BrandOrangeDark) } },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiaoColors.BrandOrange,
                            focusedLabelColor = MiaoColors.BrandOrange,
                        ),
                    )

                    // Ghi chú người giao hàng
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("Ghi chú cho tài xế (tùy chọn)") },
                        placeholder = { Text("Ví dụ: Gọi trước khi đến, gửi bảo vệ...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiaoColors.BrandOrange,
                            focusedLabelColor = MiaoColors.BrandOrange,
                        ),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { isAddingNew = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MiaoColors.SurfaceContainerHigh,
                                contentColor = MiaoColors.OnSurface,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Quay lại", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (newFullText.trim().length < 5) {
                                    formError = "Vui lòng nhập địa chỉ đầy đủ (tối thiểu 5 ký tự)"
                                } else {
                                    onAddNewAddress(newLabel, newFullText.trim(), newNote.trim())
                                    isAddingNew = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MiaoColors.BrandOrange,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f),
                        ) {
                            Text("Lưu địa chỉ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Danh sách địa chỉ đã lưu
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(addresses, key = { it.id }) { address ->
                        val isSelected = address.id == selectedAddress?.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFFF6ED) else Color.White,
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MiaoColors.BrandOrange else MiaoColors.OutlineVariant.copy(alpha = 0.6f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAddress(address)
                                    onDismiss()
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Radio indicator
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MiaoColors.BrandOrange else MiaoColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp),
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = address.label.ifBlank { "Địa chỉ nhận" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MiaoColors.OnSurface,
                                        )

                                        if (address.isDefault) {
                                            Surface(
                                                color = MiaoColors.BrandOrange.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = "Mặc định",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MiaoColors.BrandOrange,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = address.fullText,
                                        fontSize = 12.5.sp,
                                        color = MiaoColors.OnSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    if (address.recipientName.isNotBlank() || address.recipientPhone.isNotBlank()) {
                                        Text(
                                            text = "${address.recipientName} • ${address.recipientPhone}",
                                            fontSize = 11.5.sp,
                                            color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.8f),
                                        )
                                    }

                                    if (address.note.isNotBlank()) {
                                        Text(
                                            text = "Ghi chú: ${address.note}",
                                            fontSize = 11.sp,
                                            color = MiaoColors.BrandOrangeDark,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Nút thêm địa chỉ mới
                    item {
                        Surface(
                            color = MiaoColors.SurfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAddingNew = true },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MiaoColors.BrandOrange,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thêm địa chỉ giao hàng mới",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.BrandOrange,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
