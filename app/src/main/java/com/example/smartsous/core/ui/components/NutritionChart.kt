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
    // Hiệu ứng "mọc" cột lên khi mở màn hình
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
            .height(200.dp)
            .padding(16.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val barCount = data.size
        val barSpacing = 40f
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (canvasWidth - totalSpacing) / barCount

        val textPaint = Paint().apply {
            color = Gray800.toArgb()
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        data.forEachIndexed { index, item ->
            // Tính toán chiều cao của cột dựa trên tỷ lệ và animation
            val targetBarHeight = (item.value / maxValue) * (canvasHeight - 60f) // Trừ hao không gian cho text
            val currentBarHeight = targetBarHeight * animationProgress.value
            
            val xOffset = index * (barWidth + barSpacing)
            val yOffset = canvasHeight - currentBarHeight

            // Vẽ cột
            drawRoundRect(
                color = item.color,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Vẽ Text hiển thị giá trị (vd: "450 kcal") bằng nativeCanvas
            drawContext.canvas.nativeCanvas.drawText(
                "${item.value.toInt()} ${item.unit}",
                xOffset + (barWidth / 2),
                yOffset - 20f, // Cách đỉnh cột một khoảng
                textPaint
            )
        }
    }
}