package com.example.smartsous.data.local.entity

import androidx.room.Entity

@Entity(tableName = "meal_plans", primaryKeys = ["date", "mealType"])
data class MealPlanEntity(
    val date: String,           // "2025-08-20"
    val mealType: String,       // "BREAKFAST"
    val recipeIdsJson: String   // ["id1","id2"]
)