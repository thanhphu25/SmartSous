package com.example.smartsous.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object GeminiClient {
    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // Timeout dài vì streaming — response không kết thúc ngay
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}