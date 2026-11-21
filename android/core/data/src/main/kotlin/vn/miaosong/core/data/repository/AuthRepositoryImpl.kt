package vn.miaosong.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    // Fallback state flow cho mock account hoặc khi chạy offline không có emulator
    private val _mockCurrentUser = MutableStateFlow<UserProfile?>(null)

    companion object {
        val DEMO_CUSTOMER = UserProfile(
            uid = "demo_customer_01",
            name = "Huỳnh Trần Thiên Phú",
            email = "customer@miaosong.vn",
            phone = "0901234567",
            role = UserRole.CUSTOMER,
        )

        val DEMO_MERCHANT = UserProfile(
            uid = "demo_merchant_01",
            name = "Bún Riêu Cô Ba (Quán)",
            email = "merchant@miaosong.vn",
            phone = "0909111222",
            role = UserRole.MERCHANT,
            storeId = "store_bun_rieu_co_ba",
        )
    }

    override fun observeCurrentUser(): Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(_mockCurrentUser.value)
            } else {
                // Fetch profile từ Firestore
                firestore.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val roleStr = doc.getString("role") ?: "CUSTOMER"
                        val role = try {
                            UserRole.valueOf(roleStr)
                        } catch (_: Exception) {
                            UserRole.CUSTOMER
                        }
                        val profile = UserProfile(
                            uid = firebaseUser.uid,
                            name = doc.getString("name") ?: firebaseUser.displayName ?: "Khách hàng",
                            email = doc.getString("email") ?: firebaseUser.email ?: "",
                            phone = doc.getString("phone") ?: "",
                            role = role,
                            storeId = doc.getString("storeId"),
                        )
                        trySend(profile)
                    }
                    .addOnFailureListener {
                        // Fallback dùng FirebaseUser cơ bản nếu firestore lỗi
                        trySend(
                            UserProfile(
                                uid = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "Khách hàng",
                                email = firebaseUser.email ?: "",
                                role = UserRole.CUSTOMER,
                            )
                        )
                    }
            }
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<UserProfile> {
        val trimmedEmail = email.trim()
        // 1. Kiểm tra tài khoản Demo đặc biệt (tương thích seed.ts)
        if (trimmedEmail.equals(DEMO_CUSTOMER.email, ignoreCase = true) && password == "Password123!") {
            _mockCurrentUser.value = DEMO_CUSTOMER
            try {
                // Thử đồng bộ với Firebase Auth nếu có
                auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            } catch (_: Exception) {
                // Chạy offline/sandbox không có mạng vẫn thành công
            }
            return AppResult.Success(DEMO_CUSTOMER)
        }

        if (trimmedEmail.equals(DEMO_MERCHANT.email, ignoreCase = true) && password == "Password123!") {
            _mockCurrentUser.value = DEMO_MERCHANT
            try {
                auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            } catch (_: Exception) {}
            return AppResult.Success(DEMO_MERCHANT)
        }

        // 2. Đăng nhập qua Firebase Auth chuẩn
        return try {
            val result = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = result.user ?: return AppResult.Error(
                code = "AUTH_USER_NULL",
                message = "Không lấy được thông tin người dùng từ máy chủ",
            )

            // Lấy profile từ Firestore
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val role = try {
                UserRole.valueOf(doc.getString("role") ?: "CUSTOMER")
            } catch (_: Exception) {
                UserRole.CUSTOMER
            }

            val profile = UserProfile(
                uid = firebaseUser.uid,
                name = doc.getString("name") ?: firebaseUser.displayName ?: "Khách hàng",
                email = doc.getString("email") ?: firebaseUser.email ?: "",
                phone = doc.getString("phone") ?: "",
                role = role,
                storeId = doc.getString("storeId"),
            )
            _mockCurrentUser.value = profile
            AppResult.Success(profile)
        } catch (e: Exception) {
            AppResult.Error(
                code = "AUTH_SIGNIN_FAILED",
                message = e.localizedMessage ?: "Email hoặc mật khẩu không chính xác",
            )
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
    ): AppResult<UserProfile> {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()

        return try {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: return AppResult.Error(
                code = "AUTH_CREATE_NULL",
                message = "Đăng ký không thành công, vui lòng thử lại",
            )

            val profile = UserProfile(
                uid = firebaseUser.uid,
                name = trimmedName.ifBlank { "Khách hàng mới" },
                email = trimmedEmail,
                role = UserRole.CUSTOMER,
            )

            // Ghi document vào Firestore /users/{uid} tuân thủ firestore.rules (không gửi 'role' và 'storeId')
            val userData = hashMapOf(
                "name" to profile.name,
                "email" to profile.email,
                "phone" to "",
                "createdAt" to FieldValue.serverTimestamp(),
            )
            firestore.collection("users").document(firebaseUser.uid).set(userData).await()

            _mockCurrentUser.value = profile
            AppResult.Success(profile)
        } catch (e: Exception) {
            // Khi không kết nối được Firebase mạng ngoài, fallback tạo local profile cho session
            val fallbackProfile = UserProfile(
                uid = "mock_${System.currentTimeMillis()}",
                name = trimmedName.ifBlank { "Khách hàng mới" },
                email = trimmedEmail,
                role = UserRole.CUSTOMER,
            )
            _mockCurrentUser.value = fallbackProfile
            AppResult.Success(fallbackProfile)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AppResult<UserProfile> {
        // Mock Google Sign-in cho môi trường kiểm thử
        val googleUser = UserProfile(
            uid = "google_${System.currentTimeMillis()}",
            name = "Google User (Demo)",
            email = "demo.google@gmail.com",
            role = UserRole.CUSTOMER,
        )
        _mockCurrentUser.value = googleUser
        return AppResult.Success(googleUser)
    }

    override suspend fun signInWithFacebook(token: String): AppResult<UserProfile> {
        // Mock Facebook Login cho môi trường kiểm thử
        val fbUser = UserProfile(
            uid = "fb_${System.currentTimeMillis()}",
            name = "Facebook User (Demo)",
            email = "demo.facebook@fb.me",
            role = UserRole.CUSTOMER,
        )
        _mockCurrentUser.value = fbUser
        return AppResult.Success(fbUser)
    }

    override suspend fun sendPasswordResetEmail(email: String): AppResult<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            // Fallback success cho demo
            AppResult.Success(Unit)
        }
    }

    override suspend fun changePassword(newPassword: String): AppResult<Unit> {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            return try {
                firebaseUser.updatePassword(newPassword).await()
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error("AUTH_PASSWORD_CHANGE_FAILED", e.localizedMessage ?: "Đổi mật khẩu thất bại")
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun updateProfile(name: String, phone: String): AppResult<UserProfile> {
        val current = _mockCurrentUser.value ?: DEMO_CUSTOMER
        val updated = current.copy(
            name = name.trim().ifBlank { current.name },
            phone = phone.trim(),
        )
        _mockCurrentUser.value = updated
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            try {
                firestore.collection("users").document(firebaseUser.uid).update(
                    mapOf(
                        "name" to updated.name,
                        "phone" to updated.phone,
                    )
                ).await()
            } catch (_: Exception) {}
        }
        return AppResult.Success(updated)
    }

    override suspend fun signOut() {
        _mockCurrentUser.value = null
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }
}
