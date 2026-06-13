package com.example.smartsous.core.vision

import android.graphics.RectF
import com.example.smartsous.domain.model.IngredientCategory

data class DetectedIngredient(
    val rawLabel: String,
    val displayName: String,
    val category: IngredientCategory,
    val confidence: Float,
    val boundingBox: RectF
)
