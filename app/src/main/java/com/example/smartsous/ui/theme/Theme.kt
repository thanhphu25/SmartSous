package com.example.smartsous.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// ── Light Color Scheme ────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Purple400,
    onPrimary        = White,
    primaryContainer = Purple50,
    onPrimaryContainer = Purple800,

    secondary        = Teal400,
    onSecondary      = White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal800,

    tertiary         = Coral400,
    onTertiary       = White,
    tertiaryContainer = Coral50,

    error            = Coral400,
    onError          = White,
    errorContainer   = Coral50,

    background       = Gray50,
    onBackground     = Gray900,

    surface          = White,
    onSurface        = Gray800,
    surfaceVariant   = Gray50,
    onSurfaceVariant = Gray400,

    outline          = Gray100,
    outlineVariant   = Gray50,
)

// ── Dark Color Scheme ─────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Purple100,
    onPrimary        = Purple800,
    primaryContainer = Purple600,
    onPrimaryContainer = Purple50,

    secondary        = Teal50,
    onSecondary      = Teal800,
    secondaryContainer = Teal600,
    onSecondaryContainer = Teal50,

    tertiary         = Coral50,
    onTertiary       = Coral600,

    error            = Coral50,
    onError          = Coral600,

    background       = SurfaceDark,
    onBackground     = Gray50,

    surface          = CardDark,
    onSurface        = Gray50,
    surfaceVariant   = Color(0xFF2C2B42),
    onSurfaceVariant = Gray100,

    outline          = Gray800,
)

// ── SmartSousTheme ────────────────────────────────────────
@Composable
fun SmartSousTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color: Android 12+ tự lấy màu từ wallpaper
    // Tắt đi để giữ brand color nhất quán
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Đổi màu status bar theo theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SmartSousTypography,
        content     = content
    )
}