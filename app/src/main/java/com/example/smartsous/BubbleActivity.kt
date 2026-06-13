package com.example.smartsous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.smartsous.feature.chatbot.ChatScreen
import com.example.smartsous.ui.theme.SmartSousTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartSousTheme {
                ChatScreen(
                    modifier = Modifier.fillMaxSize(),
                    isBubble = true,
                    onBack = { finish() },
                    onRecipeClick = { /* Trong bubble mode, không điều hướng */ }
                )
            }
        }
    }
}
