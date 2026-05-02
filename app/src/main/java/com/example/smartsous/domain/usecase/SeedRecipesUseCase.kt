package com.example.smartsous.domain.usecase

import android.util.Log
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.data.remote.RecipeSeedDataSource
import javax.inject.Inject

class SeedRecipesUseCase @Inject constructor(
    private val seedDataSource: RecipeSeedDataSource,
    private val dataStoreManager: DataStoreManager
) {
    suspend operator fun invoke() {
        // Kiểm tra flag — nếu đã seed thì không làm gì
        if (dataStoreManager.isRecipesSeeded()) {
            Log.d("SeedUseCase", "Đã seed rồi, bỏ qua")
            return
        }

        // Chưa seed → seed và đánh flag
        seedDataSource.seedRecipes()
        dataStoreManager.markRecipesSeeded()
        Log.d("SeedUseCase", "Seed hoàn tất, đã đánh flag")
    }
}