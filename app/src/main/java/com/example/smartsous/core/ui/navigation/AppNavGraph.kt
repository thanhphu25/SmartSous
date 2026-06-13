package com.example.smartsous.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.smartsous.feature.pantry.BarcodeScanScreen
import com.example.smartsous.feature.pantry.PantryViewModel
import com.example.smartsous.feature.settings.SettingsScreen
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
                //modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onSearchClick = {
                    navController.navigate("search")
                },
                onNotificationNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }

        composable("search") {
            SearchScreen(
                //modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("planner") { PlannerScreen(/*modifier*/) }
        composable("pantry") {
            // Nhận tên sản phẩm từ barcode scan (nếu có)
            val scannedName = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<String>("scanned_name")

            val pantryViewModel: PantryViewModel = hiltViewModel()

            // Nếu có kết quả scan → tự mở sheet với tên đã điền
            LaunchedEffect(scannedName) {
                scannedName?.let { name ->
                    pantryViewModel.openAddSheetWithName(name)
                    // Xoá sau khi đọc
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("scanned_name")
                }
            }

            PantryScreen(
                //modifier = modifier,
                onNavigateToScan = {
                    navController.navigate("barcode_scan")
                },
                viewModel = pantryViewModel
            )
        }
        composable("favorites") {
            FavoritesScreen(
                //modifier = modifier,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                }
            )
        }

        composable("chat") {
            ChatScreen(
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onBack = { navController.popBackStack() }
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

        composable("barcode_scan") {
            BarcodeScanScreen(
                onBarcodeDetected = { productName ->
                    // Navigate về Pantry với tên sản phẩm
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_name", productName)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(/*modifier = modifier*/)
        }
    }
}
