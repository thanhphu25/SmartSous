package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    // Stream response từng chunk từ Gemini
    fun streamChat(
        userMessage: String,
        systemContext: String = ""
    ): Flow<String>

    // Lưu tin nhắn vào Room (lịch sử chat)
    suspend fun saveMessage(message: ChatMessage)

    // Đọc lịch sử chat từ Room
    fun getChatHistory(): Flow<List<ChatMessage>>

    // Xoá toàn bộ lịch sử
    suspend fun clearHistory()
}