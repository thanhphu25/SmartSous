package com.example.smartsous.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.example.smartsous.feature.home.HomeScreen
import com.example.smartsous.feature.chatbot.ChatScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(modifier = modifier)
        }
        composable("pantry") {
            Text(text = "Pantry Screen")
        }
        composable("search") {
            Text(text = "Search Screen")
        }
        composable("planner") {
            Text(text = "Planner Screen")
        }
        composable("favorites") {
            ChatScreen(modifier = modifier)
        }
    }
}
