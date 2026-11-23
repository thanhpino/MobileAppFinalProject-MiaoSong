package vn.miaosong.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MiaoSongApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_NEW_ORDER,
                "Đơn hàng mới (Quán)",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Báo chuông ngay khi có đơn đặt món mới gửi tới quán"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_ORDER_STATUS,
                "Tiến độ đơn hàng",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Cập nhật trạng thái chuẩn bị món và lộ trình tài xế Lalamove"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_LATE_ALERT,
                "Cảnh báo đơn giao trễ",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Cảnh báo quán khi đơn hàng giao lâu hơn dự kiến"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_MERCHANT_SERVICE,
                "Trực đơn hàng (Chế độ Quán)",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Giữ kết nối nền để báo chuông ngay khi có đơn hàng mới kể cả khi tắt màn hình"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannels(channels)
    }

    companion object {
        const val CHANNEL_NEW_ORDER = "miaosong_new_order"
        const val CHANNEL_ORDER_STATUS = "miaosong_order_status"
        const val CHANNEL_LATE_ALERT = "miaosong_late_alert"
        const val CHANNEL_MERCHANT_SERVICE = "miaosong_merchant_service"
    }
}
