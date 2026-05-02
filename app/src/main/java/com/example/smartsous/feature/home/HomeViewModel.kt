package com.example.smartsous.feature.home

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.data.repository.RecipeRepositoryImpl
import com.example.smartsous.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRecipes()
        refreshIfNeeded()
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            recipeRepository.getAllRecipes().collect { recipes ->
                _uiState.update { it.copy(recipes = recipes, isLoading = false) }
            }
        }
    }

    private fun refreshIfNeeded() {
        safeLaunch {
            _uiState.update { it.copy(isLoading = true) }
            recipeRepository.refreshIfNeeded()
        }
    }
}