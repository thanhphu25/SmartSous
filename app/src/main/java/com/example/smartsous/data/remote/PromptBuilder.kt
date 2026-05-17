package com.example.smartsous.data.remote

import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import java.time.LocalDate

object PromptBuilder {

    // System prompt chính — nói với Gemini về vai trò và cách hành xử
    fun buildSystemPrompt(pantryIngredients: List<Ingredient>): String = buildString {

        // ── Vai trò ───────────────────────────────────────────
        append("""
            Bạn là SmartSous — trợ lý nấu ăn thông minh cho gia đình Việt Nam.
            
            VAI TRÒ CỦA BẠN:
            - Gợi ý món ăn phù hợp với nguyên liệu người dùng đang có
            - Hướng dẫn cách nấu chi tiết, dễ hiểu
            - Tư vấn về dinh dưỡng và cách bảo quản thực phẩm
            - Trả lời mọi câu hỏi liên quan đến ẩm thực
            
            CÁCH TRẢ LỜI:
            - Luôn dùng tiếng Việt, thân thiện như người bạn đang trò chuyện
            - Ngắn gọn, súc tích — không lan man
            - Khi gợi ý món: nêu tên món + thời gian nấu + độ khó + 1-2 câu mô tả
            - Khi hướng dẫn nấu: đánh số từng bước rõ ràng
            - Nếu không biết → nói thẳng, đừng bịa
            - Chỉ tư vấn về ẩm thực — không trả lời câu hỏi ngoài chủ đề
            
        """.trimIndent())

        // ── Context nguyên liệu ───────────────────────────────
        if (pantryIngredients.isEmpty()) {
            append("\nNGUYÊN LIỆU: Người dùng chưa khai báo nguyên liệu trong tủ lạnh.")
            append("\nGợi ý: Hỏi người dùng họ đang có gì để gợi ý món phù hợp hơn.\n")
        } else {
            append("\nNGUYÊN LIỆU HIỆN CÓ TRONG TỦ LẠNH:\n")

            // Nhóm theo category cho dễ đọc
            val grouped = pantryIngredients.groupBy { it.category }

            grouped.forEach { (category, items) ->
                val categoryName = when (category) {
                    IngredientCategory.MEAT     -> "🥩 Thịt"
                    IngredientCategory.SEAFOOD  -> "🦐 Hải sản"
                    IngredientCategory.VEGETABLE -> "🥦 Rau củ"
                    IngredientCategory.DAIRY    -> "🥛 Sữa & trứng"
                    IngredientCategory.GRAIN    -> "🌾 Ngũ cốc"
                    IngredientCategory.SPICE    -> "🧄 Gia vị"
                    IngredientCategory.FRUIT    -> "🍎 Trái cây"
                    IngredientCategory.BEVERAGE -> "🧃 Đồ uống"
                    IngredientCategory.OTHER    -> "📦 Khác"
                }
                append("$categoryName:\n")
                items.forEach { ing ->
                    val expiryInfo = ing.expiryDate?.let { date ->
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS
                            .between(LocalDate.now(), date).toInt()
                        when {
                            daysLeft <= 0 -> " ⚠️ ĐÃ HẾT HẠN"
                            daysLeft <= 2 -> " ⚠️ hết hạn trong $daysLeft ngày"
                            daysLeft <= 5 -> " (còn $daysLeft ngày)"
                            else -> ""
                        }
                    } ?: ""
                    append("  - ${ing.name}: ${formatQuantity(ing.quantity)} ${ing.unit}$expiryInfo\n")
                }
            }

            // Nhắc về nguyên liệu sắp hết hạn
            val expiringSoon = pantryIngredients.filter { ing ->
                ing.expiryDate?.let { date ->
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS
                        .between(LocalDate.now(), date).toInt()
                    daysLeft in 0..3
                } ?: false
            }

            if (expiringSoon.isNotEmpty()) {
                append("\n⚠️ ƯU TIÊN GỢI Ý: ${expiringSoon.joinToString(", ") { it.name }}")
                append(" — những nguyên liệu này sắp hết hạn, hãy ưu tiên gợi ý dùng chúng trước!\n")
            }
        }

        // ── Hướng dẫn format output ───────────────────────────
        append("""
            
            FORMAT KHI GỢI Ý MÓN ĂN:
            **[Tên món]** (⏱ X phút | 🌟 Độ khó)
            [1-2 câu mô tả ngắn]
            Nguyên liệu cần thêm: [nếu thiếu gì]
            
            Hãy gợi ý 2-3 món khi được hỏi, không liệt kê quá nhiều.
        """.trimIndent())
    }

    // Build conversation history để gửi lên Gemini
    // Gemini cần biết context hội thoại trước để trả lời đúng
    fun buildConversationMessages(
        history: List<com.example.smartsous.domain.model.ChatMessage>,
        newUserMessage: String
    ): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()

        // Lịch sử (tối đa 10 tin nhắn gần nhất để tránh token quá lớn)
        history.takeLast(10).forEach { msg ->
            val role = when (msg.role) {
                com.example.smartsous.domain.model.MessageRole.USER      -> "user"
                com.example.smartsous.domain.model.MessageRole.ASSISTANT -> "model"
            }
            messages.add(
                mapOf(
                    "role"  to role,
                    "parts" to listOf(mapOf("text" to msg.content))
                )
            )
        }

        // Tin nhắn mới của user
        messages.add(
            mapOf(
                "role"  to "user",
                "parts" to listOf(mapOf("text" to newUserMessage))
            )
        )

        return messages
    }

    // Quick-reply suggestions dựa theo ngữ cảnh
    fun buildQuickReplies(
        pantryIngredients: List<Ingredient>,
        lastAssistantMessage: String
    ): List<String> {
        val suggestions = mutableListOf<String>()

        // Nếu pantry có nguyên liệu → gợi ý câu hỏi về nguyên liệu đó
        if (pantryIngredients.isNotEmpty()) {
            val randomIngredient = pantryIngredients.random()
            suggestions.add("Nấu gì với ${randomIngredient.name}?")
        }

        // Gợi ý chung theo thời gian
        val hour = java.time.LocalTime.now().hour
        when (hour) {
            in 6..9   -> suggestions.add("Gợi ý bữa sáng nhanh?")
            in 10..13 -> suggestions.add("Gợi ý bữa trưa hôm nay?")
            in 17..20 -> suggestions.add("Gợi ý bữa tối cho gia đình?")
        }

        // Gợi ý cố định
        suggestions.addAll(listOf(
            "Món gì nấu dưới 20 phút?",
            "Cách bảo quản thực phẩm?",
            "Món chay đơn giản?"
        ))

        return suggestions.take(3) // Chỉ hiện 3 gợi ý
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString()
        else quantity.toString()
}