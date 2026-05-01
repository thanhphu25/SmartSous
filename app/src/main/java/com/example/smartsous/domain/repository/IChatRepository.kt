package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    fun streamChat(userMessage: String, systemContext: String): Flow<String>
    suspend fun saveMessage(message: ChatMessage)
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun clearHistory()
}