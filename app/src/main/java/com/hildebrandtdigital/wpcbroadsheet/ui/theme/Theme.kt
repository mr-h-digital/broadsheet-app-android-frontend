package com.hildebrandtdigital.wpcbroadsheet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppThemePreference

// ── Material3 Dark scheme ─────────────────────────────────────────────────────
private val WpcDarkColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = PrimaryGlow,
    secondary            = Secondary,
    onSecondary          = Color.Black,
    tertiary             = Accent,
    onTertiary           = Color.Black,
    error                = Danger,
    background           = DarkAppColors.bgDeep,
    onBackground         = DarkAppColors.textBright,
    surface              = DarkAppColors.surface1,
    onSurface            = DarkAppColors.textBright,
    surfaceVariant       = DarkAppColors.surface2,
    onSurfaceVariant     = DarkAppColors.textMuted,
    outline              = DarkAppColors.borderColor,
    // Text hierarchy — bodyMedium / bodySmall / labelSmall resolve from these
    outlineVariant       = DarkAppColors.textDim,
    inverseSurface       = DarkAppColors.textMuted,
)

// ── Material3 Light scheme ────────────────────────────────────────────────────
private val WpcLightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Primary.copy(alpha = 0.12f),
    secondary            = Secondary,
    onSecondary          = Color.White,
    tertiary             = Accent,
    onTertiary           = Color.Black,
    error                = Danger,
    background           = LightAppColors.bgDeep,  // #F5F7FA — near-white
    onBackground         = LightAppColors.textBright,
    surface              = LightAppColors.surface1,
    onSurface            = LightAppColors.textBright,
    surfaceVariant       = LightAppColors.surface2,
    onSurfaceVariant     = LightAppColors.textMuted,
    outline              = LightAppColors.borderColor,
    outlineVariant       = LightAppColors.textDim,
    inverseSurface       = LightAppColors.textMuted,
)

@Composable
fun WPCBroadsheetTheme(
    themePreference: AppThemePreference = AppThemePreference.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark  = isSystemInDarkTheme()
    val useDark = when (themePreference) {
        AppThemePreference.DARK   -> true
        AppThemePreference.LIGHT  -> false
        AppThemePreference.SYSTEM -> systemDark
    }

    val appColors   = if (useDark) DarkAppColors else LightAppColors
    val colorScheme = if (useDark) WpcDarkColorScheme else WpcLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val bgColor = if (useDark) 0xFF0B0F1A.toInt() else 0xFF0A3880.toInt()
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor     = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // Set the window/decor background to match our theme base colour
            // so there is no black flash behind Scaffold or during transitions
            window.decorView.setBackgroundColor(bgColor)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = WpcTypography,
            content     = content,
        )
    }
}
