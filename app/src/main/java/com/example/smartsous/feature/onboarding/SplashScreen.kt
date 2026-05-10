package com.example.smartsous.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Purple50
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    // Animation scale + alpha cho logo
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo xuất hiện dần
        scale.animateTo(1f, animationSpec = tween(600))
        alpha.animateTo(1f, animationSpec = tween(400))

        // Đợi 1 giây sau khi hiện logo
        delay(1000)

        // Check đã xem onboarding chưa rồi navigate
        if (viewModel.isOnboardingDone()) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo box
        Column(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Purple400),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🍳",
                fontSize = 52.sp
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Tên app
        Text(
            text = "SmartSous",
            modifier = Modifier.alpha(alpha.value),
            style = MaterialTheme.typography.headlineLarge,
            color = Purple400,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = "Trợ lý nấu ăn thông minh",
            modifier = Modifier.alpha(alpha.value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}