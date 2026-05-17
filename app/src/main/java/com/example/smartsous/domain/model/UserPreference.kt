package com.example.smartsous.domain.model

data class UserPreference(
    val targetCaloriesPerMeal: Int = 400,   // calories lý tưởng mỗi bữa
    val favoriteCuisines: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val preferLowFat: Boolean = false,
    val preferHighProtein: Boolean = false,
    val maxCookingTimeMinutes: Int = 60
)