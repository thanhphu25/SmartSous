package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatWithAIUseCase @Inject constructor(
    private val chatRepository: IChatRepository
) {
    operator fun invoke(
        userMessage: String,
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList()  // ← thêm history
    ): Flow<String> = chatRepository.streamChat(
        userMessage = userMessage,
        systemContext = "",      // không cần nữa — PromptBuilder xử lý
        pantryIngredients = pantryIngredients,
        chatHistory = chatHistory
    )
}