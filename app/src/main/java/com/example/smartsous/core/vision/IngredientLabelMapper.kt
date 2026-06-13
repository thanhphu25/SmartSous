package com.example.smartsous.core.vision

import com.example.smartsous.domain.model.IngredientCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientLabelMapper @Inject constructor() {
    private val labels = mapOf(
        "almonds" to ("Hạnh nhân" to IngredientCategory.OTHER),
        "banana" to ("Chuối" to IngredientCategory.FRUIT),
        "black_chickpeas" to ("Đậu gà đen" to IngredientCategory.GRAIN),
        "capsicum" to ("Ớt chuông" to IngredientCategory.VEGETABLE),
        "carrot" to ("Cà rốt" to IngredientCategory.VEGETABLE),
        "cashew" to ("Hạt điều" to IngredientCategory.OTHER),
        "cow_peas" to ("Đậu mắt đen" to IngredientCategory.GRAIN),
        "dates" to ("Chà là" to IngredientCategory.FRUIT),
        "dried_figs" to ("Sung khô" to IngredientCategory.FRUIT),
        "finger_millet_flour" to ("Bột kê ngón tay" to IngredientCategory.GRAIN),
        "flattened_rice" to ("Gạo dẹt" to IngredientCategory.GRAIN),
        "gram_flour" to ("Bột đậu gà" to IngredientCategory.GRAIN),
        "green_gram" to ("Đậu xanh nguyên hạt" to IngredientCategory.GRAIN),
        "green_peas" to ("Đậu Hà Lan" to IngredientCategory.VEGETABLE),
        "jaggery" to ("Đường thốt nốt" to IngredientCategory.SPICE),
        "kidney_beans" to ("Đậu thận" to IngredientCategory.GRAIN),
        "lemon" to ("Chanh vàng" to IngredientCategory.FRUIT),
        "oats" to ("Yến mạch" to IngredientCategory.GRAIN),
        "onion" to ("Hành tây" to IngredientCategory.VEGETABLE),
        "peanuts" to ("Đậu phộng" to IngredientCategory.OTHER),
        "pigeon_peas" to ("Đậu triều" to IngredientCategory.GRAIN),
        "potato" to ("Khoai tây" to IngredientCategory.VEGETABLE),
        "rice_flour" to ("Bột gạo" to IngredientCategory.GRAIN),
        "rice" to ("Gạo" to IngredientCategory.GRAIN),
        "semolina" to ("Bột semolina" to IngredientCategory.GRAIN),
        "skinned_black_gram" to ("Đậu urad tách vỏ" to IngredientCategory.GRAIN),
        "skinned_green_gram" to ("Đậu xanh tách vỏ" to IngredientCategory.GRAIN),
        "split_black_gram" to ("Đậu urad tách đôi" to IngredientCategory.GRAIN),
        "sugar" to ("Đường" to IngredientCategory.SPICE),
        "tamarind" to ("Me" to IngredientCategory.SPICE),
        "tomato" to ("Cà chua" to IngredientCategory.VEGETABLE),
        "wheat_flour" to ("Bột mì" to IngredientCategory.GRAIN),
        "white_chickpeas" to ("Đậu gà trắng" to IngredientCategory.GRAIN)
    )

    fun displayName(rawLabel: String): String =
        labels[normalize(rawLabel)]?.first ?: humanize(rawLabel)

    fun category(rawLabel: String): IngredientCategory =
        labels[normalize(rawLabel)]?.second ?: inferCategory(rawLabel)

    private fun inferCategory(rawLabel: String): IngredientCategory {
        val label = normalize(rawLabel)
        return when {
            listOf("bean", "peas", "gram", "rice", "flour", "oats", "semolina", "millet")
                .any { it in label } -> IngredientCategory.GRAIN
            listOf("banana", "dates", "fig", "lemon").any { it in label } -> IngredientCategory.FRUIT
            listOf("tomato", "carrot", "potato", "onion", "capsicum").any { it in label } -> IngredientCategory.VEGETABLE
            listOf("sugar", "jaggery", "tamarind").any { it in label } -> IngredientCategory.SPICE
            else -> IngredientCategory.OTHER
        }
    }

    private fun humanize(label: String): String =
        label.replace('_', ' ')
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }

    private fun normalize(label: String): String =
        label.trim().lowercase().replace(" ", "_")
}
