package vn.miaosong.feature.customer.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AddressRepository
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.UserRole
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val user: UserProfile = UserProfile(
        uid = "",
        name = "",
        email = "",
        phone = "",
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
    val isAddingAddress: Boolean = false,
    val newAddressLabel: String = "",
    val newAddressFullText: String = "",
    val newAddressNote: String = "",
    val newAddressIsDefault: Boolean = false,
    val message: String? = null,
    val isLoggedOut: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val addressRepository: AddressRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 1. Luồng theo dõi thông tin người dùng realtime
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { currentUser ->
                if (currentUser != null) {
                    _uiState.update {
                        it.copy(
                            user = currentUser,
                            isLoggedOut = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            user = UserProfile(
                                uid = "",
                                name = "",
                                email = "",
                                phone = "",
                                role = UserRole.CUSTOMER,
                            ),
                            addresses = emptyList(),
                            avatarUri = null,
                        )
                    }
                }
            }
        }

        // 2. Luồng theo dõi địa chỉ Firestore thời gian thực (tự động chuyển UID khi đổi tài khoản)
        viewModelScope.launch {
            authRepository.observeCurrentUser().flatMapLatest { user ->
                if (user != null && user.uid.isNotBlank()) {
                    addressRepository.observeAddresses(user.uid)
                } else {
                    flowOf(emptyList())
                }
            }.collect { addrs ->
                _uiState.update { state -> state.copy(addresses = addrs) }
            }
        }
    }

    fun resetLogoutState() {
        _uiState.update { it.copy(isLoggedOut = false) }
    }

    fun uploadAvatarFromUri(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, avatarUri = uriString) }
            val bytes = try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }

            if (bytes == null || bytes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        message = "Không đọc được tệp ảnh từ thiết bị.",
                    )
                }
                return@launch
            }

            when (val result = authRepository.uploadAvatar(bytes)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            avatarUri = result.data,
                            user = it.user.copy(avatarUrl = result.data),
                            message = "Tải ảnh đại diện lên máy chủ thành công!",
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    fun openAddAddressDialog() {
        _uiState.update {
            it.copy(
                isAddingAddress = true,
                newAddressLabel = "",
                newAddressFullText = "",
                newAddressNote = "",
                newAddressIsDefault = it.addresses.isEmpty(), // Mặc định nếu là địa chỉ đầu tiên
                message = null,
            )
        }
    }

    fun closeAddAddressDialog() {
        _uiState.update { it.copy(isAddingAddress = false) }
    }

    fun onNewAddressLabelChange(label: String) {
        _uiState.update { it.copy(newAddressLabel = label) }
    }

    fun onNewAddressFullTextChange(fullText: String) {
        _uiState.update { it.copy(newAddressFullText = fullText) }
    }

    fun onNewAddressNoteChange(note: String) {
        _uiState.update { it.copy(newAddressNote = note) }
    }

    fun onNewAddressIsDefaultChange(isDefault: Boolean) {
        _uiState.update { it.copy(newAddressIsDefault = isDefault) }
    }

    fun submitAddAddress() {
        val state = _uiState.value
        val label = state.newAddressLabel.trim()
        val fullText = state.newAddressFullText.trim()
        val note = state.newAddressNote.trim()
        val uid = state.user.uid

        if (uid.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng đăng nhập để lưu địa chỉ.") }
            return
        }

        if (label.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập tên địa chỉ (Vd: Nhà, Trường học).") }
            return
        }

        if (fullText.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập địa chỉ chi tiết.") }
            return
        }

        val newAddr = DeliveryAddress(
            label = label,
            fullText = fullText,
            lat = 10.7326,
            lng = 106.6998,
            recipientName = state.user.name,
            recipientPhone = state.user.phone,
            note = note,
            isDefault = state.newAddressIsDefault,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = addressRepository.addAddress(uid, newAddr)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAddingAddress = false,
                            message = "Thêm địa chỉ nhận món thành công!",
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

    fun deleteAddress(addressId: String) {
        val uid = _uiState.value.user.uid
        if (uid.isBlank() || addressId.isBlank()) return

        viewModelScope.launch {
            when (val result = addressRepository.deleteAddress(uid, addressId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(message = "Đã xóa địa chỉ.") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun setDefaultAddress(addressId: String) {
        val uid = _uiState.value.user.uid
        if (uid.isBlank() || addressId.isBlank()) return

        viewModelScope.launch {
            when (val result = addressRepository.setDefaultAddress(uid, addressId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(message = "Đã đặt làm địa chỉ mặc định!") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(message = result.message) }
                }
            }
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
            _uiState.update {
                it.copy(
                    user = UserProfile(
                        uid = "",
                        name = "",
                        email = "",
                        phone = "",
                        role = UserRole.CUSTOMER,
                    ),
                    addresses = emptyList(),
                    avatarUri = null,
                    isLoggedOut = true,
                )
            }
        }
    }
}
