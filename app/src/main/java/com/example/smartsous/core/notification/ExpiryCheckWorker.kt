package com.example.smartsous.core.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.data.local.dao.IngredientDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ingredientDao: IngredientDao,
    private val dataStoreManager: DataStoreManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "expiry_check_worker"
        private const val TAG = "ExpiryCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "ExpiryCheckWorker started")

            val notifications = dataStoreManager.notificationPreferenceFlow.first()
            if (!notifications.expiryRemindersEnabled) {
                Log.d(TAG, "Expiry reminders disabled")
                return Result.success()
            }

            val today = LocalDate.now()
            val threshold = today
                .plusDays(ExpiryNotificationPolicy.WARNING_WINDOW_DAYS)
                .toString()
            val expiringIngredients = ingredientDao.getExpiring(threshold).first()

            Log.d(TAG, "Found ${expiringIngredients.size} expiring ingredients")

            expiringIngredients.forEach { entity ->
                val expiryDateText = entity.expiryDate ?: return@forEach
                val daysLeft = try {
                    val expiryDate = LocalDate.parse(expiryDateText)
                    ChronoUnit.DAYS.between(today, expiryDate).toInt()
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid expiry date for ${entity.name}: $expiryDateText")
                    return@forEach
                }

                if (ExpiryNotificationPolicy.shouldNotify(daysLeft)) {
                    notificationHelper.showExpiryNotification(
                        ingredientName = entity.name,
                        daysLeft = daysLeft,
                        notificationId = ExpiryNotificationPolicy.notificationId(
                            ingredientId = entity.id,
                            daysLeft = daysLeft
                        )
                    )
                    Log.d(TAG, "Sent expiry notification: ${entity.name}, daysLeft=$daysLeft")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ExpiryCheckWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
