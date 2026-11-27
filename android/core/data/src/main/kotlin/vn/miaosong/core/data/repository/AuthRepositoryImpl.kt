package vn.miaosong.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
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
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
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
                "avatarUrl" to "",
                "createdAt" to FieldValue.serverTimestamp(),
            )
            firestore.collection("users").document(firebaseUser.uid).set(userData).await()

            _mockCurrentUser.value = profile
            AppResult.Success(profile)
        } catch (e: Exception) {
            val message = when (e) {
                is FirebaseAuthUserCollisionException -> "Email này đã được sử dụng. Vui lòng đăng nhập hoặc chọn email khác."
                is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu. Vui lòng đặt mật khẩu tối thiểu 6 ký tự."
                is FirebaseAuthInvalidCredentialsException -> "Định dạng email hoặc mật khẩu không hợp lệ."
                else -> e.localizedMessage ?: "Đăng ký không thành công. Vui lòng thử lại."
            }
            AppResult.Error(
                code = "AUTH_SIGNUP_FAILED",
                message = message,
            )
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AppResult<UserProfile> {
        return AppResult.Error(
            code = "AUTH_NOT_IMPLEMENTED",
            message = "Tính năng đăng nhập qua Google đang được phát triển (Sắp ra mắt).",
        )
    }

    override suspend fun signInWithFacebook(token: String): AppResult<UserProfile> {
        return AppResult.Error(
            code = "AUTH_NOT_IMPLEMENTED",
            message = "Tính năng đăng nhập qua Facebook đang được phát triển (Sắp ra mắt).",
        )
    }

    override suspend fun sendPasswordResetEmail(email: String): AppResult<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(
                code = "AUTH_RESET_FAILED",
                message = e.localizedMessage ?: "Gửi email khôi phục mật khẩu thất bại.",
            )
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
        return AppResult.Error("AUTH_USER_NULL", "Chưa đăng nhập.")
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
                    ),
                ).await()
            } catch (e: Exception) {
                return AppResult.Error(
                    code = "PROFILE_UPDATE_FAILED",
                    message = e.localizedMessage ?: "Cập nhật thông tin thất bại.",
                )
            }
        }
        return AppResult.Success(updated)
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray): AppResult<String> {
        val firebaseUser = auth.currentUser ?: return AppResult.Error(
            code = "UNAUTHENTICATED",
            message = "Vui lòng đăng nhập để tải ảnh đại diện.",
        )
        return try {
            val optimizedBytes = compressImageIfNeeded(imageBytes)
            val ref = storage.reference.child("users/${firebaseUser.uid}/avatar.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(optimizedBytes, metadata).await()
            val url = ref.downloadUrl.await().toString()

            firestore.collection("users").document(firebaseUser.uid).update("avatarUrl", url).await()

            val current = _mockCurrentUser.value
            if (current != null) {
                _mockCurrentUser.value = current.copy(avatarUrl = url)
            }
            AppResult.Success(url)
        } catch (e: Exception) {
            AppResult.Error(
                code = "UPLOAD_AVATAR_FAILED",
                message = e.localizedMessage ?: "Tải ảnh đại diện thất bại.",
            )
        }
    }

    override suspend fun signOut() {
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            runCatching {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                if (token.isNotBlank()) {
                    firestore.collection("users")
                        .document(uid)
                        .update("fcmTokens", FieldValue.arrayRemove(token))
                        .await()
                }
            }
        }
        _mockCurrentUser.value = null
        runCatching { auth.signOut() }
    }

    private fun compressImageIfNeeded(rawBytes: ByteArray): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
        val maxDim = 800
        val longestSide = max(decoded.width, decoded.height)
        val scaled = if (longestSide > maxDim) {
            val ratio = maxDim.toFloat() / longestSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return out.toByteArray()
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
        val avatarUrl = docSnap?.getString("avatarUrl") ?: fbUser.photoUrl?.toString().orEmpty()

        return UserProfile(
            uid = fbUser.uid,
            name = docSnap?.getString("name") ?: fbUser.displayName ?: fbUser.email.orEmpty(),
            email = fbUser.email.orEmpty(),
            phone = docSnap?.getString("phone").orEmpty(),
            role = role,
            storeId = storeId,
            avatarUrl = avatarUrl,
        )
    }
}
