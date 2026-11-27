package vn.miaosong.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MiaoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        syncTokenToFirestore(token)
    }

    companion object {
        private const val PREFS_NAME = "miaosong_notif_prefs"
        private const val KEY_ACTIVE_UID = "active_uid"
        private const val KEY_ACTIVE_ROLE = "active_role"

        fun updateActiveSession(context: Context, uid: String, roleName: String) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_UID, uid)
                .putString(KEY_ACTIVE_ROLE, roleName)
                .apply()
        }

        fun updateActiveRole(context: Context, roleName: String) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_ROLE, roleName)
                .apply()
        }

        fun clearActiveSession(context: Context) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_UID)
                .remove(KEY_ACTIVE_ROLE)
                .apply()
        }

        private fun getActiveRole(context: Context): String? =
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_ROLE, null)

        /** Đồng bộ FCM token hiện tại lên Firestore `users/{uid}.fcmTokens` để Cloud Function `onOrderUpdated` bắn push thật */
        fun syncCurrentToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge())
                }
            }
        }

        fun syncTokenToFirestore(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            if (token.isBlank()) return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge())
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Chặn thông báo nếu người dùng đã đăng xuất hoặc thông báo dành cho tài khoản / vai trò khác
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid.isNullOrBlank()) return

        val targetUid = message.data["targetUid"]
        if (!targetUid.isNullOrBlank() && targetUid != currentUid) return

        val targetRole = message.data["targetRole"]
        val activeRole = getActiveRole(this)
        if (!targetRole.isNullOrBlank() && !activeRole.isNullOrBlank() &&
            !targetRole.equals(activeRole, ignoreCase = true)
        ) {
            return
        }

        val type = message.data["type"] ?: "ORDER_STATUS"
        val channelId = message.data["channelId"]?.takeIf { it.isNotBlank() } ?: when (type) {
            "NEW_ORDER" -> MiaoSongApplication.CHANNEL_NEW_ORDER
            "LATE_ALERT" -> MiaoSongApplication.CHANNEL_LATE_ALERT
            else -> MiaoSongApplication.CHANNEL_ORDER_STATUS
        }

        val title = message.notification?.title ?: message.data["title"] ?: "MiaoSong"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("orderId", message.data["orderId"])
            if (type == "NEW_ORDER" || targetRole == "MERCHANT") {
                putExtra("open_merchant_kds", true)
            }
        }
        val pending = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
