package com.example.smartsous.domain.repository

import com.example.smartsous.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

interface IPantryRepository {
    fun getAllIngredients(): Flow<List<Ingredient>>
    fun getExpiringIngredients(withinDays: Int): Flow<List<Ingredient>>
    suspend fun upsert(ingredient: Ingredient)
    suspend fun delete(ingredient: Ingredient)
    suspend fun updateQuantity(id: String, quantity: Double)
}