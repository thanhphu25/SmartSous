package com.example.smartsous.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartsous.data.local.dao.AppNotificationDao
import com.example.smartsous.data.local.entity.AppNotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeNotificationViewModel @Inject constructor(
    private val appNotificationDao: AppNotificationDao
) : ViewModel() {
    val notifications: StateFlow<List<AppNotificationEntity>> =
        appNotificationDao.observeLatest()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> =
        appNotificationDao.observeUnreadCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markAsRead(id: String) {
        viewModelScope.launch {
            appNotificationDao.markAsRead(id, System.currentTimeMillis())
        }
    }
}
