package com.watermark.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val BrandBlue = Color(0xFF2563EB)
val BrandBlueLight = Color(0xFF60A5FA)
val SurfaceLight = Color(0xFFFAFAFA)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDbeafe),
    onPrimaryContainer = Color(0xFF1e40af),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    surface = SurfaceLight,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF1F5F9),
    background = Color.White,
    onBackground = Color(0xFF1F2937),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color(0xFF1e3a8a),
    primaryContainer = Color(0xFF1e40af),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF9FAFB),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF9FAFB),
)

@Composable
fun WatermarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
