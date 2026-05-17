package com.example.smartsous.data.remote

import android.util.Log
import com.example.smartsous.BuildConfig
import com.example.smartsous.core.network.GeminiClient
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiDataSource @Inject constructor() {

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:streamGenerateContent"
        private const val TAG = "GeminiDataSource"
    }

    fun streamChat(
        userMessage: String,
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {

        val url = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}&alt=sse"

        // Build body với system prompt + history + tin nhắn mới
        val body = buildRequestBody(
            userMessage = userMessage,
            pantryIngredients = pantryIngredients,
            chatHistory = chatHistory
        )

        Log.d(TAG, "=== GỌI GEMINI === message: $userMessage")

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        val response = GeminiClient.okHttp.newCall(request).execute()
        Log.d(TAG, "Response code: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            Log.e(TAG, "LỖI HTTP: $errorBody")
            throw Exception("Gemini API lỗi ${response.code}: $errorBody")
        }

        val source = response.body?.source()
            ?: throw Exception("Response body rỗng")

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            when {
                line.startsWith("data: ") -> {
                    val jsonStr = line.removePrefix("data: ").trim()
                    if (jsonStr == "[DONE]") break
                    val text = parseChunk(jsonStr)
                    if (text.isNotEmpty()) emit(text)
                }
                else -> continue
            }
        }

    }.flowOn(Dispatchers.IO)

    private fun buildRequestBody(
        userMessage: String,
        pantryIngredients: List<Ingredient>,
        chatHistory: List<ChatMessage>
    ): String {
        val root = JSONObject()

        // System instruction với pantry context
        val systemPrompt = PromptBuilder.buildSystemPrompt(pantryIngredients)
        root.put("system_instruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemPrompt)
                })
            })
        })

        // Conversation history + tin nhắn mới
        val conversationMessages = PromptBuilder.buildConversationMessages(
            history = chatHistory,
            newUserMessage = userMessage
        )

        root.put("contents", JSONArray().apply {
            conversationMessages.forEach { msg ->
                put(JSONObject().apply {
                    put("role", msg["role"] as String)
                    @Suppress("UNCHECKED_CAST")
                    val parts = msg["parts"] as List<Map<String, String>>
                    put("parts", JSONArray().apply {
                        parts.forEach { part ->
                            put(JSONObject().apply {
                                put("text", part["text"] ?: "")
                            })
                        }
                    })
                })
            }
        })

        root.put("generationConfig", JSONObject().apply {
            put("temperature", 0.8)
            put("maxOutputTokens", 1024)
            put("topP", 0.9)
        })

        return root.toString()
    }

    private fun parseChunk(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() == 0) return ""
            val content = candidates.getJSONObject(0)
                .optJSONObject("content") ?: return ""
            val parts = content.getJSONArray("parts")
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            Log.w(TAG, "Parse chunk lỗi: ${e.message}")
            ""
        }
    }
}