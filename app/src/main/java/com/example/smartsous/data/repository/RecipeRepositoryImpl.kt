package com.example.smartsous.data.repository

import android.util.Log
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.data.local.dao.RecipeDao
import com.example.smartsous.data.local.entity.toDomain
import com.example.smartsous.data.local.entity.toEntity
import com.example.smartsous.data.remote.FirestoreRecipeSource
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IRecipeRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val firestoreSource: FirestoreRecipeSource,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) : IRecipeRepository {

    // Khởi tạo một Scope riêng biệt cho việc đồng bộ ngầm
    // Dùng Dispatchers.IO cho các tác vụ mạng/database
    // Dùng SupervisorJob để nếu tác vụ mạng lỗi thì không làm sập toàn bộ các luồng khác
    private val SyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    // Collection favorites của user
    private val favoritesCollection
        get() = firestore
            .collection("users")
            .document(authManager.uid)
            .collection("favorites")

    override suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        // Room update ngay → Flow emit → UI recompose ngay lập tức
        recipeDao.updateFavorite(recipeId, isFavorite)

        // Firestore sync fire-and-forget — KHÔNG await
        // Không block hàm này nữa
        SyncScope.launch {
            try {
                if (isFavorite) {
                    favoritesCollection
                        .document(recipeId)
                        .set(mapOf(
                            "recipeId" to recipeId,
                            "savedAt"  to System.currentTimeMillis()
                        ))
                        .await()
                } else {
                    favoritesCollection.document(recipeId).delete().await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync favorite lỗi: ${e.message}")
            }
        }
        // Hàm return ngay sau Room update → UI recompose ngay ✅
    }

    override suspend fun refreshFromRemote() {
        try {
            val remoteEntities = firestoreSource.fetchAllRecipes()
            if (remoteEntities.isEmpty()) return

            // Lấy favorites từ Firestore (source of truth)
            val firestoreFavoriteIds = try {
                favoritesCollection.get().await()
                    .documents.map { it.id }.toSet()
            } catch (e: Exception) {
                // Offline → fallback: dùng favorites đang có trong Room
                recipeDao.getAllRecipesOneShot()
                    .filter { it.isFavorite }
                    .map { it.id }
                    .toSet()
            }

            // Upsert tất cả recipes từ Firestore
            recipeDao.upsertAll(remoteEntities)

            // Restore favorites đúng theo Firestore
            firestoreFavoriteIds.forEach { id ->
                recipeDao.updateFavorite(id, true)
            }

            Log.d(TAG, "Refresh xong — ${firestoreFavoriteIds.size} favorites restored")

        } catch (e: Exception) {
            Log.e(TAG, "refreshFromRemote lỗi: ${e.message}")
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