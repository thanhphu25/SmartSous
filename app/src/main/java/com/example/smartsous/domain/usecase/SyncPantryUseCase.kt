package com.example.smartsous.domain.usecase

import android.util.Log
import com.example.smartsous.data.local.dao.IngredientDao
import com.example.smartsous.data.remote.FirestorePantrySource
import javax.inject.Inject

class SyncPantryUseCase @Inject constructor(
    private val ingredientDao: IngredientDao,
    private val firestoreSource: FirestorePantrySource
) {
    suspend operator fun invoke() {
        try {
            val localIngredients = ingredientDao.getAllOnce()
            if (localIngredients.isEmpty()) return

            // Upload tất cả lên Firestore
            firestoreSource.uploadAll(localIngredients)
            Log.d("SyncPantryUseCase", "Synced ${localIngredients.size} ingredients")

        } catch (e: Exception) {
            Log.e("SyncPantryUseCase", "Sync lỗi: ${e.message}")
        }
    }
}