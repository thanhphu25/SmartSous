package com.example.smartsous.domain.model

data class SearchFilter(
    val query: String = "",                         // text tìm kiếm
    val selectedCuisines: Set<String> = emptySet(), // "Việt Nam", "Nhật"...
    val selectedDifficulty: Set<Difficulty> = emptySet(),
    val maxCookingTime: Int? = null,                // null = không giới hạn
    val maxCalories: Int? = null,
    val onlyFavorites: Boolean = false
) {
    // Không có filter nào được chọn
    val isEmpty: Boolean get() = query.isEmpty()
            && selectedCuisines.isEmpty()
            && selectedDifficulty.isEmpty()
            && maxCookingTime == null
            && maxCalories == null
            && !onlyFavorites
}

// Các option cố định cho filter UI
object FilterOptions {
    val cuisines = listOf(
        "Việt Nam", "Nhật Bản", "Hàn Quốc",
        "Trung Quốc", "Ý", "Thái Lan"
    )
    val cookingTimes = listOf(
        15 to "Dưới 15 phút",
        30 to "Dưới 30 phút",
        60 to "Dưới 1 tiếng"
    )
    val calorieOptions = listOf(
        300 to "Dưới 300 kcal",
        500 to "Dưới 500 kcal",
        800 to "Dưới 800 kcal"
    )
}