package vn.miaosong.core.domain

import java.security.MessageDigest

/**
 * BR-CART-01: Đồng bộ 100% với backend `modules/pricing/domain/cartLineKey.ts`.
 * 2 tô bún "không hành" vs "nhiều hành" -> 2 dòng giỏ hàng tách biệt.
 */
object CartLineKey {
    fun buildRaw(
        menuItemId: String,
        sizeOptionId: String?,
        toppingOptionIds: List<String>,
        note: String?,
    ): String {
        val normalizedNote = (note ?: "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
        val sortedToppings = toppingOptionIds.sorted().joinToString(",")
        val sizePart = sizeOptionId ?: ""
        return "$menuItemId|$sizePart|$sortedToppings|$normalizedNote"
    }

    fun compute(
        menuItemId: String,
        sizeOptionId: String?,
        toppingOptionIds: List<String>,
        note: String?,
    ): String {
        val raw = buildRaw(menuItemId, sizeOptionId, toppingOptionIds, note)
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
