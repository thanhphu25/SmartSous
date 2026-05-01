package com.example.smartsous.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_ingredients")
data class IngredientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val expiryDate: String?,    // "2025-09-15"
    val addedDate: String
)