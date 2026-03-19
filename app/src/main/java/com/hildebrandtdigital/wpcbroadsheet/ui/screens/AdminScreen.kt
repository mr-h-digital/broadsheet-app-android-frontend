package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import com.hildebrandtdigital.wpcbroadsheet.data.model.User
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SiteRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.UserRepository
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

@Composable
fun AdminScreen(
    userRepository    : UserRepository,
    siteRepository    : SiteRepository,
    onNavigateBack    : () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val users by userRepository.observeActiveUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val sites by siteRepository.observeActiveSites().collectAsStateWithLifecycle(initialValue = emptyList())

    // ── Dialog state ─────────────────────────────────────────────────────────
    var showUserDialog   by remember { mutableStateOf(false) }
    var showSiteDialog   by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<User?>(null) }
    var editingUser      by remember { mutableStateOf<User?>(null) }
    var editingSite      by remember { mutableStateOf<Site?>(null) }
    var activeTab        by remember { mutableStateOf(0) }  // 0=Users, 1=Sites

    Scaffold(
        containerColor = c.bgDeep,
        topBar = {
            AdminTopBar(onBack = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) { editingUser = null; showUserDialog = true }
                    else                { editingSite = null; showSiteDialog = true }
                },
                containerColor = Primary,
                contentColor   = Color.White,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .appBackground(c)
                
                .padding(padding)
        ) {

            // ── Tab row ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdminTab(
                    label    = "👤  Users",
                    selected = activeTab == 0,
                    count    = users.size,
                    onClick  = { activeTab = 0 },
                    modifier = Modifier.weight(1f),
                )
                AdminTab(
                    label    = "🏢  Sites",
                    selected = activeTab == 1,
                    count    = sites.size,
                    onClick  = { activeTab = 1 },
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Content ───────────────────────────────────────────────────────
            when (activeTab) {
                0 -> UsersList(
                    users  = users,
                    sites  = sites,
                    onEdit = { user -> editingUser = user; showUserDialog = true },
                    onDelete = { user -> showDeleteConfirm = user },
                )
                1 -> SitesList(
                    sites  = sites,
                    users  = users,
                    onEdit = { site -> editingSite = site; showSiteDialog = true },
                    onDelete = { site ->
                        scope.launch {
                            siteRepository.deactivateSite(site.id, AppSession.currentUserName)
                        }
                    },
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showUserDialog) {
        UserFormDialog(
            existing   = editingUser,
            sites      = sites,
            onDismiss  = { showUserDialog = false },
            onSave     = { name, email, password, role, phone, siteId ->
                scope.launch {
                    if (editingUser == null) {
                        userRepository.createUser(
                            name = name, email = email, password = password,
                            role = role, phone = phone, siteId = siteId,
                            actor = AppSession.currentUserName,
                        )
                    } else {
                        userRepository.updateUser(
                            id = editingUser!!.id, name = name, email = email,
                            role = role, phone = phone, siteId = siteId,
                            newPassword = password.ifBlank { null },
                            actor = AppSession.currentUserName,
                        )
                    }
                    showUserDialog = false
                }
            },
        )
    }

    if (showSiteDialog) {
        SiteFormDialog(
            existing  = editingSite,
            onDismiss = { showSiteDialog = false },
            onSave    = { name ->
                scope.launch {
                    if (editingSite == null) {
                        siteRepository.createSite(name, AppSession.currentUserName)
                    } else {
                        siteRepository.renameSite(editingSite!!.id, name, AppSession.currentUserName)
                    }
                    showSiteDialog = false
                }
            },
        )
    }

    showDeleteConfirm?.let { user ->
        AlertDialog(
            containerColor    = c.surface1,
            onDismissRequest  = { showDeleteConfirm = null },
            icon              = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5252)) },
            title             = { Text("Remove User?", style = MaterialTheme.typography.titleMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)) },
            text              = {
                Text(
                    "This will deactivate ${user.name}'s account. Their history will be preserved.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        userRepository.deactivateUser(user.id, AppSession.currentUserName)
                        showDeleteConfirm = null
                    }
                }) {
                    Text("Remove", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = c.textMuted)
                }
            },
        )
    }
}

// ── Users list ────────────────────────────────────────────────────────────────

@Composable
private fun UsersList(
    users   : List<User>,
    sites   : List<Site>,
    onEdit  : (User) -> Unit,
    onDelete: (User) -> Unit,
) {
    val c = LocalAppColors.current
    if (users.isEmpty()) {
        EmptyState(message = "No users yet. Tap + to add one.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(users, key = { it.id }) { user ->
            val siteName = sites.find { it.id == user.siteId }?.name
            UserCard(
                user     = user,
                siteName = siteName,
                onEdit   = { onEdit(user) },
                onDelete = { onDelete(user) },
            )
        }
    }
}

@Composable
private fun UserCard(
    user    : User,
    siteName: String?,
    onEdit  : () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    val roleColor = when (user.role) {
        UserRole.ADMIN              -> Color(0xFFFFD700)
        UserRole.OPERATIONS_MANAGER -> Color(0xFF4FC3F7)
        UserRole.UNIT_MANAGER       -> Color(0xFF81C784)
    }
    val roleLabel = when (user.role) {
        UserRole.ADMIN              -> "Admin"
        UserRole.OPERATIONS_MANAGER -> "Ops Manager"
        UserRole.UNIT_MANAGER       -> "Unit Manager"
    }
    val roleEmoji = when (user.role) {
        UserRole.ADMIN              -> "🔑"
        UserRole.OPERATIONS_MANAGER -> "📊"
        UserRole.UNIT_MANAGER       -> "🏠"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.surface1,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(roleEmoji, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = user.name,
                    style = MaterialTheme.typography.titleSmall.copy(color = c.textBright, fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = user.email,
                    style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted, fontSize = 12.sp),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Role chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = roleColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text  = roleLabel,
                            style = MaterialTheme.typography.labelSmall.copy(color = roleColor, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    // Site chip
                    if (siteName != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x1AFFFFFF),
                        ) {
                            Text(
                                text  = siteName,
                                style = MaterialTheme.typography.labelSmall.copy(color = c.textDim, fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = c.textMuted)
            }
            // Don't allow deleting yourself
            if (user.id != AppSession.currentUserId) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF5252))
                }
            }
        }
    }
}

// ── Sites list ────────────────────────────────────────────────────────────────

@Composable
private fun SitesList(
    sites   : List<Site>,
    users   : List<User>,
    onEdit  : (Site) -> Unit,
    onDelete: (Site) -> Unit,
) {
    val c = LocalAppColors.current
    if (sites.isEmpty()) {
        EmptyState(message = "No sites yet. Tap + to add one.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sites, key = { it.id }) { site ->
            val unitManagers = users.filter { it.role == UserRole.UNIT_MANAGER && it.siteId == site.id }
            SiteCard(
                site         = site,
                unitManagers = unitManagers,
                onEdit       = { onEdit(site) },
                onDelete     = { onDelete(site) },
            )
        }
    }
}

@Composable
private fun SiteCard(
    site        : Site,
    unitManagers: List<User>,
    onEdit      : () -> Unit,
    onDelete    : () -> Unit,
) {
    val c = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.surface1,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏢", fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = site.name,
                    style = MaterialTheme.typography.titleSmall.copy(color = c.textBright, fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(4.dp))
                if (unitManagers.isEmpty()) {
                    Text(
                        text  = "No Unit Manager assigned",
                        style = MaterialTheme.typography.bodySmall.copy(color = c.textDim, fontSize = 12.sp),
                    )
                } else {
                    unitManagers.forEach { mgr ->
                        Text(
                            text  = "👤 ${mgr.name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted, fontSize = 12.sp),
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = c.textMuted)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF5252))
            }
        }
    }
}

// ── User form dialog ──────────────────────────────────────────────────────────

@Composable
private fun UserFormDialog(
    existing : User?,
    sites    : List<Site>,
    onDismiss: () -> Unit,
    onSave   : (name: String, email: String, password: String, role: UserRole, phone: String, siteId: String?) -> Unit,
) {
    val c = LocalAppColors.current
    var name     by remember { mutableStateOf(existing?.name   ?: "") }
    var email    by remember { mutableStateOf(existing?.email  ?: "") }
    var password by remember { mutableStateOf("") }
    var phone    by remember { mutableStateOf(existing?.phone  ?: "") }
    var role     by remember { mutableStateOf(existing?.role   ?: UserRole.UNIT_MANAGER) }
    var siteId   by remember { mutableStateOf(existing?.siteId) }
    var showPass by remember { mutableStateOf(false) }
    var roleMenuOpen by remember { mutableStateOf(false) }
    var siteMenuOpen by remember { mutableStateOf(false) }

    val isNew       = existing == null
    val passLabel   = if (isNew) "Password *" else "New Password (leave blank to keep)"
    val selectedSiteName = sites.find { it.id == siteId }?.name ?: "Select site…"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = c.surface1,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text  = if (isNew) "Add User" else "Edit User",
                    style = MaterialTheme.typography.titleMedium.copy(color = c.textBright),
                )
                Spacer(Modifier.height(16.dp))

                FormField("Full Name *") {
                    WpcDialogTextField(value = name, onChange = { name = it }, placeholder = "Jane Smith")
                }
                Spacer(Modifier.height(10.dp))

                FormField("Email *") {
                    WpcDialogTextField(value = email, onChange = { email = it }, placeholder = "jane@wpc.co.za", keyboardType = KeyboardType.Email)
                }
                Spacer(Modifier.height(10.dp))

                FormField(passLabel) {
                    WpcDialogTextField(
                        value        = password,
                        onChange     = { password = it },
                        placeholder  = if (isNew) "Min 6 characters" else "Leave blank to keep current",
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null, tint = c.textMuted,
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))

                FormField("Phone") {
                    WpcDialogTextField(value = phone, onChange = { phone = it }, placeholder = "+27 82 000 0000", keyboardType = KeyboardType.Phone)
                }
                Spacer(Modifier.height(10.dp))

                // Role selector
                FormField("Role *") {
                    Box {
                        Surface(
                            shape    = RoundedCornerShape(8.dp),
                            color    = c.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, c.borderColor, RoundedCornerShape(8.dp))
                                .clickable { roleMenuOpen = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text  = role.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodyLarge.copy(color = c.textBright),
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = c.textMuted)
                            }
                        }
                        DropdownMenu(
                            expanded         = roleMenuOpen,
                            onDismissRequest = { roleMenuOpen = false },
                            containerColor   = c.surface2,
                        ) {
                            UserRole.entries.forEach { r ->
                                DropdownMenuItem(
                                    text    = { Text(r.name.replace("_", " "), color = c.textBright) },
                                    onClick = { role = r; if (r != UserRole.UNIT_MANAGER) siteId = null; roleMenuOpen = false },
                                )
                            }
                        }
                    }
                }

                // Site selector — only shown for Unit Manager role
                AnimatedVisibility(visible = role == UserRole.UNIT_MANAGER) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        FormField("Assigned Site") {
                            Box {
                                Surface(
                                    shape    = RoundedCornerShape(8.dp),
                                    color    = c.surface2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, c.borderColor, RoundedCornerShape(8.dp))
                                        .clickable { siteMenuOpen = true }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text  = selectedSiteName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = if (siteId == null) c.textDim else c.textBright,
                                            ),
                                        )
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = c.textMuted)
                                    }
                                }
                                DropdownMenu(
                                    expanded         = siteMenuOpen,
                                    onDismissRequest = { siteMenuOpen = false },
                                    containerColor   = c.surface2,
                                ) {
                                    sites.forEach { s ->
                                        DropdownMenuItem(
                                            text    = { Text(s.name, color = c.textBright) },
                                            onClick = { siteId = s.id; siteMenuOpen = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = c.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank()) return@Button
                            if (isNew && password.length < 6) return@Button
                            onSave(name, email, password, role, phone, siteId)
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text(if (isNew) "Create" else "Save")
                    }
                }
            }
        }
    }
}

// ── Site form dialog ──────────────────────────────────────────────────────────

@Composable
private fun SiteFormDialog(
    existing : Site?,
    onDismiss: () -> Unit,
    onSave   : (name: String) -> Unit,
) {
    val c = LocalAppColors.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val isNew = existing == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = c.surface1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text  = if (isNew) "Add Site" else "Rename Site",
                    style = MaterialTheme.typography.titleMedium.copy(color = c.textBright),
                )
                Spacer(Modifier.height(16.dp))
                FormField("Site Name *") {
                    WpcDialogTextField(value = name, onChange = { name = it }, placeholder = "e.g. Oak Valley Retirement")
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = c.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name) },
                        shape  = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text(if (isNew) "Create" else "Save")
                    }
                }
            }
        }
    }
}

// ── Reusable sub-composables ─────────────────────────────────────────────────

@Composable
private fun AdminTopBar(onBack: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .headerBand(c)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Admin Panel",
                    style = MaterialTheme.typography.titleMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold),
                )
                Text(
                    text  = "Users & Sites",
                    style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)),
                )
            }
            WpcLogoBadge(size = WpcBrandingSize.ICON)
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun AdminTab(
    label   : String,
    selected: Boolean,
    count   : Int,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = if (selected) Primary.copy(alpha = 0.15f) else c.surface1,
        modifier = modifier
            .cardElevation(c)
            .border(1.dp, if (selected) Primary else c.borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) Primary else c.textMuted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) Primary else Color(0x33FFFFFF),
            ) {
                Text(
                    text  = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (selected) Color.White else c.textDim,
                        fontSize = 10.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Column {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = c.textMuted, fontSize = 11.sp, letterSpacing = 0.4.sp,
            ),
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun WpcDialogTextField(
    value               : String,
    onChange            : (String) -> Unit,
    placeholder         : String,
    keyboardType        : KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon        : @Composable (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    OutlinedTextField(
        value                = value,
        onValueChange        = onChange,
        modifier             = Modifier.fillMaxWidth(),
        placeholder          = { Text(placeholder, color = c.textDim) },
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon         = trailingIcon,
        singleLine           = true,
        shape                = RoundedCornerShape(8.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Primary,
            unfocusedBorderColor    = c.borderColor,
            focusedContainerColor   = c.surface2,
            unfocusedContainerColor = c.surface2,
            focusedTextColor        = c.textBright,
            unfocusedTextColor      = c.textBright,
            cursorColor             = Primary,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.textBright),
    )
}

@Composable
private fun EmptyState(message: String) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = c.textDim),
        )
    }
}
