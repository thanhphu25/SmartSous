package com.example.smartsous

import android.app.Application
import android.util.Log
import androidx.datastore.dataStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.smartsous.core.common.AuthManager
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.core.notification.NotificationChannels
import com.example.smartsous.core.notification.WorkerScheduler
import com.example.smartsous.domain.usecase.SeedRecipesUseCase
import com.example.smartsous.domain.usecase.SyncPantryUseCase
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

    @Inject lateinit var dataStoreManager: DataStoreManager //to reset first time
    @Inject lateinit var workerScheduler: WorkerScheduler

    @Inject lateinit var syncPantryUseCase: SyncPantryUseCase

    override fun onCreate() {
        super.onCreate()

        NotificationChannels.createAll(this)
        workerScheduler.scheduleAll()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                authManager.loginAnonymously()
            } catch (e: Exception) {
                Log.e("SmartSousApp", "Auth lỗi: ${e.message}")
                return@launch
            }

            //first time
            // dataStoreManager.reset()

            // Chạy song song — không cần đợi nhau
            launch {
                try { seedRecipesUseCase() }
                catch (e: Exception) {
                    Log.e("SmartSousApp", "Seed lỗi: ${e.message}")
                }
            }

            launch {
                try { syncPantryUseCase() }  // ← thêm
                catch (e: Exception) {
                    Log.e("SmartSousApp", "Pantry sync lỗi: ${e.message}")
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}