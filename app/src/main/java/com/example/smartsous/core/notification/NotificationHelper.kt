package com.example.smartsous.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.smartsous.R
import com.example.smartsous.SmartSousActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService<NotificationManager>()

    // Notification khi nguyên liệu sắp hết hạn
    // daysLeft: 0 = hôm nay, 1 = ngày mai, 3 = 3 ngày nữa
    fun showExpiryNotification(
        ingredientName: String,
        daysLeft: Int,
        notificationId: Int
    ) {
        val (title, body) = when (daysLeft) {
            0 -> "⚠️ Hết hạn hôm nay!" to
                    "$ingredientName hết hạn hôm nay — hãy dùng ngay nhé!"
            1 -> "🕐 Sắp hết hạn ngày mai" to
                    "$ingredientName còn 1 ngày — lên kế hoạch nấu ngay!"
            else -> "📅 Sắp hết hạn trong $daysLeft ngày" to
                    "$ingredientName còn $daysLeft ngày — đừng để lãng phí!"
        }

        val notification = NotificationCompat
            .Builder(context, NotificationChannels.CHANNEL_EXPIRY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Bấm vào notification → mở PantryScreen
            .setContentIntent(buildPendingIntent("pantry"))
            .build()

        manager?.notify(notificationId, notification)
    }

    // Notification nhắc bữa ăn buổi sáng
    fun showMealReminderNotification(mealSummary: String) {
        val notification = NotificationCompat
            .Builder(context, NotificationChannels.CHANNEL_MEAL_PLAN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🍳 Hôm nay nấu gì?")
            .setContentText(mealSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(mealSummary)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // Bấm vào notification → mở PlannerScreen
            .setContentIntent(buildPendingIntent("planner"))
            .build()

        manager?.notify(2000, notification)
    }

    // Build PendingIntent để deep link vào đúng màn hình
    private fun buildPendingIntent(route: String): PendingIntent {
        val intent = Intent(context, SmartSousActivity::class.java).apply {
            putExtra("navigate_to", route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}