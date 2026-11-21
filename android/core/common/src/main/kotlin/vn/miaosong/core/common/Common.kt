package vn.miaosong.core.common

import java.text.NumberFormat
import java.util.Locale

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(
        val code: String,
        val message: String,
        val details: Map<String, Any?> = emptyMap(),
    ) : AppResult<Nothing>
}

object MoneyFormatter {
    private val vnLocale = Locale("vi", "VN")

    fun formatVnd(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(vnLocale)
        return "${nf.format(amount)}đ"
    }
}
