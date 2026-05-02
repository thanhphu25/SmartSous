package com.example.smartsous.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel base mà mọi ViewModel kế thừa
abstract class BaseViewModel : ViewModel() {

    // Global error state mọi screen đều có thể dùng
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _error.value = throwable.message ?: "Đã có lỗi xảy ra"
    }

    // Launch coroutine an toàn — tự catch exception
    protected fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) { block() }
    }

    open fun clearError() { _error.value = null }
}