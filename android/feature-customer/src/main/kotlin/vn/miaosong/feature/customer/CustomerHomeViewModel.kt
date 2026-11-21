package vn.miaosong.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AddressRepository
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.Store
import java.text.Normalizer
import java.util.regex.Pattern
import javax.inject.Inject

enum class StoreSortFilter {
    NEARBY,
    FASTEST,
    TOP_RATED,
}

val DEFAULT_CUSTOMER_ADDRESS = DeliveryAddress(
    id = "addr_default",
    label = "Trường ĐH Tôn Đức Thắng",
    fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
    lat = 10.7327,
    lng = 106.6992,
    recipientName = "Khách hàng",
    recipientPhone = "",
    note = "Gọi khi tới cổng chính",
    isDefault = true,
)

data class CustomerHomeUiState(
    val isLoading: Boolean = true,
    val stores: List<Store> = emptyList(),
    val allStoresCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val activeFilter: StoreSortFilter = StoreSortFilter.NEARBY,
    val addresses: List<DeliveryAddress> = listOf(DEFAULT_CUSTOMER_ADDRESS),
    val selectedAddress: DeliveryAddress = DEFAULT_CUSTOMER_ADDRESS,
    val isAddressPickerVisible: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val addressRepository: AddressRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _activeFilter = MutableStateFlow(StoreSortFilter.NEARBY)
    private val _selectedAddressId = MutableStateFlow<String?>(null)
    private val _isAddressPickerVisible = MutableStateFlow(false)

    // Quan sát luồng địa chỉ người dùng realtime
    private val _userAddressesFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null && user.uid.isNotBlank()) {
            addressRepository.observeAddresses(user.uid).flatMapLatest { list ->
                if (list.isNotEmpty()) {
                    flowOf(list)
                } else {
                    val defaultAddr = DEFAULT_CUSTOMER_ADDRESS.copy(
                        recipientName = user.name.ifBlank { "Khách hàng" },
                        recipientPhone = user.phone,
                    )
                    flowOf(listOf(defaultAddr))
                }
            }
        } else {
            flowOf(listOf(DEFAULT_CUSTOMER_ADDRESS))
        }
    }

    private val _addressStateFlow = combine(
        _userAddressesFlow,
        _selectedAddressId,
        _isAddressPickerVisible,
    ) { addresses, selectedId, isVisible ->
        Triple(addresses, selectedId, isVisible)
    }

    val uiState: StateFlow<CustomerHomeUiState> = combine(
        storeRepository.observeStores(),
        _searchQuery,
        _selectedCategory,
        _activeFilter,
        _addressStateFlow,
    ) { allStores, query, category, filter, (addresses, selectedId, isPickerVisible) ->
        var list = allStores

        // 1. Lọc theo từ khóa tìm kiếm (Tên quán, món ăn, địa chỉ)
        if (query.isNotBlank()) {
            val normalizedQuery = query.trim().unaccent()
            list = list.filter { store ->
                store.name.unaccent().contains(normalizedQuery, ignoreCase = true) ||
                    store.slug.unaccent().contains(normalizedQuery, ignoreCase = true) ||
                    store.address.unaccent().contains(normalizedQuery, ignoreCase = true)
            }
        }

        // 2. Lọc theo danh mục được chọn
        if (!category.isNullOrBlank()) {
            list = list.filter { store ->
                store.matchesCategory(category)
            }
        }

        // 3. Sắp xếp theo bộ lọc
        list = when (filter) {
            StoreSortFilter.FASTEST -> list.sortedBy { store ->
                12 + (store.id.hashCode().let { kotlin.math.abs(it) } % 11)
            }
            StoreSortFilter.TOP_RATED -> list.sortedByDescending { it.ratingAvg }
            StoreSortFilter.NEARBY -> list
        }

        val effectiveAddresses = if (addresses.isNotEmpty()) addresses else listOf(DEFAULT_CUSTOMER_ADDRESS)
        val selectedAddress = effectiveAddresses.firstOrNull { it.id == selectedId }
            ?: effectiveAddresses.firstOrNull { it.isDefault }
            ?: effectiveAddresses.firstOrNull()
            ?: DEFAULT_CUSTOMER_ADDRESS

        CustomerHomeUiState(
            isLoading = false,
            stores = list,
            allStoresCount = allStores.size,
            searchQuery = query,
            selectedCategory = category,
            activeFilter = filter,
            addresses = effectiveAddresses,
            selectedAddress = selectedAddress,
            isAddressPickerVisible = isPickerVisible,
            errorMessage = null,
        )
    }.catch { e ->
        emit(
            CustomerHomeUiState(
                isLoading = false,
                stores = emptyList(),
                allStoresCount = 0,
                errorMessage = e.localizedMessage ?: "Không tải được danh sách quán.",
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerHomeUiState(isLoading = true),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun selectCategory(categoryTitle: String) {
        _selectedCategory.update { current ->
            if (current == categoryTitle) null else categoryTitle
        }
    }

    fun selectFilter(filter: StoreSortFilter) {
        _activeFilter.value = filter
    }

    fun openAddressPicker() {
        _isAddressPickerVisible.value = true
    }

    fun closeAddressPicker() {
        _isAddressPickerVisible.value = false
    }

    fun selectAddress(address: DeliveryAddress) {
        _selectedAddressId.value = address.id
        _isAddressPickerVisible.value = false
    }

    fun addNewAddress(label: String, fullText: String, note: String) {
        viewModelScope.launch {
            val user = authRepository.observeCurrentUser().first()
            val newAddr = DeliveryAddress(
                id = "",
                label = label,
                fullText = fullText,
                lat = 10.7327,
                lng = 106.6992,
                recipientName = user?.name?.ifBlank { "Khách hàng" } ?: "Khách hàng",
                recipientPhone = user?.phone?.ifBlank { "" } ?: "",
                note = note,
                isDefault = false,
            )
            if (user != null && user.uid.isNotBlank()) {
                val res = addressRepository.addAddress(user.uid, newAddr)
                if (res is AppResult.Success) {
                    _selectedAddressId.value = res.data
                }
            } else {
                _selectedAddressId.value = newAddr.id
            }
            _isAddressPickerVisible.value = false
        }
    }

    private fun Store.matchesCategory(category: String): Boolean {
        val lower = "$name $slug $address".unaccent()
        return when (category) {
            "Bún & Phở" -> lower.contains("bun") || lower.contains("pho") || lower.contains("rieu") || lower.contains("hue")
            "Cơm tấm" -> lower.contains("com") || lower.contains("tam") || lower.contains("suon")
            "Trà sữa" -> lower.contains("tra sua") || lower.contains("phuc long") || lower.contains("tea") || lower.contains("boba")
            "Ăn vặt" -> lower.contains("banh mi") || lower.contains("an vat") || lower.contains("snack")
            "Tráng miệng" -> lower.contains("che") || lower.contains("kem") || lower.contains("cake") || lower.contains("banh")
            "Cà phê" -> lower.contains("ca phe") || lower.contains("coffee") || lower.contains("espresso")
            "Giao nhanh" -> true
            "Deal 50%" -> ratingAvg >= 4.8 || slug.contains("rieu") || slug.contains("ba")
            else -> true
        }
    }

    private fun String.unaccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D').lowercase()
    }
}
