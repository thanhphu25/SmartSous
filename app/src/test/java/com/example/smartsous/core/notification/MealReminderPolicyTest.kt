package com.example.smartsous.core.notification

import com.example.smartsous.data.local.entity.MealPlanEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealReminderPolicyTest {

    @Test
    fun buildMealSummary_returnsEmptyPlanMessageWhenNoMealPlanExists() {
        val summary = MealReminderPolicy.buildMealSummary(emptyList())

        assertEquals(
            "Hôm nay chưa có kế hoạch - mở SmartSous để lên thực đơn nhé!",
            summary
        )
    }

    @Test
    fun buildMealSummary_countsRecipesByMealTypeInDailyOrder() {
        val plans = listOf(
            MealPlanEntity(
                date = "2026-06-09",
                mealType = "DINNER",
                recipeIdsJson = """["pho","com-tam","canh-chua"]"""
            ),
            MealPlanEntity(
                date = "2026-06-09",
                mealType = "BREAKFAST",
                recipeIdsJson = """["banh-mi"]"""
            )
        )

        val summary = MealReminderPolicy.buildMealSummary(plans)

        assertEquals(
            "Thực đơn hôm nay:\n- Sáng: 1 món\n- Tối: 3 món",
            summary
        )
    }

    @Test
    fun countRecipes_returnsZeroForInvalidJson() {
        assertTrue(MealReminderPolicy.countRecipes("not-json") == 0)
    }
}
