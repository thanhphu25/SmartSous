package com.example.smartsous.core.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartsous.data.local.dao.IngredientDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ingredientDao: IngredientDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "expiry_check_worker"
        private const val TAG = "ExpiryCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "ExpiryCheckWorker bắt đầu chạy...")

            // Ngưỡng: hết hạn trong 3 ngày tới
            val threshold = LocalDate.now().plusDays(3).toString()
            val expiringIngredients = ingredientDao.getExpiring(threshold).first()

            Log.d(TAG, "Tìm thấy ${expiringIngredients.size} nguyên liệu sắp hết hạn")

            expiringIngredients.forEachIndexed { index, entity ->
                if (entity.expiryDate == null) return@forEachIndexed

                val expiryDate = LocalDate.parse(entity.expiryDate)
                val daysLeft = java.time.temporal.ChronoUnit.DAYS
                    .between(LocalDate.now(), expiryDate)
                    .toInt()

                // Chỉ thông báo đúng 3 mức: 0, 1, 3 ngày
                if (daysLeft in listOf(0, 1, 3)) {
                    notificationHelper.showExpiryNotification(
                        ingredientName = entity.name,
                        daysLeft = daysLeft,
                        notificationId = 1000 + index  // ID unique cho mỗi notification
                    )
                    Log.d(TAG, "Đã gửi notification: ${entity.name}, còn $daysLeft ngày")
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ExpiryCheckWorker lỗi: ${e.message}", e)
            // Retry tối đa 3 lần nếu thất bại
            Result.retry()
        }
    }
}