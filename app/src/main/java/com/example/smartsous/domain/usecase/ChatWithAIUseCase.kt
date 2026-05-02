package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatWithAIUseCase @Inject constructor(
    private val chatRepository: IChatRepository
) {
    // operator fun invoke = gọi useCase(args) thay vì useCase.execute(args)
    operator fun invoke(
        userMessage: String,
        pantryIngredients: List<Ingredient> = emptyList()
    ): Flow<String> {
        // Build context string từ danh sách nguyên liệu
        val context = if (pantryIngredients.isEmpty()) ""
        else pantryIngredients.joinToString("\n") { ingredient ->
            "- ${ingredient.name}: ${ingredient.quantity} ${ingredient.unit}"
        }

        return chatRepository.streamChat(
            userMessage = userMessage,
            systemContext = context
        )
    }
}