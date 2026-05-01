package com.example.smartsous.domain.model

data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val nutrition: Nutrition,
    val tags: List<String>,
    val cuisine: String,
    val isFavorite: Boolean = false
)

data class RecipeIngredient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class Nutrition(
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double
)

enum class Difficulty { EASY, MEDIUM, HARD }