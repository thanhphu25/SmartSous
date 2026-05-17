package com.example.smartsous.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.RecipeCard
import com.example.smartsous.core.ui.components.RecipeListSkeleton
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading && uiState.allRecipes.isEmpty()) {
        RecipeListSkeleton()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {

        // ── Greeting ──────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.md
                )
            ) {
                Text(
                    "Hôm nay nấu gì? 👨‍🍳",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Gợi ý từ nguyên liệu trong tủ lạnh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Hero Card — món gợi ý số 1 ───────────────────────
        if (uiState.suggestedRecipes.isNotEmpty()) {
            item {
                HeroRecipeCard(
                    suggested = uiState.suggestedRecipes.first(),
                    onClick = {
                        onRecipeClick(uiState.suggestedRecipes.first().recipe.id)
                    },
                    onFavoriteClick = {
                        val recipe = uiState.suggestedRecipes.first().recipe
                        viewModel.toggleFavorite(recipe.id, recipe.isFavorite)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(Modifier.height(Spacing.md))
            }
        }

        // ── Danh sách gợi ý ngang — món 2 đến 6 ─────────────
        if (uiState.suggestedRecipes.size > 1) {
            item {
                Text(
                    "Gợi ý cho bạn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = uiState.suggestedRecipes.drop(1).take(5),
                        key = { it.recipe.id }
                    ) { suggested ->
                        SuggestedMiniCard(
                            suggested = suggested,
                            onClick = { onRecipeClick(suggested.recipe.id) }
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }

        // ── Tất cả món ────────────────────────────────────────
        item {
            Text(
                "Tất cả món ăn",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        items(
            items = uiState.allRecipes,
            key = { it.id }
        ) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
                onFavoriteClick = {
                    viewModel.toggleFavorite(recipe.id, recipe.isFavorite)
                },
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs
                )
            )
        }
    }
}

// Hero Card — card lớn hiện ở đầu màn hình
@Composable
private fun HeroRecipeCard(
    suggested: SuggestedRecipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = suggested.recipe

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            // Ảnh nền
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay từ dưới lên — để text đọc được
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 80f
                        )
                    )
            )

            // Badge lý do gợi ý — góc trên trái
            SuggestionBadge(
                reason = suggested.reason,
                matchPercent = suggested.matchPercent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.md)
            )

            // Thông tin món — góc dưới trái
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.md)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⏱ ${recipe.cookingTimeMinutes} phút",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        "🔥 ${recipe.nutrition.calories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (suggested.matchPercent > 0) {
                        Text(
                            "🥬 ${suggested.matchPercent}% nguyên liệu",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// Mini card dạng đứng trong LazyRow
@Composable
private fun SuggestedMiniCard(
    suggested: SuggestedRecipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = suggested.recipe

    Card(
        modifier = modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                if (suggested.matchPercent == 100) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Teal400, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("✓ Đủ NL", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${recipe.cookingTimeMinutes} phút · ${recipe.nutrition.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Badge "Đủ nguyên liệu", "Lành mạnh"...
@Composable
private fun SuggestionBadge(
    reason: SuggestionReason,
    matchPercent: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (reason) {
        SuggestionReason.PERFECT_MATCH      -> "✅ Đủ nguyên liệu" to Teal400
        SuggestionReason.HIGH_MATCH         -> "🥬 $matchPercent% nguyên liệu" to Purple400
        SuggestionReason.HEALTHY_CHOICE     -> "💚 Lành mạnh" to Teal400
        SuggestionReason.QUICK_COOK         -> "⚡ Nấu nhanh" to Purple400
        SuggestionReason.NOT_COOKED_RECENTLY -> "🆕 Món mới" to Purple400
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}