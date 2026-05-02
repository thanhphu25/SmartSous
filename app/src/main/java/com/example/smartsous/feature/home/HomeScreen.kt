package com.example.smartsous.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.smartsous.core.common.Spacing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// !! File test tạm thời — xoá sau khi confirm Firebase connect !!
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Chưa test") }

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Firebase Status: $status",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = {
            scope.launch {
                status = "Đang kết nối..."
                try {
                    // Thử đọc collection recipes
                    val result = FirebaseFirestore.getInstance()
                        .collection("recipes")
                        .limit(1)
                        .get()
                        .await()
                    status = "✅ Kết nối thành công! (${result.size()} doc)"
                } catch (e: Exception) {
                    status = "❌ Lỗi: ${e.message}"
                }
            }
        }) {
            Text("Test Firebase")
        }
    }
}