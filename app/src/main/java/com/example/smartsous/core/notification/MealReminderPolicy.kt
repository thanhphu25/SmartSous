package com.example.smartsous.core.notification

import com.example.smartsous.data.local.entity.MealPlanEntity

object MealReminderPolicy {
    const val REMINDER_HOUR = 7
    const val REMINDER_MINUTE = 30
    const val NOTIFICATION_ID = 2000

    fun buildMealSummary(todayPlans: List<MealPlanEntity>): String {
        val mealLines = todayPlans
            .sortedBy { mealOrder(it.mealType) }
            .mapNotNull { plan ->
                val recipeCount = countRecipes(plan.recipeIdsJson)
                if (recipeCount <= 0) return@mapNotNull null
                "- ${mealLabel(plan.mealType)}: $recipeCount món"
            }

        return if (mealLines.isEmpty()) {
            "Hôm nay chưa có kế hoạch - mở SmartSous để lên thực đơn nhé!"
        } else {
            "Thực đơn hôm nay:\n${mealLines.joinToString("\n")}"
        }
    }

    fun countRecipes(recipeIdsJson: String): Int {
        val json = recipeIdsJson.trim()
        if (!json.startsWith("[") || !json.endsWith("]")) return 0

        return Regex(""""(?:\\.|[^"\\])*"""")
            .findAll(json)
            .count()
    }

    private fun mealLabel(mealType: String): String =
        when (mealType) {
            "BREAKFAST" -> "Sáng"
            "LUNCH" -> "Trưa"
            "DINNER" -> "Tối"
            else -> "Phụ"
        }

    private fun mealOrder(mealType: String): Int =
        when (mealType) {
            "BREAKFAST" -> 0
            "LUNCH" -> 1
            "DINNER" -> 2
            "SNACK" -> 3
            else -> 4
        }
}
