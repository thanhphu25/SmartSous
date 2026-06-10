package com.example.smartsous.feature.planner

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.NutritionChart
import com.example.smartsous.core.ui.components.NutritionData
import com.example.smartsous.core.ui.components.RecipeListSkeleton
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            delay(1000) // Fake network delay
            pullRefreshState.endRefresh()
        }
    }   

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                // Calendar Grid 7 cột
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
                            onMealMoved = { recipeId, mealType, oldDate, newDate ->
                                viewModel.moveMeal(recipeId, mealType, oldDate, newDate)
                            }
                        )
                    }
                }
            }
        }
        
        // Chỉ báo Pull-to-refresh
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun DayColumn(
    date: LocalDate,
    meals: List<PlannerMealUiModel>,
    onMealMoved: (String, MealType, LocalDate, LocalDate) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    val dayOfWeek = date.dayOfWeek.name.take(3)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = dayOfWeek, style = MaterialTheme.typography.labelSmall)
        Text(text = date.format(formatter), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))

        meals.forEach { meal ->
            DraggableMealItem(
                meal = meal,
                onDrop = { columnsMoved ->
                    val newDate = meal.date.plusDays(columnsMoved.toLong())
                    if (newDate != meal.date) {
                        onMealMoved(meal.recipeId, meal.mealType, meal.date, newDate)
                    }
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun DraggableMealItem(meal: PlannerMealUiModel, onDrop: (Int) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var itemWidthPx by remember { mutableStateOf(1f) } // Lấy chiều rộng để tính toán offset

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { itemWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                scaleX = if (isDragging) 1.1f else 1f
                scaleY = if (isDragging) 1.1f else 1f
                alpha = if (isDragging) 0.8f else 1f
            }
            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Purple100)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        isDragging = false
                        // Tính toán xem kéo qua bao nhiêu cột (trái hay phải)
                        val columnsMoved = (dragOffset.x / itemWidthPx).roundToInt()
                        dragOffset = Offset.Zero
                        onDrop(columnsMoved)
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    }
                )
            }
            .padding(8.dp)
    ) {
        Text(
            text = meal.name,
            style = MaterialTheme.typography.labelSmall,
            color = Purple800,
            textAlign = TextAlign.Center
        )
    }
}