package com.example.smartsous.core.common

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    // Firestore path:/users/{uid}/pantry/...
    // Firebase Auth loginAnonymously()
    val uid: String get() = auth.currentUser?.uid
        ?: error("User chưa đăng nhập — gọi loginAnonymously() trước")

    // Đăng nhập ẩn danh — gọi 1 lần khi app khởi động
    // Nếu đã có session cũ thì Firebase tự dùng lại, không tạo user mới
    suspend fun loginAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null
}