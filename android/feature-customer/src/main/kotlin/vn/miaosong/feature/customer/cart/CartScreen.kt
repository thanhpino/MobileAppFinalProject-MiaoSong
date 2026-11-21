package vn.miaosong.feature.customer.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.PriceBreakdown
import vn.miaosong.core.model.Voucher
import vn.miaosong.feature.customer.AddressPickerBottomSheet

private const val DEFAULT_STORE_AVATAR =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDLEGTUDj1U_Yg11MfTv01vY_3AF_TF1ilrOTa0QYJDXkEjB2s9pLTEReU_XCfyVoTyXJo0-kGTiFUK-NolEEMFXJUUkBnNP63g97dPZEVofOtqWwywcKNYST6_QkTJ9nZoAxUuCD7YrBZLfgU4NZVVeXQnRpx6EdyIbVSI4sxXHPTmWpJlM_5zc3rC4k10r3U8w22Gw-jC7ahY7tLQ_DZMT3xl8an6mM7CBAleZ5Q"

/**
 * P2-04: Màn hình Giỏ hàng & Thanh toán (Working Shopping Cart)
 * Chuẩn Warm Savory Delivery (Stitch):
 * - Top Navigation: Back button + Title "Giỏ hàng & Thanh toán"
 * - Store Info Card: Logo quán, tên quán, địa chỉ, badge "Đơn tách dòng tự động"
 * - Cart Items List: Mỗi phần ăn có nhãn Tô #1, Tô #2... kèm badge ghi chú riêng, nút xóa, stepper độc lập
 * - Nút "＋ Thêm món"
 * - Thẻ Ưu đãi & Voucher (áp mã COBA25 giảm 25.000đ)
 * - Thẻ Địa chỉ nhận hàng & Phương thức thanh toán (COD / ZaloPay)
 * - Bảng kê chi tiết thanh toán (Tạm tính, Phí ship, Giảm giá, Tổng thanh toán)
 * - Fixed Bottom Checkout Bar: Nút "Đặt món" to pill cam
 */
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onOrderSuccess: (Order) -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isAddressPickerVisible by remember { mutableStateOf(false) }

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
            CartTopHeader(onBack = onBack)
        },
        bottomBar = {
            if (uiState.cartLines.isNotEmpty()) {
                CartBottomCheckoutBar(
                    breakdown = uiState.breakdown,
                    isPlacingOrder = uiState.isPlacingOrder,
                    onPlaceOrder = { viewModel.placeOrder(onOrderSuccess) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.cartLines.isEmpty()) {
                // Empty state khi giỏ hàng trống
                EmptyCartView(onDiscoverClick = onAddMore)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 1. Store Header & Badge tự động tách dòng
                    item {
                        StoreCartBadgeHeader(storeName = "Bún Bò Huế Cô Ba")
                    }

                    // 2. Thẻ thông tin quán
                    item {
                        StoreInfoCard(
                            storeName = "Bún Bò Huế Cô Ba",
                            address = "Ngõ 42 Tràng Tiền, Hoàn Kiếm • 1.2km",
                        )
                    }

                    // 3. Danh sách các món ăn trong giỏ
                    itemsIndexed(uiState.cartLines, key = { _, line -> line.lineKey }) { index, line ->
                        CartLineCard(
                            index = index + 1,
                            line = line,
                            onIncrement = { viewModel.incrementQuantity(line.lineKey, line.quantity) },
                            onDecrement = { viewModel.decrementQuantity(line.lineKey, line.quantity) },
                            onRemove = { viewModel.removeLine(line.lineKey) },
                        )
                    }

                    // 4. Nút Thêm món
                    item {
                        Surface(
                            color = MiaoColors.SurfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddMore() },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = MiaoColors.BrandOrange,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "＋ Thêm món ngon khác",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.OnSurface,
                                )
                            }
                        }
                    }

                    // 5. Thẻ Ưu đãi & Voucher
                    item {
                        VoucherAppliedCard(
                            voucher = uiState.selectedVoucher,
                            onToggleVoucher = { viewModel.toggleVoucher("COBA25") },
                        )
                    }

                    // 6. Thẻ Địa chỉ nhận hàng
                    item {
                        DeliveryAddressCard(
                            address = uiState.selectedAddress,
                            onClick = { isAddressPickerVisible = true },
                        )
                    }

                    // 7. Thẻ Phương thức thanh toán
                    item {
                        PaymentMethodCard(
                            selectedMethod = uiState.paymentMethod,
                            onSelect = viewModel::selectPaymentMethod,
                        )
                    }

                    // 8. Bảng kê chi tiết thanh toán
                    item {
                        PaymentBreakdownCard(
                            breakdown = uiState.breakdown,
                            itemCount = uiState.cartLines.sumOf { it.quantity },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Toast notification nổi ở trên
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

    // Modal chọn & đổi địa chỉ nhận hàng trong Giỏ hàng
    if (isAddressPickerVisible) {
        AddressPickerBottomSheet(
            addresses = uiState.addresses,
            selectedAddress = uiState.selectedAddress,
            onSelectAddress = { addr ->
                viewModel.selectAddress(addr)
                isAddressPickerVisible = false
            },
            onAddNewAddress = { label, fullText, note ->
                viewModel.addNewAddress(label, fullText, note)
                isAddressPickerVisible = false
            },
            onDismiss = { isAddressPickerVisible = false },
        )
    }
}

@Composable
private fun CartTopHeader(onBack: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                Text(
                    text = "Giỏ hàng & Thanh toán",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MiaoColors.OnSurface,
                )
            }
        }
    }
}

@Composable
private fun StoreCartBadgeHeader(storeName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = Color(0xFFFFDBCD),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = storeName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.BrandOrangeDark,
                    )
                }
            }

            Surface(
                color = Color(0xFFE6FAF8),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "Đơn tách dòng tự động",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiaoColors.SecondaryTeal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StoreInfoCard(storeName: String, address: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MiaoColors.SurfaceContainerHigh,
                    modifier = Modifier.size(46.dp),
                ) {
                    AsyncImage(
                        model = DEFAULT_STORE_AVATAR,
                        contentDescription = storeName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = storeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.OnSurface,
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = address,
                        fontSize = 12.sp,
                        color = MiaoColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MiaoColors.SecondaryTeal,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Giao siêu tốc 15 phút",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.SecondaryTeal,
                    )
                }

                Surface(
                    color = MiaoColors.SurfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "Mở cửa đến 22:30",
                        fontSize = 10.5.sp,
                        color = MiaoColors.OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    index: Int,
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Tô #1, Tô #2... + nút xóa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeColor = if (index % 2 == 1) Color(0xFFFFDBCD) else MiaoColors.SecondaryContainer
                val textColor = if (index % 2 == 1) MiaoColors.BrandOrangeDark else MiaoColors.OnSecondaryContainer
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "Phần #$index",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa món",
                        tint = MiaoColors.OnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Chi tiết món ăn
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MiaoColors.SurfaceContainerHigh,
                    modifier = Modifier.size(68.dp),
                ) {
                    if (line.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = line.imageUrl,
                            contentDescription = line.itemName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🍜", fontSize = 28.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = line.itemName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.OnSurface,
                    )

                    // Size + Topping
                    val details = buildString {
                        if (line.sizeName.isNotBlank()) append(line.sizeName)
                        if (line.toppingNames.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append(line.toppingNames.joinToString(", "))
                        }
                    }
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            fontSize = 12.sp,
                            color = MiaoColors.OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Ghi chú riêng biệt nổi bật dạng Pill
                    if (line.note.isNotBlank()) {
                        Surface(
                            color = Color(0xFFFFF2E6),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                text = "✏️ Ghi chú: ${line.note}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiaoColors.BrandOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Hàng giá & Bộ đếm số lượng độc lập
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatVnd(line.lineTotal),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MiaoColors.BrandOrange,
                )

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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onDecrement() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Giảm",
                                tint = MiaoColors.BrandOrange,
                                modifier = Modifier.size(15.dp),
                            )
                        }

                        Text(
                            text = "${line.quantity}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.OnSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 28.dp),
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MiaoColors.BrandOrange)
                                .clickable { onIncrement() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tăng",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherAppliedCard(
    voucher: Voucher?,
    onToggleVoucher: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    color = Color(0xFFFFDBCD),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MiaoColors.BrandOrange,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "COBA25",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.OnSurface,
                        )
                        if (voucher != null) {
                            Surface(
                                color = Color(0xFFE6FAF8),
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = "Đã áp dụng",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiaoColors.SecondaryTeal,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "Giảm 25.000đ cho đơn từ 60k",
                        fontSize = 11.5.sp,
                        color = MiaoColors.OnSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (voucher != null) "Đổi mã" else "Áp dụng",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MiaoColors.BrandOrange,
                modifier = Modifier.clickable { onToggleVoucher() },
            )
        }
    }
}

@Composable
private fun DeliveryAddressCard(
    address: DeliveryAddress?,
    onClick: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MiaoColors.BrandOrange,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Địa chỉ nhận món",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.OnSurface,
                    )
                }

                Text(
                    text = "Thay đổi",
                    color = MiaoColors.BrandOrange,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (address != null) {
                Text(
                    text = "${address.recipientName} • ${address.recipientPhone}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiaoColors.OnSurface,
                )
                Text(
                    text = address.fullText,
                    fontSize = 12.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
                if (address.note.isNotBlank()) {
                    Text(
                        text = "Ghi chú giao: ${address.note}",
                        fontSize = 11.5.sp,
                        color = MiaoColors.BrandOrangeDark,
                    )
                }
            } else {
                Text(
                    text = "Chưa chọn địa chỉ giao hàng",
                    fontSize = 12.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    selectedMethod: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MiaoColors.BrandOrange,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Phương thức thanh toán",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiaoColors.OnSurface,
                )
            }

            // COD Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(PaymentMethod.COD) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tiền mặt khi nhận món (COD)",
                    fontSize = 13.sp,
                    color = MiaoColors.OnSurface,
                )
                RadioButton(
                    selected = selectedMethod == PaymentMethod.COD,
                    onClick = { onSelect(PaymentMethod.COD) },
                    colors = RadioButtonDefaults.colors(selectedColor = MiaoColors.BrandOrange),
                )
            }

            // ZaloPay Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(PaymentMethod.ZALOPAY) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Ví điện tử ZaloPay (Chuyển khoản)",
                    fontSize = 13.sp,
                    color = MiaoColors.OnSurface,
                )
                RadioButton(
                    selected = selectedMethod == PaymentMethod.ZALOPAY,
                    onClick = { onSelect(PaymentMethod.ZALOPAY) },
                    colors = RadioButtonDefaults.colors(selectedColor = MiaoColors.BrandOrange),
                )
            }
        }
    }
}

@Composable
private fun PaymentBreakdownCard(breakdown: PriceBreakdown, itemCount: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MiaoColors.BrandOrange.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Chi tiết thanh toán",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tạm tính ($itemCount phần món)",
                    fontSize = 12.5.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Text(
                    text = formatVnd(breakdown.subtotal),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiaoColors.OnSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Phí giao hàng",
                    fontSize = 12.5.sp,
                    color = MiaoColors.OnSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "16.000đ",
                        fontSize = 12.sp,
                        color = MiaoColors.OnSurfaceVariant.copy(alpha = 0.6f),
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Text(
                        text = "0đ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.SecondaryTeal,
                    )
                }
            }

            if (breakdown.discount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Voucher giảm giá (COBA25)",
                        fontSize = 12.5.sp,
                        color = MiaoColors.OnSurfaceVariant,
                    )
                    Text(
                        text = "-${formatVnd(breakdown.discount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.BrandOrange,
                    )
                }
            }

            HorizontalDivider(
                color = MiaoColors.SurfaceContainerLow,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Tổng cộng",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiaoColors.OnSurface,
                    )
                    if (breakdown.discount > 0) {
                        Text(
                            text = "Tiết kiệm được ${formatVnd(breakdown.discount + 16_000L)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.SecondaryTeal,
                        )
                    }
                }
                Text(
                    text = formatVnd(breakdown.total),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MiaoColors.BrandOrange,
                )
            }
        }
    }
}

@Composable
private fun CartBottomCheckoutBar(
    breakdown: PriceBreakdown,
    isPlacingOrder: Boolean,
    onPlaceOrder: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MiaoColors.SurfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Tổng:",
                        fontSize = 12.sp,
                        color = MiaoColors.OnSurfaceVariant,
                    )
                    Text(
                        text = formatVnd(breakdown.total),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MiaoColors.BrandOrange,
                    )
                }
                if (breakdown.discount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MiaoColors.SecondaryTeal,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Đã tiết kiệm ${formatVnd(breakdown.discount + 16_000L)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiaoColors.SecondaryTeal,
                        )
                    }
                }
            }

            Button(
                onClick = onPlaceOrder,
                enabled = !isPlacingOrder && breakdown.total > 0,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiaoColors.BrandOrange,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Đặt món ngay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCartView(onDiscoverClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "🐾", fontSize = 52.sp)
            Text(
                text = "Giỏ hàng của bạn đang trống",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MiaoColors.OnSurface,
            )
            Text(
                text = "Hãy chọn vài món ngon nóng hổi từ quán quen để MiaoSong giao ngay cho bạn nhé!",
                fontSize = 12.5.sp,
                color = MiaoColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onDiscoverClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiaoColors.BrandOrange,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Khám phá món ngon ngay",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatVnd(amount: Long): String {
    return "%,dđ".format(amount).replace(',', '.')
}
