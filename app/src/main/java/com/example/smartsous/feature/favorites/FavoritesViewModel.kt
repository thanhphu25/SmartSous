package com.example.smartsous.feature.favorites

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Recipe> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val recipeRepository: IRecipeRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        recipeRepository.getFavorites()
            .onEach { favorites ->
                _uiState.update { state ->
                    state.copy(
                        favorites = favorites,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun removeFavorite(recipeId: String) {
        safeLaunch {
            recipeRepository.toggleFavorite(recipeId, false)
        }
    }
}