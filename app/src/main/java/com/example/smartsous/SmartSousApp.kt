package com.example.smartsous

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.domain.usecase.SeedRecipesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartSousApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var seedRecipesUseCase: SeedRecipesUseCase

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            // Bước 1: Login trước — cần UID để Firestore rules cho phép đọc
            try {
                authManager.loginAnonymously()
            } catch (e: Exception) {
                android.util.Log.e("SmartSousApp", "Auth lỗi: ${e.message}")
                return@launch  // Auth thất bại → không seed được
            }

            // Bước 2: Seed data sau khi đã có auth
            try {
                seedRecipesUseCase()
            } catch (e: Exception) {
                android.util.Log.e("SmartSousApp", "Seed lỗi: ${e.message}")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}