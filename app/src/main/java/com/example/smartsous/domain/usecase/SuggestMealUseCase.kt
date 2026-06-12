package com.example.smartsous.domain.usecase

import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.RecipeIngredient
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.domain.model.UserPreference
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class SuggestMealsUseCase @Inject constructor() {

    companion object {
        private const val WEIGHT_INGREDIENT = 0.55f
        private const val WEIGHT_NUTRITION = 0.20f
        private const val WEIGHT_EXPIRY = 0.15f
        private const val WEIGHT_VARIETY = 0.10f
        private const val IDEAL_CALORIES_MIN = 200
        private const val IDEAL_CALORIES_MAX = 500
        private const val EXPIRING_SOON_DAYS = 3L
    }

    operator fun invoke(
        allRecipes: List<Recipe>,
        pantryIngredients: List<Ingredient>,
        recentlyCookedIds: List<String> = emptyList(),
        userPreference: UserPreference = UserPreference(),
        topN: Int = 10,
        currentDate: LocalDate = LocalDate.now(),
        mealType: MealType? = null,
        userDesiredServings: Int = 2
    ): List<SuggestedRecipe> {
        if (allRecipes.isEmpty()) return emptyList()

        val pantryItems = pantryIngredients
            .filter { it.quantity > 0.0 }
            .map { it.toPantryItem() }

        return allRecipes
            .filterNot { recipe -> violatesHardPreference(recipe, userPreference) }
            .map { recipe ->
                scoreRecipe(
                    recipe = recipe,
                    pantryItems = pantryItems,
                    recentlyCookedIds = recentlyCookedIds,
                    userPreference = userPreference,
                    currentDate = currentDate,
                    mealType = mealType,
                    userDesiredServings = userDesiredServings
                )
            }
            .filter { it.score > 10f }
            .sortedWith(
                compareByDescending<SuggestedRecipe> { it.score }
                    .thenByDescending { it.matchPercent }
                    .thenBy { it.recipe.cookingTimeMinutes }
            )
            .take(topN)
    }

    private fun scoreRecipe(
        recipe: Recipe,
        pantryItems: List<PantryItem>,
        recentlyCookedIds: List<String>,
        userPreference: UserPreference,
        currentDate: LocalDate,
        mealType: MealType?,
        userDesiredServings: Int
    ): SuggestedRecipe {
        val ingredientMatches = recipe.ingredients.map { recipeIngredient ->
            matchIngredient(recipeIngredient, pantryItems, currentDate, userDesiredServings, recipe.servings.coerceAtLeast(1))
        }

        val matched = ingredientMatches
            .filter { it.availabilityScore > 0f }
            .map { it.recipeIngredient.name }

        val missing = ingredientMatches
            .filter { it.availabilityScore < 65f }
            .map { it.recipeIngredient.name }

        val ingredientScore = if (ingredientMatches.isEmpty()) {
            0f
        } else {
            ingredientMatches.map { it.availabilityScore }.average().toFloat()
        }

        val matchPercent = ingredientScore.roundToInt().coerceIn(0, 100)
        val nutritionScore = nutritionScore(recipe, userPreference, mealType)
        val expiryScore = expiryScore(ingredientMatches)
        val varietyScore = if (recipe.id in recentlyCookedIds) 0f else 100f

        var totalScore = ingredientScore * WEIGHT_INGREDIENT +
            nutritionScore * WEIGHT_NUTRITION +
            expiryScore * WEIGHT_EXPIRY +
            varietyScore * WEIGHT_VARIETY

        totalScore += timeBonus(recipe, userPreference)

        if (matchesFavoriteCuisine(recipe, userPreference.favoriteCuisines)) {
            totalScore += 8f
        }
        if (recipe.isFavorite) {
            totalScore += 6f
        }
        if (userPreference.preferHighProtein && recipe.nutrition.protein >= 25.0) {
            totalScore += 8f
        }
        if (userPreference.preferLowFat && recipe.nutrition.fat <= 15.0) {
            totalScore += 8f
        }

        val reason = when {
            matchPercent >= 95 -> SuggestionReason.PERFECT_MATCH
            matchPercent >= 70 -> SuggestionReason.HIGH_MATCH
            expiryScore >= 40f -> SuggestionReason.USE_EXPIRING_SOON
            recipe.isFavorite -> SuggestionReason.FAVORITE_PICK
            recipe.cookingTimeMinutes <= userPreference.maxCookingTimeMinutes -> SuggestionReason.QUICK_COOK
            nutritionScore >= 90f -> SuggestionReason.HEALTHY_CHOICE
            else -> SuggestionReason.NOT_COOKED_RECENTLY
        }

        val context = when (reason) {
            SuggestionReason.PERFECT_MATCH -> "Tủ lạnh có đủ nguyên liệu để nấu món này cho $userDesiredServings người."
            SuggestionReason.HIGH_MATCH -> "Gần đủ nguyên liệu để nấu món này."
            SuggestionReason.USE_EXPIRING_SOON -> {
                val expiring = ingredientMatches.filter { it.expiryUrgency >= 40f }.maxByOrNull { it.expiryUrgency }
                val name = expiring?.pantryItem?.name ?: "nguyên liệu"
                "Giải cứu $name sắp hết hạn!"
            }
            SuggestionReason.FAVORITE_PICK -> "Món yêu thích của bạn."
            SuggestionReason.QUICK_COOK -> "Nấu siêu tốc trong ${recipe.cookingTimeMinutes} phút."
            SuggestionReason.HEALTHY_CHOICE -> "Cân bằng dinh dưỡng và phù hợp mức Kcal."
            SuggestionReason.NOT_COOKED_RECENTLY -> "Đổi vị vì món này bạn chưa nấu gần đây."
        }

        return SuggestedRecipe(
            recipe = recipe,
            score = totalScore.coerceIn(0f, 130f),
            matchedIngredients = matched,
            missingIngredients = missing,
            matchPercent = matchPercent,
            reason = reason,
            context = context
        )
    }

    private fun matchIngredient(
        recipeIngredient: RecipeIngredient,
        pantryItems: List<PantryItem>,
        currentDate: LocalDate,
        userDesiredServings: Int,
        recipeServings: Int
    ): IngredientMatch {
        val recipeName = normalizeIngredientName(recipeIngredient.name)
        val recipeAliases = ingredientAliases(recipeName)
        val candidates = pantryItems.filter { pantry ->
            ingredientsMatch(recipeName, recipeAliases, pantry)
        }

        if (candidates.isEmpty()) {
            return IngredientMatch(recipeIngredient, null, 0f, 0f)
        }

        val best = candidates.maxBy { pantry ->
            val quantityScore = quantityScore(recipeIngredient, pantry, userDesiredServings, recipeServings)
            val expiry = pantry.expiryDate?.let { expiryUrgency(it, currentDate) } ?: 0f
            quantityScore + expiry * 0.15f
        }

        val availability = quantityScore(recipeIngredient, best, userDesiredServings, recipeServings)
        val expiryUrgency = best.expiryDate?.let { expiryUrgency(it, currentDate) } ?: 0f
        return IngredientMatch(recipeIngredient, best, availability, expiryUrgency)
    }

    private fun quantityScore(
        recipeIngredient: RecipeIngredient,
        pantry: PantryItem,
        userDesiredServings: Int,
        recipeServings: Int
    ): Float {
        val recipeUnit = normalizeUnit(recipeIngredient.unit)
        val pantryUnit = normalizeUnit(pantry.unit)
        
        val requiredAmount = (recipeIngredient.amount / recipeServings) * userDesiredServings

        if (requiredAmount <= 0.0 || recipeUnit.isBlank() || pantryUnit.isBlank()) {
            return 85f
        }

        if (recipeUnit != pantryUnit) {
            return 75f
        }

        return when {
            pantry.quantity >= requiredAmount -> 100f
            pantry.quantity >= requiredAmount * 0.5 -> 65f
            else -> 40f
        }
    }

    private fun expiryScore(matches: List<IngredientMatch>): Float {
        val matched = matches.filter { it.availabilityScore > 0f }
        if (matched.isEmpty()) return 0f
        return matched.maxOf { it.expiryUrgency }
    }

    private fun expiryUrgency(expiryDate: LocalDate, currentDate: LocalDate): Float {
        val daysLeft = ChronoUnit.DAYS.between(currentDate, expiryDate)
        return when {
            daysLeft < 0 -> 100f
            daysLeft == 0L -> 100f
            daysLeft <= EXPIRING_SOON_DAYS -> 90f - (daysLeft - 1) * 20f
            daysLeft <= 7L -> 25f
            else -> 0f
        }
    }

    private fun timeBonus(recipe: Recipe, userPreference: UserPreference): Float =
        when {
            recipe.cookingTimeMinutes <= 20 -> 8f
            recipe.cookingTimeMinutes <= userPreference.maxCookingTimeMinutes -> 5f
            else -> -10f
        }

    private fun nutritionScore(
        recipe: Recipe,
        userPreference: UserPreference,
        mealType: MealType?
    ): Float {
        val calories = recipe.nutrition.calories
        val targetCalories = userPreference.targetCaloriesPerMeal.coerceAtLeast(1)
        val calorieDiff = abs(calories - targetCalories)

        if (calorieDiff <= 75) return 100f
        if (calorieDiff <= 150) return 75f

        val idealMin = when(mealType) {
            MealType.BREAKFAST, MealType.SNACK -> 250
            MealType.LUNCH, MealType.DINNER -> 500
            else -> IDEAL_CALORIES_MIN
        }
        val idealMax = when(mealType) {
            MealType.BREAKFAST, MealType.SNACK -> 450
            MealType.LUNCH, MealType.DINNER -> 800
            else -> IDEAL_CALORIES_MAX
        }

        return when {
            calories in idealMin..idealMax -> 65f
            calories <= idealMax + 300 -> 45f
            else -> 20f
        }
    }

    private fun violatesHardPreference(
        recipe: Recipe,
        userPreference: UserPreference
    ): Boolean {
        val ingredientNames = recipe.ingredients.map { normalizeIngredientName(it.name) }
        val blockedKeywords = (userPreference.allergies + userPreference.dislikedIngredients)
            .flatMap { preferenceKeywords(it) }

        if (blockedKeywords.any { keyword ->
                ingredientNames.any { ingredient -> ingredient.contains(keyword) }
            }
        ) {
            return true
        }

        if (!userPreference.vegetarian) return false

        return nonVegetarianKeywords.any { keyword ->
            ingredientNames.any { ingredient -> ingredient.contains(keyword) }
        }
    }

    private fun preferenceKeywords(value: String): List<String> {
        val normalized = normalizeIngredientName(value)
        return when (normalized) {
            "hai san" -> listOf(
                "hai san", "cua", "muc", "ngheu", "so",
                "seafood", "fish", "shrimp", "squid", "crab"
            )
            "milk" -> listOf("sua", "milk", "cheese", "butter", "cream", "yogurt")
            "dau phong", "lac" -> listOf("dau phong", "lac", "peanut")
            "egg" -> listOf("trung", "egg")
            "gluten" -> listOf("gluten", "bot mi", "banh mi", "mi", "wheat")
            else -> listOf(normalized)
        }
    }

    private fun matchesFavoriteCuisine(
        recipe: Recipe,
        favoriteCuisines: List<String>
    ): Boolean {
        if (favoriteCuisines.isEmpty()) return false

        val cuisine = normalizeText(recipe.cuisine)
        return favoriteCuisines.any { favorite ->
            cuisineKeywords(normalizeText(favorite)).any { cuisine.contains(it) }
        }
    }

    private fun ingredientsMatch(
        recipeName: String,
        recipeAliases: Set<String>,
        pantry: PantryItem
    ): Boolean {
        val pantryName = pantry.normalizedName
        if (recipeName == pantryName || recipeName.contains(pantryName) || pantryName.contains(recipeName)) {
            return true
        }

        return recipeAliases.any { it in pantry.aliases }
    }

    private fun ingredientAliases(name: String): Set<String> {
        val words = name.split(" ").filter { it.length >= 2 }
        val aliases = mutableSetOf(name)
        aliases += words

        ingredientSynonyms.forEach { group ->
            if (group.any { alias -> name.contains(alias) }) {
                aliases += group
            }
        }

        return aliases
    }

    private fun normalizeIngredientName(value: String): String =
        normalizeText(value)
            .replace(Regex("""\b(thit|rau|cu|qua|trai)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun normalizeUnit(unit: String): String =
        when (normalizeText(unit)) {
            "g", "gram", "grams" -> "g"
            "kg", "kilogram", "kilograms" -> "kg"
            "ml", "milliliter", "milliliters" -> "ml"
            "l", "lit", "litre", "liter", "liters" -> "l"
            "item", "cai", "qua", "trai", "cu", "unit", "don vi" -> "unit"
            "muong", "tbsp", "tablespoon" -> "tbsp"
            "thia", "tsp", "teaspoon" -> "tsp"
            else -> normalizeText(unit)
        }

    private fun normalizeText(value: String): String {
        val canonical = canonicalizeVietnameseFoodWords(value.lowercase().trim())
        val stripped = Normalizer.normalize(canonical, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("đ", "d")
            .replace("Đ", "d")

        return stripped
            .replace(Regex("""[^\p{Alnum}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun canonicalizeVietnameseFoodWords(value: String): String =
        vietnameseFoodCanonicalNames.fold(value) { current, (source, target) ->
            current.replace(source, target)
        }

    private fun Ingredient.toPantryItem(): PantryItem =
        normalizeIngredientName(name).let { normalizedName ->
        PantryItem(
            name = name,
            normalizedName = normalizedName,
            aliases = ingredientAliases(normalizedName),
            quantity = quantity,
            unit = unit,
            expiryDate = expiryDate
        )
        }

    private data class PantryItem(
        val name: String,
        val normalizedName: String,
        val aliases: Set<String>,
        val quantity: Double,
        val unit: String,
        val expiryDate: LocalDate?
    )

    private data class IngredientMatch(
        val recipeIngredient: RecipeIngredient,
        val pantryItem: PantryItem?,
        val availabilityScore: Float,
        val expiryUrgency: Float
    )

    private val ingredientSynonyms = listOf(
        setOf("beef"),
        setOf("heo", "lon", "thit heo", "thit lon", "pork"),
        setOf("ga", "thit ga", "chicken"),
        setOf("tom", "shrimp", "prawn"),
        setOf("fish"),
        setOf("muc", "squid"),
        setOf("trung", "egg"),
        setOf("tomato"),
        setOf("khoai tay", "potato"),
        setOf("ca rot", "carrot"),
        setOf("hanh tay", "onion"),
        setOf("toi", "garlic"),
        setOf("gung", "ginger"),
        setOf("nam", "mushroom"),
        setOf("dau hu", "dau phu", "tofu"),
        setOf("sua", "milk"),
        setOf("pho mai", "cheese"),
        setOf("butter")
    )

    private val nonVegetarianKeywords = listOf(
        "thit", "beef", "heo", "lon", "ga", "vit", "fish", "cua", "muc",
        "beef", "pork", "chicken", "duck", "fish", "shrimp", "crab", "squid"
    )

    private val vietnameseFoodCanonicalNames = listOf(
        "cà chua" to "tomato",
        "thịt bò" to "beef",
        "bò" to "beef",
        "bơ" to "butter",
        "thịt heo" to "pork",
        "thịt lợn" to "pork",
        "heo" to "pork",
        "lợn" to "pork",
        "thịt gà" to "chicken",
        "gà" to "chicken",
        "cá" to "fish",
        "tôm" to "shrimp",
        "mực" to "squid",
        "trứng" to "egg",
        "khoai tây" to "potato",
        "cà rốt" to "carrot",
        "hành tây" to "onion",
        "tỏi" to "garlic",
        "gừng" to "ginger",
        "nấm" to "mushroom",
        "đậu phụ" to "tofu",
        "đậu hũ" to "tofu",
        "sữa" to "milk",
        "phô mai" to "cheese"
    )

    private fun cuisineKeywords(value: String): List<String> =
        when (value) {
            "viet" -> listOf("viet", "vietnam", "vietnamese")
            "nhat" -> listOf("nhat", "japan", "japanese")
            "han" -> listOf("han", "korea", "korean")
            "y", "italy" -> listOf("y", "italy", "italian")
            "thai" -> listOf("thai")
            "au" -> listOf("au", "europe", "western")
            else -> listOf(value)
        }
}
