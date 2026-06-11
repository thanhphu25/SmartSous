package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.domain.model.UserPreference
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class SuggestMealsUseCase @Inject constructor() {

    companion object {
        private const val WEIGHT_INGREDIENT = 0.50f
        private const val WEIGHT_NUTRITION = 0.30f
        private const val WEIGHT_VARIETY = 0.20f
        private const val IDEAL_CALORIES_MIN = 200
        private const val IDEAL_CALORIES_MAX = 500
    }

    operator fun invoke(
        allRecipes: List<Recipe>,
        pantryIngredients: List<Ingredient>,
        recentlyCookedIds: List<String> = emptyList(),
        userPreference: UserPreference = UserPreference(),
        topN: Int = 10
    ): List<SuggestedRecipe> {
        if (allRecipes.isEmpty()) return emptyList()

        val pantryNames = pantryIngredients
            .map { it.name.lowercase().trim() }
            .toSet()

        return allRecipes
            .filterNot { recipe -> violatesHardPreference(recipe, userPreference) }
            .map { recipe ->
                scoreRecipe(
                    recipe = recipe,
                    pantryNames = pantryNames,
                    recentlyCookedIds = recentlyCookedIds,
                    userPreference = userPreference
                )
            }
            .filter { it.score > 10f }
            .sortedByDescending { it.score }
            .take(topN)
    }

    private fun scoreRecipe(
        recipe: Recipe,
        pantryNames: Set<String>,
        recentlyCookedIds: List<String>,
        userPreference: UserPreference
    ): SuggestedRecipe {
        val recipeIngredientNames = recipe.ingredients
            .map { it.name.lowercase().trim() }

        val matched = recipeIngredientNames.filter { ingName ->
            pantryNames.any { pantry ->
                ingName.contains(pantry) || pantry.contains(ingName)
            }
        }

        val missing = recipeIngredientNames.filter { ingName ->
            !pantryNames.any { pantry ->
                ingName.contains(pantry) || pantry.contains(ingName)
            }
        }

        val matchPercent = if (recipeIngredientNames.isEmpty()) {
            0
        } else {
            ((matched.size.toFloat() / recipeIngredientNames.size) * 100).roundToInt()
        }

        val ingredientScore = matchPercent.toFloat()
        val nutritionScore = nutritionScore(recipe, userPreference)
        val varietyScore = if (recipe.id in recentlyCookedIds) 0f else 100f

        var totalScore = (ingredientScore * WEIGHT_INGREDIENT)
            .plus(nutritionScore * WEIGHT_NUTRITION)
            .plus(varietyScore * WEIGHT_VARIETY)

        if (recipe.cookingTimeMinutes <= userPreference.maxCookingTimeMinutes) {
            totalScore += 10f
        } else {
            totalScore -= 10f
        }
        if (matchesFavoriteCuisine(recipe, userPreference.favoriteCuisines)) {
            totalScore += 10f
        }
        if (userPreference.preferHighProtein && recipe.nutrition.protein >= 25.0) {
            totalScore += 8f
        }
        if (userPreference.preferLowFat && recipe.nutrition.fat <= 15.0) {
            totalScore += 8f
        }

        val reason = when {
            matchPercent == 100 -> SuggestionReason.PERFECT_MATCH
            matchPercent >= 70 -> SuggestionReason.HIGH_MATCH
            recipe.cookingTimeMinutes <= userPreference.maxCookingTimeMinutes -> SuggestionReason.QUICK_COOK
            nutritionScore >= 90f -> SuggestionReason.HEALTHY_CHOICE
            else -> SuggestionReason.NOT_COOKED_RECENTLY
        }

        return SuggestedRecipe(
            recipe = recipe,
            score = totalScore,
            matchedIngredients = matched,
            missingIngredients = missing,
            matchPercent = matchPercent,
            reason = reason
        )
    }

    private fun nutritionScore(
        recipe: Recipe,
        userPreference: UserPreference
    ): Float {
        val calories = recipe.nutrition.calories
        val targetCalories = userPreference.targetCaloriesPerMeal.coerceAtLeast(1)
        val calorieDiff = abs(calories - targetCalories)

        return when {
            calorieDiff <= 75 -> 100f
            calorieDiff <= 150 -> 75f
            calories in IDEAL_CALORIES_MIN..IDEAL_CALORIES_MAX -> 65f
            calories <= targetCalories + 300 -> 45f
            else -> 20f
        }
    }

    private fun violatesHardPreference(
        recipe: Recipe,
        userPreference: UserPreference
    ): Boolean {
        val ingredientNames = recipe.ingredients.map { it.name.lowercase().trim() }
        val blockedKeywords = (userPreference.allergies + userPreference.dislikedIngredients)
            .flatMap { preferenceKeywords(it) }

        if (blockedKeywords.any { keyword ->
                ingredientNames.any { ingredient -> ingredient.contains(keyword) }
            }
        ) {
            return true
        }

        if (!userPreference.vegetarian) return false

        val nonVegetarianKeywords = listOf(
            "thịt", "bò", "heo", "lợn", "gà", "vịt", "cá", "tôm", "cua", "mực",
            "beef", "pork", "chicken", "duck", "fish", "shrimp", "crab", "squid"
        )
        return nonVegetarianKeywords.any { keyword ->
            ingredientNames.any { ingredient -> ingredient.contains(keyword) }
        }
    }

    private fun preferenceKeywords(value: String): List<String> {
        val normalized = value.lowercase().trim()
        return when (normalized) {
            "hải sản" -> listOf("hải sản", "tôm", "cua", "mực", "cá", "nghêu", "sò", "seafood")
            "sữa" -> listOf("sữa", "phô mai", "bơ", "kem", "yogurt", "milk", "cheese", "butter")
            "đậu phộng" -> listOf("đậu phộng", "lạc", "peanut")
            "trứng" -> listOf("trứng", "egg")
            "gluten" -> listOf("gluten", "bột mì", "bánh mì", "mì", "wheat")
            else -> listOf(normalized)
        }
    }

    private fun matchesFavoriteCuisine(
        recipe: Recipe,
        favoriteCuisines: List<String>
    ): Boolean {
        if (favoriteCuisines.isEmpty()) return false

        val cuisine = recipe.cuisine.lowercase()
        return favoriteCuisines.any { favorite ->
            val keywords = when (favorite.lowercase()) {
                "việt" -> listOf("việt", "vietnam", "vietnamese")
                "nhật" -> listOf("nhật", "japan", "japanese")
                "hàn" -> listOf("hàn", "korea", "korean")
                "ý" -> listOf("ý", "italy", "italian")
                "thái" -> listOf("thái", "thai")
                "âu" -> listOf("âu", "europe", "western")
                else -> listOf(favorite.lowercase())
            }
            keywords.any { cuisine.contains(it) }
        }
    }
}
