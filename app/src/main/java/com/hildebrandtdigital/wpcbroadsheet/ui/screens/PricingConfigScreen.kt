package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hildebrandtdigital.wpcbroadsheet.navigation.SITE_ALL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealPricing
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.MealRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.NavTab
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBottomNav
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand

// ── Price line descriptor ─────────────────────────────────────────────────────
private data class PriceLine(
    val key: String,
    val label: String,
    val emoji: String,
    val getValue: (MealPricing) -> Double,
    val setValue: (MealPricing, Double) -> MealPricing,
)

private val PRICE_LINES = listOf(
    PriceLine("course1",       "1 Course Lunch",        "🍽️", { it.course1 },       { p, v -> p.copy(course1 = v) }),
    PriceLine("course2",       "2 Course Lunch",        "🍽️", { it.course2 },       { p, v -> p.copy(course2 = v) }),
    PriceLine("course3",       "3 Course Lunch",        "🍽️", { it.course3 },       { p, v -> p.copy(course3 = v) }),
    PriceLine("fullBoard",     "Full Board (per day)",  "🏠", { it.fullBoard },     { p, v -> p.copy(fullBoard = v) }),
    PriceLine("sun1Course",    "Sunday 1 Course",       "☀️", { it.sun1Course },    { p, v -> p.copy(sun1Course = v) }),
    PriceLine("sun3Course",    "Sunday 3 Course",       "☀️", { it.sun3Course },    { p, v -> p.copy(sun3Course = v) }),
    PriceLine("breakfast",     "Breakfast",             "🌅", { it.breakfast },     { p, v -> p.copy(breakfast = v) }),
    PriceLine("dinner",        "Dinner",                "🌙", { it.dinner },        { p, v -> p.copy(dinner = v) }),
    PriceLine("soupDessert",   "Soup / Dessert",        "🥣", { it.soupDessert },   { p, v -> p.copy(soupDessert = v) }),
    PriceLine("visitorMonSat", "Visitor Mon–Sat",       "👤", { it.visitorMonSat }, { p, v -> p.copy(visitorMonSat = v) }),
    PriceLine("visitorSun1",   "Visitor Sunday 1C",     "👤", { it.visitorSun1 },   { p, v -> p.copy(visitorSun1 = v) }),
    PriceLine("visitorSun3",   "Visitor Sunday 3C",     "👤", { it.visitorSun3 },   { p, v -> p.copy(visitorSun3 = v) }),
    PriceLine("taBakkies",     "T/A Bakkies (each)",   "🚐", { it.taBakkies },     { p, v -> p.copy(taBakkies = v) }),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingConfigScreen(
    siteId          : String = "lizane",
    currentUserRole : UserRole = UserRole.OPERATIONS_MANAGER,
    repository      : MealRepository? = null,
    onNavigateBack        : () -> Unit = {},
    onNavigateToDashboard : () -> Unit,
    onNavigateToCapture   : () -> Unit,
    onNavigateToReports   : () -> Unit,
    onNavigateToProfile   : () -> Unit,
) {
    val c = LocalAppColors.current
    val canEdit = AppSession.hasCrossSiteAccess || currentUserRole == UserRole.OPERATIONS_MANAGER
    val scope   = rememberCoroutineScope()

    // Current billing period — pricing is saved per-period
    val calendar     = java.util.Calendar.getInstance()
    val pricingYear  = calendar.get(java.util.Calendar.YEAR)
    val pricingMonth = calendar.get(java.util.Calendar.MONTH) + 1

    // ── Site selection state ──────────────────────────────────────────────────
    var currentSiteId by remember(AppSession.currentUser) {
        mutableStateOf(
            if (!AppSession.hasCrossSiteAccess) AppSession.currentSiteId ?: "lizane"
            else if (siteId == SITE_ALL) SITE_ALL else siteId
        )
    }
    val isAllSites = currentSiteId == SITE_ALL

    // ── Per-site pricing state — loaded from SampleData mutable map ───────────
    // We keep a local mutable copy per site so edits don't persist until Save
    val pricingStates: Map<String, MutableState<MealPricing>> = remember {
        SampleData.sites.associate { site ->
            site.id to mutableStateOf<MealPricing>(SampleData.pricingForSite(site.id))
        }
    }

    fun pricingFor(id: String): MealPricing =
        pricingStates[id]?.value ?: SampleData.pricingForSite(id)

    fun updatePricing(id: String, updated: MealPricing) {
        pricingStates[id]?.value = updated
    }

    // ── Dirty tracking per site ───────────────────────────────────────────────
    val dirtyStates: Map<String, MutableState<Boolean>> = remember {
        SampleData.sites.associate { site ->
            site.id to mutableStateOf(false)
        }
    }
    val currentDirty = if (isAllSites) false else (dirtyStates[currentSiteId]?.value ?: false)

    var editingLine   by remember { mutableStateOf<PriceLine?>(null) }
    var showSiteSheet by remember { mutableStateOf(false) }
    var showSaved     by remember { mutableStateOf<String?>(null) } // site name saved

    // ── Bottom sheet state ────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(
                selected = NavTab.PROFILE,
                onSelect = {
                    when (it) {
                        NavTab.DASHBOARD -> onNavigateToDashboard()
                        NavTab.CAPTURE   -> onNavigateToCapture()
                        NavTab.REPORTS   -> onNavigateToReports()
                        NavTab.PROFILE   -> onNavigateToProfile()
                    }
                }
            )
        }

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()

                .appBackground(c)

                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .background(
                        if (c.isDark) c.surface1 else c.bgDeep
                    )
                    .headerBand(c)
                    .padding(start = 4.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pricing Config", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text(
                            if (isAllSites) "All Sites  ·  Comparison View"
                            else "${SampleData.sites.first { it.id == currentSiteId }.name}  ·  Excl. ${(pricingFor(currentSiteId).vatRate * 100).toInt()}% VAT",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                            ),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!canEdit) {
                            Surface(
                                shape    = RoundedCornerShape(50),
                                color    = Secondary.copy(0.15f),
                                modifier = Modifier.cardElevation(c).border(1.dp, Secondary.copy(0.3f), RoundedCornerShape(50))
                            ) {
                                Text(
                                    "VIEW ONLY",
                                    style    = MaterialTheme.typography.labelSmall.copy(color = Secondary),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                        WpcLogoBadge(size = WpcBrandingSize.ICON)
                    }
                }

                // ── Site chip ─────────────────────────────────────────────────
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SiteChip(
                        siteId    = currentSiteId,
                        onClick   = if (AppSession.hasCrossSiteAccess) { { showSiteSheet = true } } else null,
                    )
                    if (canEdit && !isAllSites) {
                        Text(
                            "Tap any price to edit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                            ),
                        )
                    } else if (isAllSites) {
                        Text(
                            "Select a site to edit its prices",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Body: All Sites comparison OR single-site editor ──────────────
            if (isAllSites) {
                AllSitesPricingView(
                    sites        = SampleData.sites,
                    pricingFor   = ::pricingFor,
                    onSelectSite = { currentSiteId = it },
                )
            } else {
                SingleSitePricingView(
                    siteId     = currentSiteId,
                    pricing    = pricingFor(currentSiteId),
                    canEdit    = canEdit,
                    hasChanges = currentDirty,
                    showSaved  = showSaved == SampleData.sites.firstOrNull { it.id == currentSiteId }?.name,
                    onEditLine = { line -> if (canEdit) editingLine = line },
                    onSave     = {
                        val siteToPersist = currentSiteId
                        val pricingToPersist = pricingFor(siteToPersist)
                        scope.launch {
                            if (repository != null) {
                                withContext(Dispatchers.IO) {
                                    repository.savePricing(
                                        siteId  = siteToPersist,
                                        year    = pricingYear,
                                        month   = pricingMonth,
                                        pricing = pricingToPersist,
                                        actor   = AppSession.currentUserName,
                                    )
                                }
                            } else {
                                // Fallback: in-memory only
                                SampleData.savePricing(siteToPersist, pricingToPersist)
                            }
                            dirtyStates[siteToPersist]?.value = false
                            showSaved = SampleData.sites.first { it.id == siteToPersist }.name
                        }
                    },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Site switcher bottom sheet ────────────────────────────────────────────
    if (showSiteSheet && AppSession.hasCrossSiteAccess) {
        ModalBottomSheet(
            onDismissRequest = { showSiteSheet = false },
            sheetState       = sheetState,
            containerColor   = c.surface1,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Switch Site", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright))
                Text(
                    "Select a site to view or edit its pricing",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))

                // All Sites option
                PricingSheetSiteRow(
                    label    = "All Sites",
                    sub      = "Compare pricing across all villages",
                    selected = isAllSites,
                    color    = Primary,
                    onClick  = { currentSiteId = SITE_ALL; showSiteSheet = false },
                )

                Spacer(Modifier.height(8.dp))

                SampleData.sites.forEachIndexed { idx, site ->
                    val color = listOf(Primary, Accent, Secondary)[idx % 3]
                    val isDirty = dirtyStates[site.id]?.value ?: false
                    PricingSheetSiteRow(
                        label    = site.name,
                        sub      = if (isDirty) "Unsaved changes" else "Mgr: ${site.unitManagerName}",
                        selected = currentSiteId == site.id,
                        color    = color,
                        showDot  = isDirty,
                        onClick  = { currentSiteId = site.id; showSiteSheet = false },
                    )
                    if (idx < SampleData.sites.lastIndex) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Edit price dialog ─────────────────────────────────────────────────────
    val lineToEdit = editingLine
    if (lineToEdit != null && !isAllSites) {
        EditPriceDialog(
            label        = lineToEdit.label,
            emoji        = lineToEdit.emoji,
            currentValue = lineToEdit.getValue(pricingFor(currentSiteId)),
            vatRate      = pricingFor(currentSiteId).vatRate,
            onDismiss    = { editingLine = null },
            onSave       = { newValue ->
                updatePricing(currentSiteId, lineToEdit.setValue(pricingFor(currentSiteId), newValue))
                dirtyStates[currentSiteId]?.value = true
                showSaved   = null
                editingLine = null
            }
        )
    }
}

// ── All Sites comparison view ─────────────────────────────────────────────────
@Composable
private fun AllSitesPricingView(
    sites      : List<com.hildebrandtdigital.wpcbroadsheet.data.model.Site>,
    pricingFor : (String) -> MealPricing,
    onSelectSite: (String) -> Unit,
) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Site quick-select cards
        Text(
            "SITE PRICING OVERVIEW",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        )
        Spacer(Modifier.height(10.dp))

        sites.forEachIndexed { idx, site ->
            val color   = listOf(Primary, Accent, Secondary)[idx % 3]
            val pricing = pricingFor(site.id)
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = color.copy(0.06f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectSite(site.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(0.15f))
                    ) {
                        Text(site.name.take(1), style = MaterialTheme.typography.headlineMedium.copy(color = color))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(site.name, style = MaterialTheme.typography.titleMedium.copy(color = c.textBright))
                        Text(
                            "1C: R${"%.2f".format(pricing.course1 * 1.15)}  ·  2C: R${"%.2f".format(pricing.course2 * 1.15)}  ·  TA: R${"%.2f".format(pricing.taBakkies)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Edit", style = MaterialTheme.typography.labelMedium.copy(color = color))
                        Icon(Icons.Rounded.ChevronRight, null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.borderColor)
        Spacer(Modifier.height(16.dp))

        // Side-by-side comparison table
        Text(
            "PRICE COMPARISON (INCL. VAT)",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        )
        Spacer(Modifier.height(10.dp))

        // Table header
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = c.surface2,
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Meal Type", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                sites.forEachIndexed { idx, site ->
                    val color = listOf(Primary, Accent, Secondary)[idx % 3]
                    Text(
                        site.name.substringBefore(" "),
                        style    = MaterialTheme.typography.labelSmall.copy(color = color),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            color = c.surface1,
            modifier = Modifier.cardElevation(c).border(1.dp, c.borderColor, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
        ) {
            Column {
                PRICE_LINES.forEachIndexed { idx, line ->
                    Row(
                        modifier = Modifier
                            .background(if (idx % 2 == 0) Color.Transparent else c.surface2.copy(0.5f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${line.emoji} ${line.label}",
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(2f),
                        )
                        sites.forEachIndexed { sIdx, site ->
                            val color   = listOf(Primary, Accent, Secondary)[sIdx % 3]
                            val pricing = pricingFor(site.id)
                            val raw     = line.getValue(pricing)
                            val incl    = raw * (1 + pricing.vatRate)
                            Text(
                                "R${"%.2f".format(incl)}",
                                style    = MaterialTheme.typography.labelSmall.copy(color = color),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                    if (idx < PRICE_LINES.lastIndex) HorizontalDivider(color = c.borderColor.copy(0.4f))
                }
            }
        }
    }
}

// ── Single site pricing editor ────────────────────────────────────────────────
@Composable
private fun SingleSitePricingView(
    siteId    : String,
    pricing   : MealPricing,
    canEdit   : Boolean,
    hasChanges: Boolean,
    showSaved : Boolean,
    onEditLine: (PriceLine) -> Unit,
    onSave    : () -> Unit,
) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Info tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("VAT Rate",   "${(pricing.vatRate * 100).toInt()}%",             "Applied to all meals",  Accent,    Modifier.weight(1f))
            InfoTile("Compulsory", "R${"%.2f".format(pricing.compulsoryMealsDeduction)}", "Monthly deduction", Secondary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "MEAL PRICES (EXCL. VAT)",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        )
        Spacer(Modifier.height(8.dp))

        PRICE_LINES.forEach { line ->
            val exclVat = line.getValue(pricing)
            val inclVat = exclVat * (1 + pricing.vatRate)
            PriceRow(
                emoji   = line.emoji,
                label   = line.label,
                exclVat = exclVat,
                inclVat = inclVat,
                canEdit = canEdit,
                onClick = { onEditLine(line) },
            )
            Spacer(Modifier.height(8.dp))
        }

        // Save button
        if (canEdit && hasChanges) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Pricing", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
            }
        }

        // Saved confirmation
        if (showSaved) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = Accent.copy(0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Accent.copy(0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Accent, modifier = Modifier.size(18.dp))
                    Text("Pricing saved successfully", style = MaterialTheme.typography.bodyMedium.copy(color = Accent))
                }
            }
        }
    }
}

// ── Site chip (matches Dashboard/Capture/Reports style) ───────────────────────
@Composable
private fun SiteChip(
    siteId : String,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    val isAll  = siteId == SITE_ALL
    val label  = if (isAll) "All Sites" else SampleData.sites.firstOrNull { it.id == siteId }?.name ?: siteId
    val dotColor = when (siteId) {
        "lizane"  -> Primary
        "bakkies" -> Accent
        "sunhill" -> Secondary
        else      -> c.textMuted
    }

    val chipBg     = if (c.isDark) (if (isAll) Primary.copy(0.1f) else dotColor.copy(0.1f)) else androidx.compose.ui.graphics.Color.White.copy(0.22f)
    val chipBorder = if (c.isDark) (if (isAll) Primary.copy(0.3f) else dotColor.copy(0.3f)) else androidx.compose.ui.graphics.Color.White.copy(0.55f)
    val chipText   = if (c.isDark) (if (isAll) Primary else dotColor) else androidx.compose.ui.graphics.Color.White
    val chipDot    = if (c.isDark) dotColor else androidx.compose.ui.graphics.Color.White

    Surface(
        shape    = RoundedCornerShape(50),
        color    = chipBg,
        modifier = Modifier
            .cardElevation(c)
            .border(1.dp, chipBorder, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!isAll) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(chipDot)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = chipText,
                ),
            )
            Icon(
                Icons.Rounded.ExpandMore, null,
                tint     = chipText,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ── Sheet row for site switcher ───────────────────────────────────────────────
@Composable
private fun PricingSheetSiteRow(
    label   : String,
    sub     : String,
    selected: Boolean,
    color   : Color,
    showDot : Boolean = false,
    onClick : () -> Unit,
) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = if (selected) color.copy(0.1f) else c.surface2,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) color.copy(0.4f) else c.borderColor,
                RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,
                tint     = if (selected) color else c.textDim,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(color = if (selected) color else c.textBright))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (showDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Secondary)
                        )
                    }
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (showDot) Secondary else c.textMuted
                        ),
                    )
                }
            }
        }
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun PriceRow(
    emoji: String,
    label: String,
    exclVat: Double,
    inclVat: Double,
    canEdit: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = c.surface1,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .then(if (canEdit) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text("Incl. VAT: R${"%.2f".format(inclVat)}", style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R${"%.2f".format(exclVat)}", style = MaterialTheme.typography.labelLarge.copy(color = c.textBright, fontSize = 14.sp))
                if (canEdit) {
                    Spacer(Modifier.height(2.dp))
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = color.copy(alpha = if (c.isDark) 0.08f else 0.10f),
        modifier = modifier.border(1.dp, color.copy(if (c.isDark) 0.2f else 0.30f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                letterSpacing = 0.4.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            ))
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
        }
    }
}

@Composable
private fun EditPriceDialog(
    label: String,
    emoji: String,
    currentValue: Double,
    vatRate: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    val c = LocalAppColors.current
    var raw      by remember { mutableStateOf("%.6f".format(currentValue)) }
    var hasError by remember { mutableStateOf(false) }
    val preview  = raw.toDoubleOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = c.surface1,
            modifier = Modifier.fillMaxWidth().imePadding().border(1.dp, c.borderColor, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(emoji, fontSize = 24.sp)
                    Column {
                        Text("Edit Price",  style = MaterialTheme.typography.headlineMedium)
                        Text(label,         style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Price (excl. ${(vatRate * 100).toInt()}% VAT)", style = MaterialTheme.typography.labelLarge.copy(color = c.textBright))
                Spacer(Modifier.height(6.dp))

                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = c.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (hasError) Danger.copy(0.7f) else Primary.copy(0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("R", style = MaterialTheme.typography.bodyLarge.copy(color = c.textMuted))
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value         = raw,
                            onValueChange = { raw = it; hasError = false },
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(color = c.textBright),
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                        )
                    }
                }

                if (hasError) {
                    Spacer(Modifier.height(4.dp))
                    Text("Enter a valid price (e.g. 35.652174)", style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                if (preview != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = Accent.copy(0.08f),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Accent.copy(0.2f), RoundedCornerShape(10.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Incl. ${(vatRate * 100).toInt()}% VAT:", style = MaterialTheme.typography.bodyMedium.copy(color = c.textMuted))
                            Text("R${"%.2f".format(preview * (1 + vatRate))}", style = MaterialTheme.typography.labelLarge.copy(color = Accent))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        border   = BorderStroke(1.dp, c.borderColor),
                    ) { Text("Cancel", color = c.textMuted) }

                    Button(
                        onClick  = {
                            val v = raw.toDoubleOrNull()
                            if (v == null || v <= 0) hasError = true else onSave(v)
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) { Text("Save") }
                }
            }
        }
    }
}