package vn.miaosong.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import vn.miaosong.core.common.MoneyFormatter

/**
 * Foreground Service cho Chế độ Quán (§8.7 Project Plan):
 * Giữ kết nối lắng nghe đơn mới (`PLACED`) realtime trên Firestore và báo chuông + thông báo
 * ngay cả khi màn hình điện thoại Quán đang tắt hoặc ứng dụng thu nhỏ ở nền.
 */
class MerchantOrderMonitorService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    private val knownOrderIds = mutableSetOf<String>()
    private var initialSnapshotProcessed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val storeId = intent?.getStringExtra(EXTRA_STORE_ID) ?: DEFAULT_STORE_ID
        startForegroundIndicator()
        attachOrderListener(storeId)
        return START_STICKY
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onDestroy()
    }

    private fun startForegroundIndicator() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val ongoingNotification = NotificationCompat.Builder(this, MiaoSongApplication.CHANNEL_MERCHANT_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MiaoSong Quán đang trực đơn")
            .setContentText("Tự động báo chuông ngay khi có đơn hàng mới")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pending)
            .build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            ONGOING_NOTIFICATION_ID,
            ongoingNotification,
            serviceType,
        )
    }

    private fun attachOrderListener(storeId: String) {
        listenerRegistration?.remove()
        initialSnapshotProcessed = false
        knownOrderIds.clear()

        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("orders")
            .whereEqualTo("storeId", storeId)
            .whereEqualTo("status", "PLACED")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val currentIds = snap.documents.map { it.id }.toSet()
                if (!initialSnapshotProcessed) {
                    knownOrderIds.addAll(currentIds)
                    initialSnapshotProcessed = true
                    return@addSnapshotListener
                }

                for (doc in snap.documents) {
                    if (doc.id !in knownOrderIds) {
                        knownOrderIds.add(doc.id)
                        val code = doc.getString("code") ?: doc.id.take(8).uppercase()
                        val pricingMap = doc.get("pricing") as? Map<*, *>
                        val total = (pricingMap?.get("total") as? Number)?.toLong() ?: 0L
                        notifyNewOrderWithBell(orderId = doc.id, orderCode = code, totalVnd = total)
                    }
                }
            }
    }

    private fun notifyNewOrderWithBell(orderId: String, orderCode: String, totalVnd: Long) {
        // Phát chuông trực tiếp trên thiết bị Quán
        runCatching {
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, ringUri)?.play()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("orderId", orderId)
        }
        val pending = PendingIntent.getActivity(
            this,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val totalText = if (totalVnd > 0L) "Tổng ${MoneyFormatter.formatVnd(totalVnd)} · " else ""
        val alertNotification = NotificationCompat.Builder(this, MiaoSongApplication.CHANNEL_NEW_ORDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 Có đơn hàng mới #$orderCode!")
            .setContentText("${totalText}Chạm để vào Bếp (KDS) xác nhận nhận đơn")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(orderId.hashCode(), alertNotification)
    }

    companion object {
        private const val EXTRA_STORE_ID = "extra_store_id"
        private const val DEFAULT_STORE_ID = "store_bun_rieu_co_ba"
        private const val ONGOING_NOTIFICATION_ID = 7001

        fun start(context: Context, storeId: String = DEFAULT_STORE_ID) {
            val intent = Intent(context, MerchantOrderMonitorService::class.java).apply {
                putExtra(EXTRA_STORE_ID, storeId)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, MerchantOrderMonitorService::class.java))
            }
        }
    }
}
