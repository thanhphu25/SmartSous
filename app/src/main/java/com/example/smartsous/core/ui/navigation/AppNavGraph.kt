package com.example.smartsous.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smartsous.feature.chatbot.ChatScreen
import com.example.smartsous.feature.home.HomeScreen
import com.example.smartsous.feature.onboarding.OnboardingScreen
import com.example.smartsous.feature.onboarding.SplashScreen
import com.example.smartsous.feature.pantry.PantryScreen
import com.example.smartsous.feature.planner.PlannerScreen
import com.example.smartsous.feature.recipedetail.RecipeDetailScreen
import com.example.smartsous.feature.search.SearchScreen
import com.example.smartsous.feature.favorites.FavoritesScreen
import com.example.smartsous.feature.pantry.PantryScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                }
            )
        }

        composable("search") {
            SearchScreen(
                modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                }
            )
        }

        composable("planner") { PlannerScreen(modifier) }
        composable("pantry") {
            PantryScreen(modifier = modifier)
        }
        composable("favorites") {
            FavoritesScreen(
                modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                }
            )
        }

        // Route RecipeDetail — nhận recipeId từ argument
        composable(
            route = "recipe/{recipeId}",
            arguments = listOf(
                navArgument("recipeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
            RecipeDetailScreen(
                recipeId = recipeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}