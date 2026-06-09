package com.example.smartsous.domain.usecase

import javax.inject.Inject

// Kết quả sau khi phân tích intent
sealed class ChatIntent {

    // User hỏi nấu gì từ một số nguyên liệu cụ thể
    // VD: "nấu gì từ cà chua + thịt bò" → ingredients = ["cà chua", "thịt bò"]
    data class SuggestFromIngredients(
        val ingredients: List<String>
    ) : ChatIntent()

    // User hỏi tìm món theo tên/loại
    // VD: "có món gì từ thịt heo không"
    data class SearchRecipe(
        val query: String
    ) : ChatIntent()

    // User hỏi về dinh dưỡng
    // VD: "món ít calo nhất", "ăn gì để giảm cân"
    object NutritionAdvice : ChatIntent()

    // User hỏi về pantry của mình
    // VD: "tôi còn gì trong tủ lạnh", "nấu gì với đồ tôi đang có"
    object UseMyPantry : ChatIntent()

    // Câu hỏi thông thường → để Gemini xử lý
    object GeneralQuestion : ChatIntent()
}

class IntentDetector @Inject constructor() {

    // Pattern nhận dạng intent "gợi ý từ nguyên liệu cụ thể"
    private val suggestPatterns = listOf(
        Regex("""nấu\s*gì\s*(từ|với|bằng)\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(từ|với|có)\s*(.+)\s*nấu\s*gì""", RegexOption.IGNORE_CASE),
        Regex("""(làm|chế biến)\s*gì\s*(từ|với)\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(có|dùng)\s*(.+)\s*(làm|nấu)\s*(gì|món\s*gì)""", RegexOption.IGNORE_CASE),
        Regex("""món\s*gì\s*(từ|với)\s*(.+)""", RegexOption.IGNORE_CASE)
    )

    // Pattern nhận dạng intent "dùng pantry hiện tại"
    private val pantryPatterns = listOf(
        "tủ lạnh", "đang có", "hiện có", "còn lại", "nguyên liệu của tôi",
        "nấu gì hôm nay", "ăn gì hôm nay", "gợi ý món",
        "dùng đồ tôi có", "trong nhà có gì"
    )

    // Pattern nhận dạng intent "dinh dưỡng"
    private val nutritionPatterns = listOf(
        "ít calo", "giảm cân", "ít béo", "healthy", "lành mạnh",
        "dinh dưỡng", "protein", "vitamin", "ăn kiêng", "low carb"
    )

    // Phân tích message → trả về Intent
    fun detect(message: String): ChatIntent {
        val lower = message.lowercase().trim()

        // 1. Kiểm tra intent "dùng pantry"
        if (pantryPatterns.any { lower.contains(it) } &&
            !hasSeparator(lower)) {
            return ChatIntent.UseMyPantry
        }

        // 2. Kiểm tra intent "gợi ý từ nguyên liệu cụ thể"
        for (pattern in suggestPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val ingredientsText = match.groupValues.lastOrNull { it.isNotBlank() }
                    ?: continue
                val ingredients = extractIngredients(ingredientsText)
                if (ingredients.isNotEmpty()) {
                    return ChatIntent.SuggestFromIngredients(ingredients)
                }
            }
        }

        // 3. Kiểm tra nếu message chứa dấu phân cách nguyên liệu
        if (hasSeparator(lower) && !lower.contains("?") ||
            lower.split(Regex("[,+&]")).size >= 2) {
            val ingredients = extractIngredients(lower)
            if (ingredients.size >= 2) {
                return ChatIntent.SuggestFromIngredients(ingredients)
            }
        }

        // 4. Kiểm tra intent "dinh dưỡng"
        if (nutritionPatterns.any { lower.contains(it) }) {
            return ChatIntent.NutritionAdvice
        }

        // 5. Mặc định → câu hỏi thông thường
        return ChatIntent.GeneralQuestion
    }

    // Tách tên nguyên liệu từ chuỗi
    // "cà chua, thịt bò và hành tây" → ["cà chua", "thịt bò", "hành tây"]
    fun extractIngredients(text: String): List<String> {
        return text
            .split(Regex("[,+&\\nvà/]"))
            .map { it.trim() }
            .filter { word ->
                word.isNotBlank() &&
                        word.length >= 2 &&
                        // Lọc bỏ stop words
                        word !in stopWords
            }
            .map { cleanIngredientName(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8) // Tối đa 8 nguyên liệu
    }

    // Kiểm tra có dấu phân cách nguyên liệu không
    private fun hasSeparator(text: String): Boolean =
        text.contains(',') || text.contains('+') ||
                text.contains('&') || text.contains(" và ")

    // Làm sạch tên nguyên liệu
    private fun cleanIngredientName(name: String): String =
        name
            .replace(Regex("""^\d+\s*"""), "") // bỏ số đầu "2 cà chua"
            .replace(Regex("""\s*(gram|kg|ml|lít|g|kg)\s*\d*"""), "") // bỏ đơn vị
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "") // bỏ ký tự đặc biệt
            .trim()

    private val stopWords = setOf(
        "gì", "và", "với", "từ", "có", "của", "tôi", "nấu",
        "làm", "chế", "biến", "món", "ăn", "thêm", "bỏ",
        "một", "hai", "ba", "vài", "nhiều", "ít"
    )
}