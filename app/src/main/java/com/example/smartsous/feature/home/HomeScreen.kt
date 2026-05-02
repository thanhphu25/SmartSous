package com.example.smartsous.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.core.ui.components.RecipeCard
import com.example.smartsous.core.ui.components.RecipeListSkeleton

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading && uiState.recipes.isEmpty()) {
        // Hiện skeleton khi đang tải lần đầu
        RecipeListSkeleton()
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
        ) {
            items(uiState.recipes) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) },
                    onFavoriteClick = {},
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                )
            }
        }
    }
}