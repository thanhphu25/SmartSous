package com.example.smartsous.domain.model

data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole { USER, ASSISTANT }