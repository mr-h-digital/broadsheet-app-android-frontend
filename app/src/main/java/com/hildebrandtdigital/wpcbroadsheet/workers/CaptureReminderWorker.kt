package com.hildebrandtdigital.wpcbroadsheet.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.hildebrandtdigital.wpcbroadsheet.MainActivity
import com.hildebrandtdigital.wpcbroadsheet.R
import com.hildebrandtdigital.wpcbroadsheet.data.db.AppDatabase
import com.hildebrandtdigital.wpcbroadsheet.data.repository.NotificationPreferences
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CaptureReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME       = "capture_reminder"
        const val CHANNEL_ID      = "wpc_capture_reminders"
        const val CHANNEL_NAME    = "Capture Reminders"
        const val NOTIF_ID        = 1001

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now    = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                // If the time has already passed today, schedule for tomorrow
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<CaptureReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val prefs    = NotificationPreferences(context)
        val settings = prefs.settings.first()

        if (!settings.dailyCaptureReminder) return Result.success()

        val db         = AppDatabase.get(context)
        val now        = Calendar.getInstance()
        val year       = now.get(Calendar.YEAR)
        val month      = now.get(Calendar.MONTH) + 1
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

        // Collect missing capture counts per site
        val siteMessages = mutableListOf<String>()
        SampleData.sites.forEach { site ->
            val entries   = db.mealEntryDao().findForMonth(site.id, year, month)
            val residents = db.residentDao().getForSite(site.id)
            val captured  = entries.map { it.unitNumber }.toSet().size
            val missing   = residents.size - captured
            if (missing > 0) siteMessages += "${site.name.split(" ").first()}: $missing pending"
        }

        if (siteMessages.isEmpty()) return Result.success()

        // Check month-end alert
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft    = daysInMonth - dayOfMonth
        val isMonthEnd  = settings.monthEndAlert && daysLeft <= settings.monthEndDaysBefore

        val title = if (isMonthEnd)
            "⚠️ Month-end approaching — $daysLeft days left!"
        else
            "📋 Meal capture reminder"

        val body = siteMessages.joinToString("  ·  ")

        sendNotification(title, body)
        return Result.success()
    }

    private fun sendNotification(title: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (no-op on subsequent calls)
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Daily reminders to capture meal data"
        }
        nm.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notif)
    }
}
