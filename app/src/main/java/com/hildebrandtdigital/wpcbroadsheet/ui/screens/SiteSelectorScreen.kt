package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import com.hildebrandtdigital.wpcbroadsheet.navigation.SITE_ALL
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealEntry
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealPricing
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealType
import com.hildebrandtdigital.wpcbroadsheet.data.model.Resident
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import com.hildebrandtdigital.wpcbroadsheet.data.repository.BillingCalculator
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

// ── Shared data model ─────────────────────────────────────────────────────────

data class SiteStats(
    val residents     : Int,
    val mealsThisMonth: Int,
    val pendingDays   : Int,       // residents with zero entries (not captured yet)
    val completion    : Float,     // 0..1 — fraction of residents with any entries
    val meals1Course  : Int    = 0,
    val meals2Course  : Int    = 0,
    val mealsFullBoard: Int    = 0,
    val mealsBreakfast: Int    = 0,
    val revenue       : Double = 0.0,
    val prevRevenue   : Double = 0.0, // previous month for delta calculation
)

/** Build a live SiteStats from Room data for a single site. */
fun computeSiteStats(
    residents    : List<Resident>,
    entries      : List<MealEntry>,
    prevEntries  : List<MealEntry>,
    pricing      : MealPricing,
    year         : Int,
    month        : Int,
    site         : Site,
): SiteStats {
    if (residents.isEmpty()) return SiteStats(0, 0, 0, 0f)

    val report     = BillingCalculator.calculateSiteReport(site, residents, entries, year, month, pricing)
    val prevReport = BillingCalculator.calculateSiteReport(site, residents, prevEntries, year, month - 1, pricing)

    val capturedCount  = entries.map { it.unitNumber }.toSet().size
    val totalResidents = residents.size
    val completion     = if (totalResidents > 0) capturedCount.toFloat() / totalResidents else 0f
    val pendingDays    = totalResidents - capturedCount

    // Meal type totals from all entries
    val allCounts = entries.flatMap { it.counts.entries }
    fun countType(vararg types: MealType) = allCounts.filter { it.key in types }.sumOf { it.value }

    return SiteStats(
        residents      = totalResidents,
        mealsThisMonth = report.grandTotalMeals,
        pendingDays    = pendingDays.coerceAtLeast(0),
        completion     = completion.coerceIn(0f, 1f),
        meals1Course   = countType(MealType.COURSE_1, MealType.SUN_1_COURSE),
        meals2Course   = countType(MealType.COURSE_2),
        mealsFullBoard = countType(MealType.FULL_BOARD, MealType.SUN_3_COURSE, MealType.COURSE_3),
        mealsBreakfast = countType(MealType.BREAKFAST),
        revenue        = report.grandTotal,
        prevRevenue    = prevReport.grandTotal,
    )
}

/** Aggregate multiple SiteStats into a single consolidated view. */
fun aggregateStats(all: List<SiteStats>): SiteStats {
    if (all.isEmpty()) return SiteStats(0, 0, 0, 0f)
    return SiteStats(
        residents      = all.sumOf { it.residents },
        mealsThisMonth = all.sumOf { it.mealsThisMonth },
        pendingDays    = all.count { it.pendingDays > 0 },
        completion     = if (all.isNotEmpty()) all.map { it.completion }.average().toFloat() else 0f,
        meals1Course   = all.sumOf { it.meals1Course },
        meals2Course   = all.sumOf { it.meals2Course },
        mealsFullBoard = all.sumOf { it.mealsFullBoard },
        mealsBreakfast = all.sumOf { it.mealsBreakfast },
        revenue        = all.sumOf { it.revenue },
        prevRevenue    = all.sumOf { it.prevRevenue },
    )
}

// ── Bottom sheet content ──────────────────────────────────────────────────────

@Composable
fun SiteSelectorSheetContent(
    currentSiteId  : String,
    siteStatsMap   : Map<String, SiteStats> = emptyMap(), // live stats, falls back to empty
    onSiteSelected : (String) -> Unit,
    onViewDetails  : (Site) -> Unit,
) {
    val c = LocalAppColors.current
    val sites    = SampleData.sites
    val aggStats = aggregateStats(siteStatsMap.values.toList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        // Handle bar
        Box(modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(c.borderColor))

        Spacer(Modifier.height(16.dp))
        Text("Switch Site", style = MaterialTheme.typography.headlineMedium.copy(color = c.textBright))
        Text("Select a site or view all combined", style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
        Spacer(Modifier.height(16.dp))

        // ── All Sites card ────────────────────────────────────────────────────
        val allActive = currentSiteId == SITE_ALL
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = if (allActive) Primary.copy(0.08f) else c.surface2,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (allActive) Primary.copy(0.5f) else c.borderColor, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSiteSelected(SITE_ALL) }
        ) {
            Row(modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(0.12f)).border(1.dp, Primary.copy(0.25f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Rounded.GridView, null, tint = Primary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("All Sites", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright))
                    Text("${sites.size} villages  ·  ${aggStats.residents} residents  ·  ${aggStats.mealsThisMonth} meals",
                        style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                }
                if (allActive) Icon(Icons.Rounded.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = c.borderColor)
        Text("Individual sites", style = MaterialTheme.typography.labelSmall.copy(color = c.textDim),
            modifier = Modifier.padding(vertical = 10.dp))

        // ── Per-site cards ────────────────────────────────────────────────────
        sites.forEach { site ->
            val stats    = siteStatsMap[site.id] ?: SiteStats(
                residents = SampleData.residents.count { it.siteId == site.id },
                mealsThisMonth = 0, pendingDays = 0, completion = 0f,
            )
            val isActive = site.id == currentSiteId
            SiteSheetCard(site = site, stats = stats, isActive = isActive,
                onSelect = { onSiteSelected(site.id) }, onDetails = { onViewDetails(site) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SiteSheetCard(
    site    : Site,
    stats   : SiteStats,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
) {
    val c = LocalAppColors.current
    val statusColor = when {
        stats.completion >= 1f   -> Accent
        stats.completion >= 0.7f -> Secondary
        else                     -> Danger
    }
    val statusLabel = when {
        stats.completion >= 1f -> "Complete"
        stats.pendingDays > 0  -> "${stats.pendingDays} pending"
        else                   -> "In Progress"
    }

    Surface(shape = RoundedCornerShape(16.dp), color = if (isActive) Primary.copy(0.06f) else c.surface1,
        modifier = Modifier.fillMaxWidth()
            .border(1.5.dp, if (isActive) Primary.copy(0.5f) else c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).clickable { onSelect() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)) {
                    Icon(if (isActive) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null, tint = if (isActive) Primary else c.textDim, modifier = Modifier.size(20.dp))
                    Column {
                        Text(site.name, style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright))
                        Text("Mgr: ${site.unitManagerName}", style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(0.15f),
                    modifier = Modifier.cardElevation(c).border(1.dp, statusColor.copy(0.3f), RoundedCornerShape(50))) {
                    Text(statusLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontSize = 9.sp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetStatChip("👥 ${stats.residents}",               "Residents", Modifier.weight(1f))
                SheetStatChip("🍽️ ${stats.mealsThisMonth}",         "Meals",     Modifier.weight(1f))
                SheetStatChip("${(stats.completion * 100).toInt()}%","Complete",  Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(c.surface2).border(1.dp, c.borderColor, RoundedCornerShape(8.dp))
                .clickable { onDetails() }.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("View / Edit Site Details", style = MaterialTheme.typography.labelMedium.copy(color = c.textMuted))
                Icon(Icons.Rounded.ArrowOutward, null, tint = c.textDim, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SheetStatChip(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(8.dp), color = c.surface2, modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelMedium.copy(color = c.textBright, fontSize = 11.sp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
        }
    }
}
