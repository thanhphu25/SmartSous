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

    private val _filter = MutableStateFlow(SearchFilter())

    init {
        observeSearch()
    }

    private fun observeSearch() {
        combine(
            recipeRepository.getAllRecipes(),
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

    fun onQueryChange(query: String) {
        updateFilter { it.copy(query = query) }
    }

    fun onCuisineToggle(cuisine: String) {
        updateFilter { filter ->
            val current = filter.selectedCuisines.toMutableSet()
            if (cuisine in current) current.remove(cuisine) else current.add(cuisine)
            filter.copy(selectedCuisines = current)
        }
    }

    fun onDifficultyToggle(difficulty: Difficulty) {
        updateFilter { filter ->
            val current = filter.selectedDifficulty.toMutableSet()
            if (difficulty in current) current.remove(difficulty) else current.add(difficulty)
            filter.copy(selectedDifficulty = current)
        }
    }

    fun onCookingTimeSelect(maxMinutes: Int?) {
        updateFilter { it.copy(maxCookingTime = maxMinutes) }
    }

    fun onCaloriesSelect(maxCalories: Int?) {
        updateFilter { it.copy(maxCalories = maxCalories) }
    }

    fun onFavoritesToggle() {
        updateFilter { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun clearFilter() {
        updateFilter { SearchFilter() }
    }

    fun toggleFilterSheet() {
        _uiState.update { it.copy(isFilterSheetOpen = !it.isFilterSheetOpen) }
    }

    fun closeFilterSheet() {
        _uiState.update { it.copy(isFilterSheetOpen = false) }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        safeLaunch {
            recipeRepository.toggleFavorite(recipeId, !isFavorite)
        }
    }

    private fun updateFilter(transform: (SearchFilter) -> SearchFilter) {
        val updatedFilter = transform(_filter.value)
        _filter.value = updatedFilter
        _uiState.update { state ->
            state.copy(filter = updatedFilter)
        }
    }
}
