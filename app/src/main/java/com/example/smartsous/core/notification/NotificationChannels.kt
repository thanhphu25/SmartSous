package com.example.smartsous.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {

    // ID của từng kênh — dùng khi gửi notification
    const val CHANNEL_EXPIRY    = "smartsous_expiry"
    const val CHANNEL_MEAL_PLAN = "smartsous_meal_plan"

    // Tạo tất cả channels — gọi 1 lần trong SmartSousApp.onCreate()
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        // Kênh 1: Cảnh báo hết hạn — HIGH importance (có âm thanh, banner)
        val expiryChannel = NotificationChannel(
            CHANNEL_EXPIRY,
            "Cảnh báo hết hạn thực phẩm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo khi nguyên liệu trong tủ lạnh sắp hết hạn"
            enableVibration(true)
            enableLights(true)
        }

        // Kênh 2: Nhắc kế hoạch ăn — DEFAULT importance (không làm phiền)
        val mealPlanChannel = NotificationChannel(
            CHANNEL_MEAL_PLAN,
            "Nhắc kế hoạch ăn uống",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Nhắc nhở buổi sáng về món ăn trong ngày"
        }

        manager.createNotificationChannels(
            listOf(expiryChannel, mealPlanChannel)
        )
    }
}