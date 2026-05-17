package com.example.smartsous.feature.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.domain.model.Recipe
import com.example.smartsous.domain.repository.IRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: IRecipeRepository,
    savedStateHandle: SavedStateHandle   // tự inject recipeId từ navigation argument
) : BaseViewModel() {

    // Lấy recipeId từ navigation argument
    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRecipe()
    }

    private fun loadRecipe() {
        viewModelScope.launch {
            try {
                val recipe = recipeRepository.getRecipeById(recipeId)
                _uiState.update { state ->
                    state.copy(
                        recipe = recipe,
                        isLoading = false,
                        error = if (recipe == null) "Không tìm thấy món ăn" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun toggleFavorite() {
        val recipe = _uiState.value.recipe ?: return
        safeLaunch {
            recipeRepository.toggleFavorite(recipe.id, !recipe.isFavorite)
            // Reload để UI cập nhật trạng thái favorite
            loadRecipe()
        }
    }
}