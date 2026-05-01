package com.example.smartsous.data.local.dao

import androidx.room.*
import com.example.smartsous.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<RecipeEntity>>

    @Query("""
        SELECT * FROM recipes WHERE
        name LIKE '%' || :query || '%' OR
        tagsJson LIKE '%' || :query || '%' OR
        cuisine LIKE '%' || :query || '%'
    """)
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): RecipeEntity?

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Upsert
    suspend fun upsertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE cachedAt < :threshold")
    suspend fun clearStaleCache(threshold: Long)
}