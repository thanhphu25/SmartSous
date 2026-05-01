package com.example.smartsous.domain.model

import java.time.LocalDate

data class Ingredient(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: IngredientCategory,
    val expiryDate: LocalDate? = null,
    val addedDate: LocalDate = LocalDate.now()
)

enum class IngredientCategory {
    VEGETABLE, MEAT, SEAFOOD, DAIRY, GRAIN, SPICE, FRUIT, BEVERAGE, OTHER
}