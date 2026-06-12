package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.repository.IChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatWithAIUseCase @Inject constructor(
    private val chatRepository: IChatRepository
) {
    operator fun invoke(
        userMessage: String,
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList(),
        systemContext: String = ""
    ): Flow<String> = chatRepository.streamChat(
        userMessage = userMessage,
        systemContext = systemContext,
        pantryIngredients = pantryIngredients,
        chatHistory = chatHistory
    )
}
