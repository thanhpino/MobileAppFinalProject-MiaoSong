package vn.miaosong.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
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
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val fbUser = auth.currentUser
            if (fbUser == null) {
                trySend(null)
            } else {
                launch {
                    trySend(resolveProfile(fbUser, forceRefresh = false))
                }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<UserProfile> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user
                ?: return AppResult.Error("UNAUTHENTICATED", "Không thể lấy thông tin người dùng.")
            val profile = resolveProfile(fbUser, forceRefresh = true)
            AppResult.Success(profile)
        } catch (e: Exception) {
            AppResult.Error(
                code = "AUTH_FAILED",
                message = e.localizedMessage ?: "Đăng nhập thất bại. Kiểm tra lại email hoặc mật khẩu.",
            )
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
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
