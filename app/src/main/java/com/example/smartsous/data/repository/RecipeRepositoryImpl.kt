package com.example.smartsous.data.repository

import android.util.Log
import com.example.smartsous.data.local.dao.RecipeDao
import com.example.smartsous.data.local.entity.toDomain
import com.example.smartsous.data.local.entity.toEntity
import com.example.smartsous.data.remote.FirestoreRecipeSource
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IRecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val firestoreSource: FirestoreRecipeSource
) : IRecipeRepository {

    companion object {
        private const val TAG = "RecipeRepository"
        // Cache hết hạn sau 24 tiếng
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }

    // Room Flow — UI tự cập nhật khi data thay đổi
    override fun getAllRecipes(): Flow<List<Recipe>> =
        recipeDao.getAllRecipes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getFavorites(): Flow<List<Recipe>> =
        recipeDao.getFavorites().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchRecipes(query: String): Flow<List<Recipe>> =
        recipeDao.searchRecipes(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRecipeById(id: String): Recipe? =
        recipeDao.getById(id)?.toDomain()

    override suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        recipeDao.updateFavorite(recipeId, isFavorite)
    }

    // Gọi khi muốn lấy data mới từ Firestore
    override suspend fun refreshFromRemote() {
        try {
            Log.d(TAG, "Đang fetch recipes từ Firestore...")
            val entities = firestoreSource.fetchAllRecipes()
            if (entities.isNotEmpty()) {
                recipeDao.upsertAll(entities)
                Log.d(TAG, "Cache ${entities.size} recipes vào Room")
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshFromRemote lỗi: ${e.message}")
            // Không throw — app vẫn dùng được cache cũ
        }
    }

    // Kiểm tra cache có cần refresh không
    // Gọi trong ViewModel khi màn hình mở lần đầu
    suspend fun refreshIfNeeded() {
        val allEntities = recipeDao.getAllRecipesOneShot()
        val needsRefresh = allEntities.isEmpty() ||
                allEntities.any { entity ->
                    System.currentTimeMillis() - entity.cachedAt > CACHE_TTL_MS
                }
        if (needsRefresh) refreshFromRemote()
    }
}