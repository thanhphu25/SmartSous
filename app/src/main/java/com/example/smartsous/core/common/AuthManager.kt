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
    // User hiện tại — null nếu chưa đăng nhập
    val currentUser: FirebaseUser? get() = auth.currentUser

    // UID dùng để xây path Firestore: /users/{uid}/pantry/...
    // Throw exception nếu chưa login — bắt buộc loginAnonymously() trước
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