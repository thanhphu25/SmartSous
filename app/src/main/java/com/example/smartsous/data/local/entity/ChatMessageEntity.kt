package com.example.smartsous.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val role: String,           // "USER" | "ASSISTANT"
    val timestamp: Long
)