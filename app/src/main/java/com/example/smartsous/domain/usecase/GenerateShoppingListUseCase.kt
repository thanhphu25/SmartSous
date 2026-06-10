package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.ShoppingListItem
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GenerateShoppingListUseCase @Inject constructor(
    private val mealPlanRepo: IMealPlanRepository,
    private val recipeRepo: IRecipeRepository,
    private val pantryRepo: IPantryRepository
) {
    operator fun invoke(startDate: LocalDate): Flow<List<ShoppingListItem>> {
        return combine(
            mealPlanRepo.getMealPlanForWeek(startDate),
            pantryRepo.getAllIngredients()
        ) { plans, pantryItems ->
            
            // 1. Lấy tất cả recipe ID và đếm số lần xuất hiện (ví dụ ăn 2 lần trong tuần)
            val recipeCounts = mutableMapOf<String, Int>()
            for (plan in plans) {
                for (mealList in plan.meals.values) {
                    for (id in mealList) {
                        recipeCounts[id] = (recipeCounts[id] ?: 0) + 1
                    }
                }
            }

            // 2. Tính tổng nguyên liệu cần thiết (Required)
            // Key: Pair(Tên nguyên liệu, Đơn vị) -> Value: Số lượng
            val requiredMap = mutableMapOf<Pair<String, String>, Double>()
            
            for ((recipeId, count) in recipeCounts) {
                val recipe = recipeRepo.getRecipeById(recipeId) ?: continue
                for (ing in recipe.ingredients) {
                    val key = Pair(ing.name.lowercase().trim(), ing.unit.lowercase().trim())
                    requiredMap[key] = (requiredMap[key] ?: 0.0) + (ing.amount * count)
                }
            }

            // 3. Tính tổng nguyên liệu đang có trong Pantry (Available)
            val pantryMap = pantryItems
                .groupBy { Pair(it.name.lowercase().trim(), it.unit.lowercase().trim()) }
                .mapValues { entry -> entry.value.sumOf { it.quantity } }

            // 4. Trừ đi và tạo danh sách đi chợ
            val shoppingList = mutableListOf<ShoppingListItem>()
            for ((key, requiredAmount) in requiredMap) {
                val availableAmount = pantryMap[key] ?: 0.0
                val missingAmount = requiredAmount - availableAmount
                if (missingAmount > 0) {
                    // Trả lại tên với chữ viết hoa chữ cái đầu cho đẹp
                    val displayItemName = key.first.replaceFirstChar { it.uppercase() }
                    shoppingList.add(ShoppingListItem(displayItemName, missingAmount, key.second))
                }
            }

            // Trả về danh sách sắp xếp theo tên
            shoppingList.sortedBy { it.name }
        }
    }
}
