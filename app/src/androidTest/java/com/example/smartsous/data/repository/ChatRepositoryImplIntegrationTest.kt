package com.example.smartsous.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.smartsous.data.local.SmartSousDatabase
import com.example.smartsous.data.remote.GeminiDataSource
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryImplIntegrationTest {

    private lateinit var database: SmartSousDatabase
    private lateinit var repository: ChatRepositoryImpl
    private lateinit var conversationId: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmartSousDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = ChatRepositoryImpl(
            geminiDataSource = GeminiDataSource(),
            chatMessageDao = database.chatMessageDao()
        )
        conversationId = runBlocking { repository.createConversation("Test chat") }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveMessage_thenGetChatHistory_returnsMessagesInTimestampOrder() = runBlocking {
        repository.saveMessage(
            ChatMessage(
                id = "assistant-1",
                content = "Hello",
                role = MessageRole.ASSISTANT,
                timestamp = 2L
            ),
            conversationId
        )
        repository.saveMessage(
            ChatMessage(
                id = "user-1",
                content = "What should I cook?",
                role = MessageRole.USER,
                timestamp = 1L
            ),
            conversationId
        )

        val history = repository.getChatHistory(conversationId).first()

        assertEquals(listOf("user-1", "assistant-1"), history.map { it.id })
        assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT), history.map { it.role })
    }

    @Test
    fun saveMessage_persistsRecipeSuggestionMessages() = runBlocking {
        repository.saveMessage(
            ChatMessage(
                id = "suggestion-1",
                content = "Recipe suggestions",
                role = MessageRole.ASSISTANT,
                type = MessageType.RECIPE_SUGGESTION
            ),
            conversationId
        )

        val history = repository.getChatHistory(conversationId).first()

        assertEquals(1, history.size)
        assertEquals(MessageType.RECIPE_SUGGESTION, history.first().type)
    }

    @Test
    fun clearHistory_removesAllSavedMessages() = runBlocking {
        repository.saveMessage(
            ChatMessage(
                id = "user-1",
                content = "Test",
                role = MessageRole.USER,
                timestamp = 1L
            ),
            conversationId
        )

        repository.clearHistory(conversationId)

        assertTrue(repository.getChatHistory(conversationId).first().isEmpty())
    }

    @Test
    fun deleteConversation_removesMessagesAndReturnsAnotherConversation() = runBlocking {
        val secondConversationId = repository.createConversation("Second chat")
        repository.saveMessage(
            ChatMessage(
                id = "user-1",
                content = "Test",
                role = MessageRole.USER,
                timestamp = 1L
            ),
            conversationId
        )

        val nextConversationId = repository.deleteConversation(conversationId)

        assertEquals(secondConversationId, nextConversationId)
        assertTrue(repository.getChatHistory(conversationId).first().isEmpty())
    }
}
