package com.example.smartsous.core.notification

data class NotificationMessage(
    val title: String,
    val body: String
)

object NotificationMessageFactory {
    fun expiry(ingredientName: String, daysLeft: Int): NotificationMessage =
        when (daysLeft) {
            0 -> NotificationMessage(
                title = "Hết hạn hôm nay",
                body = "$ingredientName hết hạn hôm nay, hãy dùng ngay nhé."
            )
            1 -> NotificationMessage(
                title = "Sắp hết hạn ngày mai",
                body = "$ingredientName còn 1 ngày, lên kế hoạch nấu ngay."
            )
            else -> NotificationMessage(
                title = "Sắp hết hạn trong $daysLeft ngày",
                body = "$ingredientName còn $daysLeft ngày, đừng để lãng phí."
            )
        }

    fun mealPlan(mealSummary: String): NotificationMessage =
        NotificationMessage(
            title = "Hôm nay nấu gì?",
            body = mealSummary
        )
}
