package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.model.SearchFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRecipesUseCaseTest {

    private val useCase = SearchRecipesUseCase()

    @Test
    fun invoke_filtersByIngredientNameAndSortsFavoritesFirst() {
        val recipes = listOf(
            recipe(
                id = "beef-stir-fry",
                name = "Beef stir fry",
                ingredients = listOf(RecipeIngredient("beef", 300.0, "g")),
                isFavorite = false
            ),
            recipe(
                id = "favorite-beef-soup",
                name = "Favorite beef soup",
                ingredients = listOf(RecipeIngredient("beef", 200.0, "g")),
                isFavorite = true
            ),
            recipe(
                id = "morning-salad",
                name = "Morning salad",
                ingredients = listOf(RecipeIngredient("lettuce", 100.0, "g")),
                isFavorite = true
            )
        )

        val result = useCase(
            recipes = recipes,
            filter = SearchFilter(query = "beef")
        )

        assertEquals(listOf("favorite-beef-soup", "beef-stir-fry"), result.map { it.id })
    }

    @Test
    fun invoke_appliesCuisineDifficultyTimeCaloriesAndFavoriteFilters() {
        val recipes = listOf(
            recipe(
                id = "quick-vn-favorite",
                cuisine = "Vietnamese",
                difficulty = Difficulty.EASY,
                cookingTimeMinutes = 15,
                calories = 250,
                isFavorite = true
            ),
            recipe(
                id = "slow-vn-favorite",
                cuisine = "Vietnamese",
                difficulty = Difficulty.EASY,
                cookingTimeMinutes = 90,
                calories = 250,
                isFavorite = true
            ),
            recipe(
                id = "quick-jp-favorite",
                cuisine = "Japanese",
                difficulty = Difficulty.EASY,
                cookingTimeMinutes = 15,
                calories = 250,
                isFavorite = true
            )
        )

        val result = useCase(
            recipes = recipes,
            filter = SearchFilter(
                selectedCuisines = setOf("Vietnamese"),
                selectedDifficulty = setOf(Difficulty.EASY),
                maxCookingTime = 30,
                maxCalories = 300,
                onlyFavorites = true
            )
        )

        assertEquals(listOf("quick-vn-favorite"), result.map { it.id })
    }

    private fun recipe(
        id: String,
        name: String = id,
        description: String = "A test recipe",
        cuisine: String = "Vietnamese",
        difficulty: Difficulty = Difficulty.EASY,
        cookingTimeMinutes: Int = 20,
        calories: Int = 300,
        ingredients: List<RecipeIngredient> = listOf(RecipeIngredient("tomato", 2.0, "item")),
        isFavorite: Boolean = false
    ) = Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = "",
        cookingTimeMinutes = cookingTimeMinutes,
        servings = 2,
        difficulty = difficulty,
        ingredients = ingredients,
        steps = listOf("Cook"),
        nutrition = Nutrition(calories, 10.0, 20.0, 5.0, 2.0),
        tags = listOf("test"),
        cuisine = cuisine,
        isFavorite = isFavorite
    )
}
