package vn.miaosong.core.domain

import vn.miaosong.core.model.PriceBreakdown
import vn.miaosong.core.model.Voucher
import vn.miaosong.core.model.VoucherType

class PricingException(
    val code: String,
    override val message: String,
    val shortfall: Long? = null,
) : RuntimeException(message)

data class PricedLineInput(
    val basePrice: Long,
    val sizeDelta: Long,
    val toppingsDelta: List<Long>,
    val quantity: Int,
)

/**
 * BR-PRICE-01 & BR-PRICE-02: Đồng bộ 100% với backend `pricingCalculator.ts`.
 * Tổng = (Tiền món + Tiền món thêm) - min(Giảm giá, Subtotal) + Phí ship.
 * Voucher KHÔNG BAO GIỜ trừ vào phí giao hàng.
 */
object PricingCalculator {
    fun calculate(
        lines: List<PricedLineInput>,
        voucher: Voucher?,
        shippingFee: Long,
    ): PriceBreakdown {
        var itemsTotal = 0L
        var toppingsTotal = 0L

        for (line in lines) {
            itemsTotal += (line.basePrice + line.sizeDelta) * line.quantity
            toppingsTotal += line.toppingsDelta.sum() * line.quantity
        }

        val subtotal = itemsTotal + toppingsTotal
        val discount = if (voucher != null) calculateDiscount(voucher, subtotal) else 0L
        val total = subtotal - discount + shippingFee

        return PriceBreakdown(
            itemsTotal = itemsTotal,
            toppingsTotal = toppingsTotal,
            subtotal = subtotal,
            discount = discount,
            shippingFee = shippingFee,
            total = total,
        )
    }

    fun calculateDiscount(voucher: Voucher, subtotal: Long): Long {
        if (subtotal < voucher.minOrder) {
            val shortfall = voucher.minOrder - subtotal
            throw PricingException(
                code = "VOUCHER_MIN_ORDER",
                message = "Đơn tối thiểu ${voucher.minOrder}đ, thiếu ${shortfall}đ.",
                shortfall = shortfall,
            )
        }
        val raw = when (voucher.type) {
            VoucherType.PERCENT -> {
                val pct = (subtotal * voucher.value) / 100L
                val maxD = voucher.maxDiscount
                if (maxD != null && maxD > 0L) minOf(pct, maxD) else pct
            }
            VoucherType.FIXED -> voucher.value
        }
        return raw.coerceIn(0L, subtotal)
    }
}
