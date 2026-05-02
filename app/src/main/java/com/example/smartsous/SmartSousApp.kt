package com.example.smartsous

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.smartsous.core.common.AuthManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartSousApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var authManager: AuthManager  // ← thêm dòng này

    override fun onCreate() {
        super.onCreate()
        // Đăng nhập ẩn danh ngay khi app khởi động
        // Chạy trên IO thread, không block UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authManager.loginAnonymously()
            } catch (e: Exception) {
                // Sẽ retry lần sau khi user mở lại app
                android.util.Log.e("SmartSousApp", "Auth lỗi: ${e.message}")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}