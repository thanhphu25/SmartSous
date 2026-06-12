package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.domain.model.UserPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
                ingredient("tomato", quantity = 2.0, unit = "item"),
                ingredient("beef", quantity = 300.0, unit = "g")
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
            pantryIngredients = listOf(ingredient("tomato", unit = "item")),
            topN = 2
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.matchPercent == 100 })
    }

    @Test
    fun invoke_prioritizesRecipesUsingExpiringIngredients() {
        val expiringRecipe = recipe(
            id = "expiring",
            ingredients = listOf(RecipeIngredient("spinach", 1.0, "unit"))
        )
        val regularRecipe = recipe(
            id = "regular",
            ingredients = listOf(RecipeIngredient("tomato", 1.0, "unit"))
        )

        val result = useCase(
            allRecipes = listOf(regularRecipe, expiringRecipe),
            pantryIngredients = listOf(
                ingredient(
                    name = "spinach",
                    expiryDate = LocalDate.of(2026, 6, 13)
                ),
                ingredient(
                    name = "tomato",
                    expiryDate = LocalDate.of(2026, 6, 25)
                )
            ),
            currentDate = LocalDate.of(2026, 6, 12)
        )

        assertEquals("expiring", result.first().recipe.id)
    }

    @Test
    fun invoke_penalizesPartialQuantityMatch() {
        val recipe = recipe(
            id = "beef-stew",
            ingredients = listOf(RecipeIngredient("beef", 300.0, "g"))
        )

        val result = useCase(
            allRecipes = listOf(recipe),
            pantryIngredients = listOf(
                ingredient(
                    name = "beef",
                    quantity = 100.0,
                    unit = "g"
                )
            )
        )

        assertTrue(result.first().matchPercent < 100)
        assertEquals(listOf("beef"), result.first().missingIngredients)
    }

    @Test
    fun invoke_boostsFavoriteRecipesWhenScoresAreClose() {
        val favoriteRecipe = recipe(
            id = "favorite",
            ingredients = listOf(RecipeIngredient("egg", 1.0, "unit")),
            isFavorite = true
        )
        val normalRecipe = recipe(
            id = "normal",
            ingredients = listOf(RecipeIngredient("egg", 1.0, "unit"))
        )

        val result = useCase(
            allRecipes = listOf(normalRecipe, favoriteRecipe),
            pantryIngredients = listOf(ingredient("egg"))
        )

        assertEquals("favorite", result.first().recipe.id)
    }

    @Test
    fun invoke_doesNotTreatTomatoAsFishForVegetarianPreference() {
        val tomatoRecipe = recipe(
            id = "tomato-soup",
            ingredients = listOf(RecipeIngredient("cà chua", 2.0, "item"))
        )

        val result = useCase(
            allRecipes = listOf(tomatoRecipe),
            pantryIngredients = listOf(ingredient("cà chua", quantity = 2.0, unit = "item")),
            userPreference = UserPreference(vegetarian = true)
        )

        assertEquals("tomato-soup", result.first().recipe.id)
    }

    @Test
    fun invoke_returnsCorrectContextBasedOnServings() {
        val perfectMatch = recipe(
            id = "perfect",
            ingredients = listOf(RecipeIngredient("tomato", 2.0, "item")),
            servings = 4
        )

        // userDesiredServings = 2 -> requires 1 tomato. We have 1 tomato -> PERFECT_MATCH.
        val result = useCase(
            allRecipes = listOf(perfectMatch),
            pantryIngredients = listOf(ingredient("tomato", quantity = 1.0, unit = "item")),
            userDesiredServings = 2
        )

        assertEquals("perfect", result.first().recipe.id)
        assertEquals(SuggestionReason.PERFECT_MATCH, result.first().reason)
        assertEquals("Tủ lạnh có đủ nguyên liệu để nấu món này cho 2 người.", result.first().context)
    }

    private fun ingredient(
        name: String,
        quantity: Double = 1.0,
        unit: String = "unit",
        expiryDate: LocalDate? = null
    ) = Ingredient(
        id = name,
        name = name,
        quantity = quantity,
        unit = unit,
        category = IngredientCategory.OTHER,
        expiryDate = expiryDate
    )

    private fun recipe(
        id: String,
        ingredients: List<RecipeIngredient>,
        isFavorite: Boolean = false,
        servings: Int = 2
    ) = Recipe(
        id = id,
        name = id,
        description = "A test recipe",
        imageUrl = "",
        cookingTimeMinutes = 20,
        servings = servings,
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
        cuisine = "Vietnamese",
        isFavorite = isFavorite
    )
}
