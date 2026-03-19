package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.BillingCalculator
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.NavTab
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBottomNav
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand

private const val HISTORY_ALL = "all"

private data class MonthSummary(
    val year         : Int,
    val month        : Int,
    val totalMeals   : Int,
    val totalBilled  : Double,
    val residentCount: Int,
    val siteId       : String,
    val siteName     : String,
)

private enum class TrendDir { UP, DOWN, FLAT }

private fun monthName(month: Int) = when (month) {
    1  -> "January";  2  -> "February"; 3  -> "March"
    4  -> "April";    5  -> "May";       6  -> "June"
    7  -> "July";     8  -> "August";    9  -> "September"
    10 -> "October";  11 -> "November";  12 -> "December"
    else -> "Unknown"
}

private fun siteColor(siteId: String) = when (siteId) {
    "lizane"  -> Primary
    "bakkies" -> Accent
    "sunhill" -> Secondary
    else      -> TextDim
}

private fun buildHistory(): List<MonthSummary> {
    val results = mutableListOf<MonthSummary>()
    SampleData.sites.forEach { site ->
        val pricing   = SampleData.pricingForSite(site.id)
        val residents = SampleData.residents.filter { it.siteId == site.id }
        val janEntries = SampleData.januaryEntries.filter { it.siteId == site.id }
        var janMeals  = 0; var janBilled = 0.0
        residents.forEach { r ->
            val b = BillingCalculator.calculateResidentBilling(r, janEntries, pricing)
            janMeals += b.totalMeals; janBilled += b.finalTotal
        }
        results += MonthSummary(2026, 1, janMeals, janBilled, residents.size, site.id, site.name)
        val siteSeed = site.id.hashCode().toLong()
        listOf(2025 to 12, 2025 to 11, 2025 to 10, 2025 to 9, 2025 to 8,
            2025 to 7,  2025 to 6,  2025 to 5,  2025 to 4, 2025 to 3, 2025 to 2
        ).forEachIndexed { idx, (year, month) ->
            val v = Math.sin((siteSeed + idx * 7).toDouble()) * 0.12
            results += MonthSummary(
                year, month,
                (janMeals * (1.0 + v)).toInt().coerceAtLeast(1),
                janBilled * (1.0 + v),
                (residents.size - idx / 4).coerceAtLeast(1),
                site.id, site.name,
            )
        }
    }
    return results.sortedWith(compareByDescending<MonthSummary> { it.year }.thenByDescending { it.month })
}

private fun consolidate(history: List<MonthSummary>): List<MonthSummary> =
    history.groupBy { it.year * 100 + it.month }.map { (_, e) ->
        MonthSummary(e.first().year, e.first().month,
            e.sumOf { it.totalMeals }, e.sumOf { it.totalBilled },
            e.sumOf { it.residentCount }, HISTORY_ALL, "All Sites")
    }.sortedWith(compareByDescending<MonthSummary> { it.year }.thenByDescending { it.month })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyHistoryScreen(
    onNavigateBack        : () -> Unit = {},
    onNavigateToDashboard : () -> Unit,
    onNavigateToCapture   : () -> Unit,
    onNavigateToReports   : () -> Unit,
    onNavigateToProfile   : () -> Unit,
) {
    val c = LocalAppColors.current
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val isOpsManager = AppSession.hasCrossSiteAccess
    val defaultSite  = if (isOpsManager) HISTORY_ALL else (AppSession.currentSiteId ?: SampleData.sites.first().id)

    var currentSiteId  by remember { mutableStateOf(defaultSite) }
    var selectedYear   by remember { mutableStateOf(2026) }
    var showSiteSheet  by remember { mutableStateOf(false) }
    var exportingMonth by remember { mutableStateOf<MonthSummary?>(null) }

    val allHistory      = remember { buildHistory() }
    val allConsolidated = remember { consolidate(allHistory) }

    val siteHistory = remember(currentSiteId) {
        if (currentSiteId == HISTORY_ALL) allConsolidated
        else allHistory.filter { it.siteId == currentSiteId }
    }

    val years    = siteHistory.map { it.year }.distinct().sorted().reversed()
    val filtered = siteHistory.filter { it.year == selectedYear }

    LaunchedEffect(currentSiteId) {
        if (selectedYear !in years) selectedYear = years.firstOrNull() ?: 2026
    }

    val yearTotal = filtered.sumOf { it.totalBilled }
    val yearMeals = filtered.sumOf { it.totalMeals }

    val headerSub = if (currentSiteId == HISTORY_ALL) "All Sites  ·  Consolidated Archive"
    else "${SampleData.sites.firstOrNull { it.id == currentSiteId }?.name ?: currentSiteId}  ·  Billing Archive"

    // ── Site sheet ────────────────────────────────────────────────────────────
    if (showSiteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSiteSheet = false },
            containerColor   = c.surface1,
            dragHandle = {
                Box(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(c.borderColor))
            },
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Select Site", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright))
                Spacer(Modifier.height(4.dp))
                Text("View billing history for a specific site", style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
                Spacer(Modifier.height(16.dp))
                HistorySiteOption("All Sites", "Consolidated view across all villages",
                    c.textMuted, currentSiteId == HISTORY_ALL) { currentSiteId = HISTORY_ALL; showSiteSheet = false }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = c.borderColor)
                Spacer(Modifier.height(8.dp))
                SampleData.sites.forEach { site ->
                    HistorySiteOption(site.name, "Mgr: ${site.unitManagerName}",
                        siteColor(site.id), currentSiteId == site.id) { currentSiteId = site.id; showSiteSheet = false }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(selected = NavTab.REPORTS, onSelect = {
                when (it) {
                    NavTab.DASHBOARD -> onNavigateToDashboard()
                    NavTab.CAPTURE   -> onNavigateToCapture()
                    NavTab.REPORTS   -> onNavigateToReports()
                    NavTab.PROFILE   -> onNavigateToProfile()
                }
            })
        }

    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .appBackground(c)
            .padding(padding)) {

            Column(modifier = Modifier
                .background(
                    if (c.isDark) c.surface1
                    else c.bgDeep
                )
                .headerBand(c)
                .padding(start = 4.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)) {

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("History", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text(headerSub, style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)))
                    }
                    WpcLogoBadge(size = WpcBrandingSize.ICON)
                }

                if (isOpsManager) {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.padding(start = 24.dp)) {
                        HistorySiteChip(currentSiteId) { showSiteSheet = true }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    years.forEach { year ->
                        val active = selectedYear == year
                        Surface(shape = RoundedCornerShape(50),
                            color = if (c.isDark) {
                                if (active) Primary.copy(0.18f) else c.surface2
                            } else {
                                // Active = solid white; Inactive = semi-transparent white — both visible on blue
                                if (active) androidx.compose.ui.graphics.Color.White.copy(0.95f) else androidx.compose.ui.graphics.Color.White.copy(0.25f)
                            },
                            modifier = Modifier
                                .border(1.dp,
                                    if (c.isDark) (if (active) Primary.copy(0.5f) else c.borderColor)
                                    else (if (active) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(0.60f)),
                                    RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50)).clickable { selectedYear = year }) {
                            Text("$year",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (c.isDark) (if (active) Primary else c.textMuted)
                                    else if (active) c.headerStart  // dark text on near-white active pill
                                    else androidx.compose.ui.graphics.Color.White,  // white on transparent inactive
                                    fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    YearStatCard("Total Billed", "R${"%.0f".format(yearTotal)}", Accent,    Modifier.weight(1f))
                    YearStatCard("Total Meals",  "$yearMeals",                   Primary,   Modifier.weight(1f))
                    YearStatCard("Months",       "${filtered.size}",             Secondary, Modifier.weight(1f))
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.HistoryEdu, null, tint = c.textDim, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No history for $selectedYear", style = MaterialTheme.typography.bodyLarge.copy(color = c.textMuted))
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { "${it.siteId}-${it.year}-${it.month}" }) { summary ->
                        val prev = filtered.getOrNull(filtered.indexOf(summary) + 1)
                        MonthCard(
                            summary       = summary,
                            prevSummary   = prev,
                            showSiteBadge = currentSiteId == HISTORY_ALL,
                            isExporting   = exportingMonth == summary,
                            onExportCsv   = {
                                scope.launch { exportingMonth = summary; delay(800); exportingMonth = null
                                    Toast.makeText(context, "✅ ${monthName(summary.month)} CSV exported", Toast.LENGTH_SHORT).show() }
                            },
                            onExportPdf   = {
                                scope.launch { exportingMonth = summary; delay(800); exportingMonth = null
                                    Toast.makeText(context, "✅ ${monthName(summary.month)} PDF exported", Toast.LENGTH_SHORT).show() }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistorySiteChip(siteId: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val isAll  = siteId == HISTORY_ALL
    val label  = if (isAll) "All Sites" else SampleData.sites.firstOrNull { it.id == siteId }?.name ?: siteId
    val color  = if (c.isDark) (if (isAll) c.textMuted else siteColor(siteId)) else androidx.compose.ui.graphics.Color.White.copy(if (isAll) 0.70f else 1f)
    Surface(shape = RoundedCornerShape(50), color = color.copy(0.10f),
        modifier = Modifier.cardElevation(c).border(1.dp, color.copy(0.35f), RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = color))
            Icon(Icons.Rounded.ExpandMore, null, tint = color, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun HistorySiteOption(label: String, sub: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(0.10f) else c.surface2,
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, if (selected) color.copy(0.4f) else c.borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(color = if (selected) color else c.textBright))
                Text(sub, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
            }
            if (selected) Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MonthCard(
    summary      : MonthSummary,
    prevSummary  : MonthSummary?,
    showSiteBadge: Boolean,
    isExporting  : Boolean,
    onExportCsv  : () -> Unit,
    onExportPdf  : () -> Unit,
) {
    val c = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    val trend = when {
        prevSummary == null                           -> TrendDir.FLAT
        summary.totalBilled > prevSummary.totalBilled -> TrendDir.UP
        summary.totalBilled < prevSummary.totalBilled -> TrendDir.DOWN
        else                                          -> TrendDir.FLAT
    }
    val trendPct = if (prevSummary != null && prevSummary.totalBilled > 0) {
        val pct = (summary.totalBilled - prevSummary.totalBilled) / prevSummary.totalBilled * 100
        "${"%.1f".format(kotlin.math.abs(pct))}%"
    } else null
    val accentColor = if (showSiteBadge) siteColor(summary.siteId) else Primary

    Surface(shape = RoundedCornerShape(16.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(0.12f)).border(1.dp, accentColor.copy(0.25f), RoundedCornerShape(12.dp))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(monthName(summary.month).take(3).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontSize = 9.sp))
                            Text("${summary.year}",
                                style = MaterialTheme.typography.labelSmall.copy(color = c.textMuted, fontSize = 8.sp))
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${monthName(summary.month)} ${summary.year}", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright))
                            if (showSiteBadge && summary.siteId != HISTORY_ALL) {
                                Surface(shape = RoundedCornerShape(4.dp), color = accentColor.copy(0.15f)) {
                                    Text(summary.siteName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontSize = 9.sp),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("${summary.totalMeals} meals  ·  ${summary.residentCount} residents",
                            style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("R${"%.2f".format(summary.totalBilled)}",
                        style = MaterialTheme.typography.labelLarge.copy(color = Accent, fontSize = 15.sp))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val tIcon  = when (trend) { TrendDir.UP -> Icons.AutoMirrored.Rounded.TrendingUp; TrendDir.DOWN -> Icons.AutoMirrored.Rounded.TrendingDown; else -> Icons.AutoMirrored.Rounded.TrendingFlat }
                        val tColor = when (trend) { TrendDir.UP -> Accent; TrendDir.DOWN -> Danger; else -> c.textMuted }
                        Icon(tIcon, null, tint = tColor, modifier = Modifier.size(14.dp))
                        if (trendPct != null) Text(trendPct, style = MaterialTheme.typography.labelSmall.copy(color = tColor, fontSize = 9.sp))
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = c.borderColor)
                Spacer(Modifier.height(12.dp))
                if (isExporting) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Generating export…", style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Primary.copy(0.4f))) {
                            Icon(Icons.Rounded.TableChart, null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("CSV", color = Primary)
                        }
                        OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Accent.copy(0.4f))) {
                            Icon(Icons.Rounded.PictureAsPdf, null, tint = Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("PDF", color = Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = if (c.isDark) c.surface2 else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
        modifier = modifier
            .then(if (!c.isDark) Modifier.border(1.dp, androidx.compose.ui.graphics.Color.White.copy(0.35f), RoundedCornerShape(12.dp)) else Modifier),
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelLarge.copy(
                color = if (c.isDark) color else androidx.compose.ui.graphics.Color.White,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(
                color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(0.78f),
            ))
        }
    }
}