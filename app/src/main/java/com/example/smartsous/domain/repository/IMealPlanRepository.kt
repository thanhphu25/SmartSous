package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.MealPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IMealPlanRepository {
    fun getMealPlanForWeek(startDate: LocalDate): Flow<List<MealPlan>>
    suspend fun addRecipeToPlan(date: LocalDate, mealType: com.example.smartsous.domain.model.MealType, recipeId: String)
    suspend fun removeRecipeFromPlan(date: LocalDate, mealType: com.example.smartsous.domain.model.MealType, recipeId: String)
}