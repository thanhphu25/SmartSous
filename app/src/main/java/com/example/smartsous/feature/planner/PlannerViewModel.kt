package com.example.smartsous.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.ui.components.NutritionData
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class PlannerMealUiModel(
    val recipeId: String,
    val name: String,
    val date: LocalDate,
    val mealType: MealType,
)

data class PlannerUiState(
    val isLoading: Boolean = true,
    val weekDates: List<LocalDate> = emptyList(),
    val plannedMeals: List<PlannerMealUiModel> = emptyList(),
    val nutritionData: List<NutritionData> = emptyList(),
    val allRecipes: List<Recipe> = emptyList()
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val mealPlanRepository: IMealPlanRepository,
    private val recipeRepository: IRecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPlannerData()
    }

    private fun loadPlannerData() {
        val startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
        
        _uiState.update { it.copy(weekDates = weekDates, isLoading = true) }

        combine(
            mealPlanRepository.getMealPlanForWeek(startOfWeek),
            recipeRepository.getAllRecipes()
        ) { mealPlans, allRecipes ->
            val recipeMap = allRecipes.associateBy { it.id }
            
            val uiMeals = mutableListOf<PlannerMealUiModel>()
            var totalCal = 0f
            var totalPro = 0f
            var totalCarb = 0f

            mealPlans.forEach { plan ->
                plan.meals.forEach { (mealType, recipeIds) ->
                    recipeIds.forEach { recipeId ->
                        recipeMap[recipeId]?.let { recipe ->
                            uiMeals.add(
                                PlannerMealUiModel(
                                    recipeId = recipe.id,
                                    name = recipe.name,
                                    date = plan.date,
                                    mealType = mealType
                                )
                            )
                            totalCal += recipe.nutrition.calories.toFloat()
                            totalPro += recipe.nutrition.protein.toFloat()
                            totalCarb += recipe.nutrition.carbs.toFloat()
                        }
                    }
                }
            }

            val nutrition = listOf(
                NutritionData("Calories", totalCal, "kcal", Purple400),
                NutritionData("Protein", totalPro, "g", Teal400),
                NutritionData("Carbs", totalCarb, "g", Coral400)
            )

            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    plannedMeals = uiMeals, 
                    nutritionData = nutrition,
                    allRecipes = allRecipes
                )
            }
        }.launchIn(viewModelScope)
    }

    fun addRecipeToPlan(recipeId: String, mealType: MealType, date: LocalDate) {
        viewModelScope.launch {
            mealPlanRepository.addRecipeToPlan(date, mealType, recipeId)
        }
    }

    fun removeMealFromPlan(recipeId: String, mealType: MealType, date: LocalDate) {
        viewModelScope.launch {
            mealPlanRepository.removeRecipeFromPlan(date, mealType, recipeId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            recipeRepository.refreshFromRemote()
        }
    }
}
