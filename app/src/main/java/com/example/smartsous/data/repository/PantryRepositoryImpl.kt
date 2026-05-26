package com.example.smartsous.data.repository

import android.util.Log
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.data.local.dao.IngredientDao
import com.example.smartsous.data.local.entity.IngredientEntity
import com.example.smartsous.data.local.entity.toDomain
import com.example.smartsous.data.local.entity.toEntity
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.repository.IPantryRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    // ── THÊM MỚI: scope riêng cho background sync ─────────
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Path Firestore của user hiện tại — GIỮ NGUYÊN
    private val pantryCollection
        get() = firestore
            .collection("users")
            .document(authManager.uid)
            .collection("pantry")

    // ── THÊM MỚI: bắt đầu lắng nghe Firestore khi khởi tạo
    init {
        startRealtimeSync()
    }

    // ── THÊM MỚI: snapshot listener ───────────────────────
    private fun startRealtimeSync() {
        syncScope.launch {
            try {
                // Firestore listener chạy trên main thread
                // nhưng ta xử lý DB trên IO thread
                pantryCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Snapshot lỗi: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                    // Xử lý từng loại thay đổi
                    syncScope.launch {
                        snapshot.documentChanges.forEach { change ->
                            try {
                                when (change.type) {
                                    // Firestore có document mới / bị sửa → upsert vào Room
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val entity = parseFirestoreDocument(
                                            change.document.data
                                        )
                                        ingredientDao.upsert(entity)
                                        Log.d(TAG,
                                            "${change.type}: ${entity.name}")
                                    }
                                    // Firestore xoá document → xoá trong Room
                                    DocumentChange.Type.REMOVED -> {
                                        val id = change.document.id
                                        ingredientDao.getById(id)?.let {
                                            ingredientDao.delete(it)
                                        }
                                        Log.d(TAG, "REMOVED: $id")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Xử lý change lỗi: ${e.message}")
                            }
                        }
                    }
                }
                Log.d(TAG, "Realtime sync đã bắt đầu")
            } catch (e: Exception) {
                Log.e(TAG, "startRealtimeSync lỗi: ${e.message}")
            }
        }
    }

    // ── THÊM MỚI: parse Firestore document thành Room entity
    @Suppress("UNCHECKED_CAST")
    private fun parseFirestoreDocument(data: Map<String, Any>): IngredientEntity =
        IngredientEntity(
            id         = data["id"] as? String ?: "",
            name       = data["name"] as? String ?: "",
            quantity   = (data["quantity"] as? Number)?.toDouble() ?: 0.0,
            unit       = data["unit"] as? String ?: "",
            category   = data["category"] as? String ?: "OTHER",
            expiryDate = data["expiryDate"] as? String,
            addedDate  = data["addedDate"] as? String
                ?: LocalDate.now().toString()
        )

    // ════════════════════════════════════════════════════════
    // Phần bên dưới GIỮ NGUYÊN 100% — không thay đổi gì
    // ════════════════════════════════════════════════════════

    override fun getAllIngredients(): Flow<List<Ingredient>> =
        ingredientDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getExpiringIngredients(withinDays: Int): Flow<List<Ingredient>> {
        val threshold = LocalDate.now().plusDays(withinDays.toLong()).toString()
        return ingredientDao.getExpiring(threshold).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsert(ingredient: Ingredient) {
        ingredientDao.upsert(ingredient.toEntity())
        try {
            pantryCollection
                .document(ingredient.id)
                .set(ingredient.toFirestoreMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Sync Firestore upsert lỗi: ${e.message}")
        }
    }

    override suspend fun delete(ingredient: Ingredient) {
        ingredientDao.delete(ingredient.toEntity())
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