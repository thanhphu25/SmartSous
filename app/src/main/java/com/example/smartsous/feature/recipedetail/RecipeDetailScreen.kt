package com.example.smartsous.feature.recipedetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import kotlinx.coroutines.launch

private val tabs = listOf("🥬 Nguyên liệu", "👨‍🍳 Cách nấu", "📊 Dinh dưỡng")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple400)
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Lỗi không xác định")
            }
        }
        uiState.recipe != null -> {
            val recipe = uiState.recipe!!
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Hero Image + Back + Favorite ─────────────
                Box(modifier = Modifier.height(280.dp)) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )

                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(Spacing.sm)
                            .background(
                                Color.Black.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }

                    // Favorite button — góc trên phải
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(Spacing.sm)
                            .align(Alignment.TopEnd)
                            .background(
                                Color.Black.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (recipe.isFavorite)
                                Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (recipe.isFavorite) Coral400 else Color.White
                        )
                    }

                    // Tên + metadata — góc dưới ảnh
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Spacing.md)
                    ) {
                        // Difficulty badge
                        DifficultyBadge(recipe.difficulty)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            recipe.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        // Metadata row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetaItem(
                                icon = { Icon(Icons.Default.Schedule, null, Modifier.size(14.dp), tint = Color.White) },
                                text = "${recipe.cookingTimeMinutes} phút",
                                textColor = Color.White
                            )
                            MetaItem(
                                icon = { Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(14.dp), tint = Amber400) },
                                text = "${recipe.nutrition.calories} kcal",
                                textColor = Color.White
                            )
                            MetaItem(
                                icon = { Icon(Icons.Default.People, null, Modifier.size(14.dp), tint = Color.White) },
                                text = "${recipe.servings} người",
                                textColor = Color.White
                            )
                        }
                    }
                }

                // ── Tab Bar ───────────────────────────────────
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Purple400
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (pagerState.currentPage == index) Purple400
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // ── Tab Content ───────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> IngredientsTab(recipe = recipe)
                        1 -> StepsTab(recipe = recipe)
                        2 -> NutritionTab(recipe = recipe)
                    }
                }
            }
        }
    }
}

// ── Tab 1: Nguyên liệu ────────────────────────────────────
@Composable
private fun IngredientsTab(recipe: Recipe) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        Text(
            "${recipe.ingredients.size} nguyên liệu • ${recipe.servings} người",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))

        recipe.ingredients.forEach { ingredient ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Dot indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Purple400, CircleShape)
                    )
                    Text(
                        ingredient.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Amount + unit
                Text(
                    "${formatAmount(ingredient.amount)} ${ingredient.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Purple400
                )
            }
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

// ── Tab 2: Cách nấu ──────────────────────────────────────
@Composable
private fun StepsTab(recipe: Recipe) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        Text(
            "${recipe.steps.size} bước",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))

        recipe.steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Số thứ tự bước
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Purple400, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                // Nội dung bước
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Bước ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Purple400,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

// ── Tab 3: Dinh dưỡng ────────────────────────────────────
@Composable
private fun NutritionTab(recipe: Recipe) {
    val nutrition = recipe.nutrition

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        Text(
            "Giá trị dinh dưỡng / khẩu phần",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Spacing.md))

        // Calories card lớn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Purple400.copy(alpha = 0.1f))
                .padding(Spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${nutrition.calories}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Purple400,
                    fontSize = 48.sp
                )
                Text(
                    "kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Purple400
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // 4 chỉ số dinh dưỡng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            NutritionCard(
                label = "Protein",
                value = "${nutrition.protein}g",
                color = Teal400,
                modifier = Modifier.weight(1f)
            )
            NutritionCard(
                label = "Carbs",
                value = "${nutrition.carbs}g",
                color = Amber400,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            NutritionCard(
                label = "Chất béo",
                value = "${nutrition.fat}g",
                color = Coral400,
                modifier = Modifier.weight(1f)
            )
            NutritionCard(
                label = "Chất xơ",
                value = "${nutrition.fiber}g",
                color = Purple400,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Bar chart dinh dưỡng — tự vẽ
        NutritionBarSection(nutrition = nutrition)
    }
}

// Card nhỏ cho từng chỉ số dinh dưỡng
@Composable
private fun NutritionCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

// Bar chart dinh dưỡng tự vẽ bằng Composable
@Composable
private fun NutritionBarSection(nutrition: com.example.smartsous.domain.model.Nutrition) {
    val items = listOf(
        Triple("Protein", nutrition.protein.toFloat(), Teal400),
        Triple("Carbs",   nutrition.carbs.toFloat(),   Amber400),
        Triple("Béo",     nutrition.fat.toFloat(),      Coral400),
        Triple("Xơ",      nutrition.fiber.toFloat(),    Purple400)
    )
    val maxValue = items.maxOf { it.second }.coerceAtLeast(1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tỉ lệ dinh dưỡng",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        items.forEach { (label, value, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(52.dp)
                )
                // Progress bar tự vẽ
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value / maxValue)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color)
                    )
                }
                Text(
                    "${value}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────
@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val (label, color) = when (difficulty) {
        Difficulty.EASY   -> "Dễ" to Teal400
        Difficulty.MEDIUM -> "Vừa" to Amber400
        Difficulty.HARD   -> "Khó" to Coral400
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun MetaItem(
    icon: @Composable () -> Unit,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(text, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

private fun formatAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else amount.toString()