package com.example.smartsous.data.local.dao

import androidx.room.*
import com.example.smartsous.data.local.entity.ChatConversationEntity
import com.example.smartsous.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getAll(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    fun getConversations(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestConversation(): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversationEntity)

    @Query("UPDATE chat_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTitle(conversationId: String, title: String, updatedAt: Long)

    @Query("UPDATE chat_conversations SET updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun touchConversation(conversationId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearAll(conversationId: String)

    @Query("DELETE FROM chat_conversations WHERE id = :conversationId")
    suspend fun deleteConversationRow(conversationId: String)

    @Transaction
    suspend fun deleteConversation(conversationId: String) {
        clearAll(conversationId)
        deleteConversationRow(conversationId)
    }
}
