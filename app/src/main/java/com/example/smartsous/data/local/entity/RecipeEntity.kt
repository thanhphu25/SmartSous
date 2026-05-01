package com.example.smartsous.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: String,
    val ingredientsJson: String,   // serialize thành JSON
    val stepsJson: String,
    val nutritionJson: String,
    val tagsJson: String,
    val cuisine: String,
    val isFavorite: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)