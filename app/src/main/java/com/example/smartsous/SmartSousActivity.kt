package com.example.smartsous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.smartsous.core.ui.navigation.AppNavGraph
import com.example.smartsous.core.ui.navigation.SmartSousBottomBar
import com.example.smartsous.ui.theme.SmartSousTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint                // ← bắt buộc với Hilt
class SmartSousActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}