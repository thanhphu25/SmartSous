package com.example.smartsous.data.remote

import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.MessageRole
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object PromptBuilder {

    fun buildSystemPrompt(
        pantryIngredients: List<Ingredient>,
        ragContext: String = ""
    ): String = buildString {
        append(
            """
            Ban la SmartSous, tro ly nau an thong minh cho gia dinh Viet Nam.

            VAI TRO:
            - Goi y mon an dua tren du lieu that cua ung dung: recipe, pantry, so luong, han su dung.
            - Huong dan nau an, dinh duong, bao quan thuc pham va di cho.
            - Chi tra loi trong pham vi nau an/thuc pham/thuc don/dinh duong.

            CACH TRA LOI:
            - Luon tra loi bang tieng Viet, than thien, ngan gon.
            - Neu goi y mon: neu ten mon, ly do phu hop, thoi gian, nguyen lieu thieu neu co.
            - Neu huong dan nau: danh so cac buoc ro rang.
            - Neu khong co du lieu trong SmartSous, noi ro va hoi them 1 cau ngan.
            - Khong bia cong thuc, khong bia mon ngoai context khi nguoi dung hoi goi y tu du lieu app.
            """.trimIndent()
        )

        append("\n\nPANTRY HIEN TAI:\n")
        if (pantryIngredients.isEmpty()) {
            append("- Chua co nguyen lieu nao trong tu lanh.\n")
        } else {
            pantryIngredients
                .groupBy { it.category }
                .forEach { (category, items) ->
                    append("${category.displayName()}:\n")
                    items.take(30).forEach { ingredient ->
                        append(
                            "- ${ingredient.name}: ${formatQuantity(ingredient.quantity)} ${ingredient.unit}" +
                                ingredient.expiryText() + "\n"
                        )
                    }
                }
        }

        if (ragContext.isNotBlank()) {
            append("\n\nNGU CANH RAG TU SMARTSOUS:\n")
            append(ragContext)
            append(
                """

                QUY TAC RAG BAT BUOC:
                - Uu tien RECOMMENDED_RECIPES theo thu tu da cho.
                - Chi goi y mon co trong RECOMMENDED_RECIPES hoac RELEVANT_RECIPES.
                - Neu can mua them, chi dua vao missingIngredients cua recipe.
                - Neu user hoi mon sap het han, uu tien EXPIRING_SOON.
                - Neu user hoi ngoai nau an/thuc pham, tu choi ngan gon va keo ve chu de nau an.
                """.trimIndent()
            )
        }
    }

    fun buildConversationMessages(
        history: List<ChatMessage>,
        newUserMessage: String
    ): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()

        history.takeLast(10).forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
            }
            messages.add(
                mapOf(
                    "role" to role,
                    "parts" to listOf(mapOf("text" to msg.content))
                )
            )
        }

        messages.add(
            mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to newUserMessage))
            )
        )

        return messages
    }

    fun buildQuickReplies(
        pantryIngredients: List<Ingredient>,
        lastAssistantMessage: String
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (pantryIngredients.isNotEmpty()) {
            suggestions.add("Nấu gì với ${pantryIngredients.first().name}?")
        }

        val lower = lastAssistantMessage.lowercase()
        if (lower.contains("mua") || lower.contains("thiếu")) {
            suggestions.add("Gợi ý món khác ít thiếu hơn?")
        }

        when (LocalTime.now().hour) {
            in 6..9 -> suggestions.add("Gợi ý bữa sáng nhanh?")
            in 10..13 -> suggestions.add("Gợi ý bữa trưa hôm nay?")
            in 17..20 -> suggestions.add("Gợi ý bữa tối cho gia đình?")
        }

        suggestions.addAll(
            listOf(
                "Món gì nấu dưới 20 phút?",
                "Cần mua thêm gì?",
                "Món nào dùng đồ sắp hết hạn?"
            )
        )

        return suggestions.distinct().take(3)
    }

    private fun IngredientCategory.displayName(): String =
        when (this) {
            IngredientCategory.MEAT -> "Thit"
            IngredientCategory.SEAFOOD -> "Hai san"
            IngredientCategory.VEGETABLE -> "Rau cu"
            IngredientCategory.DAIRY -> "Sua & trung"
            IngredientCategory.GRAIN -> "Ngu coc"
            IngredientCategory.SPICE -> "Gia vi"
            IngredientCategory.FRUIT -> "Trai cay"
            IngredientCategory.BEVERAGE -> "Do uong"
            IngredientCategory.OTHER -> "Khac"
        }

    private fun Ingredient.expiryText(): String {
        val date = expiryDate ?: return ""
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), date)
        return when {
            daysLeft < 0 -> " - DA HET HAN"
            daysLeft == 0L -> " - HET HAN HOM NAY"
            daysLeft <= 3 -> " - sap het han trong $daysLeft ngay"
            daysLeft <= 7 -> " - con $daysLeft ngay"
            else -> ""
        }
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString()
        else quantity.toString()
}
