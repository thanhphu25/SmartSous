package com.example.smartsous.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartsous.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY createdAt DESC LIMIT 80")
    fun observeLatest(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE readAt IS NULL")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsert(notification: AppNotificationEntity)

    @Query("UPDATE app_notifications SET readAt = :readAt WHERE id = :id AND readAt IS NULL")
    suspend fun markAsRead(id: String, readAt: Long)
}
