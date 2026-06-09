package com.example.smartsous

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.smartsous.core.common.FirebaseStorageFetcher
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
class SmartSousApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var seedRecipesUseCase: SeedRecipesUseCase

    //@Inject lateinit var dataStoreManager: DataStoreManager
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .components {
                add(FirebaseStorageFetcher.Factory())
            }
            .crossfade(true)
            .build()
    }
}