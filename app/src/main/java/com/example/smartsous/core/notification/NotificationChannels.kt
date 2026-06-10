package com.example.smartsous.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val CHANNEL_EXPIRY = "smartsous_expiry"

    // v2 forces Android to create a fresh HIGH-importance channel for demo/test.
    const val CHANNEL_MEAL_PLAN = "smartsous_meal_plan_v2"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        val expiryChannel = NotificationChannel(
            CHANNEL_EXPIRY,
            "Cảnh báo hết hạn thực phẩm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo khi nguyên liệu sắp hết hạn"
            enableVibration(true)
            enableLights(true)
        }

        val mealPlanChannel = NotificationChannel(
            CHANNEL_MEAL_PLAN,
            "Nhắc kế hoạch ăn uống",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Nhắc buổi sáng về thực đơn trong ngày"
            enableVibration(true)
            enableLights(true)
        }

        manager.createNotificationChannels(
            listOf(expiryChannel, mealPlanChannel)
        )
    }
}
