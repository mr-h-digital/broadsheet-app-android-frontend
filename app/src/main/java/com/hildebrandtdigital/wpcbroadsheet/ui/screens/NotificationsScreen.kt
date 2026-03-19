package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.repository.NotificationPreferences
import com.hildebrandtdigital.wpcbroadsheet.data.repository.NotificationSettings
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import com.hildebrandtdigital.wpcbroadsheet.workers.CaptureReminderWorker
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.cardElevation
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.headerBand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = remember { NotificationPreferences(context) }
    val settings by prefs.settings.collectAsStateWithLifecycle(
        initialValue = NotificationSettings()
    )

    // ── Notification permission (Android 13+) ─────────────────────────────────
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    fun save(block: NotificationSettings.() -> NotificationSettings) {
        scope.launch {
            prefs.update(block)
            // Re-schedule or cancel worker based on new settings
            val updated = settings.block()
            if (updated.dailyCaptureReminder && hasPermission) {
                CaptureReminderWorker.schedule(context, updated.reminderHour, updated.reminderMinute)
            } else {
                CaptureReminderWorker.cancel(context)
            }
        }
    }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour   = settings.reminderHour,
        initialMinute = settings.reminderMinute,
        is24Hour      = false,
    )

    Scaffold(
        containerColor = c.bgDeep,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .headerBand(c)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Notifications", style = MaterialTheme.typography.headlineMedium.copy(color = if (c.isDark) c.textBright else androidx.compose.ui.graphics.Color.White))
                        Text("Reminders & alerts", style = MaterialTheme.typography.bodySmall.copy(color = if (c.isDark) c.textMuted else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f)))
                    }
                }
            }
        }

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .appBackground(c)
                
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Permission banner ─────────────────────────────────────────────
            if (!hasPermission) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = Secondary.copy(0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Secondary.copy(0.35f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.NotificationsOff, null, tint = Secondary, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permission required",
                                style = MaterialTheme.typography.titleMedium.copy(color = Secondary))
                            Text("Allow notifications to receive reminders",
                                style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                        }
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) {
                            Text("Allow", color = Secondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Daily Capture Reminder ────────────────────────────────────────
            NotifGroup(label = "Capture Reminders") {
                NotifToggleRow(
                    icon      = Icons.Rounded.EditNote,
                    iconBg    = Color(0x1A4F8EF7),
                    iconTint  = Primary,
                    title     = "Daily capture reminder",
                    subtitle  = "Get reminded if residents have missing meal data",
                    checked   = settings.dailyCaptureReminder,
                    onChecked = { save { copy(dailyCaptureReminder = it) } },
                    enabled   = hasPermission,
                )

                // Expandable time picker row
                AnimatedVisibility(
                    visible = settings.dailyCaptureReminder && hasPermission,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(start = 56.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x1A4FF7C8))
                                ) {
                                    Icon(Icons.Rounded.Schedule, null, tint = Accent, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Reminder time", style = MaterialTheme.typography.titleMedium.copy(color = c.textBright))
                                    Text("Tap to change", style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Primary.copy(0.12f),
                                modifier = Modifier.cardElevation(c).border(1.dp, Primary.copy(0.3f), RoundedCornerShape(8.dp)),
                            ) {
                                Text(
                                    formatTime(settings.reminderHour, settings.reminderMinute),
                                    style    = MaterialTheme.typography.labelLarge.copy(color = Primary),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Month-end Alerts ──────────────────────────────────────────────
            NotifGroup(label = "Month-end Alerts") {
                NotifToggleRow(
                    icon      = Icons.Rounded.CalendarMonth,
                    iconBg    = Color(0x1AF7A84F),
                    iconTint  = Secondary,
                    title     = "Month-end alert",
                    subtitle  = "Alert when approaching end of month with missing data",
                    checked   = settings.monthEndAlert,
                    onChecked = { save { copy(monthEndAlert = it) } },
                    enabled   = hasPermission,
                )

                AnimatedVisibility(
                    visible = settings.monthEndAlert && hasPermission,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(start = 56.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Alert days before month-end",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = c.textBright))
                                Text("${settings.monthEndDaysBefore} days",
                                    style = MaterialTheme.typography.labelLarge.copy(color = Secondary))
                            }
                            Spacer(Modifier.height(8.dp))
                            Slider(
                                value         = settings.monthEndDaysBefore.toFloat(),
                                onValueChange = { save { copy(monthEndDaysBefore = it.toInt()) } },
                                valueRange    = 1f..7f,
                                steps         = 5,
                                colors        = SliderDefaults.colors(
                                    thumbColor       = Secondary,
                                    activeTrackColor = Secondary,
                                    inactiveTrackColor = c.borderColor,
                                ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("1 day", style = MaterialTheme.typography.labelSmall)
                                Text("7 days", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Other Alerts ──────────────────────────────────────────────────
            NotifGroup(label = "Other") {
                NotifToggleRow(
                    icon      = Icons.Rounded.Description,
                    iconBg    = Color(0x1A4FF7C8),
                    iconTint  = Accent,
                    title     = "Report ready",
                    subtitle  = "Notify when a monthly report is ready to export",
                    checked   = settings.reportReadyAlert,
                    onChecked = { save { copy(reportReadyAlert = it) } },
                    enabled   = hasPermission,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Info footer ───────────────────────────────────────────────────
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = c.surface1,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.borderColor, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Info, null, tint = Primary.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                    Text(
                        "Notifications are checked daily at your scheduled time. " +
                        "The app must remain installed for reminders to work. " +
                        "You can manage all app notifications in your device Settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor   = c.surface1,
            title = { Text("Set reminder time", style = MaterialTheme.typography.headlineSmall.copy(color = c.textBright)) },
            text  = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state  = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor        = c.surface2,
                            selectorColor         = Primary,
                            containerColor        = c.surface1,
                            periodSelectorBorderColor = c.borderColor,
                            timeSelectorSelectedContainerColor = Primary.copy(0.15f),
                            timeSelectorSelectedContentColor   = Primary,
                            timeSelectorUnselectedContainerColor = c.surface2,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    save {
                        copy(
                            reminderHour   = timePickerState.hour,
                            reminderMinute = timePickerState.minute,
                        )
                    }
                    showTimePicker = false
                }) { Text("Set", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = c.textMuted)
                }
            },
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun formatTime(hour: Int, minute: Int): String {
    val h   = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val m   = minute.toString().padStart(2, '0')
    val amPm = if (hour < 12) "AM" else "PM"
    return "$h:$m $amPm"
}

@Composable
private fun NotifGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = c.textMuted,
            letterSpacing = 1.2.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = c.surface1,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun NotifToggleRow(
    icon     : ImageVector,
    iconBg   : Color,
    iconTint : Color,
    title    : String,
    subtitle : String,
    checked  : Boolean,
    onChecked: (Boolean) -> Unit,
    enabled  : Boolean = true,
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChecked(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(
                color = if (enabled) c.textBright else c.textMuted))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = c.textMuted))
        }
        Switch(
            checked         = checked && enabled,
            onCheckedChange = if (enabled) onChecked else null,
            enabled         = enabled,
            colors          = SwitchDefaults.colors(
                checkedThumbColor       = Color.White,
                checkedTrackColor       = iconTint,
                uncheckedThumbColor     = c.textDim,
                uncheckedTrackColor     = c.surface3,
                uncheckedBorderColor    = c.borderColor,
                disabledCheckedTrackColor   = iconTint.copy(0.4f),
                disabledUncheckedTrackColor = c.surface2,
            ),
        )
    }
}
