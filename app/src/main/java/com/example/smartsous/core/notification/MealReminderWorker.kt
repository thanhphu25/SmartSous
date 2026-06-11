package com.example.smartsous.core.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.data.local.dao.MealPlanDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mealPlanDao: MealPlanDao,
    private val dataStoreManager: DataStoreManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "meal_reminder_worker"
        private const val TAG = "MealReminderWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val notifications = dataStoreManager.notificationPreferenceFlow.first()
            if (!notifications.mealRemindersEnabled) {
                Log.d(TAG, "Meal reminders disabled")
                return Result.success()
            }

            val today = LocalDate.now().toString()
            val todayPlans = mealPlanDao
                .getForWeek(today, today)
                .first()

            val summary = MealReminderPolicy.buildMealSummary(todayPlans)
            notificationHelper.showMealReminderNotification(summary)
            Log.d(TAG, "Sent meal reminder: $summary")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MealReminderWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
