package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppThemePreference
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ThemePreferences
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = remember { ThemePreferences(context) }

    val currentTheme by prefs.themePreference
        .collectAsStateWithLifecycle(initialValue = AppThemePreference.DARK)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground(c)
            
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .headerBand(c)
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        ) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back",
                        tint = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Appearance",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White
                        ))
                    Text("Choose your preferred theme",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)
                        ))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Theme cards ────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Text(
                "THEME",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = c.textMuted,
                    letterSpacing = 1.2.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )

            // Dark + Light side by side
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeCard(
                    preference = AppThemePreference.DARK,
                    selected   = currentTheme == AppThemePreference.DARK,
                    modifier   = Modifier.weight(1f),
                    onClick    = { scope.launch { prefs.setTheme(AppThemePreference.DARK) } },
                )
                ThemeCard(
                    preference = AppThemePreference.LIGHT,
                    selected   = currentTheme == AppThemePreference.LIGHT,
                    modifier   = Modifier.weight(1f),
                    onClick    = { scope.launch { prefs.setTheme(AppThemePreference.LIGHT) } },
                )
            }

            Spacer(Modifier.height(12.dp))

            // System default — full width
            ThemeCard(
                preference = AppThemePreference.SYSTEM,
                selected   = currentTheme == AppThemePreference.SYSTEM,
                modifier   = Modifier.fillMaxWidth(),
                onClick    = { scope.launch { prefs.setTheme(AppThemePreference.SYSTEM) } },
                fullWidth  = true,
            )

            Spacer(Modifier.height(28.dp))

            // ── Info card ──────────────────────────────────────────────────────
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = c.surface1,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.borderColor, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("💡", fontSize = 18.sp)
                    Column {
                        Text(
                            "Theme applies instantly",
                            style = MaterialTheme.typography.labelLarge.copy(color = c.textBright, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your selection is saved and will be remembered the next time you open the app. " +
                            "System Default follows your device's dark/light mode setting.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Theme preview card ─────────────────────────────────────────────────────────

@Composable
private fun ThemeCard(
    preference: AppThemePreference,
    selected  : Boolean,
    modifier  : Modifier = Modifier,
    fullWidth : Boolean  = false,
    onClick   : () -> Unit,
) {
    val c = LocalAppColors.current

    // Animate the selection border colour
    val borderAnim by animateColorAsState(
        targetValue = if (selected) Primary else c.borderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "border",
    )
    val bgAnim by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.08f) else c.surface1,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bg",
    )

    // Colours used inside the preview — always fixed, not reactive
    val previewColors = when (preference) {
        AppThemePreference.DARK   -> DarkAppColors
        AppThemePreference.LIGHT  -> LightAppColors
        AppThemePreference.SYSTEM -> if (c.isDark) DarkAppColors else LightAppColors
    }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = bgAnim,
        modifier = modifier
            .cardElevation(c)
            .border(2.dp, borderAnim, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Mini app preview ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (fullWidth) 80.dp else 120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (previewColors.isDark)
                            Brush.linearGradient(listOf(previewColors.bgDeep, previewColors.bgDeep))
                        else
                            Brush.linearGradient(listOf(previewColors.bgDeep, previewColors.bgDeep))
                    ),
            ) {
                if (fullWidth) {
                    // System: show half dark half light
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(DarkAppColors.bgDeep)
                        ) { MiniAppContent(DarkAppColors, compact = true) }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(LightAppColors.bgDeep)
                        ) { MiniAppContent(LightAppColors, compact = true) }
                    }
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(0.2f))
                            .align(Alignment.Center)
                    )
                } else {
                    MiniAppContent(previewColors)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Label row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "${preference.emoji}  ${preference.label}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (selected) Primary else c.textBright,
                        ),
                    )
                    if (preference == AppThemePreference.SYSTEM) {
                        Text(
                            "Follows device setting",
                            style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted),
                        )
                    }
                }
                if (selected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Primary),
                    ) {
                        Icon(Icons.Rounded.Check, null,
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** Mini fake UI rendered inside the preview card. */
@Composable
private fun MiniAppContent(colors: AppColors, compact: Boolean = false) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground(c)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Fake header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(width = if (compact) 20.dp else 32.dp, height = 5.dp)
                .clip(RoundedCornerShape(2.dp)).background(Primary))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                .background(colors.surface2))
        }

        Spacer(Modifier.height(2.dp))

        // Fake stat cards row
        if (!compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier.weight(1f).height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surface1)
                            .border(0.5.dp, colors.borderColor, RoundedCornerShape(4.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.5.dp)
                            .background(if (it == 0) Primary else Accent))
                        Box(modifier = Modifier
                            .padding(top = 5.dp, start = 4.dp)
                            .size(width = 16.dp, height = 3.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(colors.textDim))
                        Box(modifier = Modifier
                            .padding(top = 11.dp, start = 4.dp)
                            .size(width = 10.dp, height = 4.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (it == 0) Primary.copy(0.7f) else Accent.copy(0.7f)))
                    }
                }
            }
        }

        // Fake list rows
        val rowCount = if (compact) 2 else 3
        repeat(rowCount) { idx ->
            Box(
                modifier = Modifier.fillMaxWidth().height(if (compact) 8.dp else 11.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surface1)
                    .border(0.5.dp, colors.borderColor, RoundedCornerShape(3.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(1.dp))
                        .background(when(idx) { 0 -> Primary; 1 -> Secondary; else -> Accent }))
                    Box(modifier = Modifier
                        .size(width = (8 + idx * 5).dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.textDim))
                }
            }
        }

        // Fake bottom nav
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(colors.surface1),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { idx ->
                Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                    .background(if (idx == 0) Primary else colors.textDim))
            }
        }
    }
}
