package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface IRecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getFavorites(): Flow<List<Recipe>>
    fun searchRecipes(query: String): Flow<List<Recipe>>
    suspend fun getRecipeById(id: String): Recipe?
    suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean)
    suspend fun refreshFromRemote()
}