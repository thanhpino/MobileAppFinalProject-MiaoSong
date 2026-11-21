package vn.miaosong.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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

    // Fallback state flow cho tài khoản demo hoặc chạy offline
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
                launch {
                    val profile = resolveProfile(firebaseUser, forceRefresh = false)
                    _mockCurrentUser.value = profile
                    trySend(profile)
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<UserProfile> {
        val trimmedEmail = email.trim()
        // 1. Tài khoản Demo nhanh (tương thích seed.ts)
        if (trimmedEmail.equals(DEMO_CUSTOMER.email, ignoreCase = true) && password == "Password123!") {
            _mockCurrentUser.value = DEMO_CUSTOMER
            runCatching { auth.signInWithEmailAndPassword(trimmedEmail, password).await() }
            return AppResult.Success(DEMO_CUSTOMER)
        }
        if (trimmedEmail.equals(DEMO_MERCHANT.email, ignoreCase = true) && password == "Password123!") {
            _mockCurrentUser.value = DEMO_MERCHANT
            runCatching { auth.signInWithEmailAndPassword(trimmedEmail, password).await() }
            return AppResult.Success(DEMO_MERCHANT)
        }

        // 2. Đăng nhập Firebase Auth thật + đọc Custom Claims (role, storeId)
        return try {
            val result = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = result.user ?: return AppResult.Error(
                code = "AUTH_USER_NULL",
                message = "Không lấy được thông tin người dùng từ máy chủ.",
            )
            val profile = resolveProfile(firebaseUser, forceRefresh = true)
            _mockCurrentUser.value = profile
            AppResult.Success(profile)
        } catch (e: Exception) {
            AppResult.Error(
                code = "AUTH_SIGNIN_FAILED",
                message = e.localizedMessage ?: "Email hoặc mật khẩu không chính xác.",
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
                message = "Đăng ký không thành công, vui lòng thử lại.",
            )

            val profile = UserProfile(
                uid = firebaseUser.uid,
                name = trimmedName.ifBlank { "Khách hàng mới" },
                email = trimmedEmail,
                role = UserRole.CUSTOMER,
            )

            // Ghi /users/{uid} tuân thủ firestore.rules (không gửi role và storeId từ client)
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
        } catch (_: Exception) {
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
                AppResult.Error("AUTH_PASSWORD_CHANGE_FAILED", e.localizedMessage ?: "Đổi mật khẩu thất bại.")
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
            runCatching {
                firestore.collection("users").document(firebaseUser.uid).update(
                    mapOf(
                        "name" to updated.name,
                        "phone" to updated.phone,
                    ),
                ).await()
            }
        }
        return AppResult.Success(updated)
    }

    override suspend fun signOut() {
        _mockCurrentUser.value = null
        runCatching { auth.signOut() }
    }

    private suspend fun resolveProfile(fbUser: FirebaseUser, forceRefresh: Boolean): UserProfile {
        val claims = try {
            fbUser.getIdToken(forceRefresh).await().claims
        } catch (_: Exception) {
            emptyMap<String, Any>()
        }

        val roleStr = claims["role"]?.toString()
        val claimStoreId = claims["storeId"]?.toString()

        val docSnap = try {
            firestore.collection("users").document(fbUser.uid).get().await()
        } catch (_: Exception) {
            null
        }

        val resolvedRoleStr = roleStr ?: docSnap?.getString("role") ?: "CUSTOMER"
        val role = if (resolvedRoleStr.equals("MERCHANT", ignoreCase = true)) {
            UserRole.MERCHANT
        } else {
            UserRole.CUSTOMER
        }
        val storeId = claimStoreId ?: docSnap?.getString("storeId")

        return UserProfile(
            uid = fbUser.uid,
            name = docSnap?.getString("name") ?: fbUser.displayName ?: fbUser.email.orEmpty(),
            email = fbUser.email.orEmpty(),
            phone = docSnap?.getString("phone").orEmpty(),
            role = role,
            storeId = storeId,
        )
    }
}
