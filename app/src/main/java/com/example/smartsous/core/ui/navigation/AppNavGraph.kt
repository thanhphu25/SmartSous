package com.example.smartsous.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartsous.feature.chatbot.ChatScreen
import com.example.smartsous.feature.home.HomeScreen
import com.example.smartsous.feature.onboarding.OnboardingScreen
import com.example.smartsous.feature.onboarding.SplashScreen
import com.example.smartsous.feature.pantry.PantryScreen
import com.example.smartsous.feature.planner.PlannerScreen
import com.example.smartsous.feature.search.SearchScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        // Splash là điểm bắt đầu
        startDestination = "splash",
        modifier = modifier
    ) {
        // Splash — không có bottom bar
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

        // Onboarding — không có bottom bar
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // 5 tab chính
        composable("home") {
            HomeScreen(modifier = modifier)
        }
        composable("search") {
            SearchScreen(modifier = modifier)
        }
        composable("planner") {
            PlannerScreen(modifier = modifier)
        }
        composable("pantry") {
            PantryScreen(modifier = modifier)
        }
        composable("favorites") {
            ChatScreen(modifier = modifier)
        }
    }
}