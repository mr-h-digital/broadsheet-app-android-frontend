package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AvatarManager
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ResidentRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.*
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.components.UserAvatar
import com.hildebrandtdigital.wpcbroadsheet.ui.components.AvatarPickerSheet
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation

@Composable
fun ProfileScreen(
    residentRepository     : ResidentRepository? = null,
    onNavigateToDashboard  : () -> Unit,
    onNavigateToCapture    : () -> Unit,
    onNavigateToReports    : () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSites      : () -> Unit = {},
    onNavigateToResidents  : () -> Unit = {},
    onNavigateToHistory    : () -> Unit = {},
    onNavigateToPricing    : () -> Unit = {},
    onNavigateToAdmin      : () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppearance   : () -> Unit = {},
    onSignOut              : () -> Unit,
) {
    val c = LocalAppColors.current
    val role       = AppSession.currentRole
    val userName   = AppSession.currentUserName
    val userEmail  = AppSession.currentUser?.email ?: ""
    val userSiteId = AppSession.currentSiteId

    val roleLabel = when (role) {
        UserRole.ADMIN              -> "Admin"
        UserRole.OPERATIONS_MANAGER -> "Operations Manager"
        UserRole.UNIT_MANAGER       -> "Unit Manager"
    }
    val roleEmoji = when (role) {
        UserRole.ADMIN              -> "🔑"
        UserRole.OPERATIONS_MANAGER -> "⚙️"
        UserRole.UNIT_MANAGER       -> "🏠"
    }
    val roleColor = when (role) {
        UserRole.ADMIN              -> Color(0xFFFFD700)
        UserRole.OPERATIONS_MANAGER -> Primary
        UserRole.UNIT_MANAGER       -> Accent
    }
    // Secondary accent for the two-tone glow — gives each role a unique look
    val roleGlow2 = when (role) {
        UserRole.ADMIN              -> Color(0xFFFFA500)  // gold → orange
        UserRole.OPERATIONS_MANAGER -> Color(0xFFA04FF7)  // blue → purple
        UserRole.UNIT_MANAGER       -> Color(0xFF4F8EF7)  // teal → blue
    }
    val siteName = userSiteId?.let { sid ->
        SampleData.sites.firstOrNull { it.id == sid }?.name
    }
    val statSites = if (AppSession.hasCrossSiteAccess)
        SampleData.sites.size.toString() else "1"

    // Pull resident count from Room so it reflects live DB data, not stale SampleData
    val statResidents by produceState(initialValue = "…") {
        value = if (residentRepository != null) {
            if (AppSession.hasCrossSiteAccess) {
                residentRepository.countAll().toString()
            } else {
                userSiteId?.let { residentRepository.countForSite(it) }?.toString() ?: "0"
            }
        } else {
            // Fallback to SampleData if repository not wired (preview/test)
            if (AppSession.hasCrossSiteAccess)
                SampleData.residents.size.toString()
            else
                SampleData.residents.count { it.siteId == userSiteId }.toString()
        }
    }

    // ── Avatar state ──────────────────────────────────────────────────────────
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val userId        = AppSession.currentUserId

    val avatarPath by AvatarManager
        .observeAvatarPath(context, userId)
        .collectAsStateWithLifecycle(initialValue = null)

    // Cache-bust key: incremented after each save so Coil reloads the file
    // even though the file path stays the same
    var avatarVersion   by remember { mutableLongStateOf(0L) }
    var showPickerSheet by remember { mutableStateOf(false) }
    var cropBitmap      by remember { mutableStateOf<Bitmap?>(null) }

    // Show the crop screen as a full-screen overlay when a bitmap is ready
    if (cropBitmap != null) {
        AvatarCropScreen(
            bitmap    = cropBitmap!!,
            onCropped = { cropped ->
                scope.launch {
                    AvatarManager.saveAvatar(context, userId, cropped)
                    avatarVersion++ // forces Coil to reload
                    cropBitmap = null
                }
            },
            onCancel = { cropBitmap = null },
        )
        return@ProfileScreen  // stop rendering the rest of the screen
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
                        else             -> {}
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
            // ── Hero ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .headerBand(c)
                    .padding(horizontal = 24.dp),
            ) {
                WpcLogoBadge(
                    size     = WpcBrandingSize.COMPACT,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 28.dp),
                ) {
                    // Avatar — tapping opens the picker sheet
                    Box(modifier = Modifier.clickable { showPickerSheet = true }) {
                        UserAvatar(
                            name          = userName,
                            avatarPath    = avatarPath,
                            avatarVersion = avatarVersion,
                            primaryColor  = roleColor,
                            secondaryColor= roleGlow2,
                            size          = 80.dp,
                            fontSize      = 28.sp,
                        )
                        // Online indicator dot
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Accent)
                                .border(2.dp, c.bgDeep, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                        // Camera icon badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c.surface1)
                                .border(1.dp, c.borderColor, CircleShape)
                                .align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = c.textMuted, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text  = userName,
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White
                        ),
                    )

                    if (userEmail.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = userEmail,
                            style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textDim else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f)),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = if (c.isDark) roleColor.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .border(1.dp, if (c.isDark) roleColor.copy(alpha = 0.30f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                    ) {
                        Text(
                            text  = "$roleEmoji  $roleLabel",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color    = if (c.isDark) roleColor else androidx.compose.ui.graphics.Color.White,
                                fontSize = 12.sp,
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }

                    if (role == UserRole.UNIT_MANAGER && siteName != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape    = RoundedCornerShape(50),
                            color    = if (c.isDark) c.surface2 else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                            modifier = Modifier.border(1.dp, if (c.isDark) c.borderColor else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                        ) {
                            Text(
                                text  = "📍  $siteName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color    = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = c.surface1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.borderColor, RoundedCornerShape(16.dp)),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ProfileStat(statSites,     "Sites",     Modifier.weight(1f))
                            VerticalDivider()
                            ProfileStat(statResidents, "Residents", Modifier.weight(1f))
                            VerticalDivider()
                            val now = java.util.Calendar.getInstance()
                            val monthName = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                                .format(now.time)
                            val year = now.get(java.util.Calendar.YEAR)
                            ProfileStat("$monthName $year", "Period",    Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Settings ──────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                Spacer(Modifier.height(20.dp))

                SettingsGroup {
                    SettingsItem(
                        icon     = Icons.Rounded.Edit,
                        iconBg   = Color(0x1A4F8EF7),
                        iconTint = Primary,
                        label    = "Edit Profile",
                        sub      = "Update your name, email and password",
                        onClick  = onNavigateToEditProfile,
                    )
                    if (AppSession.hasCrossSiteAccess) {
                        SettingsItem(
                            icon     = Icons.Rounded.Home,
                            iconBg   = Color(0x1A4F8EF7),
                            iconTint = Primary,
                            label    = "Manage Sites",
                            sub      = "Add or edit retirement villages",
                            onClick  = onNavigateToSites,
                        )
                    }
                    SettingsItem(
                        icon     = Icons.Rounded.People,
                        iconBg   = Color(0x1AF7A84F),
                        iconTint = Secondary,
                        label    = "Manage Residents",
                        sub      = if (AppSession.hasCrossSiteAccess)
                            "Update resident & unit details"
                        else
                            "Manage residents at ${siteName ?: "your site"}",
                        onClick  = onNavigateToResidents,
                    )
                    SettingsItem(
                        icon     = Icons.Rounded.Payments,
                        iconBg   = Color(0x1A4FF7C8),
                        iconTint = Accent,
                        label    = "Pricing & Meal Rates",
                        sub      = "Update VAT rates and meal prices",
                        onClick  = onNavigateToPricing,
                    )
                    if (AppSession.isAdmin) {
                        SettingsItem(
                            icon     = Icons.Rounded.AdminPanelSettings,
                            iconBg   = Color(0x1AFFD700),
                            iconTint = Color(0xFFFFD700),
                            label    = "Admin Panel",
                            sub      = "Manage users, roles and sites",
                            onClick  = onNavigateToAdmin,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                SettingsGroup {
                    SettingsItem(
                        icon     = Icons.Rounded.Archive,
                        iconBg   = Color(0x1A4F8EF7),
                        iconTint = Primary,
                        label    = "Billing History",
                        sub      = "View and export past month broadsheets",
                        onClick  = onNavigateToHistory,
                    )
                    SettingsItem(
                        icon     = Icons.Rounded.Notifications,
                        iconBg   = Color(0x1AF7A84F),
                        iconTint = Secondary,
                        label    = "Notifications",
                        sub      = "Reminders for pending data",
                        onClick  = onNavigateToNotifications,
                    )
                    SettingsItem(
                        icon     = Icons.Rounded.Palette,
                        iconBg   = Color(0x1AA04FF7),
                        iconTint = Color(0xFFA04FF7),
                        label    = "Appearance",
                        sub      = "Dark mode, light mode, or system default",
                        onClick  = onNavigateToAppearance,
                    )
                }

                Spacer(Modifier.height(16.dp))

                SettingsGroup {
                    SettingsItem(
                        icon       = Icons.AutoMirrored.Rounded.Logout,
                        iconBg     = Color(0x1AF74F6B),
                        iconTint   = Danger,
                        label      = "Sign Out",
                        sub        = null,
                        labelColor = Danger,
                        onClick    = onSignOut,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text      = "WPC Broadsheet Manager v1.0.0\n© 2026 Hildebrandt Digital",
                    style     = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
            }
        }
    }

    // ── Avatar picker sheet ───────────────────────────────────────────────────
    if (showPickerSheet) {
        AvatarPickerSheet(
            hasAvatar    = avatarPath != null,
            onBitmapReady = { bmp ->
                showPickerSheet = false
                cropBitmap = bmp
            },
            onRemove = {
                scope.launch { AvatarManager.deleteAvatar(context, userId) }
            },
            onDismiss = { showPickerSheet = false },
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(
        modifier            = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                color      = Primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                fontSize   = 16.sp,
            ),
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color       = c.textMuted,
                fontWeight  = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
        )
    }
}

@Composable
private fun VerticalDivider() {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(60.dp)
            .background(c.borderColor)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = c.surface1,
        modifier = Modifier
            .fillMaxWidth()
            .cardElevation(c, elevation = 8.dp, cornerDp = 16.dp)
            .border(1.dp, c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon      : ImageVector,
    iconBg    : Color,
    iconTint  : Color,
    label     : String,
    sub       : String?,
    labelColor: Color      = Color.Unspecified,
    onClick   : () -> Unit = {},
) {
    val c          = LocalAppColors.current
    val resolvedLabelColor = if (labelColor == Color.Unspecified) c.textBright else labelColor
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(color = resolvedLabelColor))
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim, modifier = Modifier.size(18.dp))
    }
}