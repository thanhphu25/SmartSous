package com.example.smartsous.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryNotificationPolicyTest {

    @Test
    fun shouldNotify_onlyForTodayTomorrowAndThreeDaysBeforeExpiry() {
        assertTrue(ExpiryNotificationPolicy.shouldNotify(0))
        assertTrue(ExpiryNotificationPolicy.shouldNotify(1))
        assertTrue(ExpiryNotificationPolicy.shouldNotify(3))

        assertFalse(ExpiryNotificationPolicy.shouldNotify(-1))
        assertFalse(ExpiryNotificationPolicy.shouldNotify(2))
        assertFalse(ExpiryNotificationPolicy.shouldNotify(4))
    }

    @Test
    fun notificationId_isStableForSameIngredientAndDay() {
        val first = ExpiryNotificationPolicy.notificationId("milk-id", 1)
        val second = ExpiryNotificationPolicy.notificationId("milk-id", 1)

        assertEquals(first, second)
    }

    @Test
    fun notificationId_changesForDifferentWarningMilestones() {
        val tomorrow = ExpiryNotificationPolicy.notificationId("milk-id", 1)
        val today = ExpiryNotificationPolicy.notificationId("milk-id", 0)

        assertNotEquals(tomorrow, today)
    }
}
