package com.example.smartsous.feature.chatbot

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.remote.PromptBuilder
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.repository.IChatRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.usecase.ChatWithAIUseCase
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
    private val pantryRepository: IPantryRepository  // ← inject pantry
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
        // Tự động cập nhật pantry context khi tủ lạnh thay đổi
        pantryRepository.getAllIngredients()
            .onEach { ingredients ->
                _uiState.update { state ->
                    state.copy(
                        pantryIngredients = ingredients,
                        // Cập nhật quick replies khi pantry thay đổi
                        quickReplies = if (state.messages.isEmpty()) {
                            buildInitialQuickReplies(ingredients)
                        } else state.quickReplies
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isTyping) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = text.trim(),
            role = MessageRole.USER
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isTyping = true,
                streamingText = "",
                quickReplies = emptyList(), // ẩn quick replies khi đang chat
                errorMessage = null
            )
        }

        viewModelScope.launch {
            // Lưu user message
            chatRepository.saveMessage(userMessage)

            // Gọi AI với pantry context + history
            val currentState = _uiState.value
            chatWithAIUseCase(
                userMessage = text,
                pantryIngredients = currentState.pantryIngredients,
                chatHistory = currentState.messages.dropLast(1) // bỏ message vừa thêm
            )
                .catch { e ->
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

            // Stream xong → lưu response
            val finalText = _uiState.value.streamingText
            if (finalText.isNotBlank()) {
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = finalText,
                    role = MessageRole.ASSISTANT
                )
                chatRepository.saveMessage(assistantMessage)

                // Build quick replies cho câu hỏi tiếp theo
                val quickReplies = PromptBuilder.buildQuickReplies(
                    pantryIngredients = _uiState.value.pantryIngredients,
                    lastAssistantMessage = finalText
                )

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isTyping = false,
                        streamingText = "",
                        quickReplies = quickReplies
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
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

    // Quick replies ban đầu khi chưa có cuộc hội thoại
    private fun buildInitialQuickReplies(ingredients: List<Ingredient>): List<String> {
        val replies = mutableListOf<String>()
        if (ingredients.isNotEmpty()) {
            replies.add("Tôi nên nấu gì hôm nay?")
        }
        replies.addAll(listOf(
            "Gợi ý món nhanh dưới 20 phút",
            "Món ăn lành mạnh ít calo",
            "Cách bảo quản thực phẩm lâu hơn"
        ))
        return replies.take(3)
    }
}
