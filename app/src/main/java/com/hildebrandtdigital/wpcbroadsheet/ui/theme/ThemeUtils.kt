package com.hildebrandtdigital.wpcbroadsheet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Page background:
 *  Light — near-white solid. The bold gradient cards carry all the colour.
 *  Dark  — solid bgDeep (unchanged).
 */
@Composable
fun Modifier.appBackground(c: AppColors = LocalAppColors.current): Modifier =
    this.background(c.bgDeep)

/**
 * Standard white card elevation in light mode — rich blue-tinted drop shadow.
 * Dark mode: no-op.
 */
@Composable
fun Modifier.cardElevation(
    c         : AppColors = LocalAppColors.current,
    elevation : Dp        = 12.dp,
    cornerDp  : Dp        = 16.dp,
): Modifier =
    if (c.isDark) this
    else this.shadow(
        elevation    = elevation,
        shape        = RoundedCornerShape(cornerDp),
        ambientColor = c.cardShadow,
        spotColor    = c.cardShadow,
    )

/**
 * Bold gradient card for stat/hero cards in light mode.
 * Applies a vivid gradient background + stronger drop shadow so the card
 * lifts dramatically off the near-white page.
 * Dark mode: falls back to solid surface colour.
 */
@Composable
fun Modifier.gradientCard(
    startColor : Color,
    endColor   : Color,
    c          : AppColors = LocalAppColors.current,
    elevation  : Dp        = 14.dp,
    cornerDp   : Dp        = 16.dp,
): Modifier {
    val shadow = startColor.copy(alpha = 0.38f)
    return if (c.isDark) {
        this.background(c.surface1, RoundedCornerShape(cornerDp))
    } else {
        this
            .shadow(elevation = elevation, shape = RoundedCornerShape(cornerDp),
                    ambientColor = shadow, spotColor = shadow)
            .background(
                Brush.linearGradient(listOf(startColor, endColor)),
                RoundedCornerShape(cornerDp),
            )
    }
}

/**
 * Header band modifier — solid royal-blue gradient with a deep drop shadow
 * at the bottom edge in light mode.
 * Dark mode: falls back to surface1 for the header column background.
 */
@Composable
fun Modifier.headerBand(c: AppColors = LocalAppColors.current): Modifier =
    if (c.isDark) {
        this.background(c.surface1)
    } else {
        this
            .shadow(
                elevation    = 20.dp,
                shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                ambientColor = c.headerShadow,
                spotColor    = c.headerShadow,
                clip         = false,
            )
            .background(
                Brush.linearGradient(listOf(c.headerStart, c.headerEnd)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
    }
