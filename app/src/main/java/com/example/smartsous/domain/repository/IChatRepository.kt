package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.ChatConversation
import com.example.smartsous.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    fun streamChat(
        userMessage: String,
        systemContext: String = "",
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList()
    ): Flow<String>

    suspend fun saveMessage(message: ChatMessage, conversationId: String)
    fun getChatHistory(conversationId: String): Flow<List<ChatMessage>>
    fun getConversations(): Flow<List<ChatConversation>>
    suspend fun ensureConversation(): String
    suspend fun createConversation(title: String = "Đoạn chat mới"): String
    suspend fun renameConversation(conversationId: String, title: String)
    suspend fun deleteConversation(conversationId: String): String
    suspend fun clearHistory(conversationId: String)
}
