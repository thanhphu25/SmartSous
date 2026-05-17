package com.example.smartsous.data.repository

import com.example.smartsous.data.local.dao.ChatMessageDao
import com.example.smartsous.data.local.entity.ChatMessageEntity
import com.example.smartsous.data.remote.GeminiDataSource
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val geminiDataSource: GeminiDataSource,
    private val chatMessageDao: ChatMessageDao
) : IChatRepository {

    // Delegate thẳng sang GeminiDataSource
    override fun streamChat(
        userMessage: String,
        systemContext: String,
        pantryIngredients: List<Ingredient>,
        chatHistory: List<ChatMessage>
    ): Flow<String> = geminiDataSource.streamChat(
        userMessage = userMessage,
        pantryIngredients = pantryIngredients,
        chatHistory = chatHistory
    )

    override suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao.insert(message.toEntity())
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> =
        chatMessageDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun clearHistory() {
        chatMessageDao.clearAll()
    }

    // Mapper Entity -> Domain
    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        content = content,
        role = MessageRole.valueOf(role),
        timestamp = timestamp
    )

    // Mapper Domain -> Entity
    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id.ifEmpty { UUID.randomUUID().toString() },
        content = content,
        role = role.name,
        timestamp = timestamp
    )
}