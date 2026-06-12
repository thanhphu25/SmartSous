package com.example.smartsous.domain.model

data class ChatCookingContext(
    val recommendedRecipes: List<SuggestedRecipe> = emptyList(),
    val relevantRecipes: List<Recipe> = emptyList(),
    val expiringSoonIngredients: List<Ingredient> = emptyList(),
    val userPreference: UserPreference = UserPreference()
)
