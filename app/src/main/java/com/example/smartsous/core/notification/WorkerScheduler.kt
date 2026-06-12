package com.example.smartsous.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartsous.domain.model.NotificationPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleAll(settings: NotificationPreference = NotificationPreference()) {
        if (settings.expiryRemindersEnabled) {
            scheduleExpiryCheck()
        } else {
            cancelExpiryCheck()
        }

        if (settings.mealRemindersEnabled) {
            scheduleMealReminder(
                hour = settings.mealReminderHour,
                minute = settings.mealReminderMinute
            )
        } else {
            cancelMealReminder()
        }
    }

    fun scheduleExpiryCheck() {
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(delayUntilHour(hour = 8, minute = 0), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelExpiryCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(ExpiryCheckWorker.WORK_NAME)
    }

    fun scheduleMealReminder(
        hour: Int = MealReminderPolicy.REMINDER_HOUR,
        minute: Int = MealReminderPolicy.REMINDER_MINUTE
    ) {
        val request = PeriodicWorkRequestBuilder<MealReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(
                delayUntilHour(
                    hour = hour.coerceIn(0, 23),
                    minute = minute.coerceIn(0, 59)
                ),
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MealReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelMealReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(MealReminderWorker.WORK_NAME)
    }

    private fun delayUntilHour(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
