package com.hildebrandtdigital.wpcbroadsheet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand colours — identical in both themes ────────────────────────────────
val Primary     = Color(0xFF4F8EF7)
val PrimaryGlow = Color(0x404F8EF7)
val Secondary   = Color(0xFFF7A84F)
val Accent      = Color(0xFF4FF7C8)
val Danger      = Color(0xFFF74F6B)

// ── Surface colours used during crop screen (always dark) ──────────────────
val CropOverlay = Color(0xCC000000)

/**
 * All semantic colour tokens for the app.
 * One instance for dark mode, one for light mode.
 * Accessed anywhere via [LocalAppColors].current
 */
@Immutable
data class AppColors(
    val bgDeep        : Color,
    val bgGradientEnd : Color,    // second colour for the page background gradient
    val surface1      : Color,
    val surface2      : Color,
    val surface3      : Color,
    val textBright    : Color,
    val textMuted     : Color,
    val textDim       : Color,
    val borderColor   : Color,
    val cardShadow    : Color,    // drop-shadow tint (transparent in dark, visible in light)
    val headerShadow  : Color,    // drop shadow beneath the header band
    val navBackground : Color,    // bottom nav specific background
    val navBorder     : Color,    // top border line on bottom nav
    val headerStart   : Color,    // header gradient start colour
    val headerEnd     : Color,    // header gradient end colour
    val isDark        : Boolean,
)

val DarkAppColors = AppColors(
    bgDeep        = Color(0xFF0B0F1A),
    bgGradientEnd = Color(0xFF0B0F1A),  // flat in dark — no bg gradient
    surface1      = Color(0xFF131929),
    surface2      = Color(0xFF1C2540),
    surface3      = Color(0xFF243058),
    textBright    = Color(0xFFE8EEFF),
    textMuted     = Color(0xFF7A8BB0),
    textDim       = Color(0xFF4A5A80),
    borderColor   = Color(0x12FFFFFF),
    cardShadow    = Color(0x00000000),  // no shadow in dark mode
    headerShadow  = Color(0x00000000),  // no header shadow in dark
    navBackground = Color(0xFF131929),
    navBorder     = Color(0x18FFFFFF),
    headerStart   = Color(0xFF0B0F1A),  // same as bgDeep — dark header is bg colour
    headerEnd     = Color(0xFF0B0F1A),
    isDark        = true,
)

val LightAppColors = AppColors(
    // Background: near-white — clean, spacious, lets cards do the talking
    bgDeep        = Color(0xFFF5F7FA),  // near-white with barely-there cool tint
    bgGradientEnd = Color(0xFFF0F4FF),  // faint cool blue — used by appBackground gradient
    // Cards: pure white — maximum contrast against coloured stat cards
    surface1      = Color(0xFFFFFFFF),  // white cards
    surface2      = Color(0xFFF0F4FF),  // slightly tinted input backgrounds
    surface3      = Color(0xFFE8EEFF),  // deeper tint for nested surfaces
    // Text: deep navy on white backgrounds
    textBright    = Color(0xFF0D1B3E),  // deep navy — rich, not harsh black
    textMuted     = Color(0xFF3D5280),  // clear readable muted
    textDim       = Color(0xFF8096B8),  // placeholders
    borderColor   = Color(0x18000000),  // very subtle border — cards already have shadow
    // Shadows & nav
    cardShadow    = Color(0x3A1D4D9A),  // strong blue-tinted drop shadow
    headerShadow  = Color(0x551D4D9A),  // deeper shadow beneath header band
    navBackground = Color(0xFFFFFFFF),  // pure white nav
    navBorder     = Color(0x14000000),  // barely-there top line
    // Header: royal blue gradient
    headerStart   = Color(0xFF0A3880),  // very deep royal blue
    headerEnd     = Color(0xFF1565C0),  // bright royal blue
    isDark        = false,
)

/**
 * CompositionLocal that provides the current [AppColors] to any composable
 * in the tree. Read with [LocalAppColors].current
 */
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ── Legacy top-level vals (dark values) used by Typography.kt ───────────────
val BgDeep      = DarkAppColors.bgDeep
val Surface1    = DarkAppColors.surface1
val Surface2    = DarkAppColors.surface2
val Surface3    = DarkAppColors.surface3
val TextBright  = DarkAppColors.textBright
val TextMuted   = DarkAppColors.textMuted
val TextDim     = DarkAppColors.textDim
val BorderColor = DarkAppColors.borderColor
