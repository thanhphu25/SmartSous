package com.example.smartsous.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodLookupService @Inject constructor() {

    private val client = OkHttpClient()
    private val TAG = "FoodLookupService"

    // Tra tên sản phẩm từ barcode
    // Dùng Open Food Facts API — miễn phí, không cần key
    suspend fun lookupProductName(barcode: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "SmartSous-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext barcode

            val body = response.body?.string() ?: return@withContext barcode
            val json = JSONObject(body)

            // Status 1 = tìm thấy sản phẩm
            if (json.optInt("status") != 1) return@withContext barcode

            val product = json.optJSONObject("product") ?: return@withContext barcode

            // Thử lấy tên theo thứ tự ưu tiên
            val productName = product.optString("product_name_vi")     // tiếng Việt
                .ifBlank { product.optString("product_name") }          // tên gốc
                .ifBlank { product.optString("generic_name") }          // tên chung
                .ifBlank { barcode }                                     // fallback = barcode

            Log.d(TAG, "Tìm thấy: $productName cho barcode $barcode")
            productName

        } catch (e: Exception) {
            Log.e(TAG, "Lookup lỗi: ${e.message}")
            barcode  // fallback: dùng barcode làm tên
        }
    }
}