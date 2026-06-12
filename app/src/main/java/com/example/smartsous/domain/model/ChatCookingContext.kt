package com.example.smartsous.domain.model

data class ChatCookingContext(
    val pantryIngredients: List<Ingredient> = emptyList(),
    val expiringSoonIngredients: List<Ingredient> = emptyList(),
    val recommendedRecipes: List<SuggestedRecipe> = emptyList(),
    val relevantRecipes: List<Recipe> = emptyList(),
    val userPreference: UserPreference = UserPreference()
) {
    val hasRecipeContext: Boolean
        get() = recommendedRecipes.isNotEmpty() || relevantRecipes.isNotEmpty()
}
