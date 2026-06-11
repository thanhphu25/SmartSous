package com.example.smartsous.data.repository

import com.example.smartsous.data.local.dao.ChatMessageDao
import com.example.smartsous.data.local.entity.ChatConversationEntity
import com.example.smartsous.data.local.entity.ChatMessageEntity
import com.example.smartsous.data.remote.GeminiDataSource
import com.example.smartsous.domain.model.ChatConversation
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val geminiDataSource: GeminiDataSource,
    private val chatMessageDao: ChatMessageDao
) : IChatRepository {

    override fun streamChat(
        userMessage: String,
        systemContext: String,
        pantryIngredients: List<Ingredient>,
        chatHistory: List<ChatMessage>
    ): Flow<String> = geminiDataSource.streamChat(
        userMessage = userMessage,
        pantryIngredients = pantryIngredients,
        chatHistory = chatHistory
    )

    override suspend fun saveMessage(message: ChatMessage, conversationId: String) {
        chatMessageDao.insert(message.toEntity(conversationId))
        chatMessageDao.touchConversation(conversationId, message.timestamp)
    }

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> =
        chatMessageDao.getAll(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getConversations(): Flow<List<ChatConversation>> =
        chatMessageDao.getConversations().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun ensureConversation(): String =
        chatMessageDao.getLatestConversation()?.id ?: createConversation()

    override suspend fun createConversation(title: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        chatMessageDao.insertConversation(
            ChatConversationEntity(
                id = id,
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    override suspend fun renameConversation(conversationId: String, title: String) {
        chatMessageDao.updateConversationTitle(
            conversationId = conversationId,
            title = title,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteConversation(conversationId: String): String {
        chatMessageDao.deleteConversation(conversationId)
        return ensureConversation()
    }

    override suspend fun clearHistory(conversationId: String) {
        chatMessageDao.clearAll(conversationId)
    }

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        content = content,
        role = MessageRole.valueOf(role),
        timestamp = timestamp,
        type = MessageType.valueOf(type),
        suggestedRecipes = parseSuggestedRecipes(suggestedRecipesJson)
    )

    private fun ChatMessage.toEntity(conversationId: String) = ChatMessageEntity(
        id = id.ifEmpty { UUID.randomUUID().toString() },
        conversationId = conversationId,
        content = content,
        role = role.name,
        timestamp = timestamp,
        type = type.name,
        suggestedRecipesJson = suggestedRecipesToJson(suggestedRecipes)
    )

    private fun ChatConversationEntity.toDomain() = ChatConversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun suggestedRecipesToJson(suggestions: List<SuggestedRecipe>): String {
        val arr = JSONArray()
        suggestions.forEach { suggested ->
            arr.put(JSONObject().apply {
                put("score", suggested.score)
                put("matchedIngredients", stringArray(suggested.matchedIngredients))
                put("missingIngredients", stringArray(suggested.missingIngredients))
                put("matchPercent", suggested.matchPercent)
                put("reason", suggested.reason.name)
                put("recipe", recipeToJson(suggested.recipe))
            })
        }
        return arr.toString()
    }

    private fun parseSuggestedRecipes(json: String): List<SuggestedRecipe> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { index ->
                val obj = arr.getJSONObject(index)
                SuggestedRecipe(
                    recipe = parseRecipe(obj.getJSONObject("recipe")),
                    score = obj.optDouble("score", 0.0).toFloat(),
                    matchedIngredients = parseStringArray(obj.optJSONArray("matchedIngredients")),
                    missingIngredients = parseStringArray(obj.optJSONArray("missingIngredients")),
                    matchPercent = obj.optInt("matchPercent", 0),
                    reason = SuggestionReason.valueOf(
                        obj.optString("reason", SuggestionReason.NOT_COOKED_RECENTLY.name)
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun recipeToJson(recipe: Recipe) = JSONObject().apply {
        put("id", recipe.id)
        put("name", recipe.name)
        put("description", recipe.description)
        put("imageUrl", recipe.imageUrl)
        put("cookingTimeMinutes", recipe.cookingTimeMinutes)
        put("servings", recipe.servings)
        put("difficulty", recipe.difficulty.name)
        put("ingredients", JSONArray().apply {
            recipe.ingredients.forEach { ingredient ->
                put(JSONObject().apply {
                    put("name", ingredient.name)
                    put("amount", ingredient.amount)
                    put("unit", ingredient.unit)
                })
            }
        })
        put("steps", stringArray(recipe.steps))
        put("nutrition", JSONObject().apply {
            put("calories", recipe.nutrition.calories)
            put("protein", recipe.nutrition.protein)
            put("carbs", recipe.nutrition.carbs)
            put("fat", recipe.nutrition.fat)
            put("fiber", recipe.nutrition.fiber)
        })
        put("tags", stringArray(recipe.tags))
        put("cuisine", recipe.cuisine)
        put("isFavorite", recipe.isFavorite)
    }

    private fun parseRecipe(obj: JSONObject): Recipe {
        val nutrition = obj.optJSONObject("nutrition") ?: JSONObject()
        return Recipe(
            id = obj.optString("id"),
            name = obj.optString("name"),
            description = obj.optString("description"),
            imageUrl = obj.optString("imageUrl"),
            cookingTimeMinutes = obj.optInt("cookingTimeMinutes"),
            servings = obj.optInt("servings"),
            difficulty = Difficulty.valueOf(obj.optString("difficulty", Difficulty.EASY.name)),
            ingredients = parseRecipeIngredients(obj.optJSONArray("ingredients")),
            steps = parseStringArray(obj.optJSONArray("steps")),
            nutrition = Nutrition(
                calories = nutrition.optInt("calories"),
                protein = nutrition.optDouble("protein"),
                carbs = nutrition.optDouble("carbs"),
                fat = nutrition.optDouble("fat"),
                fiber = nutrition.optDouble("fiber")
            ),
            tags = parseStringArray(obj.optJSONArray("tags")),
            cuisine = obj.optString("cuisine"),
            isFavorite = obj.optBoolean("isFavorite", false)
        )
    }

    private fun parseRecipeIngredients(arr: JSONArray?): List<RecipeIngredient> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { index ->
            val obj = arr.getJSONObject(index)
            RecipeIngredient(
                name = obj.optString("name"),
                amount = obj.optDouble("amount"),
                unit = obj.optString("unit")
            )
        }
    }

    private fun stringArray(values: List<String>) = JSONArray().apply {
        values.forEach { put(it) }
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }
}
