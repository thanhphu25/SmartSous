package com.example.smartsous.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartsous.core.common.Radius
import com.example.smartsous.core.common.Spacing

// Hàm tạo brush shimmer — animate từ trái sang phải
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.25f),
        Color.LightGray.copy(alpha = 0.55f),
        Color.LightGray.copy(alpha = 0.25f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

// Box shimmer cơ bản — dùng để vẽ bất kỳ hình chữ nhật nào
// VD: ShimmerBox(width = 100.dp, height = 14.dp) → giả text
//     ShimmerBox(width = 80.dp, height = 80.dp, radius = 40.dp) → giả avatar tròn
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = 14.dp,
    radius: Dp = Radius.sm,
) {
    val sizeModifier = if (width != Dp.Unspecified)
        modifier.width(width).height(height)
    else
        modifier.fillMaxWidth().height(height)

    Box(
        modifier = sizeModifier
            .clip(RoundedCornerShape(radius))
            .background(shimmerBrush())
    )
}

// Skeleton của 1 RecipeCard — dùng khi đang tải danh sách món
// Hiện đúng hình dạng card thật nhưng tất cả là shimmer
@Composable
fun RecipeCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md)
    ) {
        // Phần ảnh
        ShimmerBox(height = 180.dp, radius = Radius.lg)
        Spacer(Modifier.height(Spacing.sm))
        // Tên món
        ShimmerBox(height = 18.dp)
        Spacer(Modifier.height(Spacing.xs))
        // Mô tả
        ShimmerBox(height = 14.dp)
        Spacer(Modifier.height(Spacing.xs))
        ShimmerBox(width = 180.dp, height = 14.dp)
        Spacer(Modifier.height(Spacing.sm))
        // Metadata row: thời gian + calories
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ShimmerBox(width = 70.dp, height = 26.dp, radius = Radius.full)
            ShimmerBox(width = 70.dp, height = 26.dp, radius = Radius.full)
            ShimmerBox(width = 50.dp, height = 26.dp, radius = Radius.full)
        }
    }
}

// Skeleton của 1 dòng nguyên liệu trong PantryScreen
@Composable
fun IngredientRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Icon category
        ShimmerBox(modifier = Modifier.size(44.dp), height = 44.dp, radius = Radius.md)
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(height = 16.dp)
            Spacer(Modifier.height(Spacing.xs))
            ShimmerBox(width = 100.dp, height = 13.dp)
        }
        // Badge hết hạn
        ShimmerBox(width = 60.dp, height = 26.dp, radius = Radius.full)
    }
}

// Danh sách 5 skeleton card — dùng ngay khi màn hình load
// VD: if (uiState.isLoading) RecipeListSkeleton()
@Composable
fun RecipeListSkeleton() {
    Column {
        repeat(5) {
            RecipeCardSkeleton()
            Spacer(Modifier.height(Spacing.xs))
        }
    }
}