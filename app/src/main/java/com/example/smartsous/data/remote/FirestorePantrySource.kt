package com.example.smartsous.data.remote

import android.util.Log
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.data.local.entity.IngredientEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class PantryChange {
    data class Added(val entity: IngredientEntity) : PantryChange()
    data class Modified(val entity: IngredientEntity) : PantryChange()
    data class Removed(val ingredientId: String) : PantryChange()
}

@Singleton
class FirestorePantrySource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    private val TAG = "FirestorePantrySource"

    // Path collection pantry của user hiện tại
    private val pantryCollection
        get() = firestore
            .collection("users")
            .document(authManager.uid)
            .collection("pantry")

    // Stream realtime changes từ Firestore dưới dạng Flow
    // callbackFlow chuyển Firestore listener (callback-based) thành Kotlin Flow
    fun observePantryChanges(): Flow<List<PantryChange>> = callbackFlow {
        Log.d(TAG, "Bắt đầu listen Firestore pantry...")

        val listener: ListenerRegistration = pantryCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot lỗi: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                // Parse từng loại thay đổi
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val data = change.document.data
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val entity = parseToEntity(data)
                                if (change.type == DocumentChange.Type.ADDED)
                                    PantryChange.Added(entity)
                                else
                                    PantryChange.Modified(entity)
                            }
                            DocumentChange.Type.REMOVED -> {
                                PantryChange.Removed(change.document.id)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse change lỗi: ${e.message}")
                        null
                    }
                }

                if (changes.isNotEmpty()) {
                    Log.d(TAG, "Nhận ${changes.size} thay đổi từ Firestore")
                    trySend(changes)
                }
            }

        // Khi Flow bị cancel (VD: ViewModel destroyed) → remove listener
        // Tránh memory leak và tốn quota Firestore
        awaitClose {
            Log.d(TAG, "Dừng listen Firestore pantry")
            listener.remove()
        }
    }

    // Upload toàn bộ pantry hiện tại lên Firestore (dùng khi lần đầu sync)
    suspend fun uploadAll(entities: List<IngredientEntity>) {
        if (entities.isEmpty()) return
        try {
            val batch = firestore.batch()
            entities.forEach { entity ->
                val doc = pantryCollection.document(entity.id)
                batch.set(doc, entity.toFirestoreMap())
            }
            batch.commit().await()
            Log.d(TAG, "Upload ${entities.size} ingredients lên Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Upload lỗi: ${e.message}")
        }
    }

    // Write 1 ingredient lên Firestore
    suspend fun upsert(entity: IngredientEntity) {
        try {
            pantryCollection.document(entity.id)
                .set(entity.toFirestoreMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Upsert Firestore lỗi: ${e.message}")
        }
    }

    // Xoá 1 ingredient khỏi Firestore
    suspend fun delete(ingredientId: String) {
        try {
            pantryCollection.document(ingredientId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Delete Firestore lỗi: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseToEntity(data: Map<String, Any>): IngredientEntity =
        IngredientEntity(
            id       = data["id"] as? String ?: "",
            name     = data["name"] as? String ?: "",
            quantity = (data["quantity"] as? Number)?.toDouble() ?: 0.0,
            unit     = data["unit"] as? String ?: "",
            category = data["category"] as? String ?: "OTHER",
            expiryDate = data["expiryDate"] as? String,
            addedDate  = data["addedDate"] as? String
                ?: java.time.LocalDate.now().toString()
        )

    private fun IngredientEntity.toFirestoreMap() = mapOf(
        "id"         to id,
        "name"       to name,
        "quantity"   to quantity,
        "unit"       to unit,
        "category"   to category,
        "expiryDate" to expiryDate,
        "addedDate"  to addedDate
    )
}