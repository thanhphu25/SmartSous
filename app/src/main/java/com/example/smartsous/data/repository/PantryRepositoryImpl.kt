package com.example.smartsous.data.repository

import android.util.Log
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.data.local.dao.IngredientDao
import com.example.smartsous.data.local.entity.toDomain
import com.example.smartsous.data.local.entity.toEntity
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.repository.IPantryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val ingredientDao: IngredientDao,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) : IPantryRepository {

    companion object {
        private const val TAG = "PantryRepository"
    }

    // Path Firestore của user hiện tại
    private val pantryCollection
        get() = firestore
            .collection("users")
            .document(authManager.uid)
            .collection("pantry")

    // Room Flow — realtime update khi data thay đổi
    override fun getAllIngredients(): Flow<List<Ingredient>> =
        ingredientDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    // Lấy những nguyên liệu hết hạn trong X ngày tới
    override fun getExpiringIngredients(withinDays: Int): Flow<List<Ingredient>> {
        val threshold = LocalDate.now().plusDays(withinDays.toLong()).toString()
        return ingredientDao.getExpiring(threshold).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsert(ingredient: Ingredient) {
        // 1. Lưu Room ngay — UI cập nhật tức thì
        ingredientDao.upsert(ingredient.toEntity())

        // 2. Sync Firestore sau (không block UI)
        try {
            pantryCollection
                .document(ingredient.id)
                .set(ingredient.toFirestoreMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Sync Firestore upsert lỗi: ${e.message}")
            // Không throw — Room đã lưu rồi, offline vẫn hoạt động
        }
    }

    override suspend fun delete(ingredient: Ingredient) {
        // 1. Xoá Room ngay
        ingredientDao.delete(ingredient.toEntity())

        // 2. Xoá Firestore sau
        try {
            pantryCollection.document(ingredient.id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Sync Firestore delete lỗi: ${e.message}")
        }
    }

    override suspend fun updateQuantity(id: String, quantity: Double) {
        ingredientDao.updateQuantity(id, quantity)
        try {
            pantryCollection.document(id).update("quantity", quantity).await()
        } catch (e: Exception) {
            Log.e(TAG, "Sync Firestore updateQty lỗi: ${e.message}")
        }
    }

    // Mapper Domain -> Firestore Map
    private fun Ingredient.toFirestoreMap() = mapOf(
        "id"         to id,
        "name"       to name,
        "quantity"   to quantity,
        "unit"       to unit,
        "category"   to category.name,
        "expiryDate" to expiryDate?.toString(),
        "addedDate"  to addedDate.toString()
    )
}