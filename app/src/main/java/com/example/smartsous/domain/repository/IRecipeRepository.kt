package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface IRecipeRepository {
    // Stream danh sách tất cả recipes từ Room
    fun getAllRecipes(): Flow<List<Recipe>>

    // Stream recipes yêu thích
    fun getFavorites(): Flow<List<Recipe>>

    // Tìm kiếm theo tên, tag, ẩm thực
    fun searchRecipes(query: String): Flow<List<Recipe>>

    // Lấy 1 recipe theo ID
    suspend fun getRecipeById(id: String): Recipe?

    // Bật/tắt yêu thích
    suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean)

    // Fetch từ Firestore về cache Room
    suspend fun refreshFromRemote()
}