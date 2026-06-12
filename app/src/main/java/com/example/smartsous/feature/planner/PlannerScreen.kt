package com.example.smartsous.feature.planner

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.RecipeListSkeleton
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.usecase.RecipeNameSearchMatcher
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Purple50
import com.example.smartsous.ui.theme.Purple800
import com.example.smartsous.ui.theme.Teal400
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlannerPinnedHeader(
                uiState = uiState,
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek = viewModel::nextWeek
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (uiState.isLoading) {
                    item { RecipeListSkeleton() }
                } else {
                    items(uiState.weekDates, key = { it.toString() }) { date ->
                        val dayMeals = uiState.plannedMeals.filter { it.date == date }
                        DayPlannerCard(
                            date = date,
                            meals = dayMeals,
                            onAddMealClick = {
                                selectedDate = date
                                selectedMealType = MealType.LUNCH
                                showBottomSheet = true
                                viewModel.loadSuggestionsIfNeeded()
                            },
                            onMealColumnClick = { mealType ->
                                selectedDate = date
                                selectedMealType = mealType
                                showBottomSheet = true
                                viewModel.loadSuggestionsIfNeeded()
                            },
                            onDeleteMeal = { recipeId, mealType ->
                                viewModel.removeMeal(recipeId, mealType, date)
                            }
                        )
                    }
                }
            }
        }

        if (showBottomSheet && selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                RecipePickerContent(
                    date = selectedDate!!,
                    recipes = uiState.allRecipes,
                    suggestedRecipes = uiState.suggestedRecipes,
                    isSuggestionLoading = uiState.isSuggestionLoading,
                    selectedMealType = selectedMealType,
                    onMealTypeSelected = { selectedMealType = it },
                    onRecipeSelected = { recipeId, mealType ->
                        viewModel.addRecipeToPlan(recipeId, mealType, selectedDate!!)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlannerPinnedHeader(
    uiState: PlannerUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = Spacing.sm
            )
    ) {
        WeekHeader(
            weekLabel = uiState.weekLabel,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )
        Spacer(Modifier.height(Spacing.md))
        NutritionSummary(uiState = uiState)
    }
}

@Composable
private fun WeekHeader(
    weekLabel: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Kế hoạch bữa ăn",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Purple800
            )
            Text(
                text = "Tuần $weekLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Tuần trước")
        }
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Today, contentDescription = null, tint = Purple400)
        }
        IconButton(onClick = onNextWeek) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Tuần sau")
        }
    }
}

@Composable
private fun NutritionSummary(uiState: PlannerUiState) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Tổng quan dinh dưỡng tuần",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                NutritionMetric(
                    label = "Calories",
                    value = "${uiState.weeklyCalories}",
                    unit = "kcal",
                    color = Purple400,
                    modifier = Modifier.weight(1f)
                )
                NutritionMetric(
                    label = "Protein",
                    value = uiState.weeklyProtein.toInt().toString(),
                    unit = "g",
                    color = Teal400,
                    modifier = Modifier.weight(1f)
                )
                NutritionMetric(
                    label = "Carbs",
                    value = uiState.weeklyCarbs.toInt().toString(),
                    unit = "g",
                    color = Coral400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NutritionMetric(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun DayPlannerCard(
    date: LocalDate,
    meals: List<PlannerMealUiModel>,
    onAddMealClick: () -> Unit,
    onMealColumnClick: (MealType) -> Unit,
    onDeleteMeal: (String, MealType) -> Unit
) {
    val today = date == LocalDate.now()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (today) Purple50 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = date.dayLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Purple800
                        )
                        if (today) {
                            TodayPill()
                        }
                    }
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${meals.sumOf { it.calories }} kcal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.sm))
                IconButton(
                    onClick = onAddMealClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm món",
                        tint = Purple400
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                mealTypeOrder.forEach { mealType ->
                    MealColumn(
                        mealType = mealType,
                        meals = meals.filter { it.mealType == mealType },
                        onClick = { onMealColumnClick(mealType) },
                        onDelete = { recipeId -> onDeleteMeal(recipeId, mealType) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayPill() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Purple400.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "Hôm nay",
            style = MaterialTheme.typography.labelSmall,
            color = Purple400,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MealColumn(
    mealType: MealType,
    meals: List<PlannerMealUiModel>,
    onClick: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(8.dp)
    ) {
        Text(
            text = mealType.shortLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = Purple800,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))

        if (meals.isEmpty()) {
            Text(
                text = "Trống",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            meals.forEach { meal ->
                CompactMealItem(
                    meal = meal,
                    onDelete = { onDelete(meal.recipeId) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CompactMealItem(meal: PlannerMealUiModel, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Purple400.copy(alpha = 0.1f))
            .padding(6.dp)
    ) {
        Text(
            text = meal.name,
            style = MaterialTheme.typography.labelSmall,
            color = Purple800,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${meal.calories}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun RecipePickerContent(
    date: LocalDate,
    recipes: List<Recipe>,
    suggestedRecipes: List<SuggestedRecipe>,
    isSuggestionLoading: Boolean,
    selectedMealType: MealType,
    onMealTypeSelected: (MealType) -> Unit,
    onRecipeSelected: (String, MealType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val favoriteRecipes = remember(recipes) { recipes.filter { it.isFavorite } }
    val suggestedRecipeList = remember(suggestedRecipes) { suggestedRecipes.map { it.recipe } }
    val baseRecipes = when (selectedTab) {
        0 -> suggestedRecipeList
        1 -> recipes
        else -> favoriteRecipes
    }
    val filteredRecipes = remember(searchQuery, baseRecipes) {
        RecipeNameSearchMatcher.filterByName(baseRecipes, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.md)
    ) {
        Text(
            text = "Thêm món cho ${date.dayLabel()}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        Text(
            text = "Thêm vào bữa",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            mealTypeOrder.forEach { type ->
                FilterChip(
                    selected = selectedMealType == type,
                    onClick = { onMealTypeSelected(type) },
                    label = { Text(type.shortLabel()) }
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Món bạn chọn sẽ được lưu vào bữa ${selectedMealType.shortLabel().lowercase()}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        TabRow(selectedTabIndex = selectedTab) {
            listOf("Gợi ý", "Tất cả", "Yêu thích").forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm món ăn...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(Spacing.sm))

        if (filteredRecipes.isEmpty()) {
            Text(
                text = when (selectedTab) {
                    0 -> if (isSuggestionLoading) {
                        "Đang gợi ý món phù hợp..."
                    } else {
                        "Chưa có gợi ý phù hợp từ tủ lạnh."
                    }
                    2 -> "Bạn chưa lưu món yêu thích nào."
                    else -> "Không tìm thấy món phù hợp."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.lg)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipePickerRow(
                        recipe = recipe,
                        suggestion = suggestedRecipes.firstOrNull { it.recipe.id == recipe.id },
                        onClick = { onRecipeSelected(recipe.id, selectedMealType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipePickerRow(
    recipe: Recipe,
    suggestion: SuggestedRecipe?,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                recipe.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("${recipe.cookingTimeMinutes} phút")
                Text("•")
                Text("${recipe.nutrition.calories} kcal")
                suggestion?.let {
                    Text("•")
                    Text("${it.matchPercent}% khớp")
                }
            }
        },
        leadingContent = {
            RecipeThumb(recipe)
        },
        trailingContent = {
            if (recipe.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Yêu thích",
                    tint = Coral400,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    )
}

@Composable
private fun RecipeThumb(recipe: Recipe) {
    val context = LocalContext.current
    val imageRequest = remember(recipe.imageUrl) {
        ImageRequest.Builder(context)
            .data(recipe.imageUrl)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = recipe.name,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Crop,
        error = rememberVectorPainter(Icons.Default.BrokenImage),
        placeholder = rememberVectorPainter(Icons.Default.Image)
    )
}

private val mealTypeOrder = listOf(
    MealType.BREAKFAST,
    MealType.LUNCH,
    MealType.DINNER,
    MealType.SNACK
)

private fun MealType.shortLabel(): String =
    when (this) {
        MealType.BREAKFAST -> "Sáng"
        MealType.LUNCH -> "Trưa"
        MealType.DINNER -> "Tối"
        MealType.SNACK -> "Ăn vặt"
    }

private fun LocalDate.dayLabel(): String =
    when (dayOfWeek.value) {
        1 -> "Thứ Hai"
        2 -> "Thứ Ba"
        3 -> "Thứ Tư"
        4 -> "Thứ Năm"
        5 -> "Thứ Sáu"
        6 -> "Thứ Bảy"
        7 -> "Chủ Nhật"
        else -> ""
    }
