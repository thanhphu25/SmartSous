package com.example.smartsous

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartsous.core.ui.navigation.AppNavGraph
import com.example.smartsous.core.ui.navigation.SmartSousBottomBar
import com.example.smartsous.ui.theme.SmartSousTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmartSousActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"

        private val NOTIFICATION_ROUTES = setOf(
            "pantry",
            "planner"
        )
    }

    private var pendingNotificationRoute by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        pendingNotificationRoute = intent.extractNotificationRoute()

        setContent {
            SmartSousTheme {
                val navController = rememberNavController()
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route

                LaunchedEffect(pendingNotificationRoute) {
                    val route = pendingNotificationRoute ?: return@LaunchedEffect
                    if (route in NOTIFICATION_ROUTES) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    pendingNotificationRoute = null
                }

                val hideBottomBarRoutes = listOf("splash", "onboarding", "recipe/{recipeId}")
                val showBottomBar = hideBottomBarRoutes.none { route ->
                    currentRoute == route || currentRoute?.startsWith("recipe/") == true
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            SmartSousBottomBar(navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationRoute = intent.extractNotificationRoute()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun Intent?.extractNotificationRoute(): String? =
        this?.getStringExtra(EXTRA_NAVIGATE_TO)
}
