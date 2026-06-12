package com.example.smartsous.feature.home

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.data.repository.RecipeRepositoryImpl
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.SuggestMealsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val suggestedRecipes: List<SuggestedRecipe> = emptyList(),
    val allRecipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val isRecommending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeRepository: IRecipeRepository,
    private val pantryRepository: IPantryRepository,
    private val suggestMealsUseCase: SuggestMealsUseCase,
    private val recipeRepositoryImpl: RecipeRepositoryImpl,
    private val dataStoreManager: DataStoreManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private val pendingFavoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        observeSuggestions()
        refreshIfNeeded()
    }

    private fun observeSuggestions() {
        // Combine 2 Flow: recipes + pantry
        // Mỗi khi 1 trong 2 thay đổi → tính lại gợi ý
        combine(
            recipeRepository.getAllRecipes(),
            pantryRepository.getAllIngredients(),
            dataStoreManager.userPreferencesFlow,
            pendingFavoriteOverrides
        ) { recipes, pantryItems, preferences, favoriteOverrides ->
            val recipesWithFavorites = recipes.applyFavoriteOverrides(favoriteOverrides)
            _uiState.update { state ->
                state.copy(
                    allRecipes = recipesWithFavorites,
                    isLoading = false,
                    isRecommending = true
                )
            }
            val suggested = suggestMealsUseCase(
                allRecipes = recipesWithFavorites,
                pantryIngredients = pantryItems,
                userPreference = preferences,
                topN = 10
            )
            _uiState.update { state ->
                state.copy(
                    suggestedRecipes = suggested,
                    allRecipes = recipesWithFavorites,
                    isLoading = false,
                    isRecommending = false
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun refreshIfNeeded() {
        safeLaunch {
            recipeRepositoryImpl.refreshIfNeeded()
        }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        val newFavoriteValue = !isFavorite
        pendingFavoriteOverrides.update { overrides ->
            overrides + (recipeId to newFavoriteValue)
        }
        _uiState.update { state ->
            state.withFavorite(recipeId, newFavoriteValue)
        }
        safeLaunch {
            try {
                recipeRepository.toggleFavorite(recipeId, newFavoriteValue)
            } finally {
                pendingFavoriteOverrides.update { overrides ->
                    overrides - recipeId
                }
            }
        }
    }
}

private fun List<Recipe>.applyFavoriteOverrides(
    overrides: Map<String, Boolean>
): List<Recipe> =
    map { recipe ->
        overrides[recipe.id]?.let { recipe.copy(isFavorite = it) } ?: recipe
    }

private fun HomeUiState.withFavorite(recipeId: String, isFavorite: Boolean): HomeUiState =
    copy(
        allRecipes = allRecipes.map { recipe ->
            if (recipe.id == recipeId) recipe.copy(isFavorite = isFavorite) else recipe
        },
        suggestedRecipes = suggestedRecipes.map { suggested ->
            if (suggested.recipe.id == recipeId) {
                suggested.copy(recipe = suggested.recipe.copy(isFavorite = isFavorite))
            } else {
                suggested
            }
        }
    )
