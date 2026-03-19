package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import kotlinx.coroutines.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialSiteId         : String = SITE_ALL,
    repository            : MealRepository? = null,
    residentRepository    : ResidentRepository? = null,
    onNavigateToDashboard : () -> Unit,
    onNavigateToReports   : (String) -> Unit,
    onNavigateToProfile   : () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()

    // ── Site ──────────────────────────────────────────────────────────────────
    var currentSiteId by remember(AppSession.currentUser) {
        mutableStateOf(
            if (!AppSession.hasCrossSiteAccess) AppSession.currentSiteId ?: "lizane"
            else if (initialSiteId == SITE_ALL) "lizane"
            else initialSiteId
        )
    }
    val currentSite = SampleData.sites.firstOrNull { it.id == currentSiteId }
        ?: SampleData.sites.first()

    // ── Billing period — mutable so month nav arrows work ────────────────────
    val calendar = remember { java.util.Calendar.getInstance() }
    var captureYear  by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var captureMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH) + 1) }

    fun prevMonth() {
        if (captureMonth == 1) { captureMonth = 12; captureYear-- }
        else captureMonth--
    }
    fun nextMonth() {
        val now = java.util.Calendar.getInstance()
        val nowYear  = now.get(java.util.Calendar.YEAR)
        val nowMonth = now.get(java.util.Calendar.MONTH) + 1
        // Don't allow navigating into the future
        if (captureYear < nowYear || (captureYear == nowYear && captureMonth < nowMonth)) {
            if (captureMonth == 12) { captureMonth = 1; captureYear++ }
            else captureMonth++
        }
    }

    // ── Live residents from Room ───────────────────────────────────────────────
    val dbResidents: List<Resident> by (
            residentRepository?.observeResidents(currentSiteId)
                ?: kotlinx.coroutines.flow.flowOf(
                    SampleData.residents.filter { it.siteId == currentSiteId }
                )
            ).collectAsStateWithLifecycle(
            initialValue = SampleData.residents.filter { it.siteId == currentSiteId }
        )

    // ── Live meal entries from Room ────────────────────────────────────────────
    val dbEntries: List<MealEntry> by (
            repository?.observeEntries(currentSiteId, captureYear, captureMonth)
                ?: kotlinx.coroutines.flow.flowOf(
                    SampleData.januaryEntries.filter { it.siteId == currentSiteId }
                )
            ).collectAsStateWithLifecycle(
            initialValue = SampleData.januaryEntries.filter { it.siteId == currentSiteId }
        )

    // ── Local mutable counts ───────────────────────────────────────────────────
    val counts = remember(currentSiteId) { mutableStateMapOf<String, MutableMap<MealType, Int>>() }

    LaunchedEffect(dbEntries) {
        dbEntries.forEach { entry ->
            counts[entry.unitNumber] = entry.counts.toMutableMap()
        }
    }

    // Reset counts when month changes (entries for new month will arrive via Flow)
    LaunchedEffect(captureYear, captureMonth, currentSiteId) {
        counts.clear()
    }

    // ── Live pricing from Room ─────────────────────────────────────────────────
    val dbPricing: MealPricing? by (
            repository?.observePricing(currentSiteId, captureYear, captureMonth)
                ?: kotlinx.coroutines.flow.flowOf(null)
            ).collectAsStateWithLifecycle(initialValue = null)
    val activePricing = dbPricing ?: SampleData.pricingForSite(currentSiteId)

    // ── UI state ───────────────────────────────────────────────────────────────
    var toastMessage   by remember { mutableStateOf<String?>(null) }
    var searchQuery    by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet      by remember { mutableStateOf(false) }

    LaunchedEffect(currentSiteId) { searchQuery = ""; selectedFilter = "All" }

    // ── Filtered resident list ─────────────────────────────────────────────────
    val filteredResidents = dbResidents
        .filter { r ->
            when (selectedFilter) {
                "Rental" -> r.residentType == ResidentType.RENTAL
                "Owner"  -> r.residentType == ResidentType.OWNER
                "OTP"    -> r.residentType == ResidentType.OTP
                else     -> true
            }
        }
        .filter { r ->
            searchQuery.isBlank() ||
                    r.clientName.contains(searchQuery, ignoreCase = true) ||
                    r.unitNumber.contains(searchQuery, ignoreCase = true)
        }

    // ── Is current month? (future nav arrow dimmed if at current month) ────────
    val nowCal    = remember { java.util.Calendar.getInstance() }
    val isCurrentMonth = captureYear  == nowCal.get(java.util.Calendar.YEAR) &&
            captureMonth == nowCal.get(java.util.Calendar.MONTH) + 1

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(
                selected = NavTab.CAPTURE,
                onSelect = {
                    when (it) {
                        NavTab.DASHBOARD -> onNavigateToDashboard()
                        NavTab.REPORTS   -> onNavigateToReports(currentSiteId)
                        NavTab.PROFILE   -> onNavigateToProfile()
                        else             -> {}
                    }
                }
            )
        }

    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().appBackground(c).padding(padding)) {

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {

                // ── Sticky header ──────────────────────────────────────────────
                stickyHeader {
                    Column(
                        modifier = Modifier

                            .headerBand(c)
                            .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 24.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Meal Capture", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                                Text(
                                    "${currentSite.name}  ·  ${monthName(captureMonth)} $captureYear",
                                    style    = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            WpcLogoBadge(
                                size     = WpcBrandingSize.ICON,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (repository != null) {
                                            withContext(Dispatchers.IO) {
                                                repository.saveAllCounts(
                                                    siteId    = currentSiteId,
                                                    year      = captureYear,
                                                    month     = captureMonth,
                                                    allCounts = counts.mapValues { it.value.toMap() },
                                                    actor     = AppSession.currentUserName,
                                                )
                                            }
                                        }
                                        toastMessage = "✅ All data saved!"
                                        delay(2500)
                                        toastMessage = null
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Save, null, tint = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Site chip ──────────────────────────────────────────
                        Surface(
                            shape    = RoundedCornerShape(50),
                            color    = if (c.isDark) c.surface2 else androidx.compose.ui.graphics.Color.White.copy(0.22f),
                            modifier = Modifier
                                .cardElevation(c)
                                .border(1.dp, if (c.isDark) c.borderColor else androidx.compose.ui.graphics.Color.White.copy(0.50f), RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .then(
                                    if (AppSession.hasCrossSiteAccess) Modifier.clickable { showSheet = true }
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val dotColor = when (currentSiteId) {
                                    "lizane"  -> Primary
                                    "bakkies" -> Accent
                                    "sunhill" -> Secondary
                                    else      -> c.textMuted
                                }
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (c.isDark) dotColor else androidx.compose.ui.graphics.Color.White))
                                Text(currentSite.name, style = MaterialTheme.typography.labelMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                                if (AppSession.hasCrossSiteAccess) {
                                    Icon(Icons.Rounded.ExpandMore, "Switch site", tint = if (c.isDark) c.textDim else androidx.compose.ui.graphics.Color.White.copy(0.75f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Month navigator ────────────────────────────────────
                        Surface(
                            shape    = RoundedCornerShape(10.dp),
                            color    = c.surface1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, c.borderColor, RoundedCornerShape(10.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { prevMonth() }) {
                                    Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = if (c.isDark) Primary else c.headerStart)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${monthName(captureMonth)} $captureYear",
                                        style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else c.textBright),
                                    )
                                    if (isCurrentMonth) {
                                        Text(
                                            "Current period",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Accent),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick  = { nextMonth() },
                                    enabled  = !isCurrentMonth,
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        "Next month",
                                        tint = if (isCurrentMonth) c.textDim else (if (c.isDark) Primary else c.headerStart),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Filter tabs ────────────────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Rental", "Owner", "OTP").forEach { filter ->
                                FilterTab(
                                    label    = filter,
                                    selected = selectedFilter == filter,
                                    onClick  = { selectedFilter = filter },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Search ─────────────────────────────────────────────
                        CaptureSearchBar(query = searchQuery, onChange = { searchQuery = it })

                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── Empty state ────────────────────────────────────────────────
                if (filteredResidents.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("🏠", fontSize = 40.sp)
                            Text(
                                if (searchQuery.isNotBlank()) "No residents match \"$searchQuery\""
                                else "No residents for ${currentSite.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted),
                            )
                        }
                    }
                }

                // ── Resident cards ─────────────────────────────────────────────
                items(filteredResidents, key = { "${currentSiteId}_${it.unitNumber}" }) { resident ->
                    ResidentCaptureCard(
                        resident = resident,
                        counts   = counts.getOrPut(resident.unitNumber) { mutableMapOf() },
                        pricing  = activePricing,
                        onCountChange = { type, value ->
                            val updated = counts.getOrElse(resident.unitNumber) { mutableMapOf() }
                                .toMutableMap()
                            if (value <= 0) updated.remove(type) else updated[type] = value
                            counts[resident.unitNumber] = updated
                        },
                        onSave = {
                            scope.launch {
                                if (repository != null) {
                                    withContext(Dispatchers.IO) {
                                        repository.saveCounts(
                                            siteId     = currentSiteId,
                                            unitNumber = resident.unitNumber,
                                            year       = captureYear,
                                            month      = captureMonth,
                                            counts     = counts.getOrElse(resident.unitNumber) { emptyMap() },
                                            actor      = AppSession.currentUserName,
                                        )
                                    }
                                }
                                toastMessage = "✅ Unit ${resident.unitNumber} saved!"
                                delay(2500)
                                toastMessage = null
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            // ── Toast ──────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = toastMessage != null,
                enter    = fadeIn() + slideInVertically { it / 2 },
                exit     = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            ) {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = c.surface3,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✅", fontSize = 18.sp)
                        Text(toastMessage ?: "", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                    }
                }
            }
        }
    }

    // ── Site switcher bottom sheet ─────────────────────────────────────────────
    if (showSheet && AppSession.hasCrossSiteAccess) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            containerColor   = c.surface1,
            dragHandle       = null,
        ) {
            SiteSelectorSheetContent(
                currentSiteId  = currentSiteId,
                onSiteSelected = { siteId ->
                    if (siteId != SITE_ALL) currentSiteId = siteId
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

// ── Resident Capture Card ──────────────────────────────────────────────────────
@Composable
private fun ResidentCaptureCard(
    resident     : Resident,
    counts       : MutableMap<MealType, Int>,
    pricing      : MealPricing,
    onCountChange: (MealType, Int) -> Unit,
    onSave       : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    val c = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    val (subtotal, vat, finalTotal) = BillingCalculator.previewBilling(counts, pricing)
    val bakkiesTotal = (counts[MealType.TA_BAKKIES] ?: 0) * pricing.taBakkies
    val totalMeals   = counts.filter { it.key != MealType.TA_BAKKIES }.values.sum()
    val totalDisplay = BillingCalculator.formatRand(subtotal + vat + bakkiesTotal)
    val borderColor  = if (expanded) Primary else c.borderColor

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = c.surface1,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = c.surface2,
                    modifier = Modifier.size(38.dp).border(1.dp, c.borderColor, RoundedCornerShape(10.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(resident.unitNumber, style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontSize = 11.sp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(resident.clientName, style = MaterialTheme.typography.titleMedium.copy(color = c.textBright), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${resident.totalOccupants} occupant${if (resident.totalOccupants > 1) "s" else ""}  ·  ${resident.residentType.name}",
                        style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(totalDisplay, style = MaterialTheme.typography.headlineSmall.copy(color = Accent))
                    Text("this month", style = MaterialTheme.typography.labelSmall.copy(color = c.textMuted))
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.textDim)
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = c.borderColor)
                    Spacer(Modifier.height(14.dp))

                    val visibleMealTypes = MealType.entries.filter { it != MealType.FULL_BOARD }
                    visibleMealTypes.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { mealType ->
                                MealCounterInput(
                                    label    = mealType.shortLabel,
                                    value    = counts[mealType] ?: 0,
                                    onChange = { onCountChange(mealType, it) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(4.dp))

                    BillingPreview(
                        totalMeals = totalMeals,
                        subtotal   = subtotal,
                        vat        = vat,
                        bakkies    = bakkiesTotal,
                        deduction  = pricing.compulsoryMealsDeduction,
                        finalTotal = finalTotal,
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick  = onSave,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0x264F8EF7), contentColor = Primary),
                        border   = BorderStroke(1.dp, Color(0x4D4F8EF7)),
                    ) {
                        Text("Save Unit ${resident.unitNumber}",
                            style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

// ── Meal Counter Input ─────────────────────────────────────────────────────────
@Composable
private fun MealCounterInput(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = c.surface2,
            modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(8.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange(maxOf(0, value - 1)) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Remove, null, tint = if (value > 0) Primary else c.textDim, modifier = Modifier.size(16.dp))
                }
                Text("$value",
                    style    = MaterialTheme.typography.headlineSmall.copy(color = if (value > 0) Primary else c.textDim, fontSize = 15.sp),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { onChange(value + 1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Add, null, tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Billing Preview ────────────────────────────────────────────────────────────
@Composable
private fun BillingPreview(totalMeals: Int, subtotal: Double, vat: Double, bakkies: Double, deduction: Double, finalTotal: Double) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(8.dp), color = c.surface3, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            BillingRow("Total Meals",          "$totalMeals",                               c.textBright)
            BillingRow("Subtotal (excl. VAT)", BillingCalculator.formatRand(subtotal),      c.textMuted)
            BillingRow("VAT (15%)",            BillingCalculator.formatRand(vat),           c.textMuted)
            BillingRow("T/A Bakkies",          BillingCalculator.formatRand(bakkies),       c.textMuted)
            BillingRow("Less: 6 Compulsory",   "−${BillingCalculator.formatRand(deduction)}", Danger)
            HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(vertical = 6.dp))
            BillingRow("Final Amount", BillingCalculator.formatRand(finalTotal), if (finalTotal >= 0) Accent else Danger, bold = true)
        }
    }
}

@Composable
private fun BillingRow(label: String, value: String, color: Color, bold: Boolean = false) {
    val c = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.labelLarge.copy(color = c.textBright) else MaterialTheme.typography.bodySmall)
        Text(value, style = if (bold) MaterialTheme.typography.headlineSmall.copy(color = color, fontSize = 14.sp)
        else MaterialTheme.typography.labelMedium.copy(color = color))
    }
}

// ── Filter Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(8.dp), color = if (selected) Primary else c.surface1,
        modifier = modifier.cardElevation(c).border(1.dp, if (selected) Primary else c.borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(color = if (selected) Color.White else c.textMuted, fontSize = 12.sp),
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

// ── Search Bar ─────────────────────────────────────────────────────────────────
@Composable
private fun CaptureSearchBar(query: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(10.dp))) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, null, tint = c.textDim, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = query, onValueChange = onChange,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textBright),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search resident or unit...", color = c.textDim, style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
                    inner()
                },
                singleLine = true,
            )
        }
    }
}