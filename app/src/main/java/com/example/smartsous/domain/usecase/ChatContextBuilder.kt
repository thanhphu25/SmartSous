package com.example.smartsous.domain.usecase

import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.domain.model.ChatCookingContext
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IRecipeRepository
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChatContextBuilder @Inject constructor(
    private val recipeRepository: IRecipeRepository,
    private val dataStoreManager: DataStoreManager,
    private val suggestMealsUseCase: SuggestMealsUseCase
) {
    suspend fun build(
        intent: ChatIntent,
        userMessage: String,
        currentPantry: List<Ingredient>,
        topN: Int = 5
    ): ChatCookingContext = withContext(Dispatchers.Default) {
        val allRecipes = recipeRepository.getAllRecipes().first()
        val preferences = dataStoreManager.userPreferencesFlow.first()
        val pantryForIntent = when (intent) {
            is ChatIntent.SuggestFromIngredients -> intent.ingredients.toSyntheticIngredients()
            else -> currentPantry
        }

        val recommended = suggestMealsUseCase(
            allRecipes = allRecipes,
            pantryIngredients = pantryForIntent,
            userPreference = preferences,
            topN = topN
        )

        ChatCookingContext(
            pantryIngredients = currentPantry,
            expiringSoonIngredients = currentPantry.expiringSoon(),
            recommendedRecipes = recommended,
            relevantRecipes = findRelevantRecipes(allRecipes, intent, userMessage),
            userPreference = preferences
        )
    }

    private fun List<String>.toSyntheticIngredients(): List<Ingredient> =
        map { name ->
            Ingredient(
                id = "chat-${UUID.randomUUID()}",
                name = name,
                quantity = 1.0,
                unit = "don vi",
                category = IngredientCategory.OTHER
            )
        }

    private fun List<Ingredient>.expiringSoon(
        today: LocalDate = LocalDate.now()
    ): List<Ingredient> =
        filter { ingredient ->
            ingredient.expiryDate?.let { date ->
                ChronoUnit.DAYS.between(today, date) in 0..3
            } ?: false
        }

    private fun findRelevantRecipes(
        allRecipes: List<Recipe>,
        intent: ChatIntent,
        userMessage: String
    ): List<Recipe> {
        val query = when (intent) {
            is ChatIntent.SearchRecipe -> intent.query
            is ChatIntent.AskRecipeDetail -> intent.query
            else -> userMessage
        }.normalizedTokens()

        if (query.isEmpty()) return emptyList()

        return allRecipes
            .map { recipe -> recipe to recipe.relevanceScore(query) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (recipe, _) -> recipe }
            .take(5)
    }

    private fun Recipe.relevanceScore(queryTokens: Set<String>): Int {
        val searchable = buildString {
            append(name)
            append(' ')
            append(description)
            append(' ')
            append(cuisine)
            append(' ')
            append(tags.joinToString(" "))
            append(' ')
            append(ingredients.joinToString(" ") { it.name })
        }.normalizedTokens()

        return queryTokens.count { token ->
            searchable.any { candidate ->
                candidate == token || candidate.contains(token) || token.contains(candidate)
            }
        }
    }

    private fun String.normalizedTokens(): Set<String> =
        Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("đ", "d")
            .replace(Regex("""[^\p{Alnum}\s]"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()

    private val stopWords = setOf(
        "toi", "ban", "hay", "cho", "voi", "tu", "co", "gi", "mon",
        "nau", "an", "lam", "can", "them", "huong", "dan", "cach",
        "duoc", "khong", "trong", "tu", "lanh"
    )
}
