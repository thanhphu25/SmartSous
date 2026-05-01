package com.example.smartsous.domain.model

import java.time.LocalDate

data class MealPlan(
    val id: String,
    val date: LocalDate,
    val meals: Map<MealType, List<String>> // MealType -> List<recipeId>
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }