package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.NavTab
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBottomNav
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground

@Composable
fun SiteManagementScreen(
    initialSiteId: String? = null,
    currentUserRole: UserRole = UserRole.OPERATIONS_MANAGER,
    onNavigateBack        : () -> Unit = {},
    onNavigateToDashboard: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val c = LocalAppColors.current
    val canEdit = currentUserRole == UserRole.OPERATIONS_MANAGER

    val sites = remember {
        mutableStateListOf<Site>().also { it.addAll(SampleData.sites) }
    }

    var showAddDialog  by remember { mutableStateOf(false) }
    var editingSite    by remember { mutableStateOf<Site?>(null) }
    var confirmDelete  by remember { mutableStateOf<Site?>(null) }
    // Auto-expand the site drilled into from the sheet
    var expandedSiteId by remember { mutableStateOf<String?>(initialSiteId) }

    // Resident count per site (will come from API)
    val residentCounts = remember {
        SampleData.residents.groupBy { it.siteId }.mapValues { it.value.size }
    }

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
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick        = { showAddDialog = true },
                    containerColor = Primary,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, "Add site")
                }
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
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Sites", style = MaterialTheme.typography.headlineLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text("${sites.size} retirement ${if (sites.size == 1) "village" else "villages"}", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
                    }
                    WpcLogoBadge(size = WpcBrandingSize.ICON)
                }

                Spacer(Modifier.height(14.dp))

                // Summary row
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val totalResidents = residentCounts.values.sum()
                    SiteSummaryChip("${sites.size}",       "Sites",     Primary,   Modifier.weight(1f))
                    SiteSummaryChip("$totalResidents",     "Residents", Accent,    Modifier.weight(1f))
                    SiteSummaryChip("${sites.size}",       "Managers",  Secondary, Modifier.weight(1f))
                }
            }

            // ── Site list ─────────────────────────────────────────────────────
            if (sites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏗️", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No sites yet", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text("Tap + to add a retirement village", style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(sites, key = { it.id }) { site ->
                        SiteCard(
                            site           = site,
                            residentCount  = residentCounts[site.id] ?: 0,
                            isExpanded     = expandedSiteId == site.id,
                            isHighlighted  = site.id == initialSiteId,
                            canEdit        = canEdit,
                            onToggle       = {
                                expandedSiteId = if (expandedSiteId == site.id) null else site.id
                            },
                            onEdit         = { editingSite = site },
                            onDelete       = { confirmDelete = site },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────
    if (showAddDialog || editingSite != null) {
        SiteFormDialog(
            existing  = editingSite,
            onDismiss = { showAddDialog = false; editingSite = null },
            onSave    = { updated ->
                if (editingSite != null) {
                    val idx = sites.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) sites[idx] = updated
                } else {
                    sites.add(updated)
                }
                showAddDialog = false
                editingSite   = null
            }
        )
    }

    // ── Confirm delete dialog ─────────────────────────────────────────────────
    val toDelete = confirmDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor   = c.surface1,
            title = { Text("Remove Site?", style = MaterialTheme.typography.headlineSmall.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)) },
            text  = {
                Text(
                    "\"${toDelete.name}\" and all its associated data will be removed. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { sites.remove(toDelete); confirmDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Danger),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("Cancel", color = c.textMuted)
                }
            }
        )
    }
}

// ── Site Card ─────────────────────────────────────────────────────────────────
@Composable
private fun SiteCard(
    site: Site,
    residentCount: Int,
    isExpanded: Boolean,
    isHighlighted: Boolean = false,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    val initials = site.name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
    val cardColor = when (site.id) {
        "lizane"  -> Primary
        "bakkies" -> Accent
        "sunhill" -> Secondary
        else      -> Primary
    }

    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = if (isHighlighted) cardColor.copy(0.06f) else c.surface1,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isHighlighted) cardColor.copy(0.5f) else c.borderColor,
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {

            // ── Top row ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Site avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardColor.copy(0.15f))
                            .border(1.dp, cardColor.copy(0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Text(initials, style = MaterialTheme.typography.labelLarge.copy(color = cardColor, fontSize = 16.sp))
                    }

                    Column {
                        Text(site.name, style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Surface(shape = RoundedCornerShape(50), color = cardColor.copy(0.10f)) {
                            Text(
                                "Active",
                                style    = MaterialTheme.typography.labelSmall.copy(color = cardColor, fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (canEdit) {
                        IconButton(onClick = onEdit,   modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Edit,         null, tint = c.textMuted,              modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = Danger.copy(0.7f),     modifier = Modifier.size(18.dp))
                        }
                    }
                    Icon(
                        if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null, tint = c.textDim, modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SiteStatPill("👥 $residentCount residents",   cardColor, Modifier.weight(1f))
                SiteStatPill("👤 1 manager",                  c.textMuted, Modifier.weight(1f))
            }

            // ── Expanded detail ───────────────────────────────────────────────
            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = c.borderColor)
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(Icons.Rounded.Person,       "Unit Manager", site.unitManagerName)
                    DetailRow(Icons.Rounded.Email,        "Manager Email", site.unitManagerEmail)
                    DetailRow(Icons.Rounded.Tag,          "Site ID", site.id)
                }
            }
        }
    }
}

// ── Add / Edit Form ───────────────────────────────────────────────────────────
@Composable
private fun SiteFormDialog(
    existing: Site?,
    onDismiss: () -> Unit,
    onSave: (Site) -> Unit,
) {
    val c = LocalAppColors.current
    var siteName     by remember { mutableStateOf(existing?.name ?: "") }
    var managerName  by remember { mutableStateOf(existing?.unitManagerName ?: "") }
    var managerEmail by remember { mutableStateOf(existing?.unitManagerEmail ?: "") }
    var siteId       by remember { mutableStateOf(existing?.id ?: "") }

    var nameError    by remember { mutableStateOf(false) }
    var managerError by remember { mutableStateOf(false) }
    var emailError   by remember { mutableStateOf(false) }
    var idError      by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = c.surface1,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, c.borderColor, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏢", fontSize = 22.sp)
                    Column {
                        Text(
                            if (existing != null) "Edit Site" else "Add Site",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (existing != null) "Update site details" else "Add a new retirement village",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                SiteFormField(
                    value         = siteName,
                    onValueChange = { siteName = it; nameError = false },
                    label         = "Site Name",
                    placeholder   = "e.g. Lizane Village",
                    isError       = nameError,
                    errorMessage  = "Site name is required",
                )
                Spacer(Modifier.height(12.dp))

                SiteFormField(
                    value         = siteId,
                    onValueChange = { siteId = it.lowercase().replace(" ", "_"); idError = false },
                    label         = "Site ID",
                    placeholder   = "e.g. lizane  (auto-generated)",
                    isError       = idError,
                    errorMessage  = "Site ID is required",
                    enabled       = existing == null,
                )
                Spacer(Modifier.height(12.dp))

                SiteFormField(
                    value         = managerName,
                    onValueChange = { managerName = it; managerError = false },
                    label         = "Unit Manager Name",
                    placeholder   = "e.g. Me van Rooyen",
                    isError       = managerError,
                    errorMessage  = "Manager name is required",
                )
                Spacer(Modifier.height(12.dp))

                SiteFormField(
                    value         = managerEmail,
                    onValueChange = { managerEmail = it; emailError = false },
                    label         = "Manager Email",
                    placeholder   = "e.g. manager@wpc.co.za",
                    isError       = emailError,
                    errorMessage  = "Valid email is required",
                )

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        border   = BorderStroke(1.dp, c.borderColor),
                    ) { Text("Cancel", color = c.textMuted) }

                    Button(
                        onClick  = {
                            nameError    = siteName.isBlank()
                            idError      = siteId.isBlank()
                            managerError = managerName.isBlank()
                            emailError   = managerEmail.isBlank() || !managerEmail.contains("@")
                            if (!nameError && !idError && !managerError && !emailError) {
                                onSave(
                                    Site(
                                        id                 = siteId.trim(),
                                        name               = siteName.trim(),
                                        unitManagerName    = managerName.trim(),
                                        unitManagerEmail   = managerEmail.trim(),
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) { Text(if (existing != null) "Save" else "Add Site") }
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────
@Composable
private fun SiteFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
) {
    val c = LocalAppColors.current
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
        Spacer(Modifier.height(5.dp))
        Surface(
            shape    = RoundedCornerShape(10.dp),
            color    = if (enabled) c.surface2 else c.surface3,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isError) Danger.copy(0.7f) else c.borderColor, RoundedCornerShape(10.dp))
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
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.surface2)
        ) {
            Icon(icon, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
        }
    }
}

@Composable
private fun SiteStatPill(text: String, color: Color, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = c.surface2,
        modifier = modifier,
    ) {
        Text(
            text,
            style    = MaterialTheme.typography.bodySmall.copy(color = color),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun SiteSummaryChip(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = if (c.isDark) c.surface2 else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
        modifier = modifier.then(
            if (!c.isDark) Modifier.border(1.dp, androidx.compose.ui.graphics.Color.White.copy(0.35f), RoundedCornerShape(12.dp)) else Modifier
        ),
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelLarge.copy(
                color = if (c.isDark) color else androidx.compose.ui.graphics.Color.White,
                fontSize = 16.sp,
            ))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(
                color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(0.75f)
            ))
        }
    }
}