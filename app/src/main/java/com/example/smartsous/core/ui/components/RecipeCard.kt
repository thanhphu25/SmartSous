package com.example.smartsous.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.smartsous.core.common.Radius
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Teal400

// Dùng trong HomeScreen, SearchScreen, FavoritesScreen:
// RecipeCard(
//     recipe = recipe,
//     onClick = { navController.navigate("recipe/${recipe.id}") },
//     onFavoriteClick = { viewModel.toggleFavorite(recipe.id) }
// )
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(Radius.lg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // ── Ảnh món ăn ─────────────────────────────────
            Box {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(
                            topStart = Radius.lg,
                            topEnd = Radius.lg
                        ))
                )
                // Nút yêu thích góc phải
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .background(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(Radius.full)
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite)
                            Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (recipe.isFavorite) Coral400 else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Badge độ khó góc trái
                DifficultyBadge(
                    difficulty = recipe.difficulty,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm)
                )
            }

            // ── Thông tin bên dưới ảnh ───────────────────
            Column(modifier = Modifier.padding(Spacing.md)) {
                // Tên món
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xs))
                // Mô tả ngắn
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.sm))
                // Row metadata: thời gian + calories + ẩm thực
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaChip(
                        icon = { Icon(Icons.Default.Schedule, null,
                            Modifier.size(12.dp)) },
                        label = "${recipe.cookingTimeMinutes} phút"
                    )
                    MetaChip(
                        icon = { Icon(Icons.Default.LocalFireDepartment, null,
                            Modifier.size(12.dp), tint = Amber400) },
                        label = "${recipe.nutrition.calories} kcal"
                    )
                    // Tag ẩm thực (Việt, Nhật, ...)
                    if (recipe.cuisine.isNotEmpty()) {
                        MetaChip(label = recipe.cuisine)
                    }
                }
            }
        }
    }
}

// Badge EASY / MEDIUM / HARD hiện trên ảnh
@Composable
private fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (difficulty) {
        Difficulty.EASY   -> "Dễ" to Teal400
        Difficulty.MEDIUM -> "Vừa" to Amber400
        Difficulty.HARD   -> "Khó" to Coral400
    }
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(Radius.sm)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

// Chip nhỏ hiện metadata (thời gian, calories)
@Composable
private fun MetaChip(
    label: String,
    icon: (@Composable () -> Unit)? = null,
) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = icon,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// Dùng trong SearchScreen, kết quả tìm kiếm dạng nằm ngang
@Composable
fun RecipeCardHorizontal(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(Radius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh nhỏ bên trái
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(Radius.md))
            )
            Spacer(Modifier.width(Spacing.md))
            // Thông tin bên phải
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "${recipe.cookingTimeMinutes} phút",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${recipe.nutrition.calories} kcal",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            // Nút yêu thích
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (recipe.isFavorite)
                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (recipe.isFavorite) Coral400
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}