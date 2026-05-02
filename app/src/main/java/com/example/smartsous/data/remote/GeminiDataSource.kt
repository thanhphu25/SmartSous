package com.example.smartsous.data.remote

import android.util.Log
import com.example.smartsous.BuildConfig
import com.example.smartsous.core.network.GeminiClient
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
        // streamGenerateContent = endpoint trả về SSE stream
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:streamGenerateContent"

        private const val TAG = "GeminiDataSource"
    }

    // Gọi Gemini và stream response về từng chunk
    // systemContext = danh sách nguyên liệu trong tủ lạnh
    // userMessage   = câu hỏi của user
    fun streamChat(
        userMessage: String,
        systemContext: String = ""
    ): Flow<String> = flow {

        // BƯỚC 2: Lấy API Key an toàn từ BuildConfig
        val url = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}&alt=sse"

        Log.d(TAG, "=== BẮT ĐẦU GỌI GEMINI ===")
        // BƯỚC 3: Đã tắt log URL để tránh lộ key trên Logcat
        // Log.d(TAG, "URL: $url")
        Log.d(TAG, "Message: $userMessage")

        val body = buildRequestBody(userMessage, systemContext)

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Đang gửi request...")

        // execute() chạy trên Dispatchers.IO nên sẽ KHÔNG làm treo UI
        val response = GeminiClient.okHttp.newCall(request).execute()

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response successful: ${response.isSuccessful}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            Log.e(TAG, "LỖI HTTP: $errorBody")
            throw Exception("Gemini API lỗi ${response.code}: $errorBody")
        }

        val source = response.body?.source()
        if (source == null) {
            Log.e(TAG, "Source null!")
            return@flow
        }

        Log.d(TAG, "Bắt đầu đọc stream...")
        var lineCount = 0

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            lineCount++
            Log.d(TAG, "Line $lineCount: $line")

            when {
                line.startsWith("data: ") -> {
                    val jsonStr = line.removePrefix("data: ").trim()
                    if (jsonStr == "[DONE]") {
                        Log.d(TAG, "=== STREAM XONG ===")
                        break
                    }
                    val text = parseChunk(jsonStr)
                    Log.d(TAG, "Parsed text: '$text'")
                    if (text.isNotEmpty()) emit(text)
                }
                line.isEmpty() -> continue
                else -> Log.d(TAG, "Unknown line: $line")
            }
        }
        Log.d(TAG, "Tổng số line đọc được: $lineCount")

    }.flowOn(Dispatchers.IO)

    // Build JSON request body theo Gemini API format
    private fun buildRequestBody(
        userMessage: String,
        systemContext: String
    ): String {
        // System prompt: nói với Gemini về vai trò và context
        val systemPrompt = buildString {
            append("Bạn là trợ lý nấu ăn thông minh tên SmartSous. ")
            append("Trả lời ngắn gọn, thực tế, bằng tiếng Việt. ")
            append("Gợi ý món ăn cụ thể, có bước nấu rõ ràng.\n\n")
            if (systemContext.isNotEmpty()) {
                append("Nguyên liệu hiện có trong tủ lạnh của người dùng:\n")
                append(systemContext)
                append("\n\nHãy ưu tiên gợi ý món có thể nấu từ các nguyên liệu trên.")
            }
        }

        val root = JSONObject()

        // System instruction
        root.put("system_instruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemPrompt)
                })
            })
        })

        // User message
        root.put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userMessage)
                    })
                })
            })
        })

        // Generation config
        root.put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)       // sáng tạo vừa phải
            put("maxOutputTokens", 1024)  // giới hạn độ dài response
            put("topP", 0.9)
        })

        return root.toString()
    }

    // Parse 1 chunk SSE JSON lấy text delta
    // Format: {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}
    private fun parseChunk(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() == 0) return ""

            val content = candidates
                .getJSONObject(0)
                .optJSONObject("content") ?: return ""

            val parts = content.getJSONArray("parts")
            if (parts.length() == 0) return ""

            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            Log.w(TAG, "Parse chunk lỗi: ${e.message}, json: $jsonStr")
            ""
        }
    }
}