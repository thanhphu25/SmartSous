package com.example.smartsous.domain.usecase

import java.text.Normalizer
import javax.inject.Inject

sealed class ChatIntent {
    data class SuggestFromIngredients(val ingredients: List<String>) : ChatIntent()
    data class SearchRecipe(val query: String) : ChatIntent()
    data class AskRecipeDetail(val query: String) : ChatIntent()
    object UseMyPantry : ChatIntent()
    object ExpiringFood : ChatIntent()
    object ShoppingAdvice : ChatIntent()
    object NutritionAdvice : ChatIntent()
    object GeneralCookingQuestion : ChatIntent()
    object OutOfDomain : ChatIntent()
}

class IntentDetector @Inject constructor() {

    fun detect(message: String): ChatIntent {
        val normalized = message.normalizeForIntent()

        if (normalized.isBlank()) return ChatIntent.GeneralCookingQuestion

        if (isOutOfDomain(normalized)) {
            return ChatIntent.OutOfDomain
        }

        if (expiringKeywords.any { normalized.contains(it) }) {
            return ChatIntent.ExpiringFood
        }

        if (shoppingKeywords.any { normalized.contains(it) }) {
            return ChatIntent.ShoppingAdvice
        }

        if (nutritionKeywords.any { normalized.contains(it) }) {
            return ChatIntent.NutritionAdvice
        }

        if (pantryKeywords.any { normalized.contains(it) } && !hasIngredientSeparator(normalized)) {
            return ChatIntent.UseMyPantry
        }

        suggestPatterns.forEach { pattern ->
            val match = pattern.find(normalized)
            if (match != null) {
                val ingredientsText = match.groupValues.lastOrNull { it.isNotBlank() }.orEmpty()
                val ingredients = extractIngredients(ingredientsText)
                if (ingredients.isNotEmpty()) {
                    return ChatIntent.SuggestFromIngredients(ingredients)
                }
            }
        }

        if (hasIngredientSeparator(normalized)) {
            val ingredients = extractIngredients(normalized)
            if (ingredients.size >= 2) {
                return ChatIntent.SuggestFromIngredients(ingredients)
            }
        }

        if (recipeDetailKeywords.any { normalized.contains(it) }) {
            return ChatIntent.AskRecipeDetail(normalized)
        }

        if (searchKeywords.any { normalized.contains(it) }) {
            return ChatIntent.SearchRecipe(normalized)
        }

        return if (cookingDomainKeywords.any { normalized.contains(it) }) {
            ChatIntent.GeneralCookingQuestion
        } else {
            ChatIntent.OutOfDomain
        }
    }

    fun extractIngredients(text: String): List<String> {
        val normalized = text.normalizeForIntent()
        val bySeparator = normalized
            .split(Regex("""[,/+&]|\s+va\s+"""))
            .map { cleanIngredientName(it) }
            .filter { it.length >= 2 && it !in stopWords }

        if (bySeparator.size >= 2) return bySeparator

        val matched = knownIngredients.filter { keyword ->
            normalized.contains(keyword)
        }
        if (matched.isNotEmpty()) return matched

        return bySeparator
    }

    private fun isOutOfDomain(message: String): Boolean {
        if (cookingDomainKeywords.any { message.contains(it) }) return false
        return outOfDomainKeywords.any { message.contains(it) }
    }

    private fun hasIngredientSeparator(text: String): Boolean =
        text.contains(',') || text.contains('+') || text.contains('/') ||
            text.contains('&') || text.contains(" va ")

    private fun cleanIngredientName(name: String): String =
        name
            .replace(Regex("""^\d+\s*"""), "")
            .replace(Regex("""\b(gram|kg|ml|lit|litre|liter|g|goi|qua|cai|cu)\b"""), "")
            .replace(Regex("""\b(nau|lam|mon|an|voi|tu|bang|co|gi|cua|toi|them)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun String.normalizeForIntent(): String =
        Normalizer.normalize(lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("đ", "d")
            .replace(Regex("""[^\p{Alnum}\s,+/&]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private val suggestPatterns = listOf(
        Regex("""nau\s+gi\s+(tu|voi|bang)\s+(.+)"""),
        Regex("""(tu|voi|co)\s+(.+)\s+nau\s+gi"""),
        Regex("""(lam|che bien)\s+gi\s+(tu|voi)\s+(.+)"""),
        Regex("""mon\s+gi\s+(tu|voi)\s+(.+)""")
    )

    private val pantryKeywords = listOf(
        "tu lanh", "dang co", "hien co", "con lai", "nguyen lieu cua toi",
        "nau gi hom nay", "an gi hom nay", "goi y mon", "do toi co", "trong nha"
    )

    private val expiringKeywords = listOf(
        "sap het han", "het han", "can dung truoc", "do nao sap hong", "do nao nen nau truoc"
    )

    private val shoppingKeywords = listOf(
        "can mua", "mua them", "di cho", "shopping", "thieu gi", "nguyen lieu can them"
    )

    private val nutritionKeywords = listOf(
        "it calo", "calo", "calories", "giam can", "it beo", "healthy",
        "lanh manh", "dinh duong", "protein", "vitamin", "an kieng", "low carb"
    )

    private val recipeDetailKeywords = listOf(
        "cach nau", "huong dan", "cong thuc", "lam mon", "nau mon", "cac buoc"
    )

    private val searchKeywords = listOf(
        "tim mon", "co mon", "mon nao", "mon gi"
    )

    private val cookingDomainKeywords = listOf(
        "nau", "mon", "an", "nguyen lieu", "thuc pham", "tu lanh", "bua",
        "cong thuc", "dinh duong", "calo", "protein", "rau", "thit", "ca",
        "tom", "trung", "gao", "bun", "pho", "salad", "soup", "sot", "chien",
        "xao", "luoc", "hap", "nuong", "kho", "ham", "bao quan", "het han"
    )

    private val outOfDomainKeywords = listOf(
        "viet nam co bao nhieu", "tinh giap bien", "lap trinh", "code", "toan",
        "lich su", "dia ly", "chinh tri", "bong da", "thoi tiet", "gia vang",
        "crypto", "bitcoin", "co phieu", "phim", "game", "du lich"
    )

    private val knownIngredients = listOf(
        "ca chua", "beef", "thit bo", "pork", "thit heo", "thit lon", "chicken", "thit ga",
        "fish", "ca", "shrimp", "tom", "egg", "trung", "tofu", "dau phu", "dau hu",
        "rau muong", "bi do", "hanh tay", "toi", "gung", "ot", "ca rot",
        "khoai tay", "bap cai", "gia do", "nam", "muc", "ngao", "cua",
        "gao", "bun", "mi", "sua", "pho mai"
    )

    private val stopWords = setOf(
        "gi", "va", "voi", "tu", "co", "cua", "toi", "nau", "lam", "che",
        "bien", "mon", "an", "them", "bo", "mot", "hai", "ba", "vai", "nhieu", "it"
    )
}
