package com.example.smartsous.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeSeedDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "RecipeSeedDataSource"
    }

    // Đọc JSON từ assets và seed lên Firestore
    suspend fun seedRecipes() {
        try {
            Log.d(TAG, "Bắt đầu seed recipes...")

            // Đọc file JSON từ assets
            val jsonString = context.assets
                .open("recipes_seed.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            Log.d(TAG, "Tổng số recipes cần seed: ${jsonArray.length()}")

            // Batch write — tất cả hoặc không gì cả
            val batch: WriteBatch = firestore.batch()
            val recipesCol = firestore.collection("recipes")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id  = obj.getString("id")

                // Parse ingredients array
                val ingredientsRaw = obj.getJSONArray("ingredients")
                val ingredients = (0 until ingredientsRaw.length()).map { j ->
                    val ing = ingredientsRaw.getJSONObject(j)
                    mapOf(
                        "name"   to ing.getString("name"),
                        "amount" to ing.getDouble("amount"),
                        "unit"   to ing.getString("unit")
                    )
                }

                // Parse steps array
                val stepsRaw = obj.getJSONArray("steps")
                val steps = (0 until stepsRaw.length()).map { j ->
                    stepsRaw.getString(j)
                }

                // Parse tags array
                val tagsRaw = obj.getJSONArray("tags")
                val tags = (0 until tagsRaw.length()).map { j ->
                    tagsRaw.getString(j)
                }

                // Parse nutrition object
                val nutritionObj = obj.getJSONObject("nutrition")
                val nutrition = mapOf(
                    "calories" to nutritionObj.getInt("calories"),
                    "protein"  to nutritionObj.getDouble("protein"),
                    "carbs"    to nutritionObj.getDouble("carbs"),
                    "fat"      to nutritionObj.getDouble("fat"),
                    "fiber"    to nutritionObj.getDouble("fiber")
                )

                // Build Firestore document
                val recipeMap = mapOf(
                    "id"                  to id,
                    "name"                to obj.getString("name"),
                    "description"         to obj.getString("description"),
                    "imageUrl"            to obj.getString("imageUrl"),
                    "cookingTimeMinutes"  to obj.getInt("cookingTimeMinutes"),
                    "servings"            to obj.getInt("servings"),
                    "difficulty"          to obj.getString("difficulty"),
                    "cuisine"             to obj.getString("cuisine"),
                    "tags"                to tags,
                    "ingredients"         to ingredients,
                    "steps"               to steps,
                    "nutrition"           to nutrition,
                    "isFavorite"          to false
                )

                // Thêm vào batch — dùng id từ JSON làm document ID
                batch.set(recipesCol.document(id), recipeMap)
            }

            // Commit toàn bộ batch 1 lần
            batch.commit().await()
            Log.d(TAG, "Seed thành công ${jsonArray.length()} recipes!")

        } catch (e: Exception) {
            Log.e(TAG, "Seed thất bại: ${e.message}", e)
            throw e
        }
    }
}