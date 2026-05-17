package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    fun streamChat(
        userMessage: String,
        systemContext: String = "",
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList()
    ): Flow<String>

    suspend fun saveMessage(message: ChatMessage)
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun clearHistory()
}