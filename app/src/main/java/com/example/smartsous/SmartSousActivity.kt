package com.example.smartsous

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartsous.core.ui.navigation.AppNavGraph
import com.example.smartsous.core.ui.navigation.SmartSousBottomBar
import com.example.smartsous.ui.theme.SmartSousTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize

@AndroidEntryPoint
class SmartSousActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            SmartSousTheme {
                val navController = rememberNavController()
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route

                // Các route KHÔNG hiện bottom bar (Đã giữ nguyên logic mới của bạn)
                val hideBottomBarRoutes = listOf("splash", "onboarding", "recipe/{recipeId}", "chat")
                val showBottomBar = hideBottomBarRoutes.none { route ->
                    currentRoute == route || currentRoute?.startsWith("recipe/") == true
                }

                // Khai báo danh sách các màn hình muốn hiển thị nút Chatbot AI
                val showFab = currentRoute in listOf("home", "search", "planner", "favorites", "pantry")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            SmartSousBottomBar(navController)
                        }
                    },
                    // THÊM NÚT CHATBOT AI VÀO ĐÂY
                    floatingActionButton = {
                        if (showFab) {
                            FloatingActionButton(
                                onClick = {
                                    // Chuyển hướng sang màn hình chat
                                    navController.navigate("chat")
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Mở trợ lý AI"
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
}