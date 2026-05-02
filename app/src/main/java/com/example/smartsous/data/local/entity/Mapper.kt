package com.example.smartsous.data.local.entity

import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.Nutrition
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// ── RecipeEntity ↔ Recipe ─────────────────────────────────

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = Difficulty.valueOf(difficulty),
    ingredients = parseIngredientsJson(ingredientsJson),
    steps = parseStringListJson(stepsJson),
    nutrition = parseNutritionJson(nutritionJson),
    tags = parseStringListJson(tagsJson),
    cuisine = cuisine,
    isFavorite = isFavorite
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = difficulty.name,
    ingredientsJson = ingredientsToJson(ingredients),
    stepsJson = listToJson(steps),
    nutritionJson = nutritionToJson(nutrition),
    tagsJson = listToJson(tags),
    cuisine = cuisine,
    isFavorite = isFavorite,
    cachedAt = System.currentTimeMillis()
)

// ── IngredientEntity ↔ Ingredient ────────────────────────

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = IngredientCategory.valueOf(category),
    expiryDate = expiryDate?.let { LocalDate.parse(it) },
    addedDate = LocalDate.parse(addedDate)
)

fun Ingredient.toEntity(): IngredientEntity = IngredientEntity(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category.name,
    expiryDate = expiryDate?.toString(),
    addedDate = addedDate.toString()
)

// ── JSON helpers ──────────────────────────────────────────

private fun parseIngredientsJson(json: String): List<RecipeIngredient> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RecipeIngredient(
                name   = obj.getString("name"),
                amount = obj.getDouble("amount"),
                unit   = obj.getString("unit")
            )
        }
    } catch (e: Exception) { emptyList() }
}

private fun parseNutritionJson(json: String): Nutrition {
    return try {
        val obj = JSONObject(json)
        Nutrition(
            calories = obj.getInt("calories"),
            protein  = obj.getDouble("protein"),
            carbs    = obj.getDouble("carbs"),
            fat      = obj.getDouble("fat"),
            fiber    = obj.getDouble("fiber")
        )
    } catch (e: Exception) {
        Nutrition(0, 0.0, 0.0, 0.0, 0.0)
    }
}

private fun parseStringListJson(json: String): List<String> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) { emptyList() }
}

private fun ingredientsToJson(list: List<RecipeIngredient>): String {
    val arr = JSONArray()
    list.forEach { ing ->
        arr.put(JSONObject().apply {
            put("name",   ing.name)
            put("amount", ing.amount)
            put("unit",   ing.unit)
        })
    }
    return arr.toString()
}

private fun nutritionToJson(n: Nutrition): String =
    JSONObject().apply {
        put("calories", n.calories)
        put("protein",  n.protein)
        put("carbs",    n.carbs)
        put("fat",      n.fat)
        put("fiber",    n.fiber)
    }.toString()

private fun listToJson(list: List<String>): String {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
}