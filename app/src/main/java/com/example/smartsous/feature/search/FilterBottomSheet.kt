package com.example.smartsous.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.FilterOptions
import com.example.smartsous.domain.model.SearchFilter
import com.example.smartsous.ui.theme.Purple400

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filter: SearchFilter,
    onCuisineToggle: (String) -> Unit,
    onDifficultyToggle: (Difficulty) -> Unit,
    onCookingTimeSelect: (Int?) -> Unit,
    onCaloriesSelect: (Int?) -> Unit,
    onFavoritesToggle: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bộ lọc tìm kiếm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Xoá tất cả", color = Purple400)
                }
            }

            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.md))

            // ── Section: Ẩm thực ─────────────────────────────
            FilterSectionTitle("Ẩm thực")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterOptions.cuisines.forEach { cuisine ->
                    val selected = cuisine in filter.selectedCuisines
                    FilterChip(
                        selected = selected,
                        onClick = { onCuisineToggle(cuisine) },
                        label = { Text(cuisine) },
                        colors = chipColors(selected)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Section: Độ khó ──────────────────────────────
            FilterSectionTitle("Độ khó")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                listOf(
                    Difficulty.EASY   to "🟢 Dễ",
                    Difficulty.MEDIUM to "🟡 Vừa",
                    Difficulty.HARD   to "🔴 Khó"
                ).forEach { (diff, label) ->
                    val selected = diff in filter.selectedDifficulty
                    FilterChip(
                        selected = selected,
                        onClick = { onDifficultyToggle(diff) },
                        label = { Text(label) },
                        colors = chipColors(selected)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Section: Thời gian nấu ───────────────────────
            FilterSectionTitle("Thời gian nấu")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterOptions.cookingTimes.forEach { (minutes, label) ->
                    val selected = filter.maxCookingTime == minutes
                    FilterChip(
                        selected = selected,
                        onClick = {
                            // Bấm lại chip đang chọn → bỏ chọn
                            onCookingTimeSelect(if (selected) null else minutes)
                        },
                        label = { Text(label) },
                        colors = chipColors(selected)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Section: Calories ────────────────────────────
            FilterSectionTitle("Calories")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterOptions.calorieOptions.forEach { (cal, label) ->
                    val selected = filter.maxCalories == cal
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onCaloriesSelect(if (selected) null else cal)
                        },
                        label = { Text(label) },
                        colors = chipColors(selected)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Section: Yêu thích ───────────────────────────
            FilterSectionTitle("Khác")
            FilterChip(
                selected = filter.onlyFavorites,
                onClick = onFavoritesToggle,
                label = { Text("❤️ Chỉ hiện yêu thích") },
                colors = chipColors(filter.onlyFavorites)
            )

            Spacer(Modifier.height(Spacing.lg))

            // Nút áp dụng
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple400
                )
            ) {
                Text(
                    "Áp dụng (${countActiveFilters(filter)} bộ lọc)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

@Composable
private fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Purple400.copy(alpha = 0.15f),
    selectedLabelColor = Purple400,
    selectedLeadingIconColor = Purple400
)

private fun countActiveFilters(filter: SearchFilter): Int {
    var count = 0
    if (filter.selectedCuisines.isNotEmpty()) count++
    if (filter.selectedDifficulty.isNotEmpty()) count++
    if (filter.maxCookingTime != null) count++
    if (filter.maxCalories != null) count++
    if (filter.onlyFavorites) count++
    return count
}