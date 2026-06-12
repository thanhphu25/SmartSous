package com.example.smartsous.data.remote

import android.util.Log
import com.example.smartsous.BuildConfig
import com.example.smartsous.core.network.GeminiClient
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MessageRole
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

import com.example.smartsous.core.common.DataStoreManager
import kotlinx.coroutines.flow.first

@Singleton
class GeminiDataSource @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {

    companion object {
        // Groq endpoint — OpenAI compatible
        private const val BASE_URL =
            "https://api.groq.com/openai/v1/chat/completions"

        private const val TAG = "GeminiDataSource"
    }

    fun streamChat(
        userMessage: String,
        systemContext: String = "",
        pantryIngredients: List<Ingredient> = emptyList(),
        chatHistory: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {

        val prefs = dataStoreManager.userPreferencesFlow.first()
        val apiKey = prefs.aiApiKey.takeIf { it.isNotBlank() } ?: BuildConfig.GROQ_API_KEY
        val model = prefs.aiModel.takeIf { it.isNotBlank() } ?: "llama-3.3-70b-versatile"

        Log.d(TAG, "=== GỌI GROQ === message: $userMessage")

        val body = buildGroqRequestBody(
            userMessage       = userMessage,
            systemContext     = systemContext,
            pantryIngredients = pantryIngredients,
            chatHistory       = chatHistory,
            model             = model
        )

        val request = Request.Builder()
            .url(BASE_URL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            // Groq dùng Bearer token thay vì query param
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = GeminiClient.okHttp.newCall(request).execute()
        Log.d(TAG, "Response code: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            Log.e(TAG, "LỖI HTTP: $errorBody")
            throw Exception("Groq API lỗi ${response.code}: $errorBody")
        }

        val source = response.body?.source()
            ?: throw Exception("Response body rỗng")

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break

            when {
                line.startsWith("data: ") -> {
                    val jsonStr = line.removePrefix("data: ").trim()
                    if (jsonStr == "[DONE]") break

                    // Parse format OpenAI/Groq — khác với Gemini
                    val text = parseGroqChunk(jsonStr)
                    if (text.isNotEmpty()) emit(text)
                }
                else -> continue
            }
        }

    }.flowOn(Dispatchers.IO)

    // ── Build request body theo format OpenAI ────────────────
    private fun buildGroqRequestBody(
        userMessage: String,
        systemContext: String,
        pantryIngredients: List<Ingredient>,
        chatHistory: List<ChatMessage>,
        model: String
    ): String {
        val root = JSONObject()
        root.put("model", model)
        root.put("stream", true)
        root.put("max_tokens", 1024)
        root.put("temperature", 0.7)

        val messages = JSONArray()

        // System message — đầu tiên trong array
        val systemContent = PromptBuilder.buildSystemPrompt(
            pantryIngredients = pantryIngredients,
            ragContext = systemContext
        )
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemContent)
        })

        // Conversation history
        chatHistory.takeLast(10).forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER      -> "user"
                MessageRole.ASSISTANT -> "assistant"
            }
            messages.put(JSONObject().apply {
                put("role", role)
                put("content", msg.content)
            })
        }

        // Tin nhắn mới của user
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        root.put("messages", messages)
        return root.toString()
    }

    // ── Parse SSE chunk theo format Groq/OpenAI ───────────────
    // Gemini:  {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}
    // Groq:    {"choices":[{"delta":{"content":"Hello"},"index":0}]}
    private fun parseGroqChunk(jsonStr: String): String {
        return try {
            val json     = JSONObject(jsonStr)
            val choices  = json.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""

            val delta    = choices.getJSONObject(0).optJSONObject("delta") ?: return ""
            delta.optString("content", "")

        } catch (e: Exception) {
            Log.w(TAG, "Parse chunk lỗi: ${e.message}")
            ""
        }
    }
}
