package vn.miaosong.feature.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.UserRole
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserProfile = UserProfile(
        uid = "demo_customer_01",
        name = "Huỳnh Trần Thiên Phú",
        email = "customer@miaosong.vn",
        phone = "0901234567",
        role = UserRole.CUSTOMER,
    ),
    val avatarUri: String? = null,
    val addresses: List<DeliveryAddress> = emptyList(),
    val isEditing: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val isChangingPassword: Boolean = false,
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmNewPasswordVisible: Boolean = false,
    val message: String? = null,
    val isLoggedOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            addresses = listOf(
                DeliveryAddress(
                    id = "addr_tdtu",
                    label = "Trường ĐH Tôn Đức Thắng",
                    fullText = "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
                    lat = 10.7326,
                    lng = 106.6998,
                    note = "Gọi khi tới cổng chính",
                    isDefault = true,
                ),
                DeliveryAddress(
                    id = "addr_bitexco",
                    label = "Văn phòng / Bitexco",
                    fullText = "2 Hải Triều, Bến Nghé, Quận 1, TP. HCM",
                    lat = 10.7716,
                    lng = 106.7044,
                    note = "Tầng 18, gửi tại bàn lễ tân",
                    isDefault = false,
                ),
            )
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { currentUser ->
                if (currentUser != null) {
                    _uiState.update {
                        it.copy(
                            user = currentUser,
                            isLoggedOut = false,
                        )
                    }
                }
            }
        }
    }

    fun resetLogoutState() {
        _uiState.update { it.copy(isLoggedOut = false) }
    }

    fun updateAvatar(uri: String) {
        _uiState.update {
            it.copy(
                avatarUri = uri,
                message = "Cập nhật ảnh đại diện thành công!",
            )
        }
    }

    fun startEditing() {
        val user = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = user.name,
                editPhone = user.phone,
                message = null,
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onEditPhoneChange(phone: String) {
        _uiState.update { it.copy(editPhone = phone) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val name = state.editName.trim()
        val phone = state.editPhone.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(message = "Họ và tên không được để trống") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = authRepository.updateProfile(name, phone)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = false,
                            user = result.data,
                            message = "Cập nhật thông tin thành công!",
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    // Luồng Đổi mật khẩu trong ứng dụng (In-app Password Change)
    fun startChangingPassword() {
        _uiState.update {
            it.copy(
                isChangingPassword = true,
                newPassword = "",
                confirmNewPassword = "",
                isNewPasswordVisible = false,
                isConfirmNewPasswordVisible = false,
                message = null,
            )
        }
    }

    fun cancelChangingPassword() {
        _uiState.update { it.copy(isChangingPassword = false) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password) }
    }

    fun onConfirmNewPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmNewPassword = confirm) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun toggleConfirmNewPasswordVisibility() {
        _uiState.update { it.copy(isConfirmNewPasswordVisible = !it.isConfirmNewPasswordVisible) }
    }

    fun submitPasswordChange() {
        val state = _uiState.value
        val newPwd = state.newPassword.trim()
        val confirmPwd = state.confirmNewPassword.trim()

        if (newPwd.length < 6) {
            _uiState.update { it.copy(message = "Mật khẩu mới phải có ít nhất 6 ký tự") }
            return
        }

        if (newPwd != confirmPwd) {
            _uiState.update { it.copy(message = "Mật khẩu xác nhận không trùng khớp") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = authRepository.changePassword(newPwd)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isChangingPassword = false,
                            message = "Đổi mật khẩu thành công!",
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.user.email
        if (email.isBlank()) {
            _uiState.update { it.copy(message = "Tài khoản chưa có email để đặt lại mật khẩu") }
            return
        }

        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
            _uiState.update {
                it.copy(
                    isChangingPassword = false,
                    message = "Đã gửi liên kết đặt lại mật khẩu đến $email",
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
