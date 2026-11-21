package vn.miaosong.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.Store
import javax.inject.Inject

data class CustomerHomeUiState(
    val isLoading: Boolean = true,
    val stores: List<Store> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    storeRepository: StoreRepository,
) : ViewModel() {

    val uiState: StateFlow<CustomerHomeUiState> = storeRepository.observeStores()
        .map { stores ->
            CustomerHomeUiState(
                isLoading = false,
                stores = stores,
                errorMessage = null,
            )
        }
        .catch { e ->
            emit(
                CustomerHomeUiState(
                    isLoading = false,
                    stores = emptyList(),
                    errorMessage = e.localizedMessage ?: "Không tải được danh sách quán.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomerHomeUiState(isLoading = true),
        )
}
