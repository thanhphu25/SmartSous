package com.example.smartsous.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartsous.data.local.dao.*
import com.example.smartsous.data.local.entity.*
// room db
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        MealPlanEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartSousDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun chatMessageDao(): ChatMessageDao
}