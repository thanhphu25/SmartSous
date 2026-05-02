package com.example.smartsous.data.remote

import android.util.Log
import com.example.smartsous.data.local.entity.RecipeEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRecipeSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreRecipeSource"
    }

    // Lấy tất cả recipes từ Firestore
    suspend fun fetchAllRecipes(): List<RecipeEntity> {
        return try {
            val snapshot = firestore
                .collection("recipes")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    // Firestore trả về Map — parse thủ công
                    val data = doc.data ?: return@mapNotNull null
                    parseToEntity(data)
                } catch (e: Exception) {
                    Log.w(TAG, "Parse doc ${doc.id} lỗi: ${e.message}")
                    null
                }
            }.also {
                Log.d(TAG, "Fetch được ${it.size} recipes từ Firestore")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fetch Firestore lỗi: ${e.message}")
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseToEntity(data: Map<String, Any>): RecipeEntity {
        // Parse ingredients list
        val ingredientsList = (data["ingredients"] as? List<Map<String, Any>>)
            ?: emptyList()
        val ingredientsJson = org.json.JSONArray().apply {
            ingredientsList.forEach { ing ->
                put(org.json.JSONObject().apply {
                    put("name",   ing["name"] as? String ?: "")
                    put("amount", (ing["amount"] as? Number)?.toDouble() ?: 0.0)
                    put("unit",   ing["unit"] as? String ?: "")
                })
            }
        }.toString()

        // Parse steps list
        val stepsList = (data["steps"] as? List<String>) ?: emptyList()
        val stepsJson = org.json.JSONArray().apply {
            stepsList.forEach { put(it) }
        }.toString()

        // Parse tags list
        val tagsList = (data["tags"] as? List<String>) ?: emptyList()
        val tagsJson = org.json.JSONArray().apply {
            tagsList.forEach { put(it) }
        }.toString()

        // Parse nutrition map
        val nutritionMap = (data["nutrition"] as? Map<String, Any>) ?: emptyMap()
        val nutritionJson = org.json.JSONObject().apply {
            put("calories", (nutritionMap["calories"] as? Number)?.toInt() ?: 0)
            put("protein",  (nutritionMap["protein"]  as? Number)?.toDouble() ?: 0.0)
            put("carbs",    (nutritionMap["carbs"]    as? Number)?.toDouble() ?: 0.0)
            put("fat",      (nutritionMap["fat"]      as? Number)?.toDouble() ?: 0.0)
            put("fiber",    (nutritionMap["fiber"]    as? Number)?.toDouble() ?: 0.0)
        }.toString()

        return RecipeEntity(
            id                   = data["id"] as? String ?: "",
            name                 = data["name"] as? String ?: "",
            description          = data["description"] as? String ?: "",
            imageUrl             = data["imageUrl"] as? String ?: "",
            cookingTimeMinutes   = (data["cookingTimeMinutes"] as? Number)?.toInt() ?: 0,
            servings             = (data["servings"] as? Number)?.toInt() ?: 1,
            difficulty           = data["difficulty"] as? String ?: "EASY",
            ingredientsJson      = ingredientsJson,
            stepsJson            = stepsJson,
            nutritionJson        = nutritionJson,
            tagsJson             = tagsJson,
            cuisine              = data["cuisine"] as? String ?: "",
            isFavorite           = data["isFavorite"] as? Boolean ?: false,
            cachedAt             = System.currentTimeMillis()
        )
    }
}