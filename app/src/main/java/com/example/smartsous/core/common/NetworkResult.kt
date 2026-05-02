package com.example.smartsous.core.common

// Dùng trong Repository khi gọi Firebase hoặc API
// VD: suspend fun fetchRecipes(): NetworkResult<List<RecipeEntity>>
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,          // HTTP code hoặc Firebase error code
        val cause: Throwable? = null
    ) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

// Chuyển NetworkResult (Data layer) sang Resource (Presentation layer)
fun <T> NetworkResult<T>.toResource(): Resource<T> = when (this) {
    is NetworkResult.Success -> Resource.Success(data)
    is NetworkResult.Error   -> Resource.Error(message, cause)
    is NetworkResult.Loading -> Resource.Loading
}

// Bọc 1 suspend call Firebase trong try/catch tự động
// Dùng: safeFirebaseCall { firestore.collection("recipes").get().await() }
suspend fun <T> safeFirebaseCall(call: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
    } catch (e: Exception) {
        NetworkResult.Error(
            message = e.message ?: "Lỗi Firebase không xác định",
            cause = e
        )
    }
}