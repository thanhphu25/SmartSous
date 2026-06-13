package com.example.smartsous.domain.model

data class UserPreference(
    // Basic Indicators
    val age: Int = 25,
    val gender: String = "Nam", // "Nam", "Nữ", "Khác"
    val weightKg: Float = 65f,
    val heightCm: Float = 170f,
    val activityLevel: String = "Vừa", // "Ít", "Vừa", "Mạnh"
    
    // Health & Diet
    val healthGoal: String = "Duy trì sức khỏe", // "Giảm cân", "Tăng cơ", "Duy trì sức khỏe", "Tiểu đường", "Cao huyết áp"
    val dietaryType: String = "Không có yêu cầu", // "Ăn chay", "Keto", "Low-carb", "Không có yêu cầu"
    
    val targetCaloriesPerMeal: Int = 400,
    val favoriteCuisines: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val preferLowFat: Boolean = false,
    val preferHighProtein: Boolean = false,
    val vegetarian: Boolean = false,
    val maxCookingTimeMinutes: Int = 60,
    val aiApiKey: String = "",
    val aiModel: String = "llama-3.3-70b-versatile"
) {
    val bmi: Float 
        get() {
            if (heightCm <= 0) return 0f
            val heightM = heightCm / 100f
            return weightKg / (heightM * heightM)
        }

    fun calculateTdee(): Int {
        val s = if (gender == "Nam") 5 else if (gender == "Nữ") -161 else -80
        val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + s
        val factor = when (activityLevel) {
            "Ít" -> 1.2f
            "Vừa" -> 1.55f
            "Mạnh" -> 1.9f
            else -> 1.375f
        }
        return (bmr * factor).toInt()
    }
}
