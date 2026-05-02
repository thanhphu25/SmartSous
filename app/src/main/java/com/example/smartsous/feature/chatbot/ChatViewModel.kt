package com.example.smartsous.feature.chatbot

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.usecase.ChatWithAIUseCase
import com.example.smartsous.domain.repository.IChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// UiState — toàn bộ trạng thái UI của ChatScreen
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,          // đang stream → hiện typing indicator
    val streamingText: String = "",         // text đang được stream về, chưa hoàn chỉnh
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatWithAIUseCase: ChatWithAIUseCase,
    private val chatRepository: IChatRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Load lịch sử chat từ Room khi mở màn hình
        viewModelScope.launch {
            chatRepository.getChatHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isTyping) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = text.trim(),
            role = MessageRole.USER
        )

        // Thêm message của user vào list ngay lập tức
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isTyping = true,
                streamingText = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            // Lưu user message vào Room
            chatRepository.saveMessage(userMessage)

            // Gọi Gemini và collect stream
            chatWithAIUseCase(
                userMessage = text,
                pantryIngredients = emptyList() // TODO: inject pantry sau
            )
                .catch { e ->
                    // Lỗi stream → hiện thông báo
                    _uiState.update { state ->
                        state.copy(
                            isTyping = false,
                            streamingText = "",
                            errorMessage = "Lỗi: ${e.message}"
                        )
                    }
                }
                .collect { chunk ->
                    // Mỗi chunk → append vào streamingText
                    _uiState.update { state ->
                        state.copy(streamingText = state.streamingText + chunk)
                    }
                }

            // Stream xong → chuyển streamingText thành message hoàn chỉnh
            val finalText = _uiState.value.streamingText
            if (finalText.isNotBlank()) {
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = finalText,
                    role = MessageRole.ASSISTANT
                )
                // Lưu vào Room
                chatRepository.saveMessage(assistantMessage)
                // Update UI: xoá streamingText, thêm message hoàn chỉnh
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isTyping = false,
                        streamingText = ""
                    )
                }
            }
        }
    }

    override fun clearError() {
        super.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _uiState.update { ChatUiState() }
        }
    }
}