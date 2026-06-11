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

    // Trạng thái hiển thị hướng dẫn
    var showHint by remember { mutableStateOf(true) }

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
            .pointerInput(Unit) {
                // Bắt sự kiện chạm vào màn hình để ẩn hướng dẫn
                awaitPointerEventScope {
                    while (showHint) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        showHint = false
                    }
                }
            }
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.weekDates) { date ->
                        val dayMeals = uiState.plannedMeals.filter { it.date == date }
                        DayColumn(
                            date = date,
                            meals = dayMeals,
                            onDeleteMeal = { recipeId, mealType ->
                                viewModel.removeMeal(recipeId, mealType, date)
                            },
                            onLongClick = {
                                selectedDate = date
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Hướng dẫn ở góc dưới cùng bên trái
        AnimatedVisibility(
            visible = showHint,
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.md)
                .padding(bottom = 16.dp) // Tránh dính sát cạnh dưới
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "Nhấn giữ vào cột ngày để thêm món ăn",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (showBottomSheet && selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                RecipePickerContent(
                    recipes = uiState.allRecipes,
                    onRecipeSelected = { recipeId ->
                        viewModel.addRecipeToPlan(recipeId, MealType.LUNCH, selectedDate!!)
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
fun DayColumn(
    date: LocalDate,
    meals: List<PlannerMealUiModel>,
    onDeleteMeal: (String, MealType) -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    val dayOfWeek = date.dayOfWeek.name.take(3)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark.copy(alpha = 0.05f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = dayOfWeek, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        Text(text = date.format(formatter), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(8.dp))

        if (meals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .border(1.dp, Purple400.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nhấn giữ để thêm",
                    tint = Purple400.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            meals.forEach { meal ->
                MealItem(
                    meal = meal,
                    onDelete = { onDeleteMeal(meal.recipeId, meal.mealType) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MealItem(meal: PlannerMealUiModel, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Purple100)
            .padding(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.labelSmall,
                color = Purple800,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                lineHeight = 10.sp
            )
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp)
                .offset(x = 2.dp, y = (-2).dp)
                .clickable { onDelete() },
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Xóa",
                tint = Color.White,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

@Composable
fun RecipePickerContent(
    recipes: List<Recipe>,
    onRecipeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Chọn món ăn để thêm",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes) { recipe ->
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
                        .clickable { onRecipeSelected(recipe.id) }
                )
            }
        }
    }
}
