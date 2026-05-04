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
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.smartsous.core.ui.navigation.AppNavGraph
import com.example.smartsous.core.ui.navigation.SmartSousBottomBar
import com.example.smartsous.ui.theme.SmartSousTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmartSousActivity : ComponentActivity() {

    // Launcher xin quyền notification
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("Permission",
            if (isGranted) "Notification permission granted"
            else "Notification permission denied"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Xin quyền notification ngay khi app mở
        requestNotificationPermission()

        setContent {
            SmartSousTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { SmartSousBottomBar(navController) }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        // Chỉ cần xin từ Android 13 trở lên
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