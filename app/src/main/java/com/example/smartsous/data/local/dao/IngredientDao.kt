package com.example.smartsous.data.local.dao

import androidx.room.*
import com.example.smartsous.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM pantry_ingredients ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END ASC, expiryDate ASC")
    fun getAll(): Flow<List<IngredientEntity>>

    @Query("""
        SELECT * FROM pantry_ingredients
        WHERE expiryDate IS NOT NULL
        AND expiryDate <= :thresholdDate
        ORDER BY expiryDate ASC
    """)
    fun getExpiring(thresholdDate: String): Flow<List<IngredientEntity>>

    @Upsert
    suspend fun upsert(ingredient: IngredientEntity)

    @Delete
    suspend fun delete(ingredient: IngredientEntity)

    @Query("UPDATE pantry_ingredients SET quantity = :qty WHERE id = :id")
    suspend fun updateQuantity(id: String, qty: Double)
}
