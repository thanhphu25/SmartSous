package com.example.smartsous.core.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.smartsous.ui.theme.Gray800

data class NutritionData(
    val label: String,
    val value: Float,
    val unit: String,
    val color: Color
)

@Composable
fun NutritionChart(
    data: List<NutritionData>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val maxValue = data.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp) // Tăng chiều cao để đủ chỗ cho nhãn ở dưới
            .padding(16.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val barCount = data.size
        val barSpacing = 40f
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (canvasWidth - totalSpacing) / barCount

        // Paint cho giá trị (trên đỉnh cột)
        val valuePaint = Paint().apply {
            color = Gray800.toArgb()
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Paint cho nhãn (dưới chân cột)
        val labelPaint = Paint().apply {
            color = Gray800.toArgb()
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        data.forEachIndexed { index, item ->
            // Trừ hao 40f cho nhãn ở dưới và 40f cho giá trị ở trên
            val availableHeight = canvasHeight - 80f
            val targetBarHeight = (item.value / maxValue) * availableHeight
            val currentBarHeight = targetBarHeight * animationProgress.value
            
            val xOffset = index * (barWidth + barSpacing)
            val yOffset = canvasHeight - currentBarHeight - 40f // Cách đáy 40f để vẽ nhãn

            // Vẽ cột
            drawRoundRect(
                color = item.color,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Vẽ giá trị trên đỉnh cột
            drawContext.canvas.nativeCanvas.drawText(
                "${item.value.toInt()} ${item.unit}",
                xOffset + (barWidth / 2),
                yOffset - 15f,
                valuePaint
            )

            // Vẽ nhãn dưới chân cột
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                xOffset + (barWidth / 2),
                canvasHeight - 5f,
                labelPaint
            )
        }
    }
}
