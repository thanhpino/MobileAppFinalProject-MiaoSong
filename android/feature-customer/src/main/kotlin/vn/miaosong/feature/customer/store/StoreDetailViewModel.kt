package vn.miaosong.feature.customer.store

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.miaosong.core.domain.CartLineKey
import vn.miaosong.core.domain.CartRepository
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.MenuOption
import vn.miaosong.core.model.OptionGroup
import vn.miaosong.core.model.OptionGroupType
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.Voucher
import vn.miaosong.feature.customer.FavoriteStoresHolder
import java.text.Normalizer
import java.util.regex.Pattern
import javax.inject.Inject

data class StoreDetailUiState(
    val isLoading: Boolean = true,
    val storeId: String = "",
    val store: Store? = null,
    val categories: List<MenuCategory> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val filteredMenuItems: List<MenuItem> = emptyList(),
    val vouchers: List<Voucher> = emptyList(),
    val selectedCategoryPill: String = "bestseller",
    val isFavorited: Boolean = false,
    val isVoucherClaimed: Boolean = false,
    val cartItemCount: Int = 0,
    val cartTotalVnd: Long = 0L,
    val cartSummaryPreview: String = "",
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
)

private data class StoreContent(
    val store: Store?,
    val categories: List<MenuCategory>,
    val menuItems: List<MenuItem>,
    val vouchers: List<Voucher>,
)

private data class LocalControlState(
    val selectedCategoryPill: String = "bestseller",
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val isFavorited: Boolean = false,
    val isVoucherClaimed: Boolean = false,
    val toastMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoreDetailViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val cartRepository: CartRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialStoreId: String = savedStateHandle.get<String>("storeId") ?: "store_bun_rieu_co_ba"
    private val _storeIdFlow = MutableStateFlow(initialStoreId)
    private val _localState = MutableStateFlow(LocalControlState())

    val uiState: StateFlow<StoreDetailUiState> = _storeIdFlow.flatMapLatest { storeId ->
        val storeContentFlow = combine(
            storeRepository.observeStore(storeId),
            storeRepository.observeCategories(storeId),
            storeRepository.observeMenuItems(storeId),
            storeRepository.observeVouchers(storeId),
        ) { store, categories, items, vouchers ->
            StoreContent(store, categories, items, vouchers)
        }

        combine(
            storeContentFlow,
            cartRepository.observeCart(),
            FavoriteStoresHolder.favoriteIds,
            _localState,
        ) { content, cartLines, favIds, local ->
            val effectiveCategories = if (content.categories.isNotEmpty()) content.categories else DEFAULT_CATEGORIES
            val effectiveItems = if (content.menuItems.isNotEmpty()) content.menuItems else DEFAULT_ITEMS

            // Lọc món theo từ khóa tìm kiếm & danh mục được chọn
            val filtered = effectiveItems.filter { item ->
                val matchesQuery = if (local.searchQuery.isBlank()) true else {
                    item.name.unaccent().contains(local.searchQuery.unaccent(), ignoreCase = true) ||
                        item.description.unaccent().contains(local.searchQuery.unaccent(), ignoreCase = true)
                }

                val matchesPill = when (local.selectedCategoryPill) {
                    "bestseller" -> true
                    "main" -> item.categoryId.contains("main", ignoreCase = true) ||
                        item.name.contains("Bún", ignoreCase = true) ||
                        item.name.contains("Cơm", ignoreCase = true)
                    "side" -> item.categoryId.contains("side", ignoreCase = true) ||
                        item.name.contains("Topping", ignoreCase = true) ||
                        item.name.contains("Chả", ignoreCase = true)
                    "drinks" -> item.categoryId.contains("drinks", ignoreCase = true) ||
                        item.name.contains("Trà", ignoreCase = true) ||
                        item.name.contains("Nước", ignoreCase = true) ||
                        item.name.contains("Sữa", ignoreCase = true)
                    else -> item.categoryId == local.selectedCategoryPill
                }

                matchesQuery && matchesPill
            }

            // Tính tổng giỏ hàng cho quán này (hoặc toàn bộ nếu giỏ hàng đơn quán)
            val storeCartLines = cartLines.filter { it.storeId == storeId || it.storeId.isEmpty() }
            val cartCount = storeCartLines.sumOf { it.quantity }
            val cartTotal = storeCartLines.sumOf { it.lineTotal }
            val preview = storeCartLines.joinToString(", ") { it.itemName }

            StoreDetailUiState(
                isLoading = false,
                storeId = storeId,
                store = content.store,
                categories = effectiveCategories,
                menuItems = effectiveItems,
                filteredMenuItems = filtered,
                vouchers = content.vouchers,
                selectedCategoryPill = local.selectedCategoryPill,
                isFavorited = storeId in favIds,
                isVoucherClaimed = local.isVoucherClaimed,
                cartItemCount = cartCount,
                cartTotalVnd = cartTotal,
                cartSummaryPreview = preview,
                searchQuery = local.searchQuery,
                isSearchVisible = local.isSearchVisible,
                toastMessage = local.toastMessage,
                errorMessage = null,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StoreDetailUiState(isLoading = true, storeId = initialStoreId),
    )

    fun initStoreId(storeId: String) {
        if (storeId.isNotBlank() && storeId != _storeIdFlow.value) {
            _storeIdFlow.value = storeId
        }
    }

    fun selectCategoryPill(pillId: String) {
        _localState.update { it.copy(selectedCategoryPill = pillId) }
    }

    fun onSearchQueryChange(query: String) {
        _localState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearchVisibility() {
        _localState.update { current ->
            val nextVisible = !current.isSearchVisible
            current.copy(
                isSearchVisible = nextVisible,
                searchQuery = if (!nextVisible) "" else current.searchQuery,
            )
        }
    }

    fun toggleFavorite() {
        val added = FavoriteStoresHolder.toggle(_storeIdFlow.value)
        _localState.update { current ->
            current.copy(
                isFavorited = added,
                toastMessage = if (added) "Đã lưu quán vào danh sách yêu thích ❤️" else "Đã xóa khỏi danh sách yêu thích",
            )
        }
    }

    fun claimVoucher(voucherCode: String = "COBA25") {
        _localState.update {
            it.copy(
                isVoucherClaimed = true,
                toastMessage = "Đã lưu mã $voucherCode thành công! 🎉",
            )
        }
    }

    fun addToCart(line: CartLine) {
        viewModelScope.launch {
            val targetItem = uiState.value.menuItems.firstOrNull { it.id == line.menuItemId }
            if (targetItem != null && !targetItem.isAvailable) {
                showToast("Món \"${line.itemName}\" hiện đang tạm hết hàng")
                return@launch
            }
            cartRepository.upsertLine(line)
            showToast("Đã thêm \"${line.itemName}\" vào giỏ hàng 🐾")
        }
    }

    fun quickAddToCart(item: MenuItem) {
        if (!item.isAvailable) {
            showToast("Món \"${item.name}\" hiện đang tạm hết hàng")
            return
        }
        viewModelScope.launch {
            val storeId = _storeIdFlow.value
            val lineKey = CartLineKey.compute(item.id, null, emptyList(), null)
            val line = CartLine(
                lineKey = lineKey,
                storeId = storeId,
                menuItemId = item.id,
                itemName = item.name,
                imageUrl = item.imageUrl,
                basePrice = item.basePrice,
                quantity = 1,
            )
            cartRepository.upsertLine(line)
            showToast("Đã thêm \"${item.name}\" vào giỏ hàng")
        }
    }

    fun showToast(msg: String) {
        _localState.update { it.copy(toastMessage = msg) }
    }

    fun dismissToast() {
        _localState.update { it.copy(toastMessage = null) }
    }

    private fun String.unaccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D').lowercase()
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            MenuCategory(id = "bestseller", name = "Bán chạy nhất", sortOrder = 0),
            MenuCategory(id = "main", name = "Món chính", sortOrder = 1),
            MenuCategory(id = "side", name = "Món thêm & Topping", sortOrder = 2),
            MenuCategory(id = "drinks", name = "Thức uống", sortOrder = 3),
        )

        val DEFAULT_ITEMS = listOf(
            MenuItem(
                id = "dish_1",
                categoryId = "main",
                name = "Bún Bò Huế Đặc Biệt Cô Ba",
                description = "Thịt bắp bò hoa mềm tan, chả cua Huế nguyên chất, nạm mềm, gân giòn, nước dùng cay the thơm sả chuẩn vị cố đô.",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBBVh_q7NEC4Dm6gzcSsG3DmZX0eOm-fgSIC4fA1_nj6SMhyv621X-N2CTGYkfYoLFKxTe8yr0L6BJT8Xmkd40lu00vCD-bOz7Qjk5s7jH5TVOikXs9Eu6PhGVnsFY_xBY1r8n2_cz7hIaPUXnFSir28sX50aWOlizhfdSiY_T9YYKO-xBcKKNqOR05OJMAPPrQevEJaLXqsuG5wc4v7VWjS_mEhROqPQqfR_q211A",
                basePrice = 55_000L,
                isAvailable = true,
                optionGroups = listOf(
                    OptionGroup(
                        id = "size_group",
                        name = "Kích cỡ",
                        type = OptionGroupType.SINGLE,
                        required = true,
                        min = 1,
                        max = 1,
                        options = listOf(
                            MenuOption("opt_reg", "Tô tiêu chuẩn", 0L),
                            MenuOption("opt_big", "Tô lớn (+10k)", 10_000L),
                        ),
                    ),
                ),
            ),
            MenuItem(
                id = "dish_2",
                categoryId = "main",
                name = "Cơm Tấm Sườn Bì Chả Trứng Ốp La",
                description = "Sườn nướng than hoa thơm lừng mỡ hành, bì thính vàng giòn, chả hấp béo ngậy cùng trứng ốp la lòng đào vàng ươm.",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCEBiL4AIGbHXLkxb8FuNt105Uiyo-yCjJyl_aYulwSvznF-WMFeATEfdS1BewJT_79vsXkamcjtjFm0k2Ol7x6fXhfkgnIMzflDM0dKyATESjoncv2YYyc6QC450rFQNkm9JKJzwazUcVsWTEHq1RVcnD1rtqGRfjBs0UXwuYbWp3dXtgtmUXqeiq0UyytMCp0FtcG-zAIFAi7WD6KIQWPjosxlrPeicLA4CUY3wU",
                basePrice = 48_000L,
                isAvailable = true,
            ),
            MenuItem(
                id = "dish_3",
                categoryId = "main",
                name = "Bún Bò Giò Heo Chả Cua",
                description = "Khoanh giò heo rút xương hầm mềm ngọt béo ngậy quyện cùng 2 viên chả cua tươi ngon đậm vị.",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCwcq0m3nINlmuB2KG0iqONcr9xO1cdiAGHK9vo-O-DQWz1pnjK0Nu1_RIQwaVrBh9N7xhlx-o16KMQ8IozPwPuN1tkWXvtlbDfpGAv3bN_sKGDLCzEG_TUd7lWBGFli6mMqC0ETSO8HPa_A2NHYPdjq_bQy7kT7x38PQ-g6z1OWiJFIg-jOuYMnR76klNR4TsQEmpDN5U8D2UmfWcqbB_gh9OgivjXWr7YoJeu7eM",
                basePrice = 52_000L,
                isAvailable = true,
            ),
            MenuItem(
                id = "dish_4",
                categoryId = "drinks",
                name = "Trà Tắc Xí Muội Khổng Lồ",
                description = "Trà lài ủ lạnh hòa quyện nước cốt tắc thanh mát, xí muội mặn ngọt kích thích vị giác giải ngấy tuyệt hảo.",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCSgFyvurAxNBhD3FUkp4d0APxADrWGjNc9X_5_AOXQjP4TwbH_wdpsFm6aVPUBhWoLH_fdK8ceBr-87CWNz5uBcTTq38rA02dXH962HqoGod8HHChH1Q4DycVg82qLHCpeKc1Y_jq7X-OJFCTzK7rJrQtImY9ZbDijQ5HhQIosKRSFocPs4hKiUka1dvlVvz35YVbwUFx9EN3FYhhySWx9iXe-GbmLhRnRzd-P1iE",
                basePrice = 20_000L,
                isAvailable = true,
            ),
        )
    }
}
