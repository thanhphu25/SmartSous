package com.example.smartsous.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : Screen("home", "Trang chủ", { Icon(Icons.Default.Home, contentDescription = null) })
    object Pantry : Screen("pantry", "Tủ lạnh", { Icon(Icons.Default.Kitchen, contentDescription = null) })
    object Planner : Screen("planner", "Kế hoạch", { Icon(Icons.Default.Schedule, contentDescription = null) })
    object Favorites : Screen(
        "favorites",
        "Yêu thích",
        { Icon(Icons.Default.Favorite, contentDescription = null) }
    )
    object Settings : Screen(
        "settings",
        "Cá nhân",
        { Icon(Icons.Default.Person, contentDescription = null) }
    )
}

@Composable
fun SmartSousBottomBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Pantry,
        Screen.Planner,
        Screen.Favorites,
        Screen.Settings
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = screen.icon,
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    if (currentDestination?.route == screen.route) return@NavigationBarItem

                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
