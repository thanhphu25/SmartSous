package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SearchFilter
import java.text.Normalizer
import javax.inject.Inject

class SearchRecipesUseCase @Inject constructor() {

    private val combiningMarksRegex = "\\p{Mn}+".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()

    // Nhận list recipes + filter → trả về list đã lọc + sort
    operator fun invoke(
        recipes: List<Recipe>,
        filter: SearchFilter
    ): List<Recipe> {
        if (filter.isEmpty) return recipes

        return recipes
            .filter { recipe -> matchesQuery(recipe, filter.query) }
            .filter { recipe -> matchesCuisine(recipe, filter.selectedCuisines) }
            .filter { recipe -> matchesDifficulty(recipe, filter.selectedDifficulty) }
            .filter { recipe -> matchesCookingTime(recipe, filter.maxCookingTime) }
            .filter { recipe -> matchesCalories(recipe, filter.maxCalories) }
            .filter { recipe -> matchesFavorites(recipe, filter.onlyFavorites) }
            // Sort: yêu thích lên đầu, sau đó theo tên
            .sortedWith(
                compareByDescending<Recipe> { it.isFavorite }
                    .thenBy { it.name }
            )
    }

    private fun matchesQuery(recipe: Recipe, query: String): Boolean {
        if (query.isBlank()) return true
        val stripDiacritics = !query.hasSearchDiacritics()
        val queryTokens = query.normalizeForSearch(stripDiacritics).toSearchTokens()
        if (queryTokens.isEmpty()) return true

        return recipe.name
            .normalizeForSearch(stripDiacritics)
            .toSearchTokens()
            .matchesQueryTokens(queryTokens)
    }

    private fun List<String>.matchesQueryTokens(queryTokens: List<String>): Boolean {
        return queryTokens.all { queryToken ->
            any { nameToken ->
                if (queryToken.length <= 2) {
                    nameToken == queryToken
                } else {
                    nameToken.startsWith(queryToken)
                }
            }
        }
    }

    private fun String.normalizeForSearch(stripDiacritics: Boolean): String {
        val normalized = Normalizer.normalize(
            trim().lowercase(),
            if (stripDiacritics) Normalizer.Form.NFD else Normalizer.Form.NFC
        )
        val searchable = if (stripDiacritics) {
            normalized
                .replace(combiningMarksRegex, "")
                .replace('đ', 'd')
        } else {
            normalized
        }
        return searchable.replace(whitespaceRegex, " ")
    }

    private fun String.hasSearchDiacritics(): Boolean {
        val normalized = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        val folded = normalized
            .replace(combiningMarksRegex, "")
            .replace('đ', 'd')
        return normalized != folded
    }

    private fun String.toSearchTokens(): List<String> =
        split(" ").filter { it.isNotBlank() }

    private fun matchesCuisine(
        recipe: Recipe,
        cuisines: Set<String>
    ): Boolean {
        if (cuisines.isEmpty()) return true
        return recipe.cuisine in cuisines
    }

    private fun matchesDifficulty(
        recipe: Recipe,
        difficulties: Set<com.example.smartsous.domain.model.Difficulty>
    ): Boolean {
        if (difficulties.isEmpty()) return true
        return recipe.difficulty in difficulties
    }

    private fun matchesCookingTime(
        recipe: Recipe,
        maxMinutes: Int?
    ): Boolean {
        if (maxMinutes == null) return true
        return recipe.cookingTimeMinutes <= maxMinutes
    }

    private fun matchesCalories(
        recipe: Recipe,
        maxCalories: Int?
    ): Boolean {
        if (maxCalories == null) return true
        return recipe.nutrition.calories <= maxCalories
    }

    private fun matchesFavorites(
        recipe: Recipe,
        onlyFavorites: Boolean
    ): Boolean {
        if (!onlyFavorites) return true
        return recipe.isFavorite
    }
}
