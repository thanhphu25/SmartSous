package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.ChatCookingContext
import com.example.smartsous.domain.model.Ingredient
import javax.inject.Inject

class ChatContextBuilder @Inject constructor(
    private val suggestMealsUseCase: SuggestMealsUseCase
) {
    suspend fun build(
        intent: ChatIntent,
        userMessage: String,
        currentPantry: List<Ingredient>,
        topN: Int
    ): ChatCookingContext {
        // TODO: Implement actual context building logic for RAG
        return ChatCookingContext()
    }
}
