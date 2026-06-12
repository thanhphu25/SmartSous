package com.example.smartsous.feature.chatbot

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.remote.PromptBuilder
import com.example.smartsous.domain.model.ChatConversation
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import com.example.smartsous.domain.repository.IChatRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.ChatIntent
import com.example.smartsous.domain.usecase.ChatWithAIUseCase
import com.example.smartsous.domain.usecase.IntentDetector
import com.example.smartsous.domain.usecase.SuggestMealsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

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
    private val recipeRepository: IRecipeRepository,
    private val suggestMealsUseCase: SuggestMealsUseCase,
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
            chatRepository.renameConversation(
                conversationId = conversationId,
                title = trimmedTitle
            )
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

    private fun observePantry() {
        pantryRepository.getAllIngredients()
            .onEach { ingredients ->
                _uiState.update { state ->
                    state.copy(
                        pantryIngredients = ingredients,
                        quickReplies = if (state.messages.isEmpty())
                            buildInitialQuickReplies(ingredients)
                        else state.quickReplies
                    )
                }
            }
            .launchIn(viewModelScope)
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
                is ChatIntent.SuggestFromIngredients -> {
                    handleIngredientIntent(
                        ingredients = intent.ingredients,
                        originalMessage = text,
                        conversationId = conversationId
                    )
                }

                is ChatIntent.UseMyPantry -> {
                    handlePantryIntent(
                        originalMessage = text,
                        conversationId = conversationId
                    )
                }

                else -> {
                    streamGeminiResponse(text, conversationId)
                }
            }
        }
    }

    private suspend fun handleIngredientIntent(
        ingredients: List<String>,
        originalMessage: String,
        conversationId: String
    ) {
        val allRecipes = recipeRepository.getAllRecipes().first()
        val fakeIngredients = ingredients.map { name ->
            com.example.smartsous.domain.model.Ingredient(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = 1.0,
                unit = "đơn vị",
                category = com.example.smartsous.domain.model.IngredientCategory.OTHER
            )
        }

        val suggestions = suggestMealsUseCase(
            allRecipes = allRecipes,
            pantryIngredients = fakeIngredients,
            topN = 3
        )

        if (suggestions.isNotEmpty()) {
            val recipeMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Với ${ingredients.joinToString(", ")}, tôi gợi ý ${suggestions.size} món:",
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
                        "Hướng dẫn nấu món đầu tiên?",
                        "Gợi ý thêm món khác?",
                        "Cần mua thêm gì?"
                    )
                )
            }
        } else {
            streamGeminiResponse(originalMessage, conversationId)
        }
    }

    private suspend fun handlePantryIntent(originalMessage: String, conversationId: String) {
        val currentPantry = _uiState.value.pantryIngredients

        if (currentPantry.isEmpty()) {
            val emptyMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Tủ lạnh của bạn đang trống 🧊\nHãy vào tab Pantry để thêm nguyên liệu - tôi sẽ gợi ý món phù hợp nhất!",
                role = MessageRole.ASSISTANT
            )
            chatRepository.saveMessage(emptyMsg, conversationId)
            _uiState.update { state ->
                state.copy(
                    messages = appendMessage(state.messages, emptyMsg),
                    isTyping = false,
                    quickReplies = listOf("Mở Pantry", "Gợi ý món phổ biến")
                )
            }
            return
        }

        val allRecipes = recipeRepository.getAllRecipes().first()
        val suggestions = suggestMealsUseCase(
            allRecipes = allRecipes,
            pantryIngredients = currentPantry,
            topN = 3
        )

        if (suggestions.isNotEmpty()) {
            val recipeMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Từ ${currentPantry.size} nguyên liệu trong tủ, tôi gợi ý:",
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
                    quickReplies = PromptBuilder.buildQuickReplies(
                        currentPantry, ""
                    )
                )
            }
        } else {
            streamGeminiResponse(originalMessage, conversationId)
        }
    }

    private suspend fun streamGeminiResponse(text: String, conversationId: String) {
        val currentState = _uiState.value

        chatWithAIUseCase(
            userMessage = text,
            pantryIngredients = currentState.pantryIngredients,
            chatHistory = currentState.messages.dropLast(1)
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
                        state.pantryIngredients, finalText
                    )
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    isTyping = false,
                    streamingText = ""
                )
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

    private fun buildInitialQuickReplies(ingredients: List<Ingredient>): List<String> {
        val replies = mutableListOf<String>()
        if (ingredients.isNotEmpty()) {
            replies.add("Nấu gì với ${ingredients.first().name}?")
        }
        replies.addAll(listOf(
            "Hôm nay nên nấu gì?",
            "Món nhanh dưới 20 phút",
            "Món chay đơn giản"
        ))
        return replies.take(3)
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
