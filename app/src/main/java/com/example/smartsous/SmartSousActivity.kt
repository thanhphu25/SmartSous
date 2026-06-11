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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

    @javax.inject.Inject
    lateinit var ingredientDao: com.example.smartsous.data.local.dao.IngredientDao

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
        
        lifecycleScope.launch {
            if (ingredientDao.getAllOnce().size < 50) {
                val dummyData = listOf(
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_1", name = "Thịt bò", quantity = 500.0, unit = "g", category = "MEAT", expiryDate = "2026-06-20", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_2", name = "Thịt lợn", quantity = 1000.0, unit = "g", category = "MEAT", expiryDate = "2026-06-18", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_3", name = "Thịt gà", quantity = 800.0, unit = "g", category = "MEAT", expiryDate = "2026-06-19", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_4", name = "Cá hồi", quantity = 400.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-13", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_5", name = "Tôm sú", quantity = 500.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-12", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_6", name = "Mực", quantity = 300.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_7", name = "Trứng gà", quantity = 20.0, unit = "quả", category = "DAIRY", expiryDate = "2026-06-30", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_8", name = "Sữa tươi", quantity = 2.0, unit = "lít", category = "DAIRY", expiryDate = "2026-06-20", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_9", name = "Phô mai", quantity = 200.0, unit = "g", category = "DAIRY", expiryDate = "2026-08-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_10", name = "Bơ", quantity = 100.0, unit = "g", category = "DAIRY", expiryDate = "2026-07-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_11", name = "Cà rốt", quantity = 500.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-25", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_12", name = "Hành tây", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_13", name = "Cải thảo", quantity = 800.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-18", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_14", name = "Rau muống", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_15", name = "Khoai tây", quantity = 1000.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-07-20", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_16", name = "Cà chua", quantity = 500.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-17", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_17", name = "Tỏi", quantity = 100.0, unit = "g", category = "SPICE", expiryDate = "2026-09-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_18", name = "Hành lá", quantity = 50.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_19", name = "Gừng", quantity = 100.0, unit = "g", category = "SPICE", expiryDate = "2026-08-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_20", name = "Nước mắm", quantity = 500.0, unit = "ml", category = "SPICE", expiryDate = "2027-06-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_21", name = "Nước tương", quantity = 500.0, unit = "ml", category = "SPICE", expiryDate = "2027-06-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_22", name = "Dầu ăn", quantity = 1000.0, unit = "ml", category = "SPICE", expiryDate = "2027-01-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_23", name = "Gạo", quantity = 10.0, unit = "kg", category = "GRAIN", expiryDate = "2026-12-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_24", name = "Bún", quantity = 500.0, unit = "g", category = "GRAIN", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_25", name = "Mì tôm", quantity = 10.0, unit = "gói", category = "GRAIN", expiryDate = "2026-12-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_26", name = "Mực", quantity = 300.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_27", name = "Tôm càng", quantity = 500.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_28", name = "Nấm rơm", quantity = 200.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_29", name = "Cà chua bi", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-20", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_30", name = "Sả", quantity = 100.0, unit = "g", category = "SPICE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_31", name = "Riềng", quantity = 100.0, unit = "g", category = "SPICE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_32", name = "Ớt", quantity = 50.0, unit = "g", category = "SPICE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_33", name = "Ngò gai", quantity = 50.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_34", name = "Lá chanh", quantity = 50.0, unit = "g", category = "SPICE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_35", name = "Xương gà", quantity = 1000.0, unit = "g", category = "MEAT", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_36", name = "Thơm", quantity = 500.0, unit = "g", category = "FRUIT", expiryDate = "2026-06-18", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_37", name = "Cá chép", quantity = 1000.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-13", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_38", name = "Đậu bắp", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-17", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_39", name = "Bào ngư", quantity = 500.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_40", name = "Sò điệp khô", quantity = 200.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-12-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_41", name = "Gà ta", quantity = 1500.0, unit = "g", category = "MEAT", expiryDate = "2026-06-15", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_42", name = "Giò heo", quantity = 1000.0, unit = "g", category = "MEAT", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_43", name = "Quả vả", quantity = 500.0, unit = "g", category = "FRUIT", expiryDate = "2026-06-20", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_44", name = "Ốc bươu", quantity = 1000.0, unit = "g", category = "SEAFOOD", expiryDate = "2026-06-13", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_45", name = "Rau đay", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_46", name = "Mồng tơi", quantity = 300.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-14", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_47", name = "Mướp hương", quantity = 500.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-06-18", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_48", name = "Bí đỏ", quantity = 800.0, unit = "g", category = "VEGETABLE", expiryDate = "2026-07-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_49", name = "Măng khô", quantity = 500.0, unit = "g", category = "VEGETABLE", expiryDate = "2027-06-11", addedDate = "2026-06-11"),
                com.example.smartsous.data.local.entity.IngredientEntity(id = "ing_50", name = "Mực khô", quantity = 500.0, unit = "g", category = "SEAFOOD", expiryDate = "2027-06-11", addedDate = "2026-06-11")
            )
            dummyData.forEach { ingredientDao.upsert(it) }
            }
        }

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

                val hideBottomBarRoutes = listOf("splash", "onboarding", "recipe/{recipeId}", "chat")
                val showBottomBar = hideBottomBarRoutes.none { route ->
                    currentRoute == route || currentRoute?.startsWith("recipe/") == true
                }
                val showFab = currentRoute in listOf("home", "search", "planner", "favorites", "pantry")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            SmartSousBottomBar(navController)
                        }
                    },
                    floatingActionButton = {
                        if (showFab) {
                            FloatingActionButton(
                                onClick = { navController.navigate("chat") },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Mo tro ly AI"
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
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
