package vn.miaosong.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MiaoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        syncTokenToFirestore(token)
    }

    companion object {
        /** Đồng bộ FCM token hiện tại lên Firestore `users/{uid}.fcmTokens` để Cloud Function `onOrderUpdated` bắn push thật */
        fun syncCurrentToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmTokens", FieldValue.arrayUnion(token))
                }
            }
        }

        fun syncTokenToFirestore(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            if (token.isBlank()) return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "ORDER_STATUS"
        val channelId = when (type) {
            "NEW_ORDER" -> MiaoSongApplication.CHANNEL_NEW_ORDER
            "LATE_ALERT" -> MiaoSongApplication.CHANNEL_LATE_ALERT
            else -> MiaoSongApplication.CHANNEL_ORDER_STATUS
        }

        val title = message.notification?.title ?: message.data["title"] ?: "MiaoSong"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("orderId", message.data["orderId"])
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
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
