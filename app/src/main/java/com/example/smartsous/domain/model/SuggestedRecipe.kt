package com.example.smartsous.domain.model

data class SuggestedRecipe(
    val recipe: Recipe,
    val score: Float,               // 0..100
    val matchedIngredients: List<String>,   // nguyên liệu đang có
    val missingIngredients: List<String>,   // nguyên liệu còn thiếu
    val matchPercent: Int,          // % nguyên liệu khớp
    val reason: SuggestionReason    // lý do chính được gợi ý
)

enum class SuggestionReason {
    PERFECT_MATCH,      // có đủ 100% nguyên liệu
    HIGH_MATCH,         // có >70% nguyên liệu
    HEALTHY_CHOICE,     // dinh dưỡng tốt
    QUICK_COOK,         // nấu nhanh dưới 20 phút
    NOT_COOKED_RECENTLY // chưa nấu món này gần đây
}