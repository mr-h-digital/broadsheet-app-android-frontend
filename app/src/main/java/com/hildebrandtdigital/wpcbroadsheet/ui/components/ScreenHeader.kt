package com.hildebrandtdigital.wpcbroadsheet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*

/**
 * Rich gradient hero header used across all detail screens.
 *
 * Dark mode  : subtle radial Primary glow, as before.
 * Light mode : deep saturated gradient from a vibrant Primary/brand tint at
 *              the top, fading into the page's bgDeep — gives each screen a
 *              distinctive "hero" look without being heavy.
 *
 * @param accentColor  Brand colour for this screen's gradient (default: Primary).
 * @param content      Header content (title, subtitle, controls).
 */
@Composable
fun ScreenHeader(
    accentColor: Color    = Primary,
    modifier   : Modifier = Modifier,
    content    : @Composable ColumnScope.() -> Unit,
) {
    val c = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Deep gradient behind the header content
            .background(
                if (c.isDark)
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent),
                        radius = 700f,
                    )
                else
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to accentColor.copy(alpha = 0.28f),
                            0.45f to accentColor.copy(alpha = 0.12f),
                            1.00f to Color.Transparent,
                        )
                    )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 12.dp),
            content = content,
        )
    }
}
