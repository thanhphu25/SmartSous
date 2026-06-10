package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.MealPlan
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GenerateShoppingListUseCaseTest {

    @Test
    fun invoke_returnsMissingIngredientsAfterSubtractingPantry() = runBlocking {
        val startDate = LocalDate.of(2026, 6, 10)
        val useCase = GenerateShoppingListUseCase(
            mealPlanRepo = FakeMealPlanRepository(
                plans = listOf(
                    MealPlan(
                        id = "plan-1",
                        date = startDate,
                        meals = mapOf(
                            MealType.LUNCH to listOf("beef-soup"),
                            MealType.DINNER to listOf("beef-soup")
                        )
                    )
                )
            ),
            recipeRepo = FakeRecipeRepository(
                recipes = mapOf(
                    "beef-soup" to recipe(
                        id = "beef-soup",
                        ingredients = listOf(
                            RecipeIngredient("beef", 200.0, "g"),
                            RecipeIngredient("tomato", 2.0, "item")
                        )
                    )
                )
            ),
            pantryRepo = FakePantryRepository(
                ingredients = listOf(
                    ingredient(name = "beef", quantity = 150.0, unit = "g"),
                    ingredient(name = "tomato", quantity = 5.0, unit = "item")
                )
            )
        )

        val result = useCase(startDate).first()

        assertEquals(1, result.size)
        assertEquals("Beef", result.first().name)
        assertEquals(250.0, result.first().amountToBuy, 0.001)
        assertEquals("g", result.first().unit)
    }

    @Test
    fun invoke_groupsIngredientsByNameAndUnit() = runBlocking {
        val startDate = LocalDate.of(2026, 6, 10)
        val useCase = GenerateShoppingListUseCase(
            mealPlanRepo = FakeMealPlanRepository(
                plans = listOf(
                    MealPlan(
                        id = "plan-1",
                        date = startDate,
                        meals = mapOf(MealType.BREAKFAST to listOf("a", "b"))
                    )
                )
            ),
            recipeRepo = FakeRecipeRepository(
                recipes = mapOf(
                    "a" to recipe("a", listOf(RecipeIngredient("egg", 2.0, "item"))),
                    "b" to recipe("b", listOf(RecipeIngredient("egg", 1.0, "item")))
                )
            ),
            pantryRepo = FakePantryRepository(
                ingredients = listOf(ingredient("egg", 1.0, "item"))
            )
        )

        val result = useCase(startDate).first()

        assertEquals(1, result.size)
        assertEquals("Egg", result.first().name)
        assertEquals(2.0, result.first().amountToBuy, 0.001)
        assertEquals("item", result.first().unit)
    }

    private class FakeMealPlanRepository(
        private val plans: List<MealPlan>
    ) : IMealPlanRepository {
        override fun getMealPlanForWeek(startDate: LocalDate): Flow<List<MealPlan>> = flowOf(plans)
        override suspend fun addRecipeToPlan(date: LocalDate, mealType: MealType, recipeId: String) = Unit
        override suspend fun removeRecipeFromPlan(date: LocalDate, mealType: MealType, recipeId: String) = Unit
    }

    private class FakeRecipeRepository(
        private val recipes: Map<String, Recipe>
    ) : IRecipeRepository {
        override fun getAllRecipes(): Flow<List<Recipe>> = flowOf(recipes.values.toList())
        override fun getFavorites(): Flow<List<Recipe>> = flowOf(recipes.values.filter { it.isFavorite })
        override fun searchRecipes(query: String): Flow<List<Recipe>> = flowOf(emptyList())
        override suspend fun getRecipeById(id: String): Recipe? = recipes[id]
        override suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean) = Unit
        override suspend fun refreshFromRemote() = Unit
    }

    private class FakePantryRepository(
        private val ingredients: List<Ingredient>
    ) : IPantryRepository {
        override fun getAllIngredients(): Flow<List<Ingredient>> = flowOf(ingredients)
        override fun getExpiringIngredients(withinDays: Int): Flow<List<Ingredient>> = flowOf(emptyList())
        override suspend fun upsert(ingredient: Ingredient) = Unit
        override suspend fun delete(ingredient: Ingredient) = Unit
        override suspend fun updateQuantity(id: String, quantity: Double) = Unit
    }

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
        nutrition = Nutrition(300, 10.0, 20.0, 5.0, 2.0),
        tags = listOf("test"),
        cuisine = "Vietnamese"
    )

    private fun ingredient(
        name: String,
        quantity: Double,
        unit: String
    ) = Ingredient(
        id = name,
        name = name,
        quantity = quantity,
        unit = unit,
        category = IngredientCategory.OTHER
    )
}
