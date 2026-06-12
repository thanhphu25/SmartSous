package com.example.smartsous.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.core.ui.components.NutritionData
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.SuggestMealsUseCase
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class PlannerMealUiModel(
    val recipeId: String,
    val name: String,
    val date: LocalDate,
    val mealType: MealType,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val imageUrl: String
)

data class PlannerUiState(
    val isLoading: Boolean = true,
    val weekStart: LocalDate = currentWeekStart(),
    val weekDates: List<LocalDate> = emptyList(),
    val plannedMeals: List<PlannerMealUiModel> = emptyList(),
    val nutritionData: List<NutritionData> = emptyList(),
    val allRecipes: List<Recipe> = emptyList(),
    val suggestedRecipes: List<SuggestedRecipe> = emptyList(),
    val isSuggestionLoading: Boolean = false,
    val weeklyCalories: Int = 0,
    val weeklyProtein: Double = 0.0,
    val weeklyCarbs: Double = 0.0,
    val weeklyFat: Double = 0.0
) {
    val weekLabel: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("dd/MM")
            val end = weekStart.plusDays(6)
            return "${weekStart.format(formatter)} - ${end.format(formatter)}"
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val mealPlanRepository: IMealPlanRepository,
    private val recipeRepository: IRecipeRepository,
    private val pantryRepository: IPantryRepository,
    private val dataStoreManager: DataStoreManager,
    private val suggestMealsUseCase: SuggestMealsUseCase
) : ViewModel() {

    private val _weekStart = MutableStateFlow(currentWeekStart())
    private val pendingDeletions = MutableStateFlow<Set<String>>(emptySet())

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observePlanner()
    }

    private fun observePlanner() {
        val plansForSelectedWeek = _weekStart.flatMapLatest { weekStart ->
            mealPlanRepository.getMealPlanForWeek(weekStart)
                .distinctUntilChanged()
                .map { plans -> weekStart to plans }
        }

        combine(
            plansForSelectedWeek,
            recipeRepository.getAllRecipes().distinctUntilChanged(),
            pendingDeletions
        ) { (weekStart, mealPlans), allRecipes, deletions ->
            val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
            val recipeMap = allRecipes.associateBy { it.id }
            val uiMeals = mutableListOf<PlannerMealUiModel>()

            var totalCal = 0
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFat = 0.0

            mealPlans.forEach { plan ->
                plan.meals.forEach { (mealType, recipeIds) ->
                    recipeIds.forEach { recipeId ->
                        val itemKey = mealKey(recipeId, plan.date, mealType)
                        if (itemKey !in deletions) {
                            recipeMap[recipeId]?.let { recipe ->
                                uiMeals.add(
                                    PlannerMealUiModel(
                                        recipeId = recipe.id,
                                        name = recipe.name,
                                        date = plan.date,
                                        mealType = mealType,
                                        calories = recipe.nutrition.calories,
                                        protein = recipe.nutrition.protein,
                                        carbs = recipe.nutrition.carbs,
                                        imageUrl = recipe.imageUrl
                                    )
                                )
                                totalCal += recipe.nutrition.calories
                                totalProtein += recipe.nutrition.protein
                                totalCarbs += recipe.nutrition.carbs
                                totalFat += recipe.nutrition.fat
                            }
                        }
                    }
                }
            }

            val nutrition = listOf(
                NutritionData("Calories", totalCal.toFloat(), "kcal", Purple400),
                NutritionData("Protein", totalProtein.toFloat(), "g", Teal400),
                NutritionData("Carbs", totalCarbs.toFloat(), "g", Coral400),
                NutritionData("Fat", totalFat.toFloat(), "g", Amber400)
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    weekStart = weekStart,
                    weekDates = weekDates,
                    plannedMeals = uiMeals.sortedWith(compareBy({ meal -> meal.date }, { meal -> meal.mealType.ordinal })),
                    nutritionData = nutrition,
                    allRecipes = allRecipes,
                    weeklyCalories = totalCal,
                    weeklyProtein = totalProtein,
                    weeklyCarbs = totalCarbs,
                    weeklyFat = totalFat
                )
            }
        }.launchIn(viewModelScope)
    }

    fun loadSuggestionsIfNeeded() {
        val current = _uiState.value
        if (current.isSuggestionLoading || current.suggestedRecipes.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionLoading = true) }
            val recipes = _uiState.value.allRecipes
            val pantryItems = pantryRepository.getAllIngredients().first()
            val preferences = dataStoreManager.userPreferencesFlow.first()
            val suggestions = withContext(Dispatchers.Default) {
                suggestMealsUseCase(
                    allRecipes = recipes,
                    pantryIngredients = pantryItems,
                    userPreference = preferences,
                    topN = 12
                )
            }
            _uiState.update {
                it.copy(
                    suggestedRecipes = suggestions,
                    isSuggestionLoading = false
                )
            }
        }
    }

    fun previousWeek() {
        _uiState.update { it.copy(isLoading = true) }
        _weekStart.update { it.minusWeeks(1) }
    }

    fun nextWeek() {
        _uiState.update { it.copy(isLoading = true) }
        _weekStart.update { it.plusWeeks(1) }
    }

    fun goToCurrentWeek() {
        _uiState.update { it.copy(isLoading = true) }
        _weekStart.value = currentWeekStart()
    }

    fun addRecipeToPlan(recipeId: String, mealType: MealType, date: LocalDate) {
        viewModelScope.launch {
            pendingDeletions.update { it - mealKey(recipeId, date, mealType) }
            mealPlanRepository.addRecipeToPlan(date, mealType, recipeId)
        }
    }

    fun removeMeal(recipeId: String, mealType: MealType, date: LocalDate) {
        val itemKey = mealKey(recipeId, date, mealType)
        pendingDeletions.update { it + itemKey }
        viewModelScope.launch {
            mealPlanRepository.removeRecipeFromPlan(date, mealType, recipeId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            recipeRepository.refreshFromRemote()
        }
    }

    private fun mealKey(recipeId: String, date: LocalDate, mealType: MealType): String =
        "$recipeId-$date-$mealType"
}

private fun currentWeekStart(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
