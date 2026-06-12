package com.example.smartsous.domain.model

data class SuggestedRecipe(
    val recipe: Recipe,
    val score: Float,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val matchPercent: Int,
    val reason: SuggestionReason,
    val context: String? = null
)

enum class SuggestionReason {
    PERFECT_MATCH,
    HIGH_MATCH,
    USE_EXPIRING_SOON,
    FAVORITE_PICK,
    HEALTHY_CHOICE,
    QUICK_COOK,
    NOT_COOKED_RECENTLY
}
