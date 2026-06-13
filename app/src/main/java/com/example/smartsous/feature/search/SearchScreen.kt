package com.example.smartsous.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    onBack: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

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
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            keyboardController?.show()
                        }
                    },
                placeholder = { Text("Tìm tên món ăn...") },
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                trailingIcon = {
                    if (uiState.filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Xoá")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                ),
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
                        SearchStartContent(
                            modifier = Modifier.fillParentMaxSize(),
                            onQuerySelect = viewModel::onQueryChange,
                            onCookingTimeSelect = viewModel::onCookingTimeSelect,
                            onCaloriesSelect = viewModel::onCaloriesSelect,
                            onBlankClick = onBack
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

@Composable
private fun SearchStartContent(
    modifier: Modifier = Modifier,
    onQuerySelect: (String) -> Unit,
    onCookingTimeSelect: (Int) -> Unit,
    onCaloriesSelect: (Int) -> Unit,
    onBlankClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onBlankClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Decorative Header
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Purple400.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Purple400
            )
        }
        
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Khám phá món ngon",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Nhập tên món hoặc chọn gợi ý bên dưới",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "🔥 Từ khóa thịnh hành",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    listOf("Cá hồi", "Thịt bò", "Gà", "Tôm", "Đậu hũ").forEach { chip ->
                        androidx.compose.material3.Surface(
                            onClick = { onQuerySelect(chip) },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))

                Text(
                    text = "🌟 Danh mục nổi bật",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.md))
                
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        CategoryCard(
                            title = "Món Canh", emoji = "🍲",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuerySelect("Canh") }
                        )
                        CategoryCard(
                            title = "Ăn Chay", emoji = "🥗",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuerySelect("Chay") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        CategoryCard(
                            title = "Nấu Nhanh", emoji = "⏱️",
                            modifier = Modifier.weight(1f),
                            onClick = { onCookingTimeSelect(30) }
                        )
                        CategoryCard(
                            title = "Giảm Cân", emoji = "🏃‍♀️",
                            modifier = Modifier.weight(1f),
                            onClick = { onCaloriesSelect(400) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Purple400.copy(alpha = 0.05f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = Purple400
            )
        }
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
