package com.example.smartsous.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.smartsous.SmartSousActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService<NotificationManager>()

    fun showExpiryNotification(
        ingredientName: String,
        daysLeft: Int,
        notificationId: Int
    ) {
        val message = NotificationMessageFactory.expiry(ingredientName, daysLeft)

        val notification = NotificationCompat
            .Builder(context, NotificationChannels.CHANNEL_EXPIRY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent("pantry"))
            .build()

        manager?.notify(notificationId, notification)
    }

    fun showMealReminderNotification(mealSummary: String) {
        val message = NotificationMessageFactory.mealPlan(mealSummary)

        val notification = NotificationCompat
            .Builder(context, NotificationChannels.CHANNEL_MEAL_PLAN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message.body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent("planner"))
            .build()

        manager?.notify(MealReminderPolicy.NOTIFICATION_ID, notification)
    }

    private fun buildPendingIntent(route: String): PendingIntent {
        val intent = Intent(context, SmartSousActivity::class.java).apply {
            putExtra(SmartSousActivity.EXTRA_NAVIGATE_TO, route)
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
