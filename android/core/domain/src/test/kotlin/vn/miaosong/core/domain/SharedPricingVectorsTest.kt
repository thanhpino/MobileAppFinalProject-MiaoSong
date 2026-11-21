package vn.miaosong.core.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import vn.miaosong.core.model.Voucher
import vn.miaosong.core.model.VoucherType
import java.io.File

/**
 * Đọc trực tiếp file shared/test-vectors/pricing.json dùng chung với Backend TypeScript
 * để đảm bảo Android (Kotlin) và Cloud Functions (TypeScript) cho ra kết quả khớp 100%.
 */
class SharedPricingVectorsTest {

    @Serializable
    data class CartLineInputJson(
        val menuItemId: String,
        val sizeOptionId: String? = null,
        val toppingOptionIds: List<String> = emptyList(),
        val note: String? = null,
    )

    @Serializable
    data class CartLineKeyVector(
        val id: String,
        val description: String,
        val lineA: CartLineInputJson,
        val lineB: CartLineInputJson,
        val expectedRawA: String,
        val expectedRawB: String,
        val expectEqual: Boolean,
    )

    @Serializable
    data class PricedItemJson(
        val basePrice: Long,
        val sizeDelta: Long,
        val toppingsDelta: List<Long>,
        val quantity: Int,
    )

    @Serializable
    data class VoucherJson(
        val type: String,
        val value: Long,
        val maxDiscount: Long? = null,
        val minOrder: Long = 0L,
    )

    @Serializable
    data class ExpectedPricingJson(
        val itemsTotal: Long,
        val toppingsTotal: Long,
        val subtotal: Long,
        val discount: Long,
        val shippingFee: Long,
        val total: Long,
    )

    @Serializable
    data class PricingVector(
        val id: String,
        val description: String,
        val items: List<PricedItemJson>,
        val voucher: VoucherJson? = null,
        val shippingFee: Long,
        val expected: ExpectedPricingJson? = null,
        val expectedError: String? = null,
        val expectedShortfall: Long? = null,
    )

    @Serializable
    data class VectorFile(
        val description: String,
        val cartLineKeyVectors: List<CartLineKeyVector>,
        val pricingVectors: List<PricingVector>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadVectors(): VectorFile {
        // Working dir khi chạy Gradle test trong :core:domain là android/core/domain
        val candidates = listOf(
            File("../../../shared/test-vectors/pricing.json"),
            File("../shared/test-vectors/pricing.json"),
            File("shared/test-vectors/pricing.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Không tìm thấy shared/test-vectors/pricing.json từ ${File(".").absolutePath}")
        return json.decodeFromString(VectorFile.serializer(), file.readText(Charsets.UTF_8))
    }

    @Test
    fun `cartLineKeyVectors khop 100 phan tram voi backend`() {
        val data = loadVectors()
        for (v in data.cartLineKeyVectors) {
            val rawA = CartLineKey.buildRaw(
                v.lineA.menuItemId,
                v.lineA.sizeOptionId,
                v.lineA.toppingOptionIds,
                v.lineA.note,
            )
            val rawB = CartLineKey.buildRaw(
                v.lineB.menuItemId,
                v.lineB.sizeOptionId,
                v.lineB.toppingOptionIds,
                v.lineB.note,
            )
            assertEquals("${v.id} rawA", v.expectedRawA, rawA)
            assertEquals("${v.id} rawB", v.expectedRawB, rawB)

            val keyA = CartLineKey.compute(
                v.lineA.menuItemId,
                v.lineA.sizeOptionId,
                v.lineA.toppingOptionIds,
                v.lineA.note,
            )
            val keyB = CartLineKey.compute(
                v.lineB.menuItemId,
                v.lineB.sizeOptionId,
                v.lineB.toppingOptionIds,
                v.lineB.note,
            )
            assertEquals("${v.id} equality", v.expectEqual, keyA == keyB)
        }
    }

    @Test
    fun `pricingVectors khop 100 phan tram voi backend`() {
        val data = loadVectors()
        for (v in data.pricingVectors) {
            val lines = v.items.map {
                PricedLineInput(it.basePrice, it.sizeDelta, it.toppingsDelta, it.quantity)
            }
            val voucher = v.voucher?.let {
                Voucher(
                    id = "test",
                    code = "TEST",
                    type = VoucherType.valueOf(it.type),
                    value = it.value,
                    maxDiscount = it.maxDiscount,
                    minOrder = it.minOrder,
                )
            }

            if (v.expectedError != null) {
                try {
                    PricingCalculator.calculate(lines, voucher, v.shippingFee)
                    fail("${v.id}: Expected error ${v.expectedError}")
                } catch (e: PricingException) {
                    assertEquals("${v.id} errorCode", v.expectedError, e.code)
                    if (v.expectedShortfall != null) {
                        assertEquals("${v.id} shortfall", v.expectedShortfall, e.shortfall)
                    }
                }
            } else {
                val actual = PricingCalculator.calculate(lines, voucher, v.shippingFee)
                val exp = v.expected!!
                assertEquals("${v.id} itemsTotal", exp.itemsTotal, actual.itemsTotal)
                assertEquals("${v.id} toppingsTotal", exp.toppingsTotal, actual.toppingsTotal)
                assertEquals("${v.id} subtotal", exp.subtotal, actual.subtotal)
                assertEquals("${v.id} discount", exp.discount, actual.discount)
                assertEquals("${v.id} shippingFee", exp.shippingFee, actual.shippingFee)
                assertEquals("${v.id} total", exp.total, actual.total)
            }
        }
    }
}
