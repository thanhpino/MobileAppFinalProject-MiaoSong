package vn.miaosong.feature.customer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lưu trạng thái Danh sách Quán yêu thích dùng chung giữa StoreDetailScreen và ProfileScreen.
 */
object FavoriteStoresHolder {
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun isFavorite(storeId: String): Boolean = storeId in _favoriteIds.value

    fun toggle(storeId: String): Boolean {
        var added = false
        _favoriteIds.update { current ->
            if (storeId in current) {
                added = false
                current - storeId
            } else {
                added = true
                current + storeId
            }
        }
        return added
    }
}
