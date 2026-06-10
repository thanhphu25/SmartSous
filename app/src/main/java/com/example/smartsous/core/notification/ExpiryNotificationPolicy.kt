package com.example.smartsous.core.notification

object ExpiryNotificationPolicy {
    const val WARNING_WINDOW_DAYS = 3L

    private val NOTIFICATION_DAYS = setOf(0, 1, 3)

    fun shouldNotify(daysLeft: Int): Boolean =
        daysLeft in NOTIFICATION_DAYS

    fun notificationId(ingredientId: String, daysLeft: Int): Int =
        "expiry:$ingredientId:$daysLeft".hashCode() and Int.MAX_VALUE
}
