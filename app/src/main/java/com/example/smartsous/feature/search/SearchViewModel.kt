package com.example.smartsous.feature.search

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.domain.model.Difficulty
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SearchFilter
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.SearchRecipesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchUiState(
    val results: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val filter: SearchFilter = SearchFilter(),
    val isFilterSheetOpen: Boolean = false,
    val totalCount: Int = 0
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val recipeRepository: IRecipeRepository,
    private val searchRecipesUseCase: SearchRecipesUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    // Filter state riêng để debounce query text
    private val _filter = MutableStateFlow(SearchFilter())

    init {
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        // Combine: allRecipes + filter → filtered results
        combine(
            recipeRepository.getAllRecipes(),
            // Debounce 300ms chỉ cho query text
            _filter.debounce { filter ->
                if (filter.query.isNotEmpty()) 300L else 0L
            }
        ) { allRecipes, filter ->
            val results = searchRecipesUseCase(allRecipes, filter)
            _uiState.update { state ->
                state.copy(
                    results = results,
                    filter = filter,
                    totalCount = allRecipes.size,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    // User gõ vào search box
    fun onQueryChange(query: String) {
        _filter.update { it.copy(query = query) }
    }

    // Toggle cuisine filter
    fun onCuisineToggle(cuisine: String) {
        _filter.update { filter ->
            val current = filter.selectedCuisines.toMutableSet()
            if (cuisine in current) current.remove(cuisine)
            else current.add(cuisine)
            filter.copy(selectedCuisines = current)
        }
    }

    // Toggle difficulty filter
    fun onDifficultyToggle(difficulty: Difficulty) {
        _filter.update { filter ->
            val current = filter.selectedDifficulty.toMutableSet()
            if (difficulty in current) current.remove(difficulty)
            else current.add(difficulty)
            filter.copy(selectedDifficulty = current)
        }
    }

    // Set max cooking time
    fun onCookingTimeSelect(maxMinutes: Int?) {
        _filter.update { it.copy(maxCookingTime = maxMinutes) }
    }

    // Set max calories
    fun onCaloriesSelect(maxCalories: Int?) {
        _filter.update { it.copy(maxCalories = maxCalories) }
    }

    // Toggle chỉ hiện yêu thích
    fun onFavoritesToggle() {
        _filter.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    // Xoá toàn bộ filter
    fun clearFilter() {
        _filter.update { SearchFilter() }
    }

    // Mở/đóng filter bottom sheet
    fun toggleFilterSheet() {
        _uiState.update { it.copy(isFilterSheetOpen = !it.isFilterSheetOpen) }
    }

    fun closeFilterSheet() {
        _uiState.update { it.copy(isFilterSheetOpen = false) }
    }

    // Toggle favorite một recipe
    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        safeLaunch {
            recipeRepository.toggleFavorite(recipeId, !isFavorite)
        }
    }
}