package com.example.smartsous.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_notifications",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["readAt"])
    ]
)
data class AppNotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val route: String,
    val referenceId: String?,
    val createdAt: Long,
    val readAt: Long? = null
)
