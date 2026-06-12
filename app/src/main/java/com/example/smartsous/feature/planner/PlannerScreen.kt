package com.example.smartsous.feature.planner

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.NutritionChart
import com.example.smartsous.core.ui.components.RecipeListSkeleton
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.usecase.RecipeNameSearchMatcher
import com.example.smartsous.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            delay(1000)
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Tổng quan Dinh dưỡng Tuần",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Spacing.md)
            )

            NutritionChart(data = uiState.nutritionData)

            Spacer(modifier = Modifier.height(Spacing.md))

            if (uiState.isLoading) {
                RecipeListSkeleton()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(uiState.weekDates) { date ->
                        val dayMeals = uiState.plannedMeals.filter { it.date == date }
                        DayCard(
                            date = date,
                            meals = dayMeals,
                            onDeleteMeal = { recipeId, mealType ->
                                viewModel.removeMeal(recipeId, mealType, date)
                            },
                            onAddMealClick = {
                                selectedDate = date
                                showBottomSheet = true
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(Spacing.xxl))
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Hướng dẫn cũ đã được xóa

        if (showBottomSheet && selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                RecipePickerContent(
                    recipes = uiState.allRecipes,
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
fun DayCard(
    date: LocalDate,
    meals: List<PlannerMealUiModel>,
    onDeleteMeal: (String, MealType) -> Unit,
    onAddMealClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dayOfWeek = when (date.dayOfWeek.value) {
        1 -> "Thứ Hai"
        2 -> "Thứ Ba"
        3 -> "Thứ Tư"
        4 -> "Thứ Năm"
        5 -> "Thứ Sáu"
        6 -> "Thứ Bảy"
        7 -> "Chủ Nhật"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = date.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(onClick = onAddMealClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm món",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm món")
                }
            }

            if (meals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val mealsByType = meals.groupBy { it.mealType }
                val mealTypesOrder = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
                
                mealTypesOrder.forEach { type ->
                    val typeMeals = mealsByType[type]
                    if (!typeMeals.isNullOrEmpty()) {
                        val typeLabel = when(type) {
                            MealType.BREAKFAST -> "Bữa sáng"
                            MealType.LUNCH -> "Bữa trưa"
                            MealType.DINNER -> "Bữa tối"
                            MealType.SNACK -> "Ăn vặt"
                        }
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                        typeMeals.forEach { meal ->
                            MealItemCard(
                                meal = meal,
                                onDelete = { onDeleteMeal(meal.recipeId, meal.mealType) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealItemCard(meal: PlannerMealUiModel, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Purple100
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Purple800,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePickerContent(
    recipes: List<Recipe>,
    onRecipeSelected: (String, MealType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    
    val filteredRecipes = remember(searchQuery, recipes) {
        RecipeNameSearchMatcher.filterByName(recipes, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Chọn món ăn để thêm",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(
                MealType.BREAKFAST to "Sáng",
                MealType.LUNCH to "Trưa",
                MealType.DINNER to "Tối",
                MealType.SNACK to "Ăn vặt"
            )
            options.forEach { (type, label) ->
                FilterChip(
                    selected = selectedMealType == type,
                    onClick = { selectedMealType = type },
                    label = { Text(label) }
                )
            }
        }
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Tìm kiếm món ăn...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRecipes) { recipe ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = { Text(recipe.cuisine) },
                    leadingContent = {
                        val context = LocalContext.current
                        val imageRequest = remember(recipe.imageUrl) {
                            ImageRequest.Builder(context)
                                .data(recipe.imageUrl)
                                .listener(onError = { _, result ->
                                    println("Coil Error: ${recipe.name} - ${result.throwable}")
                                })
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = recipe.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                            placeholder = rememberVectorPainter(Icons.Default.Image)
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRecipeSelected(recipe.id, selectedMealType) }
                )
            }
        }
    }
}
