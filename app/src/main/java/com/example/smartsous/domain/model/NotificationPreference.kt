package com.example.smartsous.domain.model

data class NotificationPreference(
    val expiryRemindersEnabled: Boolean = true,
    val mealRemindersEnabled: Boolean = true,
    val mealReminderHour: Int = 7,
    val mealReminderMinute: Int = 30
)
