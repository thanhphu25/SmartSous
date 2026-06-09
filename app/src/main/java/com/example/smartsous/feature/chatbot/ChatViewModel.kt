package com.example.smartsous.feature.chatbot

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.remote.PromptBuilder
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.repository.IChatRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.ChatIntent
import com.example.smartsous.domain.usecase.ChatWithAIUseCase
import com.example.smartsous.domain.usecase.IntentDetector
import com.example.smartsous.domain.usecase.SuggestMealsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
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
    private val intentDetector: IntentDetector      // ← inject
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeChatHistory()
        observePantry()
    }

    private fun observeChatHistory() {
        chatRepository.getChatHistory()
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

        val userMessage = ChatMessage(
            id      = UUID.randomUUID().toString(),
            content = text.trim(),
            role    = MessageRole.USER
        )

        _uiState.update { state ->
            state.copy(
                messages     = state.messages + userMessage,
                isTyping     = true,
                streamingText = "",
                quickReplies = emptyList(),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            chatRepository.saveMessage(userMessage)

            // ── Phát hiện intent ──────────────────────────────
            val intent = intentDetector.detect(text)

            when (intent) {

                // Intent: gợi ý từ nguyên liệu cụ thể
                is ChatIntent.SuggestFromIngredients -> {
                    handleIngredientIntent(
                        ingredients = intent.ingredients,
                        originalMessage = text
                    )
                }

                // Intent: dùng pantry hiện tại
                is ChatIntent.UseMyPantry -> {
                    handlePantryIntent(originalMessage = text)
                }

                // Intent thông thường → để Gemini xử lý
                else -> {
                    streamGeminiResponse(text)
                }
            }
        }
    }

    // ── Handler: gợi ý từ nguyên liệu được nhắc tên ──────────
    private suspend fun handleIngredientIntent(
        ingredients: List<String>,
        originalMessage: String
    ) {
        val allRecipes = recipeRepository.getAllRecipes().first()

        // Tạo fake pantry từ nguyên liệu user đề cập
        val fakeIngredients = ingredients.map { name ->
            com.example.smartsous.domain.model.Ingredient(
                id       = UUID.randomUUID().toString(),
                name     = name,
                quantity = 1.0,
                unit     = "đơn vị",
                category = com.example.smartsous.domain.model.IngredientCategory.OTHER
            )
        }

        // Gọi SuggestMealsUseCase với nguyên liệu trích xuất
        val suggestions = suggestMealsUseCase(
            allRecipes         = allRecipes,
            pantryIngredients  = fakeIngredients,
            topN               = 3
        )

        if (suggestions.isNotEmpty()) {
            // Hiện recipe cards ngay lập tức — không cần đợi Gemini
            val recipeMessage = ChatMessage(
                id               = UUID.randomUUID().toString(),
                content          = "Với ${ingredients.joinToString(", ")}, tôi gợi ý ${suggestions.size} món:",
                role             = MessageRole.ASSISTANT,
                type             = MessageType.RECIPE_SUGGESTION,
                suggestedRecipes = suggestions
            )
            chatRepository.saveMessage(recipeMessage)
            _uiState.update { state ->
                state.copy(
                    messages      = state.messages + recipeMessage,
                    isTyping      = false,
                    streamingText = "",
                    quickReplies  = listOf(
                        "Hướng dẫn nấu món đầu tiên?",
                        "Gợi ý thêm món khác?",
                        "Cần mua thêm gì?"
                    )
                )
            }
        } else {
            // Không có recipe phù hợp → hỏi Gemini
            streamGeminiResponse(originalMessage)
        }
    }

    // ── Handler: dùng pantry hiện tại ─────────────────────────
    private suspend fun handlePantryIntent(originalMessage: String) {
        val currentPantry = _uiState.value.pantryIngredients

        if (currentPantry.isEmpty()) {
            // Pantry trống → nhắc user thêm nguyên liệu
            val emptyMsg = ChatMessage(
                id      = UUID.randomUUID().toString(),
                content = "Tủ lạnh của bạn đang trống 🧊\nHãy vào tab Pantry để thêm nguyên liệu — tôi sẽ gợi ý món phù hợp nhất!",
                role    = MessageRole.ASSISTANT
            )
            chatRepository.saveMessage(emptyMsg)
            _uiState.update { state ->
                state.copy(
                    messages      = state.messages + emptyMsg,
                    isTyping      = false,
                    quickReplies  = listOf("Mở Pantry", "Gợi ý món phổ biến")
                )
            }
            return
        }

        val allRecipes = recipeRepository.getAllRecipes().first()
        val suggestions = suggestMealsUseCase(
            allRecipes        = allRecipes,
            pantryIngredients = currentPantry,
            topN              = 3
        )

        if (suggestions.isNotEmpty()) {
            val recipeMessage = ChatMessage(
                id               = UUID.randomUUID().toString(),
                content          = "Từ ${currentPantry.size} nguyên liệu trong tủ, tôi gợi ý:",
                role             = MessageRole.ASSISTANT,
                type             = MessageType.RECIPE_SUGGESTION,
                suggestedRecipes = suggestions
            )
            chatRepository.saveMessage(recipeMessage)
            _uiState.update { state ->
                state.copy(
                    messages      = state.messages + recipeMessage,
                    isTyping      = false,
                    streamingText = "",
                    quickReplies  = PromptBuilder.buildQuickReplies(
                        currentPantry, ""
                    )
                )
            }
        } else {
            streamGeminiResponse(originalMessage)
        }
    }

    // ── Stream Gemini response ────────────────────────────────
    private suspend fun streamGeminiResponse(text: String) {
        val currentState = _uiState.value

        chatWithAIUseCase(
            userMessage       = text,
            pantryIngredients = currentState.pantryIngredients,
            chatHistory       = currentState.messages.dropLast(1)
        )
            .catch { e ->
                _uiState.update { state ->
                    state.copy(
                        isTyping      = false,
                        streamingText = "",
                        errorMessage  = "Không thể kết nối. Kiểm tra mạng và thử lại."
                    )
                }
            }
            .collect { chunk ->
                _uiState.update { state ->
                    state.copy(streamingText = state.streamingText + chunk)
                }
            }

        // Stream xong → lưu message hoàn chỉnh
        val finalText = _uiState.value.streamingText
        if (finalText.isNotBlank()) {
            val assistantMessage = ChatMessage(
                id      = UUID.randomUUID().toString(),
                content = finalText,
                role    = MessageRole.ASSISTANT
            )
            chatRepository.saveMessage(assistantMessage)

            _uiState.update { state ->
                state.copy(
                    messages      = state.messages + assistantMessage,
                    isTyping      = false,
                    streamingText = "",
                    quickReplies  = PromptBuilder.buildQuickReplies(
                        state.pantryIngredients, finalText
                    )
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _uiState.update { state ->
                state.copy(
                    messages      = emptyList(),
                    streamingText = "",
                    quickReplies  = buildInitialQuickReplies(state.pantryIngredients)
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
}