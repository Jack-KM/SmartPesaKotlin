package com.example.smartpesa.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.smartpesa.data.preferences.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary = AppAccentIndigo,
    onPrimary = AppPrimaryText,
    primaryContainer = AppSurfaceAlt,
    onPrimaryContainer = AppPrimaryText,
    secondary = AppExpenseRose,
    onSecondary = AppBackground,
    secondaryContainer = AppSurfaceSoft,
    onSecondaryContainer = AppPrimaryText,
    tertiary = AppIncomeGreen,
    onTertiary = AppBackground,
    tertiaryContainer = AppSurfaceSoft,
    onTertiaryContainer = AppPrimaryText,
    error = AppDangerRed,
    onError = AppPrimaryText,
    background = AppBackground,
    onBackground = AppPrimaryText,
    surface = AppSurface,
    onSurface = AppPrimaryText,
    surfaceVariant = AppSurfaceAlt,
    onSurfaceVariant = AppSecondaryText,
    outline = AppDivider,
    outlineVariant = AppDivider
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentIndigo,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceAlt,
    onPrimaryContainer = LightPrimaryText,
    secondary = LightExpenseRose,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceAlt,
    onSecondaryContainer = LightPrimaryText,
    tertiary = LightIncomeGreen,
    onTertiary = LightSurface,
    tertiaryContainer = LightSurfaceAlt,
    onTertiaryContainer = LightPrimaryText,
    error = LightDangerRed,
    onError = LightSurface,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightSecondaryText,
    outline = LightDivider,
    outlineVariant = LightDivider
)

@Composable
fun SmartPesaTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val background = colorScheme.background.toArgb()
            window.statusBarColor = background
            window.navigationBarColor = background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
