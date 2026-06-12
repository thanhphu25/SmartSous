package com.example.smartsous.feature.chatbot

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.remote.PromptBuilder
import com.example.smartsous.domain.model.ChatConversation
import com.example.smartsous.domain.model.ChatCookingContext
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.repository.IChatRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.usecase.ChatContextBuilder
import com.example.smartsous.domain.usecase.ChatIntent
import com.example.smartsous.domain.usecase.ChatWithAIUseCase
import com.example.smartsous.domain.usecase.IntentDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<ChatConversation> = emptyList(),
    val currentConversationId: String? = null,
    val isTyping: Boolean = false,
    val streamingText: String = "",
    val quickReplies: List<String> = emptyList(),
    val pantryIngredients: List<Ingredient> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatWithAIUseCase: ChatWithAIUseCase,
    private val chatRepository: IChatRepository,
    private val pantryRepository: IPantryRepository,
    private val chatContextBuilder: ChatContextBuilder,
    private val intentDetector: IntentDetector
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    private val currentConversationId = MutableStateFlow<String?>(null)

    init {
        observeConversations()
        observeCurrentConversation()
        observePantry()
        viewModelScope.launch {
            switchConversation(chatRepository.ensureConversation())
        }
    }

    private fun observeConversations() {
        chatRepository.getConversations()
            .onEach { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCurrentConversation() {
        currentConversationId
            .filterNotNull()
            .flatMapLatest { conversationId ->
                chatRepository.getChatHistory(conversationId)
            }
            .onEach { history ->
                _uiState.update { it.copy(messages = history) }
            }
            .launchIn(viewModelScope)
    }

    private fun observePantry() {
        pantryRepository.getAllIngredients()
            .onEach { ingredients ->
                _uiState.update { state ->
                    state.copy(
                        pantryIngredients = ingredients,
                        quickReplies = if (state.messages.isEmpty()) {
                            buildInitialQuickReplies(ingredients)
                        } else {
                            state.quickReplies
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun switchConversation(conversationId: String) {
        currentConversationId.value = conversationId
        _uiState.update {
            it.copy(
                currentConversationId = conversationId,
                streamingText = "",
                isTyping = false,
                errorMessage = null
            )
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val conversationId = chatRepository.createConversation()
            switchConversation(conversationId)
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    quickReplies = buildInitialQuickReplies(state.pantryIngredients)
                )
            }
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        viewModelScope.launch {
            chatRepository.renameConversation(conversationId, trimmedTitle)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val nextConversationId = chatRepository.deleteConversation(conversationId)
            if (currentConversationId.value == conversationId) {
                switchConversation(nextConversationId)
                _uiState.update { state ->
                    state.copy(
                        messages = emptyList(),
                        quickReplies = buildInitialQuickReplies(state.pantryIngredients)
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isTyping) return

        val conversationId = currentConversationId.value ?: return
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = text.trim(),
            role = MessageRole.USER
        )

        _uiState.update { state ->
            state.copy(
                messages = appendMessage(state.messages, userMessage),
                isTyping = true,
                streamingText = "",
                quickReplies = emptyList(),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            if (_uiState.value.messages.size == 1) {
                chatRepository.renameConversation(
                    conversationId = conversationId,
                    title = buildConversationTitle(text)
                )
            }
            chatRepository.saveMessage(userMessage, conversationId)

            when (val intent = intentDetector.detect(text)) {
                ChatIntent.OutOfDomain -> handleOutOfDomain(conversationId)
                is ChatIntent.SuggestFromIngredients,
                ChatIntent.UseMyPantry,
                ChatIntent.ExpiringFood -> handleRecommendationIntent(intent, text, conversationId)
                else -> handleRagTextIntent(intent, text, conversationId)
            }
        }
    }

    private suspend fun handleRecommendationIntent(
        intent: ChatIntent,
        originalMessage: String,
        conversationId: String
    ) {
        val currentPantry = _uiState.value.pantryIngredients

        if (intent is ChatIntent.UseMyPantry && currentPantry.isEmpty()) {
            saveAssistantText(
                conversationId = conversationId,
                content = "Tủ lạnh của bạn đang trống. Hãy thêm nguyên liệu ở Pantry, rồi mình sẽ gợi ý món phù hợp hơn.",
                quickReplies = listOf(
                    "Món nhanh dưới 20 phút?",
                    "Cách bảo quản thực phẩm?",
                    "Món chay đơn giản?"
                )
            )
            return
        }

        val context = chatContextBuilder.build(
            intent = intent,
            userMessage = originalMessage,
            currentPantry = currentPantry,
            topN = 3
        )

        if (context.recommendedRecipes.isNotEmpty()) {
            saveRecipeSuggestions(
                conversationId = conversationId,
                content = buildSuggestionIntro(intent, context.recommendedRecipes),
                suggestions = context.recommendedRecipes
            )
        } else {
            streamGeminiResponse(
                text = originalMessage,
                conversationId = conversationId,
                systemContext = context.toRagContextText()
            )
        }
    }

    private suspend fun handleRagTextIntent(
        intent: ChatIntent,
        originalMessage: String,
        conversationId: String
    ) {
        val context = chatContextBuilder.build(
            intent = intent,
            userMessage = originalMessage,
            currentPantry = _uiState.value.pantryIngredients,
            topN = 5
        )

        streamGeminiResponse(
            text = originalMessage,
            conversationId = conversationId,
            systemContext = context.toRagContextText()
        )
    }

    private suspend fun handleOutOfDomain(conversationId: String) {
        saveAssistantText(
            conversationId = conversationId,
            content = "Mình là trợ lý nấu ăn của SmartSous nên mình chỉ tập trung vào món ăn, nguyên liệu, thực đơn và dinh dưỡng. Bạn muốn mình gợi ý món từ tủ lạnh hiện tại không?",
            quickReplies = listOf(
                "Hôm nay nên nấu gì?",
                "Món nào dùng đồ sắp hết hạn?",
                "Món nhanh dưới 20 phút?"
            )
        )
    }

    private suspend fun streamGeminiResponse(
        text: String,
        conversationId: String,
        systemContext: String = ""
    ) {
        val currentState = _uiState.value

        chatWithAIUseCase(
            userMessage = text,
            pantryIngredients = currentState.pantryIngredients,
            chatHistory = currentState.messages.dropLast(1),
            systemContext = systemContext
        )
            .catch {
                _uiState.update { state ->
                    state.copy(
                        isTyping = false,
                        streamingText = "",
                        errorMessage = "Không thể kết nối. Kiểm tra mạng và thử lại."
                    )
                }
            }
            .collect { chunk ->
                _uiState.update { state ->
                    state.copy(streamingText = state.streamingText + chunk)
                }
            }

        val finalText = _uiState.value.streamingText
        if (finalText.isNotBlank()) {
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = finalText,
                role = MessageRole.ASSISTANT
            )
            chatRepository.saveMessage(assistantMessage, conversationId)

            _uiState.update { state ->
                state.copy(
                    messages = appendMessage(state.messages, assistantMessage),
                    isTyping = false,
                    streamingText = "",
                    quickReplies = PromptBuilder.buildQuickReplies(
                        state.pantryIngredients,
                        finalText
                    )
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(isTyping = false, streamingText = "")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val conversationId = currentConversationId.value ?: return@launch
            chatRepository.clearHistory(conversationId)
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    streamingText = "",
                    quickReplies = buildInitialQuickReplies(state.pantryIngredients)
                )
            }
        }
    }

    override fun clearError() {
        super.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun saveAssistantText(
        conversationId: String,
        content: String,
        quickReplies: List<String>
    ) {
        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            role = MessageRole.ASSISTANT
        )
        chatRepository.saveMessage(assistantMessage, conversationId)
        _uiState.update { state ->
            state.copy(
                messages = appendMessage(state.messages, assistantMessage),
                isTyping = false,
                streamingText = "",
                quickReplies = quickReplies
            )
        }
    }

    private suspend fun saveRecipeSuggestions(
        conversationId: String,
        content: String,
        suggestions: List<SuggestedRecipe>
    ) {
        val recipeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            role = MessageRole.ASSISTANT,
            type = MessageType.RECIPE_SUGGESTION,
            suggestedRecipes = suggestions
        )
        chatRepository.saveMessage(recipeMessage, conversationId)
        _uiState.update { state ->
            state.copy(
                messages = appendMessage(state.messages, recipeMessage),
                isTyping = false,
                streamingText = "",
                quickReplies = listOf(
                    "Hướng dẫn món đầu tiên?",
                    "Cần mua thêm gì?",
                    "Gợi ý món khác?"
                )
            )
        }
    }

    private fun buildSuggestionIntro(
        intent: ChatIntent,
        suggestions: List<SuggestedRecipe>
    ): String =
        when (intent) {
            is ChatIntent.SuggestFromIngredients ->
                "Với ${intent.ingredients.joinToString(", ")}, mình gợi ý ${suggestions.size} món phù hợp nhất:"
            ChatIntent.ExpiringFood ->
                "Mình ưu tiên các nguyên liệu sắp hết hạn và gợi ý ${suggestions.size} món nên nấu trước:"
            else ->
                "Từ nguyên liệu trong tủ lạnh, mình gợi ý ${suggestions.size} món phù hợp nhất:"
        }

    private fun ChatCookingContext.toRagContextText(): String = buildString {
        append("EXPIRING_SOON:\n")
        if (expiringSoonIngredients.isEmpty()) {
            append("- Khong co nguyen lieu sap het han trong 3 ngay.\n")
        } else {
            expiringSoonIngredients.forEach { ingredient ->
                append("- ${ingredient.name}: ${ingredient.quantity} ${ingredient.unit}, han ${ingredient.expiryDate}\n")
            }
        }

        append("\nUSER_PREFERENCE:\n")
        append("- targetCaloriesPerMeal=${userPreference.targetCaloriesPerMeal}\n")
        append("- maxCookingTimeMinutes=${userPreference.maxCookingTimeMinutes}\n")
        append("- vegetarian=${userPreference.vegetarian}\n")
        append("- favoriteCuisines=${userPreference.favoriteCuisines.joinToString(", ").ifBlank { "none" }}\n")
        append("- allergies=${userPreference.allergies.joinToString(", ").ifBlank { "none" }}\n")
        append("- dislikedIngredients=${userPreference.dislikedIngredients.joinToString(", ").ifBlank { "none" }}\n")

        append("\nRECOMMENDED_RECIPES:\n")
        if (recommendedRecipes.isEmpty()) {
            append("- Khong co goi y du diem.\n")
        } else {
            recommendedRecipes.forEachIndexed { index, suggestion ->
                val recipe = suggestion.recipe
                append("${index + 1}. ${recipe.name}\n")
                append("   id=${recipe.id}, score=${suggestion.score}, match=${suggestion.matchPercent}%\n")
                append("   time=${recipe.cookingTimeMinutes} phut, calories=${recipe.nutrition.calories}, difficulty=${recipe.difficulty}\n")
                append("   reason=${suggestion.reason}\n")
                append("   matchedIngredients=${suggestion.matchedIngredients.joinToString(", ").ifBlank { "none" }}\n")
                append("   missingIngredients=${suggestion.missingIngredients.joinToString(", ").ifBlank { "none" }}\n")
                append("   steps=${recipe.steps.take(5).joinToString(" | ")}\n")
            }
        }

        append("\nRELEVANT_RECIPES:\n")
        if (relevantRecipes.isEmpty()) {
            append("- Khong co recipe lien quan khac.\n")
        } else {
            relevantRecipes.forEachIndexed { index, recipe ->
                append("${index + 1}. ${recipe.name}: ${recipe.cookingTimeMinutes} phut, ${recipe.nutrition.calories} kcal\n")
                append("   ingredients=${recipe.ingredients.joinToString(", ") { it.name }}\n")
            }
        }
    }

    private fun buildInitialQuickReplies(ingredients: List<Ingredient>): List<String> {
        val replies = mutableListOf<String>()
        if (ingredients.isNotEmpty()) {
            replies.add("Nấu gì với ${ingredients.first().name}?")
        }
        replies.addAll(
            listOf(
                "Hôm nay nên nấu gì?",
                "Món nhanh dưới 20 phút",
                "Món nào dùng đồ sắp hết hạn?"
            )
        )
        return replies.distinct().take(3)
    }

    private fun buildConversationTitle(text: String): String =
        text.trim()
            .replace(Regex("\\s+"), " ")
            .take(32)
            .ifBlank { "Đoạn chat mới" }

    private fun appendMessage(
        messages: List<ChatMessage>,
        message: ChatMessage
    ): List<ChatMessage> =
        (messages + message)
            .distinctBy { it.id }
            .sortedBy { it.timestamp }
}
