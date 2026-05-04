package com.example.smartsous.core.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "meal_reminder_worker"
        private const val TAG = "MealReminderWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now().toString()

            // Lấy tất cả meal plans của hôm nay
            val todayPlans = mealPlanDao
                .getForWeek(today, today)
                .first()

            val summary = if (todayPlans.isEmpty()) {
                "Hôm nay chưa có kế hoạch — mở SmartSous để lên thực đơn nhé!"
            } else {
                // Build summary từ danh sách meal plans
                val mealLines = todayPlans.joinToString("\n") { plan ->
                    val mealLabel = when (plan.mealType) {
                        "BREAKFAST" -> "Sáng"
                        "LUNCH"     -> "Trưa"
                        "DINNER"    -> "Tối"
                        else        -> "Phụ"
                    }
                    "• $mealLabel: có ${countRecipes(plan.recipeIdsJson)} món"
                }
                "Thực đơn hôm nay:\n$mealLines"
            }

            notificationHelper.showMealReminderNotification(summary)
            Log.d(TAG, "Đã gửi meal reminder: $summary")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "MealReminderWorker lỗi: ${e.message}", e)
            Result.retry()
        }
    }

    // Đếm số recipe trong JSON array string
    private fun countRecipes(recipeIdsJson: String): Int {
        return try {
            org.json.JSONArray(recipeIdsJson).length()
        } catch (e: Exception) { 0 }
    }
}