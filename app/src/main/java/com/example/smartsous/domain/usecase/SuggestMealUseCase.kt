package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import javax.inject.Inject
import kotlin.math.roundToInt

class SuggestMealsUseCase @Inject constructor() {

    companion object {
        // Trọng số 3 tiêu chí — tổng = 100
        private const val WEIGHT_INGREDIENT = 0.50f  // nguyên liệu sẵn có
        private const val WEIGHT_NUTRITION  = 0.30f  // dinh dưỡng hợp lý
        private const val WEIGHT_VARIETY    = 0.20f  // chưa nấu gần đây

        // Calories lý tưởng cho 1 bữa ăn gia đình
        private const val IDEAL_CALORIES_MIN = 200
        private const val IDEAL_CALORIES_MAX = 500

        // Số ngày không gợi ý lại món đã nấu
        private const val RECENT_COOK_DAYS = 7
    }

    // Trả về top N món gợi ý tốt nhất
    operator fun invoke(
        allRecipes: List<Recipe>,
        pantryIngredients: List<Ingredient>,
        recentlyCookedIds: List<String> = emptyList(), // ID món đã nấu gần đây
        topN: Int = 10
    ): List<SuggestedRecipe> {

        if (allRecipes.isEmpty()) return emptyList()

        // Tên nguyên liệu trong tủ — lowercase để so sánh không phân biệt hoa thường
        val pantryNames = pantryIngredients
            .map { it.name.lowercase().trim() }
            .toSet()

        return allRecipes
            .map { recipe ->
                scoreRecipe(recipe, pantryNames, recentlyCookedIds)
            }
            // Chỉ lấy những món có ít nhất 1 nguyên liệu khớp
            // hoặc là món nhanh/lành mạnh
            .filter { it.score > 10f }
            // Sort điểm cao → thấp
            .sortedByDescending { it.score }
            .take(topN)
    }

    private fun scoreRecipe(
        recipe: Recipe,
        pantryNames: Set<String>,
        recentlyCookedIds: List<String>
    ): SuggestedRecipe {

        // ── Tiêu chí 1: Nguyên liệu (50đ) ───────────────────
        val recipeIngredientNames = recipe.ingredients
            .map { it.name.lowercase().trim() }

        val matched = recipeIngredientNames.filter { ingName ->
            // So sánh linh hoạt — "thịt bò" khớp với "bò", "thịt ba chỉ" khớp với "ba chỉ"
            pantryNames.any { pantry ->
                ingName.contains(pantry) || pantry.contains(ingName)
            }
        }

        val missing = recipeIngredientNames.filter { ingName ->
            !pantryNames.any { pantry ->
                ingName.contains(pantry) || pantry.contains(ingName)
            }
        }

        val matchPercent = if (recipeIngredientNames.isEmpty()) 0
        else ((matched.size.toFloat() / recipeIngredientNames.size) * 100).roundToInt()

        val ingredientScore = matchPercent.toFloat() // 0..100 → * 0.5 sau

        // ── Tiêu chí 2: Dinh dưỡng (30đ) ────────────────────
        val calories = recipe.nutrition.calories
        val nutritionScore = when {
            calories in IDEAL_CALORIES_MIN..IDEAL_CALORIES_MAX -> 100f // lý tưởng
            calories < IDEAL_CALORIES_MIN -> 70f                        // ít quá
            calories <= 700 -> 50f                                      // nhiều nhưng chấp nhận được
            else -> 20f                                                  // quá nhiều
        }

        // ── Tiêu chí 3: Chưa nấu gần đây (20đ) ─────────────
        val varietyScore = if (recipe.id in recentlyCookedIds) 0f
        else 100f

        // ── Điểm tổng ────────────────────────────────────────
        val totalScore = (ingredientScore * WEIGHT_INGREDIENT)
            .plus(nutritionScore * WEIGHT_NUTRITION)
            .plus(varietyScore * WEIGHT_VARIETY)

        // ── Xác định lý do gợi ý chính ───────────────────────
        val reason = when {
            matchPercent == 100 -> SuggestionReason.PERFECT_MATCH
            matchPercent >= 70  -> SuggestionReason.HIGH_MATCH
            recipe.cookingTimeMinutes <= 20 -> SuggestionReason.QUICK_COOK
            nutritionScore == 100f -> SuggestionReason.HEALTHY_CHOICE
            else -> SuggestionReason.NOT_COOKED_RECENTLY
        }

        return SuggestedRecipe(
            recipe = recipe,
            score = totalScore,
            matchedIngredients = matched,
            missingIngredients = missing,
            matchPercent = matchPercent,
            reason = reason
        )
    }
}