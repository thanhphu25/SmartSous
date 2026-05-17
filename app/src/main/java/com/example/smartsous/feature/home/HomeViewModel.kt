package com.example.smartsous.feature.home

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.repository.RecipeRepositoryImpl
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import com.example.smartsous.domain.usecase.SuggestMealsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val suggestedRecipes: List<SuggestedRecipe> = emptyList(),
    val allRecipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeRepository: IRecipeRepository,
    private val pantryRepository: IPantryRepository,
    private val suggestMealsUseCase: SuggestMealsUseCase,
    private val recipeRepositoryImpl: RecipeRepositoryImpl
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeSuggestions()
        refreshIfNeeded()
    }

    private fun observeSuggestions() {
        // Combine 2 Flow: recipes + pantry
        // Mỗi khi 1 trong 2 thay đổi → tính lại gợi ý
        combine(
            recipeRepository.getAllRecipes(),
            pantryRepository.getAllIngredients()
        ) { recipes, pantryItems ->
            val suggested = suggestMealsUseCase(
                allRecipes = recipes,
                pantryIngredients = pantryItems,
                topN = 10
            )
            _uiState.update { state ->
                state.copy(
                    suggestedRecipes = suggested,
                    allRecipes = recipes,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshIfNeeded() {
        safeLaunch {
            recipeRepositoryImpl.refreshIfNeeded()
        }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        safeLaunch {
            recipeRepository.toggleFavorite(recipeId, !isFavorite)
        }
    }
}