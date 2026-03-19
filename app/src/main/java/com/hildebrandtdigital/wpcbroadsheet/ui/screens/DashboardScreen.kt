package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.hildebrandtdigital.wpcbroadsheet.navigation.SITE_ALL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealEntry
import com.hildebrandtdigital.wpcbroadsheet.data.model.Resident
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AvatarManager
import com.hildebrandtdigital.wpcbroadsheet.data.repository.MealRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ResidentRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.gradientCard
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

private fun prevMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to month - 1

private fun deltaLabel(current: Double, prev: Double): Pair<String?, Boolean> {
    if (prev == 0.0) return null to true
    val pct = ((current - prev) / prev * 100).toInt()
    return when {
        pct > 0  -> "↑ ${pct}% vs last month" to true
        pct < 0  -> "↓ ${-pct}% vs last month" to false
        else     -> null to true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    initialSiteId          : String = SITE_ALL,
    mealRepository         : MealRepository? = null,
    residentRepository     : ResidentRepository? = null,
    onNavigateToCapture    : (String) -> Unit,
    onNavigateToReports    : (String) -> Unit,
    onNavigateToProfile    : () -> Unit,
    onNavigateToSiteDetails: (String) -> Unit = {},
) {
    val c       = LocalAppColors.current
    val context = LocalContext.current
    val userId  = AppSession.currentUserId

    // Live avatar path — same DataStore flow as ProfileScreen & EditProfileScreen.
    // Automatically recomposed whenever the user updates their photo anywhere in the app.
    val avatarPath by AvatarManager
        .observeAvatarPath(context, userId)
        .collectAsStateWithLifecycle(initialValue = null)
    var avatarVersion by remember { mutableLongStateOf(0L) }

    val effectiveSiteId = if (!AppSession.hasCrossSiteAccess)
        AppSession.currentSiteId ?: initialSiteId else initialSiteId

    var currentSiteId by remember(AppSession.currentUser) { mutableStateOf(effectiveSiteId) }
    val isAllSites    = currentSiteId == SITE_ALL
    val currentSite: Site? = if (isAllSites) null else SampleData.sites.firstOrNull { it.id == currentSiteId }

    // ── Current billing period ────────────────────────────────────────────────
    val calendar     = remember { java.util.Calendar.getInstance() }
    val currentYear  = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
    val (prevYear, prevMonth) = prevMonth(currentYear, currentMonth)

    // ── Live entries — all three sites, current + previous month ──────────────
    val lizaneEntries by produceState<List<MealEntry>>(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "lizane" },
        key1 = currentYear, key2 = currentMonth,
    ) {
        (mealRepository?.observeEntries("lizane", currentYear, currentMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (currentYear == 2026 && currentMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "lizane" } else emptyList()
            )).collect { value = it }
    }
    val bakkiesEntries by produceState<List<MealEntry>>(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "bakkies" },
        key1 = currentYear, key2 = currentMonth,
    ) {
        (mealRepository?.observeEntries("bakkies", currentYear, currentMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (currentYear == 2026 && currentMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "bakkies" } else emptyList()
            )).collect { value = it }
    }
    val sunhillEntries by produceState<List<MealEntry>>(
        initialValue = SampleData.januaryEntries.filter { it.siteId == "sunhill" },
        key1 = currentYear, key2 = currentMonth,
    ) {
        (mealRepository?.observeEntries("sunhill", currentYear, currentMonth)
            ?: kotlinx.coroutines.flow.flowOf(
                if (currentYear == 2026 && currentMonth == 1)
                    SampleData.januaryEntries.filter { it.siteId == "sunhill" } else emptyList()
            )).collect { value = it }
    }

    // Previous month entries for delta
    val lizanePrev by produceState<List<MealEntry>>(emptyList(), prevYear, prevMonth) {
        (mealRepository?.observeEntries("lizane", prevYear, prevMonth)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())).collect { value = it }
    }
    val bakkiesPrev by produceState<List<MealEntry>>(emptyList(), prevYear, prevMonth) {
        (mealRepository?.observeEntries("bakkies", prevYear, prevMonth)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())).collect { value = it }
    }
    val sunhillPrev by produceState<List<MealEntry>>(emptyList(), prevYear, prevMonth) {
        (mealRepository?.observeEntries("sunhill", prevYear, prevMonth)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())).collect { value = it }
    }

    // ── Live residents ────────────────────────────────────────────────────────
    val lizaneResidents by produceState<List<Resident>>(
        SampleData.residents.filter { it.siteId == "lizane" }
    ) {
        (residentRepository?.observeResidents("lizane")
            ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "lizane" })
                ).collect { value = it }
    }
    val bakkiesResidents by produceState<List<Resident>>(
        SampleData.residents.filter { it.siteId == "bakkies" }
    ) {
        (residentRepository?.observeResidents("bakkies")
            ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "bakkies" })
                ).collect { value = it }
    }
    val sunhillResidents by produceState<List<Resident>>(
        SampleData.residents.filter { it.siteId == "sunhill" }
    ) {
        (residentRepository?.observeResidents("sunhill")
            ?: kotlinx.coroutines.flow.flowOf(SampleData.residents.filter { it.siteId == "sunhill" })
                ).collect { value = it }
    }

    // ── Compute live SiteStats for each site ──────────────────────────────────
    val lizaneStats = remember(lizaneResidents, lizaneEntries, lizanePrev, currentYear, currentMonth) {
        computeSiteStats(lizaneResidents, lizaneEntries, lizanePrev,
            SampleData.pricingForSite("lizane"), currentYear, currentMonth,
            SampleData.sites.first { it.id == "lizane" })
    }
    val bakkiesStats = remember(bakkiesResidents, bakkiesEntries, bakkiesPrev, currentYear, currentMonth) {
        computeSiteStats(bakkiesResidents, bakkiesEntries, bakkiesPrev,
            SampleData.pricingForSite("bakkies"), currentYear, currentMonth,
            SampleData.sites.first { it.id == "bakkies" })
    }
    val sunhillStats = remember(sunhillResidents, sunhillEntries, sunhillPrev, currentYear, currentMonth) {
        computeSiteStats(sunhillResidents, sunhillEntries, sunhillPrev,
            SampleData.pricingForSite("sunhill"), currentYear, currentMonth,
            SampleData.sites.first { it.id == "sunhill" })
    }

    val siteStatsMap = mapOf(
        "lizane"  to lizaneStats,
        "bakkies" to bakkiesStats,
        "sunhill" to sunhillStats,
    )

    // Stat shown in UI depends on selected site
    val stats = when (currentSiteId) {
        "bakkies" -> bakkiesStats
        "sunhill" -> sunhillStats
        "lizane"  -> lizaneStats
        else      -> aggregateStats(siteStatsMap.values.toList())
    }

    val visibleSites = if (isAllSites) SampleData.sites
    else SampleData.sites.filter { it.id == currentSiteId }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()
    var showSheet  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(selected = NavTab.DASHBOARD, onSelect = {
                when (it) {
                    NavTab.CAPTURE -> onNavigateToCapture(currentSiteId)
                    NavTab.REPORTS -> onNavigateToReports(currentSiteId)
                    NavTab.PROFILE -> onNavigateToProfile()
                    else           -> {}
                }
            })
        }

    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().appBackground(c).padding(padding).verticalScroll(rememberScrollState())) {

            // ── Header ────────────────────────────────────────────────────────
            Column(modifier = Modifier
                .headerBand(c)
                .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 24.dp)) {

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    WpcLogoBadge(size = WpcBrandingSize.STANDARD)
                    // Avatar — shows user initials by default; switches to photo
                    // automatically once the user sets one from Profile or Edit Profile.
                    Box(modifier = Modifier.clickable { onNavigateToProfile() }) {
                        UserAvatar(
                            name          = AppSession.currentUserName,
                            avatarPath    = avatarPath,
                            avatarVersion = avatarVersion,
                            primaryColor  = if (c.isDark) Primary else Color.White,
                            size          = 40.dp,
                            fontSize      = 14.sp,
                            borderColor   = if (c.isDark) Primary else Color.White,
                            borderWidth   = 2.dp,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val (greeting, greetingEmoji) = when {
                    hour < 12 -> "Good morning"   to "☀️"
                    hour < 18 -> "Good afternoon" to "🌤️"
                    else      -> "Good evening"   to "🌙"
                }
                Text("$greeting, ${AppSession.currentUserName.substringBefore(" ")} $greetingEmoji",
                    style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else Color.White))
                Text(
                    if (isAllSites) "All Sites Overview" else currentSite?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textBright.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.80f)), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(12.dp))

                // ── Site chip ──────────────────────────────────────────────────
                val chipColor = if (isAllSites) Primary else when (currentSiteId) {
                    "bakkies" -> Accent; "sunhill" -> Secondary; else -> Primary
                }
                val canSwitchSites = AppSession.hasCrossSiteAccess
                Surface(shape = RoundedCornerShape(50),
                    color = if (c.isDark) chipColor.copy(0.10f) else Color.White.copy(0.18f),
                    modifier = Modifier
                        .border(1.dp, if (c.isDark) chipColor.copy(0.35f) else Color.White.copy(0.45f), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .then(if (canSwitchSites) Modifier.clickable { showSheet = true } else Modifier)
                ) {
                    Row(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAllSites) {
                            Icon(Icons.Rounded.GridView, null, tint = if (c.isDark) chipColor else Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (c.isDark) chipColor else Color.White))
                        }
                        Text(currentSite?.name ?: "All Sites",
                            style = MaterialTheme.typography.labelMedium.copy(color = if (c.isDark) chipColor else Color.White))
                        if (canSwitchSites)
                            Icon(Icons.Rounded.ExpandMore, "Switch site",
                                tint = if (c.isDark) chipColor.copy(0.7f) else Color.White.copy(0.75f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Body ──────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── Stat grid ──────────────────────────────────────────────────
                val (mealDelta, mealDeltaUp) = deltaLabel(stats.mealsThisMonth.toDouble(),
                    when (currentSiteId) {
                        "bakkies" -> bakkiesStats.prevRevenue; "sunhill" -> sunhillStats.prevRevenue
                        "lizane"  -> lizaneStats.prevRevenue
                        else      -> siteStatsMap.values.sumOf { it.prevRevenue }
                    }.let { 0.0 }) // meals delta would need prev meal count; skip for now
                val (revDelta, revDeltaUp) = deltaLabel(stats.revenue,
                    when (currentSiteId) {
                        "bakkies" -> bakkiesStats.prevRevenue; "sunhill" -> sunhillStats.prevRevenue
                        "lizane"  -> lizaneStats.prevRevenue
                        else      -> siteStatsMap.values.sumOf { it.prevRevenue }
                    })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label       = "Total Meals",
                        value       = "%,d".format(stats.mealsThisMonth),
                        accentColor = Primary,
                        modifier    = Modifier.weight(1f),
                    )
                    StatCard(
                        label       = "Monthly Revenue",
                        value       = "R${"%.0f".format(stats.revenue / 1000)}k",
                        delta       = revDelta,
                        deltaUp     = revDeltaUp,
                        accentColor = Secondary,
                        modifier    = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label       = "Active Residents",
                        value       = "${stats.residents}",
                        sub         = if (isAllSites) "Across all sites" else currentSite?.name,
                        accentColor = Accent,
                        modifier    = Modifier.weight(1f),
                    )
                    val completionPct = (stats.completion * 100).toInt()
                    StatCard(
                        label       = if (isAllSites) "Pending Sites" else "Capture Progress",
                        value       = if (isAllSites) "${stats.pendingDays}" else "$completionPct%",
                        sub         = if (isAllSites) "Sites with missing data"
                        else if (stats.pendingDays > 0) "${stats.pendingDays} residents not captured"
                        else "All residents captured ✓",
                        accentColor = when {
                            stats.completion >= 1f   -> Accent
                            stats.completion >= 0.7f -> Secondary
                            else                     -> Danger
                        },
                        modifier    = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Meal breakdown ─────────────────────────────────────────────
                SectionLabel("Meal Breakdown",
                    actionLabel = if (isAllSites) "All sites · This month" else "${currentSite?.name} · This month")
                val maxMeals = maxOf(stats.meals1Course, stats.meals2Course, stats.mealsFullBoard, stats.mealsBreakfast, 1)
                MealBreakdownBar("1 Course Lunch",  stats.meals1Course,   maxMeals, Primary)
                Spacer(Modifier.height(10.dp))
                MealBreakdownBar("2 Course Lunch",  stats.meals2Course,   maxMeals, Secondary)
                Spacer(Modifier.height(10.dp))
                MealBreakdownBar("Full Board",       stats.mealsFullBoard, maxMeals, Accent)
                Spacer(Modifier.height(10.dp))
                MealBreakdownBar("Breakfast",        stats.mealsBreakfast, maxMeals, Primary)

                Spacer(Modifier.height(24.dp))

                // ── Sites status ───────────────────────────────────────────────
                SectionLabel(
                    if (isAllSites) "Sites Status" else "Site Overview",
                    actionLabel = if (isAllSites) "${SampleData.sites.size} sites" else null,
                )

                visibleSites.forEachIndexed { index, site ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    val sStats     = siteStatsMap[site.id] ?: SiteStats(0, 0, 0, 0f)
                    val sColor     = when {
                        sStats.completion >= 1f   -> Accent
                        sStats.completion >= 0.7f -> Secondary
                        else                      -> Danger
                    }
                    val sLabel = when {
                        sStats.completion >= 1f -> "Complete"
                        sStats.pendingDays > 0  -> "${sStats.pendingDays} pending"
                        else                    -> "In Progress"
                    }
                    val captured   = sStats.residents - sStats.pendingDays
                    val daysLabel  = "$captured/${sStats.residents} residents captured"
                    SiteStatusCard(
                        name        = site.name,
                        manager     = "Unit Mgr: ${site.unitManagerName}",
                        progress    = sStats.completion,
                        statusLabel = sLabel,
                        statusColor = sColor,
                        daysLabel   = daysLabel,
                        isActive    = !isAllSites,
                        onClick     = { onNavigateToCapture(site.id) },
                    )
                }

                Spacer(Modifier.height(24.dp))
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
                siteStatsMap   = siteStatsMap,
                onSiteSelected = { siteId ->
                    currentSiteId = siteId
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                },
                onViewDetails  = { site ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        onNavigateToSiteDetails(site.id)
                    }
                },
            )
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, actionLabel: String? = null) {
    val c = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = c.textBright,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 14.sp,
            )
        )
        if (actionLabel != null)
            Text(actionLabel, style = MaterialTheme.typography.labelSmall.copy(color = c.textMuted))
    }
}

@Composable
private fun StatCard(
    label      : String,
    value      : String,
    delta      : String? = null,
    deltaUp    : Boolean = true,
    sub        : String? = null,
    accentColor: Color,
    modifier   : Modifier = Modifier,
) {
    val c = LocalAppColors.current
    // In light mode: bold gradient card with white text.
    // In dark mode: existing surface card with accent-coloured value.
    // Darken the gradient: multiply RGB by ~0.72 for start, ~0.85 for end
    // This gives deep, rich card colours with the white text really popping
    val gradStart = accentColor.copy(
        red   = (accentColor.red   * 0.65f).coerceIn(0f, 1f),
        green = (accentColor.green * 0.65f).coerceIn(0f, 1f),
        blue  = (accentColor.blue  * 0.65f).coerceIn(0f, 1f),
        alpha = 1f,
    )
    val gradEnd = accentColor.copy(
        red   = (accentColor.red   * 0.85f).coerceIn(0f, 1f),
        green = (accentColor.green * 0.85f).coerceIn(0f, 1f),
        blue  = (accentColor.blue  * 0.85f).coerceIn(0f, 1f),
        alpha = 1f,
    )
    Box(modifier = modifier
        .gradientCard(gradStart, gradEnd, c, elevation = 14.dp, cornerDp = 16.dp)
        .then(if (c.isDark) Modifier.border(1.dp, c.borderColor, RoundedCornerShape(16.dp)) else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (c.isDark) c.textMuted else Color.White.copy(alpha = 0.80f),
                    letterSpacing = 0.6.sp,
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = if (c.isDark) accentColor else Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                )
            )
            if (sub != null)
                Text(sub,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (c.isDark) c.textMuted else Color.White.copy(alpha = 0.70f)
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (delta != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (c.isDark) {
                        if (deltaUp) Color(0x264FF7C8) else Color(0x26F74F6B)
                    } else Color.White.copy(0.22f),
                ) {
                    Text(delta,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (c.isDark) (if (deltaUp) Accent else Danger) else Color.White,
                            fontSize = 11.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun MealBreakdownBar(name: String, count: Int, maxCount: Int, color: Color) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = c.surface1,
        modifier = Modifier.fillMaxWidth()
            .cardElevation(c, elevation = 6.dp, cornerDp = 10.dp)
            .border(1.dp, c.borderColor, RoundedCornerShape(10.dp))) {
        Column(modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name,     style = MaterialTheme.typography.bodyMedium.copy(color = c.textBright))
                Text("$count", style = MaterialTheme.typography.labelLarge.copy(color = Primary))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { if (maxCount > 0) count.toFloat() / maxCount else 0f },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = color, trackColor = c.surface3.copy(if (c.isDark) 1f else 0.6f),
            )
        }
    }
}

@Composable
private fun SiteStatusCard(
    name       : String,
    manager    : String,
    progress   : Float,
    statusLabel: String,
    statusColor: Color,
    daysLabel  : String,
    isActive   : Boolean = false,
    onClick    : () -> Unit,
) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(16.dp),
        color = if (isActive) Primary.copy(0.05f) else c.surface1,
        modifier = Modifier.fillMaxWidth()
            .border(1.5.dp, if (isActive) Primary.copy(0.4f) else c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isActive) Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Primary))
                        Text(name, style = MaterialTheme.typography.labelLarge.copy(color = c.textBright))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(manager, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(0.15f),
                    modifier = Modifier.border(1.dp, statusColor.copy(0.3f), RoundedCornerShape(50))) {
                    Text(statusLabel, style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(daysLabel, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                Text("${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = statusColor))
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = statusColor, trackColor = c.surface3,
            )
        }
    }
}