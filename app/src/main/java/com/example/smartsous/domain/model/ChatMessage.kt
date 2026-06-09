package com.example.smartsous.domain.model

data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    // ── Mở rộng: loại message đặc biệt ──
    val type: MessageType = MessageType.TEXT,
    val suggestedRecipes: List<SuggestedRecipe> = emptyList()
)

enum class MessageRole { USER, ASSISTANT }

enum class MessageType {
    TEXT,              // Tin nhắn text thông thường
    RECIPE_SUGGESTION  // Hiện recipe cards — từ intent detection
}