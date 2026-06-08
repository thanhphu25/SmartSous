package com.example.smartsous.data.remote

import com.example.smartsous.domain.model.MealPlan
import com.example.smartsous.domain.model.MealType
import java.time.LocalDate

data class MealPlanDto(
    val date: String = "",
    val meals: Map<String, List<String>> = emptyMap()
)

fun MealPlan.toDto(): MealPlanDto {
    val mealsMap = meals.mapKeys { it.key.name }
    return MealPlanDto(
        date = date.toString(),
        meals = mealsMap
    )
}

fun MealPlanDto.toDomain(): MealPlan {
    val mealsMap = meals.mapKeys { MealType.valueOf(it.key) }
    return MealPlan(
        id = date,
        date = LocalDate.parse(date),
        meals = mealsMap
    )
}
