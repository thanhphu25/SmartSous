package com.example.smartsous.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.EmptyState
import com.example.smartsous.core.ui.components.RecipeCardHorizontal
import com.example.smartsous.ui.theme.Purple400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        // ── Search bar + Filter button ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedTextField(
                value = uiState.filter.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tìm món ăn, nguyên liệu...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Xoá")
                        }
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple400
                )
            )

            // Nút filter — đổi màu khi có filter active
            val hasActiveFilter = !uiState.filter.isEmpty
            IconButton(
                onClick = { viewModel.toggleFilterSheet() },
                modifier = Modifier
                    .size(52.dp)
                    .then(
                        if (hasActiveFilter) Modifier.padding(2.dp)
                        else Modifier
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Bộ lọc",
                    tint = if (hasActiveFilter) Purple400
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Active filter chips ───────────────────────────────
        val filter = uiState.filter
        val hasChips = filter.selectedCuisines.isNotEmpty()
                || filter.selectedDifficulty.isNotEmpty()
                || filter.maxCookingTime != null
                || filter.maxCalories != null
                || filter.onlyFavorites

        if (hasChips) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Chip từng cuisine đã chọn
                filter.selectedCuisines.forEach { cuisine ->
                    ActiveFilterChip(
                        label = cuisine,
                        onRemove = { viewModel.onCuisineToggle(cuisine) }
                    )
                }
                // Chip từng difficulty đã chọn
                filter.selectedDifficulty.forEach { diff ->
                    ActiveFilterChip(
                        label = when (diff) {
                            com.example.smartsous.domain.model.Difficulty.EASY   -> "Dễ"
                            com.example.smartsous.domain.model.Difficulty.MEDIUM -> "Vừa"
                            com.example.smartsous.domain.model.Difficulty.HARD   -> "Khó"
                        },
                        onRemove = { viewModel.onDifficultyToggle(diff) }
                    )
                }
                // Chip thời gian nấu
                filter.maxCookingTime?.let { time ->
                    ActiveFilterChip(
                        label = "≤ $time phút",
                        onRemove = { viewModel.onCookingTimeSelect(null) }
                    )
                }
                // Chip calories
                filter.maxCalories?.let { cal ->
                    ActiveFilterChip(
                        label = "≤ $cal kcal",
                        onRemove = { viewModel.onCaloriesSelect(null) }
                    )
                }
                // Chip yêu thích
                if (filter.onlyFavorites) {
                    ActiveFilterChip(
                        label = "❤️ Yêu thích",
                        onRemove = { viewModel.onFavoritesToggle() }
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
        }

        // ── Số kết quả ───────────────────────────────────────
        if (uiState.filter.query.isNotEmpty() || hasChips) {
            Text(
                text = "Tìm thấy ${uiState.results.size} món",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs
                )
            )
        }

        // ── Danh sách kết quả ────────────────────────────────
        if (uiState.results.isEmpty() && !uiState.filter.isEmpty) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "Không tìm thấy món nào",
                subtitle = "Thử thay đổi từ khoá hoặc bỏ bớt bộ lọc",
                actionText = "Xoá bộ lọc",
                onAction = { viewModel.clearFilter() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Khi chưa có filter → hiện tất cả
                // Khi có filter → hiện kết quả lọc
                val displayList = if (uiState.filter.isEmpty)
                    emptyList() // Màn hình trống khi chưa search
                else uiState.results

                if (displayList.isEmpty() && uiState.filter.isEmpty) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "Tìm kiếm món ăn",
                            subtitle = "Nhập tên món, nguyên liệu hoặc dùng bộ lọc",
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }

                items(
                    items = displayList,
                    key = { it.id }
                ) { recipe ->
                    RecipeCardHorizontal(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(recipe.id, recipe.isFavorite)
                        }
                    )
                }
            }
        }
    }

    // ── Filter Bottom Sheet ───────────────────────────────────
    if (uiState.isFilterSheetOpen) {
        FilterBottomSheet(
            filter = uiState.filter,
            onCuisineToggle = viewModel::onCuisineToggle,
            onDifficultyToggle = viewModel::onDifficultyToggle,
            onCookingTimeSelect = viewModel::onCookingTimeSelect,
            onCaloriesSelect = viewModel::onCaloriesSelect,
            onFavoritesToggle = viewModel::onFavoritesToggle,
            onClearAll = viewModel::clearFilter,
            onDismiss = viewModel::closeFilterSheet
        )
    }
}

// Chip hiện filter đang active — bấm X để xoá filter đó
@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Xoá filter",
                modifier = Modifier.size(14.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Purple400.copy(alpha = 0.15f),
            selectedLabelColor = Purple400,
            selectedTrailingIconColor = Purple400
        )
    )
}