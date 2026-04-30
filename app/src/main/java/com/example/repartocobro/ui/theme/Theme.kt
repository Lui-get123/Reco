package com.example.repartocobro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FreshLightScheme = lightColorScheme(
    primary              = MintPrimary,
    onPrimary            = TextOnAccent,
    primaryContainer     = MintLight,
    onPrimaryContainer   = TextDark,
    secondary            = LavenderPrimary,
    onSecondary          = TextOnAccent,
    secondaryContainer   = LavenderLight,
    onSecondaryContainer = TextDark,
    tertiary             = YellowPrimary,
    onTertiary           = TextDark,
    background           = LightBackground,
    onBackground         = TextDark,
    surface              = LightSurface,
    onSurface            = TextDark,
    surfaceVariant       = LightSurfaceHigh,
    onSurfaceVariant     = TextMedium,
    error                = StatusError,
    onError              = TextOnAccent,
    outline              = SoftBorder,
    outlineVariant       = LightSurfaceHigh
)

@Composable
fun RepartoCobroTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = FreshLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LightBackground.toArgb()
            window.navigationBarColor = NavBarBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}