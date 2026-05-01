package com.example.smartsous.data.local.dao

import androidx.room.*
import com.example.smartsous.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("""
        SELECT * FROM meal_plans
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    fun getForWeek(startDate: String, endDate: String): Flow<List<MealPlanEntity>>

    @Upsert
    suspend fun upsert(plan: MealPlanEntity)

    @Query("DELETE FROM meal_plans WHERE date = :date AND mealType = :mealType")
    suspend fun delete(date: String, mealType: String)
}