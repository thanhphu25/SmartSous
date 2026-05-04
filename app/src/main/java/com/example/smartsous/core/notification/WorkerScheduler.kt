package com.example.smartsous.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Schedule tất cả workers — gọi 1 lần trong SmartSousApp
    fun scheduleAll() {
        scheduleExpiryCheck()
        scheduleMealReminder()
    }

    // ExpiryCheck: chạy mỗi ngày lúc 8:00 sáng
    private fun scheduleExpiryCheck() {
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(
                delayUntilHour(hour = 8, minute = 0),
                TimeUnit.MILLISECONDS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryCheckWorker.WORK_NAME,
            // KEEP: nếu đã schedule rồi thì giữ nguyên, không tạo lại
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // MealReminder: chạy mỗi ngày lúc 7:30 sáng
    private fun scheduleMealReminder() {
        val request = PeriodicWorkRequestBuilder<MealReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(
                delayUntilHour(hour = 7, minute = 30),
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MealReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // Tính số millisecond từ bây giờ đến giờ cụ thể ngày mai
    // VD: bây giờ là 10:00, muốn 8:00 sáng mai → delay = 22 tiếng
    // VD: bây giờ là 6:00, muốn 8:00 sáng nay → delay = 2 tiếng
    private fun delayUntilHour(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Nếu giờ target đã qua hôm nay → schedule sang ngày mai
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}