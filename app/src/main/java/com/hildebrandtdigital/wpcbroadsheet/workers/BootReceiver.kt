package com.hildebrandtdigital.wpcbroadsheet.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hildebrandtdigital.wpcbroadsheet.data.repository.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules the daily capture reminder after a device reboot.
 * WorkManager's PeriodicWorkRequest survives normal app restarts but
 * is cleared on reboot — this receiver restores it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val prefs    = NotificationPreferences(context)
            val settings = prefs.settings.first()
            if (settings.dailyCaptureReminder) {
                CaptureReminderWorker.schedule(
                    context,
                    settings.reminderHour,
                    settings.reminderMinute,
                )
            }
        }
    }
}
