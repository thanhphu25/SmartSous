package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.model.SuggestionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestMealsUseCaseTest {

    private val useCase = SuggestMealsUseCase()

    @Test
    fun invoke_prioritizesRecipesWithMatchingPantryIngredients() {
        val perfectMatch = recipe(
            id = "perfect",
            ingredients = listOf(
                RecipeIngredient("tomato", 2.0, "item"),
                RecipeIngredient("beef", 300.0, "g")
            )
        )
        val partialMatch = recipe(
            id = "partial",
            ingredients = listOf(
                RecipeIngredient("tomato", 2.0, "item"),
                RecipeIngredient("egg", 2.0, "item")
            )
        )

        val result = useCase(
            allRecipes = listOf(partialMatch, perfectMatch),
            pantryIngredients = listOf(
                ingredient("tomato"),
                ingredient("beef")
            )
        )

        assertEquals("perfect", result.first().recipe.id)
        assertEquals(SuggestionReason.PERFECT_MATCH, result.first().reason)
        assertEquals(100, result.first().matchPercent)
    }

    @Test
    fun invoke_limitsResultsToTopN() {
        val recipes = (1..5).map { index ->
            recipe(
                id = "recipe-$index",
                ingredients = listOf(RecipeIngredient("tomato", 1.0, "item"))
            )
        }

        val result = useCase(
            allRecipes = recipes,
            pantryIngredients = listOf(ingredient("tomato")),
            topN = 2
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.matchPercent == 100 })
    }

    private fun ingredient(name: String) = Ingredient(
        id = name,
        name = name,
        quantity = 1.0,
        unit = "unit",
        category = IngredientCategory.OTHER
    )

    private fun recipe(
        id: String,
        ingredients: List<RecipeIngredient>
    ) = Recipe(
        id = id,
        name = id,
        description = "A test recipe",
        imageUrl = "",
        cookingTimeMinutes = 20,
        servings = 2,
        difficulty = Difficulty.EASY,
        ingredients = ingredients,
        steps = listOf("Cook"),
        nutrition = Nutrition(
            calories = 300,
            protein = 10.0,
            carbs = 20.0,
            fat = 5.0,
            fiber = 2.0
        ),
        tags = listOf("test"),
        cuisine = "Vietnamese"
    )
}
