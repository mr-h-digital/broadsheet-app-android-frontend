package com.hildebrandtdigital.wpcbroadsheet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.R
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors

/**
 * Unified WPC logo + wordmark lockup.
 *
 * HERO     → Login screen: large 88dp logo, stacked layout, glowing ring
 * STANDARD → Dashboard header: 36dp logo + "WPC. / Broadsheet" wordmark
 * COMPACT  → Secondary-screen top-bars: 28dp logo + wordmark
 * ICON     → Tight spots (capture top-right): 26dp logo circle only
 */
enum class WpcBrandingSize { HERO, STANDARD, COMPACT, ICON }

@Composable
fun WpcLogoBadge(
    size    : WpcBrandingSize = WpcBrandingSize.STANDARD,
    modifier: Modifier        = Modifier,
) {
    val c = LocalAppColors.current
    when (size) {
        WpcBrandingSize.HERO     -> HeroBranding(modifier)
        WpcBrandingSize.STANDARD -> InlineBranding(36.dp, 18f, 11f, modifier)
        WpcBrandingSize.COMPACT  -> InlineBranding(28.dp, 14f, 10f, modifier)
        WpcBrandingSize.ICON     -> LogoCircle(26.dp, modifier)
    }
}

// ── HERO — Login screen ───────────────────────────────────────────────────────
// Horizontal layout: logo circle (88dp glow ring) | "WPC." + "Broadsheet Manager"

@Composable
private fun HeroBranding(modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Logo with gradient glow ring
        Box(contentAlignment = Alignment.Center) {
            // Outer glow halo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x554F8EF7),
                                Color(0x22A04FF7),
                                Color.Transparent,
                            )
                        )
                    )
            )
            // Logo image with thin accent ring
            Image(
                painter            = painterResource(id = R.drawable.wpc_logo),
                contentDescription = "WPC Broadsheet logo",
                modifier           = Modifier
                    .size(88.dp)
                    .shadow(12.dp, CircleShape, ambientColor = Color(0x554F8EF7))
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0x994F8EF7),
                                Color(0x99A04FF7),
                                Color(0x994FF7C8),
                                Color(0x994F8EF7),
                            )
                        ),
                        shape = CircleShape,
                    )
            )
        }

        // Wordmark
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text  = "WPC.",
                style = MaterialTheme.typography.displayLarge.copy(
                    color         = c.textBright,
                    fontWeight    = FontWeight.ExtraBold,
                    lineHeight    = 48.sp,
                    letterSpacing = (-1).sp,
                ),
            )
            Text(
                text  = "Broadsheet Manager",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color         = c.textMuted,
                    fontSize      = 16.sp,
                    letterSpacing = 0.2.sp,
                ),
            )
        }
    }
}

// ── INLINE — Standard (dashboard) and Compact (secondary screens) ─────────────
// Horizontal: logo circle | "WPC." + "Broadsheet"

@Composable
private fun InlineBranding(
    logoSize    : Dp,
    titleSizeSp : Float,
    subSizeSp   : Float,
    modifier    : Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // Logo circle with subtle glow border
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter            = painterResource(id = R.drawable.wpc_logo),
                contentDescription = null,
                modifier           = Modifier
                    .size(logoSize)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0x664F8EF7),
                                Color(0x66A04FF7),
                                Color(0x664FF7C8),
                                Color(0x664F8EF7),
                            )
                        ),
                        shape = CircleShape,
                    )
            )
        }

        // Wordmark
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text  = "WPC.",
                style = MaterialTheme.typography.labelLarge.copy(
                    color         = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = titleSizeSp.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight    = (titleSizeSp + 1f).sp,
                ),
            )
            Text(
                text  = "Broadsheet",
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f),
                    fontSize      = subSizeSp.sp,
                    letterSpacing = 0.4.sp,
                    lineHeight    = (subSizeSp + 2f).sp,
                ),
            )
        }
    }
}

// ── ICON ONLY — tight spaces where wordmark would crowd ───────────────────────

@Composable
private fun LogoCircle(
    size    : Dp,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Image(
        painter            = painterResource(id = R.drawable.wpc_logo),
        contentDescription = "WPC",
        modifier           = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0x554F8EF7),
                        Color(0x55A04FF7),
                        Color(0x554FF7C8),
                        Color(0x554F8EF7),
                    )
                ),
                shape = CircleShape,
            )
    )
}
