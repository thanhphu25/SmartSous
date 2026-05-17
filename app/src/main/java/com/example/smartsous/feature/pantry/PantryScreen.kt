package com.example.smartsous.feature.pantry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.EmptyState
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Danh sách category để hiện filter
private val categoryFilters = listOf(
    null to "Tất cả",
    IngredientCategory.MEAT      to "🥩 Thịt",
    IngredientCategory.SEAFOOD   to "🦐 Hải sản",
    IngredientCategory.VEGETABLE to "🥦 Rau củ",
    IngredientCategory.DAIRY     to "🥛 Sữa/Trứng",
    IngredientCategory.GRAIN     to "🌾 Ngũ cốc",
    IngredientCategory.SPICE     to "🧄 Gia vị",
    IngredientCategory.FRUIT     to "🍎 Trái cây",
    IngredientCategory.OTHER     to "📦 Khác",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantryScreen(
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────
            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.md
                )
            ) {
                Text(
                    "Tủ lạnh 🧊",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${uiState.allIngredients.size} nguyên liệu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Expiry warning banner ──────────────────────
            AnimatedVisibility(visible = uiState.expiringCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Coral400.copy(alpha = 0.12f))
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Coral400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${uiState.expiringCount} nguyên liệu sắp hết hạn trong 3 ngày!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Coral400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Category filter chips ─────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(categoryFilters) { (category, label) ->
                    val selected = uiState.selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple400.copy(alpha = 0.15f),
                            selectedLabelColor = Purple400
                        )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Danh sách nguyên liệu ─────────────────────
            if (uiState.filteredIngredients.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "Tủ lạnh trống",
                    subtitle = "Thêm nguyên liệu để SmartSous gợi ý món phù hợp nhé!",
                    actionText = "Thêm nguyên liệu",
                    onAction = { viewModel.openAddSheet() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = 80.dp // space for FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = uiState.filteredIngredients,
                        key = { it.id }
                    ) { ingredient ->
                        // SwipeToDismissBox để xoá
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteIngredient(ingredient)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.animateItemPlacement(),
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                // Background đỏ khi swipe
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.targetValue ==
                                        SwipeToDismissBoxValue.EndToStart) Coral400
                                    else Color.Transparent,
                                    animationSpec = tween(200),
                                    label = "swipe_color"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .padding(end = Spacing.md),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue ==
                                        SwipeToDismissBoxValue.EndToStart) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Xoá",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            IngredientRow(
                                ingredient = ingredient,
                                onClick = { viewModel.openEditSheet(ingredient) }
                            )
                        }
                    }
                }
            }
        }

        // ── FAB thêm mới ──────────────────────────────────
        FloatingActionButton(
            onClick = { viewModel.openAddSheet() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.md),
            containerColor = Purple400,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm nguyên liệu")
        }

        // ── Add/Edit Bottom Sheet ─────────────────────────
        if (uiState.showAddEditSheet) {
            AddEditIngredientSheet(
                formState = uiState.formState,
                onNameChange = viewModel::onNameChange,
                onQuantityChange = viewModel::onQuantityChange,
                onUnitChange = viewModel::onUnitChange,
                onCategoryChange = viewModel::onCategoryChange,
                onExpiryDateChange = viewModel::onExpiryDateChange,
                onSave = viewModel::saveIngredient,
                onDismiss = viewModel::closeSheet
            )
        }
    }
}

@Composable
fun IngredientRow(
    ingredient: Ingredient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expiryStatus = getExpiryStatus(ingredient.expiryDate)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Purple400.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getCategoryEmoji(ingredient.category),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                ingredient.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${formatQty(ingredient.quantity)} ${ingredient.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        expiryStatus?.let { (label, color) ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getExpiryStatus(expiryDate: LocalDate?): Pair<String, Color>? {
    val date = expiryDate ?: return null
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
    return when {
        days < 0  -> "Hết hạn" to Coral400
        days == 0 -> "Hôm nay" to Coral400
        days <= 3 -> "Còn $days ngày" to Amber400
        days <= 7 -> "Còn $days ngày" to Teal400
        else      -> null
    }
}

private fun getCategoryEmoji(category: IngredientCategory) = when (category) {
    IngredientCategory.MEAT      -> "🥩"
    IngredientCategory.SEAFOOD   -> "🦐"
    IngredientCategory.VEGETABLE -> "🥦"
    IngredientCategory.DAIRY     -> "🥛"
    IngredientCategory.GRAIN     -> "🌾"
    IngredientCategory.SPICE     -> "🧄"
    IngredientCategory.FRUIT     -> "🍎"
    IngredientCategory.BEVERAGE  -> "🧃"
    IngredientCategory.OTHER     -> "📦"
}

private fun formatQty(qty: Double) =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
