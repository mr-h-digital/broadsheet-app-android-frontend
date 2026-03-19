package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.hildebrandtdigital.wpcbroadsheet.navigation.SITE_ALL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.Resident
import com.hildebrandtdigital.wpcbroadsheet.data.model.ResidentType
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ResidentRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.components.NavTab
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBottomNav
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentManagementScreen(
    initialSiteId        : String = SITE_ALL,
    repository           : ResidentRepository? = null,   // null = fallback to SampleData (preview/demo)
    onNavigateBack       : () -> Unit = {},
    onNavigateToDashboard: () -> Unit,
    onNavigateToCapture  : () -> Unit,
    onNavigateToReports  : () -> Unit,
    onNavigateToProfile  : () -> Unit,
) {
    val c = LocalAppColors.current
    val isOpsManager = AppSession.isOpsManager
    val canManage    = true                          // all roles can add/edit/delete
    val canRelocate  = AppSession.hasCrossSiteAccess // only Ops Manager / Admin can relocate
    val scope        = rememberCoroutineScope()

    // Unit Managers are locked to their own site regardless of initialSiteId
    var currentSiteId by remember(AppSession.currentUser) {
        mutableStateOf(
            if (AppSession.hasCrossSiteAccess) initialSiteId
            else AppSession.currentSiteId ?: "lizane"
        )
    }
    val isAllSites = AppSession.hasCrossSiteAccess && currentSiteId == SITE_ALL

    // ── Resident data — Room Flow when repository is available, SampleData otherwise ──
    val dbResidents: List<Resident> by (
            repository
                ?.observeResidents(if (isAllSites) null else currentSiteId)
                ?: kotlinx.coroutines.flow.flowOf(SampleData.residents)
            ).collectAsStateWithLifecycle(initialValue = SampleData.residents)

    // For the All Sites view when using Room, we always observe all sites (null siteId).
    // For in-memory fallback, we filter SampleData ourselves.
    val allResidents: List<Resident> = if (repository != null) {
        dbResidents
    } else {
        if (isAllSites) SampleData.residents else SampleData.residents.filter { it.siteId == currentSiteId }
    }

    val siteResidents = if (isAllSites) allResidents
    else allResidents.filter { it.siteId == currentSiteId }

    var searchQuery     by remember { mutableStateOf("") }
    var selectedFilter  by remember { mutableStateOf("All") }
    var showAddDialog   by remember { mutableStateOf(false) }
    var editingResident by remember { mutableStateOf<Resident?>(null) }
    var confirmDelete   by remember { mutableStateOf<Resident?>(null) }
    var showSiteSheet   by remember { mutableStateOf(false) }
    var pendingRelocation by remember { mutableStateOf<Pair<Resident, String>?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filters    = listOf("All", "Rental", "Owner", "OTP")

    fun applyFilters(list: List<Resident>) = list
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

    val groupedFiltered: List<Pair<Site?, List<Resident>>> = if (isAllSites) {
        SampleData.sites.map { site ->
            site to applyFilters(allResidents.filter { it.siteId == site.id }).sortedBy { it.unitNumber }
        }.filter { it.second.isNotEmpty() }
    } else {
        listOf(null to applyFilters(siteResidents).sortedBy { it.unitNumber })
    }

    val totalShown    = groupedFiltered.sumOf { it.second.size }
    val siteLabel     = if (isAllSites) "All Sites" else SampleData.sites.firstOrNull { it.id == currentSiteId }?.name ?: currentSiteId
    val headerSub     = if (isAllSites) "${allResidents.size} residents · ${SampleData.sites.size} sites"
    else "$siteLabel  ·  ${siteResidents.size} residents"

    Scaffold(
        containerColor = c.bgDeep,
        bottomBar = {
            WpcBottomNav(
                selected = NavTab.DASHBOARD,
                onSelect = {
                    when (it) {
                        NavTab.DASHBOARD -> onNavigateToDashboard()
                        NavTab.CAPTURE   -> onNavigateToCapture()
                        NavTab.REPORTS   -> onNavigateToReports()
                        NavTab.PROFILE   -> onNavigateToProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            if (canManage && !isAllSites) {
                FloatingActionButton(
                    onClick        = { showAddDialog = true },
                    containerColor = Primary,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp),
                ) { Icon(Icons.Rounded.PersonAdd, "Add resident") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .appBackground(c)
            .padding(padding)) {

            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .headerBand(c)
                    .padding(start = 4.dp, end = 20.dp, top = 52.dp, bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Residents", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text(headerSub, style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!isAllSites) {
                            val rc = siteResidents.count { it.residentType == ResidentType.RENTAL }
                            val oc = siteResidents.count { it.residentType == ResidentType.OWNER }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniPill("$rc R", Primary)
                                MiniPill("$oc O", Accent)
                            }
                        }
                        WpcLogoBadge(size = WpcBrandingSize.ICON)
                    }
                }

                // Site chip — Ops Manager only
                if (isOpsManager) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.padding(start = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResSiteChip(siteId = currentSiteId, onClick = { showSiteSheet = true })
                        if (isAllSites) {
                            Text("Showing all villages", style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search bar
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = c.surface2,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp).border(1.dp, c.borderColor, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = c.textDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(color = c.textBright),
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search by name or unit…", style = MaterialTheme.typography.bodyLarge.copy(color = c.textDim))
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Rounded.Close, null, tint = c.textMuted, modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Filter chips
                Row(modifier = Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { f ->
                        val active = selectedFilter == f
                        Surface(
                            shape    = RoundedCornerShape(50),
                            color    = if (active) Primary.copy(0.18f) else c.surface2,
                            modifier = Modifier
                                .cardElevation(c)
                                .border(1.dp, if (active) Primary.copy(0.5f) else c.borderColor, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedFilter = f }
                        ) {
                            Text(
                                f,
                                style    = MaterialTheme.typography.labelMedium.copy(color = if (active) (if (c.isDark) Primary else c.textBright) else c.textMuted),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            // ── Resident list ─────────────────────────────────────────────────
            if (totalShown == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No residents found", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text("Try adjusting your search or filter", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.90f)))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    groupedFiltered.forEach { (site, residents) ->
                        if (isAllSites && site != null) {
                            item(key = "hdr_${site.id}") { SiteGroupHeader(site = site, count = residents.size) }
                        }
                        items(residents, key = { "${it.siteId}_${it.unitNumber}" }) { resident ->
                            ResidentRow(
                                resident      = resident,
                                showSiteBadge = isAllSites,
                                canEdit       = canManage,
                                onEdit        = { editingResident = resident },
                                onDelete      = { confirmDelete  = resident },
                            )
                        }
                        if (isAllSites && site != null) {
                            item(key = "sp_${site.id}") { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Site switcher sheet ───────────────────────────────────────────────────
    if (showSiteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSiteSheet = false },
            sheetState       = sheetState,
            containerColor   = c.surface1,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Filter by Site", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                Spacer(Modifier.height(4.dp))
                Text("Select a village to view its residents", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.90f)))
                Spacer(Modifier.height(16.dp))

                ResSiteSheetRow(
                    label    = "All Sites",
                    sub      = "${allResidents.size} residents across all villages",
                    selected = isAllSites,
                    color    = Primary,
                    onClick  = { currentSiteId = SITE_ALL; showSiteSheet = false },
                )
                Spacer(Modifier.height(8.dp))

                SampleData.sites.forEachIndexed { idx, site ->
                    val color = listOf(Primary, Accent, Secondary)[idx % 3]
                    val cnt   = allResidents.count { it.siteId == site.id }
                    ResSiteSheetRow(
                        label    = site.name,
                        sub      = "$cnt residents  ·  Mgr: ${site.unitManagerName}",
                        selected = currentSiteId == site.id,
                        color    = color,
                        onClick  = { currentSiteId = site.id; showSiteSheet = false },
                    )
                    if (idx < SampleData.sites.lastIndex) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────
    if (showAddDialog || editingResident != null) {
        ResidentFormDialog(
            existing      = editingResident,
            currentSiteId = if (isAllSites) (editingResident?.siteId ?: currentSiteId) else currentSiteId,
            isOpsManager  = canRelocate,
            onDismiss = { showAddDialog = false; editingResident = null },
            onSave    = { updated, originalSiteId ->
                val isRelocation = originalSiteId != null && updated.siteId != originalSiteId
                if (isRelocation) {
                    pendingRelocation = updated to originalSiteId!!
                } else {
                    val actor = AppSession.currentUserName
                    scope.launch(Dispatchers.IO) {
                        if (repository != null) {
                            if (originalSiteId == null) repository.addResident(updated, actor)
                            else repository.updateResident(updated, actor)
                        }
                        // In-memory fallback is read-only from SampleData — no mutation needed;
                        // the Flow will not update but the dialog will close cleanly for preview.
                    }
                    showAddDialog   = false
                    editingResident = null
                }
            }
        )
    }

    // ── Relocation confirmation ───────────────────────────────────────────────
    val reloc = pendingRelocation
    if (reloc != null) {
        val (updated, fromSiteId) = reloc
        val fromName = SampleData.sites.firstOrNull { it.id == fromSiteId }?.name ?: fromSiteId
        val toName   = SampleData.sites.firstOrNull { it.id == updated.siteId }?.name ?: updated.siteId
        AlertDialog(
            onDismissRequest = {},
            containerColor   = c.surface1,
            icon = { Icon(Icons.Rounded.SwapHoriz, null, tint = Secondary, modifier = Modifier.size(28.dp)) },
            title = { Text("Confirm Relocation", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "You are about to move ${updated.clientName} (Unit ${updated.unitNumber}) from $fromName to $toName.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = Secondary.copy(0.08f),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Secondary.copy(0.25f), RoundedCornerShape(10.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Info, null, tint = Secondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Text(
                                "Past meal records remain on $fromName. Future captures and billing will be under $toName.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Secondary),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actor = AppSession.currentUserName
                        scope.launch(Dispatchers.IO) {
                            repository?.relocateResident(
                                resident   = updated,
                                fromSiteId = fromSiteId,
                                toSiteId   = updated.siteId,
                                actor      = actor,
                            )
                        }
                        pendingRelocation = null
                        editingResident   = null
                        showAddDialog     = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                ) { Text("Confirm Move") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRelocation = null }) { Text("Cancel", color = c.textMuted) }
            }
        )
    }

    // ── Deactivate confirmation ───────────────────────────────────────────────
    val toDelete = confirmDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor   = c.surface1,
            title = { Text("Deactivate Resident?", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)) },
            text  = {
                Text(
                    "Unit ${toDelete.unitNumber} – ${toDelete.clientName} will be removed from active capture.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actor = AppSession.currentUserName
                        scope.launch(Dispatchers.IO) {
                            repository?.deactivateResident(
                                siteId     = toDelete.siteId,
                                unitNumber = toDelete.unitNumber,
                                actor      = actor,
                            )
                        }
                        confirmDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                ) { Text("Deactivate") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = c.textMuted) }
            }
        )
    }
}

// ── Site group header ─────────────────────────────────────────────────────────
@Composable
private fun SiteGroupHeader(site: Site, count: Int) {
    val c = LocalAppColors.current
    val color = when (site.id) {
        "lizane"  -> Primary
        "bakkies" -> Accent
        "sunhill" -> Secondary
        else      -> c.textMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(site.name, style = MaterialTheme.typography.labelLarge.copy(color = color))
        Text("$count residents", style = MaterialTheme.typography.labelSmall)
        HorizontalDivider(modifier = Modifier.weight(1f), color = color.copy(0.2f))
    }
}

// ── Resident row ──────────────────────────────────────────────────────────────
@Composable
private fun ResidentRow(
    resident     : Resident,
    showSiteBadge: Boolean,
    canEdit      : Boolean,
    onEdit       : () -> Unit,
    onDelete     : () -> Unit,
) {
    val c = LocalAppColors.current
    val typeColor = when (resident.residentType) {
        ResidentType.RENTAL -> Primary
        ResidentType.OWNER  -> Accent
        ResidentType.OTP    -> Secondary
    }
    val typeLabel = when (resident.residentType) {
        ResidentType.RENTAL -> "Rental"
        ResidentType.OWNER  -> "Owner"
        ResidentType.OTP    -> "OTP"
    }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = c.surface1,
        modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(14.dp)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(0.12f))
                    .border(1.dp, typeColor.copy(0.3f), RoundedCornerShape(12.dp))
            ) {
                Text(resident.unitNumber, style = MaterialTheme.typography.labelLarge.copy(color = typeColor, fontSize = 11.sp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(resident.clientName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = typeColor.copy(0.12f)) {
                        Text(typeLabel, style = MaterialTheme.typography.labelSmall.copy(color = typeColor), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    val occ = resident.totalOccupants
                    Text("$occ occupant${if (occ > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
                    // Site badge only visible in All Sites view
                    if (showSiteBadge) {
                        val sc = when (resident.siteId) { "lizane" -> Primary; "bakkies" -> Accent; "sunhill" -> Secondary; else -> c.textMuted }
                        val sn = SampleData.sites.firstOrNull { it.id == resident.siteId }?.name?.substringBefore(" ") ?: resident.siteId
                        Surface(shape = RoundedCornerShape(50), color = sc.copy(0.1f), modifier = Modifier.cardElevation(c).border(1.dp, sc.copy(0.25f), RoundedCornerShape(50))) {
                            Text(sn, style = MaterialTheme.typography.labelSmall.copy(color = sc), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit,   modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Edit,         null, tint = c.textMuted,              modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.DeleteOutline, null, tint = Danger.copy(0.7f), modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

// ── Site chip ─────────────────────────────────────────────────────────────────
@Composable
private fun ResSiteChip(siteId: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val isAll    = siteId == SITE_ALL
    val label    = if (isAll) "All Sites" else SampleData.sites.firstOrNull { it.id == siteId }?.name ?: siteId
    val dotColor = when (siteId) { "lizane" -> Primary; "bakkies" -> Accent; "sunhill" -> Secondary; else -> c.textMuted }
    val chipBg      = if (c.isDark) (if (isAll) Primary.copy(0.1f) else dotColor.copy(0.1f)) else androidx.compose.ui.graphics.Color.White.copy(0.22f)
    val chipBorder  = if (c.isDark) (if (isAll) Primary.copy(0.3f) else dotColor.copy(0.3f)) else androidx.compose.ui.graphics.Color.White.copy(0.55f)
    val chipDot     = if (c.isDark) dotColor else androidx.compose.ui.graphics.Color.White
    val chipText    = if (c.isDark) (if (isAll) Primary else dotColor) else androidx.compose.ui.graphics.Color.White
    Surface(
        shape    = RoundedCornerShape(50),
        color    = chipBg,
        modifier = Modifier
            .cardElevation(c)
            .border(1.dp, chipBorder, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!isAll) Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(chipDot))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = chipText))
            Icon(Icons.Rounded.ExpandMore, null, tint = chipText, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Sheet site row ────────────────────────────────────────────────────────────
@Composable
private fun ResSiteSheetRow(label: String, sub: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = if (selected) color.copy(0.1f) else c.surface2,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) color.copy(0.4f) else c.borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null, tint = if (selected) color else c.textDim, modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(color = if (selected) color else c.textBright))
                Text(sub,   style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
            }
        }
    }
}

// ── Add / Edit form dialog ────────────────────────────────────────────────────
@Composable
private fun ResidentFormDialog(
    existing      : Resident?,
    currentSiteId : String,
    isOpsManager  : Boolean,
    onDismiss     : () -> Unit,
    onSave        : (Resident, String?) -> Unit,
) {
    val c = LocalAppColors.current
    val originalSiteId = existing?.siteId
    var unitNumber     by remember { mutableStateOf(existing?.unitNumber ?: "") }
    var clientName     by remember { mutableStateOf(existing?.clientName ?: "") }
    var occupants      by remember { mutableStateOf((existing?.totalOccupants ?: 1).toString()) }
    var resType        by remember { mutableStateOf(existing?.residentType ?: ResidentType.RENTAL) }
    var selectedSite   by remember { mutableStateOf(
        existing?.siteId ?: currentSiteId.takeIf { it != SITE_ALL } ?: SampleData.sites.first().id
    ) }
    var showPicker     by remember { mutableStateOf(false) }
    var unitError      by remember { mutableStateOf(false) }
    var nameError      by remember { mutableStateOf(false) }
    val isEditing      = existing != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = c.surface1,
            modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (isEditing) "Edit Resident" else "Add Resident", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(if (isEditing) "Update resident details" else "Add a new resident", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.90f)))
                Spacer(Modifier.height(20.dp))

                ResFormField(unitNumber, { unitNumber = it; unitError = false }, "Unit Number", "e.g. 025", unitError, "Unit number is required", !isEditing)
                Spacer(Modifier.height(12.dp))
                ResFormField(clientName, { clientName = it; nameError = false }, "Client Name", "e.g. Me van Rooyen", nameError, "Name is required")
                Spacer(Modifier.height(12.dp))
                ResFormField(occupants, { v -> if (v.all { it.isDigit() }) occupants = v }, "Total Occupants", "1")
                Spacer(Modifier.height(16.dp))

                // Resident type
                Text("Resident Type", style = MaterialTheme.typography.labelLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResidentType.entries.forEach { type ->
                        val active = resType == type
                        val color  = when (type) { ResidentType.RENTAL -> Primary; ResidentType.OWNER -> Accent; ResidentType.OTP -> Secondary }
                        Surface(
                            shape    = RoundedCornerShape(10.dp),
                            color    = if (active) color.copy(0.18f) else c.surface2,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (active) color.copy(0.5f) else c.borderColor, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { resType = type }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                Text(type.name, style = MaterialTheme.typography.labelMedium.copy(color = if (active) color else c.textMuted))
                            }
                        }
                    }
                }

                // Site assignment — Ops Manager on both add and edit
                if (isOpsManager) {
                    Spacer(Modifier.height(16.dp))
                    Text("Assigned Site", style = MaterialTheme.typography.labelLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                    Spacer(Modifier.height(8.dp))

                    val sc = when (selectedSite) { "lizane" -> Primary; "bakkies" -> Accent; "sunhill" -> Secondary; else -> c.textMuted }
                    val siteObj = SampleData.sites.firstOrNull { it.id == selectedSite }

                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = sc.copy(0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, sc.copy(0.3f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showPicker = !showPicker }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(sc))
                            Text(siteObj?.name ?: selectedSite, style = MaterialTheme.typography.bodyLarge.copy(color = sc), modifier = Modifier.weight(1f))
                            Icon(if (showPicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = sc, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (showPicker) {
                        Spacer(Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = c.surface2, modifier = Modifier.fillMaxWidth().border(1.dp, c.borderColor, RoundedCornerShape(10.dp))) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                SampleData.sites.forEach { site ->
                                    val sel       = site.id == selectedSite
                                    val siteColor = when (site.id) { "lizane" -> Primary; "bakkies" -> Accent; "sunhill" -> Secondary; else -> c.textMuted }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) siteColor.copy(0.1f) else Color.Transparent)
                                            .clickable { selectedSite = site.id; showPicker = false }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(siteColor))
                                        Text(site.name, style = MaterialTheme.typography.bodyMedium.copy(color = if (sel) siteColor else c.textBright), modifier = Modifier.weight(1f))
                                        if (sel) Icon(Icons.Rounded.Check, null, tint = siteColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Relocation warning banner
                    if (selectedSite != originalSiteId) {
                        Spacer(Modifier.height(8.dp))
                        val fromName = SampleData.sites.firstOrNull { it.id == originalSiteId }?.name ?: originalSiteId
                        val toName   = SampleData.sites.firstOrNull { it.id == selectedSite }?.name ?: selectedSite
                        Surface(
                            shape    = RoundedCornerShape(8.dp),
                            color    = Secondary.copy(0.08f),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Secondary.copy(0.25f), RoundedCornerShape(8.dp))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Rounded.SwapHoriz, null, tint = Secondary, modifier = Modifier.size(16.dp))
                                Text(
                                    "Will be relocated from $fromName to $toName",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Secondary),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, c.borderColor)) {
                        Text("Cancel", color = c.textMuted)
                    }
                    Button(
                        onClick = {
                            unitError = unitNumber.isBlank()
                            nameError = clientName.isBlank()
                            if (!unitError && !nameError) {
                                onSave(
                                    Resident(
                                        unitNumber     = unitNumber.trim().padStart(3, '0'),
                                        clientName     = clientName.trim(),
                                        totalOccupants = occupants.toIntOrNull() ?: 1,
                                        residentType   = resType,
                                        siteId         = selectedSite,
                                    ),
                                    originalSiteId,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) { Text(if (isEditing) "Save" else "Add") }
                }
            }
        }
    }
}

@Composable
private fun ResFormField(
    value        : String,
    onValueChange: (String) -> Unit,
    label        : String,
    placeholder  : String = "",
    isError      : Boolean = false,
    errorMessage : String = "",
    enabled      : Boolean = true,
) {
    val c = LocalAppColors.current
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
        Spacer(Modifier.height(5.dp))
        Surface(
            shape    = RoundedCornerShape(10.dp),
            color    = if (enabled) c.surface2 else c.surface3,
            modifier = Modifier.fillMaxWidth().border(1.dp, if (isError) Danger.copy(0.7f) else c.borderColor, RoundedCornerShape(10.dp))
        ) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                enabled       = enabled,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) c.textBright else c.textMuted),
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                singleLine    = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(color = c.textDim))
                    inner()
                }
            )
        }
        if (isError) {
            Spacer(Modifier.height(4.dp))
            Text(errorMessage, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
        }
    }
}

@Composable
private fun MiniPill(text: String, color: Color) {
    val c = LocalAppColors.current
    Surface(shape = RoundedCornerShape(50), color = color.copy(0.12f)) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = color), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}