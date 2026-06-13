package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Recipe
import java.text.Normalizer

object RecipeNameSearchMatcher {

    private val combiningMarksRegex = "\\p{Mn}+".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()

    fun filterByName(recipes: List<Recipe>, query: String): List<Recipe> =
        if (query.isBlank()) {
            recipes
        } else {
            recipes.filter { recipe -> matches(recipe.name, query) }
        }

    fun matches(recipeName: String, query: String): Boolean {
        if (query.isBlank()) return true

        val stripDiacritics = !query.hasSearchDiacritics()
        val queryTokens = query.normalizeForSearch(stripDiacritics).toSearchTokens()
        if (queryTokens.isEmpty()) return true

        return recipeName
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
}
