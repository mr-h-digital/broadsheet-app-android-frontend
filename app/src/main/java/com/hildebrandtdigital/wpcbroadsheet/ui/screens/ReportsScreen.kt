package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.hildebrandtdigital.wpcbroadsheet.navigation.SITE_ALL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.*
import com.hildebrandtdigital.wpcbroadsheet.data.repository.*
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

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

private data class ConsolidatedReport(
    val siteReports    : List<SiteMonthlyReport>,
    val grandTotalMeals: Int,
    val grandSubtotal  : Double,
    val grandVat       : Double,
    val grandTaBakkies : Double,
    val grandTotal     : Double,
    val totalDeductions: Double,
)

private fun buildConsolidated(reports: List<SiteMonthlyReport>) = ConsolidatedReport(
    siteReports     = reports,
    grandTotalMeals = reports.sumOf { it.grandTotalMeals },
    grandSubtotal   = reports.sumOf { it.grandSubtotal },
    grandVat        = reports.sumOf { it.grandVat },
    grandTaBakkies  = reports.sumOf { it.grandTaBakkies },
    grandTotal      = reports.sumOf { it.grandTotal },
    totalDeductions = reports.sumOf { r -> r.residentBillings.sumOf { it.compulsoryDeduction } },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    initialSiteId         : String = SITE_ALL,
    mealRepository        : MealRepository? = null,
    residentRepository    : ResidentRepository? = null,
    onNavigateToDashboard : () -> Unit,
    onNavigateToCapture   : (String) -> Unit,
    onNavigateToProfile   : () -> Unit,
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Site ──────────────────────────────────────────────────────────────────
    var currentSiteId by remember(AppSession.currentUser) {
        mutableStateOf(
            if (!AppSession.hasCrossSiteAccess) AppSession.currentSiteId ?: initialSiteId
            else initialSiteId
        )
    }
    val isAllSites  = currentSiteId == SITE_ALL
    val currentSite = if (isAllSites) null else SampleData.sites.firstOrNull { it.id == currentSiteId }

    // ── Billing period — mutable for month navigation ─────────────────────────
    val calendar = remember { java.util.Calendar.getInstance() }
    var reportYear  by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var reportMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH) + 1) }

    fun prevMonth() {
        if (reportMonth == 1) { reportMonth = 12; reportYear-- } else reportMonth--
    }
    fun nextMonth() {
        val now = java.util.Calendar.getInstance()
        val ny = now.get(java.util.Calendar.YEAR); val nm = now.get(java.util.Calendar.MONTH) + 1
        if (reportYear < ny || (reportYear == ny && reportMonth < nm)) {
            if (reportMonth == 12) { reportMonth = 1; reportYear++ } else reportMonth++
        }
    }
    val nowCal = remember { java.util.Calendar.getInstance() }
    val isCurrentMonth = reportYear == nowCal.get(java.util.Calendar.YEAR) &&
            reportMonth == nowCal.get(java.util.Calendar.MONTH) + 1

    // ── Live entries from Room — produceState keys force resubscription ─────────
    // collectAsStateWithLifecycle does NOT restart the flow when reportYear/Month
    // change because the flow object itself is created inline at first composition
    // only. produceState with key1/key2 cancels and restarts the collector each
    // time the period changes, giving us a fresh DB query per month.
    val lizaneEntries by produceState(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "lizane" },
        key1 = reportYear, key2 = reportMonth,
    ) {
        (mealRepository?.observeEntries("lizane", reportYear, reportMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (reportYear == 2026 && reportMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "lizane" }
                else emptyList()
            )
                ).collect { value = it }
    }

    val bakkiesEntries by produceState(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "bakkies" },
        key1 = reportYear, key2 = reportMonth,
    ) {
        (mealRepository?.observeEntries("bakkies", reportYear, reportMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (reportYear == 2026 && reportMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "bakkies" }
                else emptyList()
            )
                ).collect { value = it }
    }

    val sunhillEntries by produceState(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "sunhill" },
        key1 = reportYear, key2 = reportMonth,
    ) {
        (mealRepository?.observeEntries("sunhill", reportYear, reportMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (reportYear == 2026 && reportMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "sunhill" }
                else emptyList()
            )
                ).collect { value = it }
    }

    // ── Live residents from Room ───────────────────────────────────────────────
    val lizaneResidents by (
            residentRepository?.observeResidents("lizane")
                ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "lizane" })
            ).collectAsStateWithLifecycle(initialValue = SampleData.residents.filter { it.siteId == "lizane" })

    val bakkiesResidents by (
            residentRepository?.observeResidents("bakkies")
                ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "bakkies" })
            ).collectAsStateWithLifecycle(initialValue = SampleData.residents.filter { it.siteId == "bakkies" })

    val sunhillResidents by (
            residentRepository?.observeResidents("sunhill")
                ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "sunhill" })
            ).collectAsStateWithLifecycle(initialValue = SampleData.residents.filter { it.siteId == "sunhill" })

    // ── Build reports from live data ───────────────────────────────────────────
    val lizaneReport = remember(lizaneResidents, lizaneEntries, reportYear, reportMonth) {
        BillingCalculator.calculateSiteReport(
            site = SampleData.sites.first { it.id == "lizane" },
            residents = lizaneResidents, entries = lizaneEntries,
            year = reportYear, month = reportMonth,
            pricing = SampleData.pricingForSite("lizane"),
        )
    }
    val bakkiesReport = remember(bakkiesResidents, bakkiesEntries, reportYear, reportMonth) {
        BillingCalculator.calculateSiteReport(
            site = SampleData.sites.first { it.id == "bakkies" },
            residents = bakkiesResidents, entries = bakkiesEntries,
            year = reportYear, month = reportMonth,
            pricing = SampleData.pricingForSite("bakkies"),
        )
    }
    val sunhillReport = remember(sunhillResidents, sunhillEntries, reportYear, reportMonth) {
        BillingCalculator.calculateSiteReport(
            site = SampleData.sites.first { it.id == "sunhill" },
            residents = sunhillResidents, entries = sunhillEntries,
            year = reportYear, month = reportMonth,
            pricing = SampleData.pricingForSite("sunhill"),
        )
    }
    val allReports   = listOf(lizaneReport, bakkiesReport, sunhillReport)
    val consolidated = remember(allReports) { buildConsolidated(allReports) }

    val activeReport = when (currentSiteId) {
        "bakkies" -> bakkiesReport
        "sunhill" -> sunhillReport
        else      -> lizaneReport
    }

    // ── Export state ───────────────────────────────────────────────────────────
    var isExporting by remember { mutableStateOf(false) }
    var lastCsvUri  by remember { mutableStateOf<Uri?>(null) }
    var lastPdfUri  by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(currentSiteId, reportYear, reportMonth) { lastCsvUri = null; lastPdfUri = null }

    fun doExport(action: suspend () -> Unit) {
        scope.launch {
            isExporting = true
            try { action() }
            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally { isExporting = false }
        }
    }

    // ── Sheet state ────────────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(selected = NavTab.REPORTS, onSelect = {
                when (it) {
                    NavTab.DASHBOARD -> onNavigateToDashboard()
                    NavTab.CAPTURE   -> onNavigateToCapture(currentSiteId)
                    NavTab.PROFILE   -> onNavigateToProfile()
                    else             -> {}
                }
            })
        }

    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().appBackground(c).padding(padding).verticalScroll(rememberScrollState())) {

            // ── Header ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()
                .headerBand(c)
                .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 20.dp)) {

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly Report", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text(
                            if (isAllSites) "${monthName(reportMonth)} $reportYear  ·  All Sites Consolidated"
                            else            "${monthName(reportMonth)} $reportYear  ·  ${currentSite?.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WpcLogoBadge(size = WpcBrandingSize.ICON)
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (c.isDark) Primary else androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Site chip ──────────────────────────────────────────────────
                val chipColor = if (isAllSites) Primary else siteColor(currentSiteId)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (c.isDark) chipColor.copy(0.10f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.20f),
                    modifier = Modifier
                        .border(1.dp, if (c.isDark) chipColor.copy(0.35f) else androidx.compose.ui.graphics.Color.White.copy(0.45f), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .then(if (AppSession.hasCrossSiteAccess) Modifier.clickable { showSheet = true } else Modifier)
                ) {
                    Row(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAllSites) {
                            Icon(Icons.Rounded.GridView, null, tint = if (c.isDark) chipColor else androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (c.isDark) chipColor else androidx.compose.ui.graphics.Color.White))
                        }
                        Text(currentSite?.name ?: "All Sites",
                            style = MaterialTheme.typography.labelMedium.copy(color = if (c.isDark) chipColor else androidx.compose.ui.graphics.Color.White))
                        if (AppSession.hasCrossSiteAccess)
                            Icon(Icons.Rounded.ExpandMore, "Switch site", tint = if (c.isDark) chipColor.copy(0.7f) else androidx.compose.ui.graphics.Color.White.copy(0.75f), modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Month navigator ────────────────────────────────────────────
                Surface(shape = RoundedCornerShape(10.dp), color = c.surface1,
                    modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(10.dp))) {
                    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { prevMonth() }) {
                            Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = if (c.isDark) Primary else c.headerStart)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${monthName(reportMonth)} $reportYear", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else c.textBright))
                            if (isCurrentMonth) Text("Current period", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
                        }
                        IconButton(onClick = { nextMonth() }, enabled = !isCurrentMonth) {
                            Icon(Icons.Rounded.ChevronRight, "Next month", tint = if (isCurrentMonth) c.textDim else (if (c.isDark) Primary else c.headerStart))
                        }
                    }
                }
            }

            // ── Body ───────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                Spacer(Modifier.height(20.dp))

                // ── Export buttons ─────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportButton("📊", "CSV", Modifier.weight(1f), isExporting) {
                        doExport {
                            val uri = withContext(Dispatchers.IO) {
                                if (isAllSites) ExportManager.exportConsolidatedCsv(context, allReports)
                                else ExportManager.exportCsv(context, activeReport)
                            }
                            lastCsvUri = uri
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "✅ CSV saved to Downloads!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ExportButton("📄", "PDF", Modifier.weight(1f), isExporting) {
                        doExport {
                            val uri = withContext(Dispatchers.IO) {
                                if (isAllSites) ExportManager.exportConsolidatedPdf(context, allReports)
                                else ExportManager.exportPdf(context, activeReport)
                            }
                            lastPdfUri = uri
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "✅ PDF saved to Downloads!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ExportButton("📧", "Email", Modifier.weight(1f), isExporting) {
                        doExport {
                            val csv = lastCsvUri ?: withContext(Dispatchers.IO) {
                                if (isAllSites) ExportManager.exportConsolidatedCsv(context, allReports)
                                else ExportManager.exportCsv(context, activeReport)
                            }.also { lastCsvUri = it }
                            val pdf = lastPdfUri ?: withContext(Dispatchers.IO) {
                                if (isAllSites) ExportManager.exportConsolidatedPdf(context, allReports)
                                else ExportManager.exportPdf(context, activeReport)
                            }.also { lastPdfUri = it }
                            withContext(Dispatchers.Main) {
                                if (isAllSites) ExportManager.shareConsolidatedViaEmail(context, allReports, csv, pdf)
                                else ExportManager.shareViaEmail(context, activeReport, csv, pdf)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Report content ─────────────────────────────────────────────
                if (isAllSites) {
                    AllSitesSummaryCard(consolidated)
                    Spacer(Modifier.height(24.dp))
                    consolidated.siteReports.forEachIndexed { i, sr ->
                        SiteReportSection(report = sr, accentColor = listOf(Primary, Accent, Secondary)[i % 3])
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    SingleSiteSummaryCard(activeReport)
                    Spacer(Modifier.height(24.dp))
                    Text("Per-Resident Billing", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright),
                        modifier = Modifier.padding(bottom = 12.dp))
                    ResidentTable(activeReport.residentBillings)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ── Site switcher sheet ────────────────────────────────────────────────────
    if (showSheet && AppSession.hasCrossSiteAccess) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState, containerColor = c.surface1, dragHandle = null,
        ) {
            SiteSelectorSheetContent(
                currentSiteId  = currentSiteId,
                onSiteSelected = { siteId ->
                    currentSiteId = siteId
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                },
                onViewDetails  = { site ->
                    currentSiteId = site.id
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                },
            )
        }
    }
}

// ── Summary cards ──────────────────────────────────────────────────────────────

@Composable
private fun SingleSiteSummaryCard(report: SiteMonthlyReport) {
    val c = LocalAppColors.current
    val totalDeduction = report.residentBillings.sumOf { it.compulsoryDeduction }
    Surface(shape = RoundedCornerShape(16.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("💰  Billing Summary", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else c.textBright))
            Spacer(Modifier.height(16.dp))
            ReportRow("Total Meals Served",    "${report.grandTotalMeals}",                         Primary)
            ReportRow("Revenue (excl. VAT)",   BillingCalculator.formatRand(report.grandSubtotal),  Secondary)
            ReportRow("VAT (15%)",             BillingCalculator.formatRand(report.grandVat),       c.textBright)
            ReportRow("T/A Bakkies",           BillingCalculator.formatRand(report.grandTaBakkies), c.textBright)
            ReportRow("Less: Compulsory Meals","−${BillingCalculator.formatRand(totalDeduction)}",  Danger)
            HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(vertical = 8.dp))
            ReportRow("TOTAL BILLED", BillingCalculator.formatRand(report.grandTotal), Accent, bold = true, largeValue = true)
        }
    }
}

@Composable
private fun AllSitesSummaryCard(report: ConsolidatedReport) {
    val c = LocalAppColors.current
    val pillColors = listOf(Primary, Accent, Secondary)
    Surface(shape = RoundedCornerShape(16.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth().border(1.dp, Primary.copy(0.25f), RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.GridView, null, tint = Primary, modifier = Modifier.size(18.dp))
                Text("Consolidated — All Sites", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else c.textBright))
            }
            Spacer(Modifier.height(4.dp))
            Text("${report.siteReports.size} sites  ·  ${report.siteReports.sumOf { it.residentBillings.size }} residents",
                style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                report.siteReports.forEachIndexed { i, sr ->
                    val pillColor = pillColors[i % pillColors.size]
                    Surface(shape = RoundedCornerShape(10.dp), color = pillColor.copy(0.10f),
                        modifier = Modifier.weight(1f).border(1.dp, pillColor.copy(0.25f), RoundedCornerShape(10.dp))) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sr.site.name.split(" ").first(), style = MaterialTheme.typography.labelSmall.copy(color = pillColor))
                            Text(BillingCalculator.formatRand(sr.grandTotal),
                                style = MaterialTheme.typography.labelMedium.copy(color = pillColor, fontSize = 11.sp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = c.borderColor)
            Spacer(Modifier.height(12.dp))
            ReportRow("Total Meals Served",    "${report.grandTotalMeals}",                         Primary)
            ReportRow("Revenue (excl. VAT)",   BillingCalculator.formatRand(report.grandSubtotal),  Secondary)
            ReportRow("VAT (15%)",             BillingCalculator.formatRand(report.grandVat),       c.textBright)
            ReportRow("T/A Bakkies",           BillingCalculator.formatRand(report.grandTaBakkies), c.textBright)
            ReportRow("Less: Compulsory Meals","−${BillingCalculator.formatRand(report.totalDeductions)}", Danger)
            HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(vertical = 8.dp))
            ReportRow("TOTAL BILLED", BillingCalculator.formatRand(report.grandTotal), Accent, bold = true, largeValue = true)
        }
    }
}

@Composable
private fun SiteReportSection(report: SiteMonthlyReport, accentColor: Color) {
    val c = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(accentColor))
                    Column {
                        Text(report.site.name, style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else c.textBright))
                        Text("${report.residentBillings.size} residents  ·  ${report.grandTotalMeals} meals",
                            style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(BillingCalculator.formatRand(report.grandTotal),
                        style = MaterialTheme.typography.headlineSmall.copy(color = accentColor))
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.textDim)
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit  = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
                Column {
                    HorizontalDivider(color = c.borderColor)
                    ResidentTable(report.residentBillings)
                }
            }
        }
    }
}

@Composable
private fun ResidentTable(billings: List<ResidentMonthlyBilling>) {
    val c = LocalAppColors.current
    Column {
        Row(modifier = Modifier.fillMaxWidth().background(c.surface2)
            .padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Unit",     style = tableHeaderStyle().copy(color = LocalAppColors.current.textMuted), modifier = Modifier.width(48.dp))
            Text("Resident", style = tableHeaderStyle(), modifier = Modifier.weight(1f))
            Text("Meals",    style = tableHeaderStyle(), modifier = Modifier.width(54.dp))
            Text("Amount",   style = tableHeaderStyle(), modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
        }
        billings.sortedBy { it.resident.unitNumber }.forEach { billing ->
            HorizontalDivider(color = c.borderColor)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(billing.resident.unitNumber,
                    style = MaterialTheme.typography.labelMedium.copy(color = c.textMuted, fontSize = 11.sp),
                    modifier = Modifier.width(48.dp))
                Text(billing.resident.clientName, style = MaterialTheme.typography.titleMedium.copy(color = c.textBright),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp))
                Text("${billing.totalMeals}",
                    style = MaterialTheme.typography.labelLarge.copy(color = Primary),
                    modifier = Modifier.width(54.dp))
                Text(BillingCalculator.formatRand(billing.finalTotal),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = if (billing.isCredit) Danger else Accent, fontSize = 13.sp),
                    textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
            }
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun ExportButton(emoji: String, label: String, modifier: Modifier = Modifier, disabled: Boolean = false, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = if (disabled) c.surface1.copy(alpha = 0.5f) else c.surface1,
        modifier = modifier.cardElevation(c).border(1.dp, c.borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)).clickable(enabled = !disabled) { onClick() }) {
        Column(modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 22.sp)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = c.textMuted))
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, valueColor: Color = Color.Unspecified, bold: Boolean = false, largeValue: Boolean = false) {
    val c = LocalAppColors.current
    val resolvedValueColor = if (valueColor == Color.Unspecified) c.textBright else valueColor
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (bold) MaterialTheme.typography.labelLarge.copy(color = c.textBright)
        else MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(color = resolvedValueColor, fontSize = if (largeValue) 20.sp else 16.sp))
    }
}

@Composable
private fun tableHeaderStyle() = MaterialTheme.typography.labelSmall.copy(
    fontSize = 10.sp, letterSpacing = 0.5.sp,
    color = LocalAppColors.current.textMuted
)