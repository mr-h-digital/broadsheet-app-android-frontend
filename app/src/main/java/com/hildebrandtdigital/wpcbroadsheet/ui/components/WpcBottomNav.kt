package com.hildebrandtdigital.wpcbroadsheet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*

enum class NavTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DASHBOARD("Dashboard", Icons.Rounded.BarChart,    Icons.Rounded.BarChart),
    CAPTURE  ("Capture",   Icons.Rounded.EditNote,    Icons.Rounded.EditNote),
    REPORTS  ("Reports",   Icons.Rounded.Description, Icons.Rounded.Description),
    PROFILE  ("Profile",   Icons.Rounded.Person,      Icons.Rounded.Person),
}

@Composable
fun WpcBottomNav(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Card shadow only in light mode
            .then(
                if (!c.isDark) Modifier.shadow(
                    elevation        = 16.dp,
                    shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ambientColor     = c.cardShadow,
                    spotColor        = c.cardShadow,
                )
                else Modifier
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(c.navBackground)
    ) {
        // Top accent line — gradient in light, subtle in dark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (!c.isDark) Brush.horizontalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0f),
                            Primary.copy(alpha = 0.7f),
                            Accent.copy(alpha = 0.5f),
                            Primary.copy(alpha = 0f),
                        )
                    )
                    else Brush.horizontalGradient(
                        colors = listOf(c.navBorder, c.navBorder)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            NavTab.entries.forEach { tab ->
                NavItem(
                    tab      = tab,
                    selected = tab == selected,
                    isDark   = c.isDark,
                    onClick  = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab     : NavTab,
    selected: Boolean,
    isDark  : Boolean,
    onClick : () -> Unit,
) {
    val c = LocalAppColors.current

    val iconTint by animateColorAsState(
        targetValue   = if (selected) Primary else c.textMuted,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "navIconTint",
    )

    // In light mode: selected item gets a punchy filled pill bg
    val pillBg by animateColorAsState(
        targetValue   = if (selected && !isDark) Primary.copy(alpha = 0.12f)
                        else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "pillBg",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(pillBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector        = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = tab.label,
            tint               = iconTint,
            modifier           = Modifier.size(22.dp),
        )
        Text(
            text  = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
        )
        // Active dot
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) Primary else Color.Transparent)
        )
    }
}
