package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore("wpc_notifications")

data class NotificationSettings(
    val dailyCaptureReminder : Boolean = true,
    val reminderHour         : Int     = 9,    // 9 AM default
    val reminderMinute       : Int     = 0,
    val monthEndAlert        : Boolean = true,
    val monthEndDaysBefore   : Int     = 3,    // alert 3 days before month end
    val reportReadyAlert     : Boolean = false,
    val emailOnSave          : Boolean = false,
)

class NotificationPreferences(private val context: Context) {

    companion object {
        val KEY_DAILY_CAPTURE   = booleanPreferencesKey("daily_capture_reminder")
        val KEY_REMINDER_HOUR   = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_MONTH_END_ALERT = booleanPreferencesKey("month_end_alert")
        val KEY_MONTH_END_DAYS  = intPreferencesKey("month_end_days_before")
        val KEY_REPORT_READY    = booleanPreferencesKey("report_ready_alert")
        val KEY_EMAIL_ON_SAVE   = booleanPreferencesKey("email_on_save")
    }

    val settings: Flow<NotificationSettings> = context.notifDataStore.data.map { prefs ->
        NotificationSettings(
            dailyCaptureReminder = prefs[KEY_DAILY_CAPTURE]   ?: true,
            reminderHour         = prefs[KEY_REMINDER_HOUR]   ?: 9,
            reminderMinute       = prefs[KEY_REMINDER_MINUTE] ?: 0,
            monthEndAlert        = prefs[KEY_MONTH_END_ALERT] ?: true,
            monthEndDaysBefore   = prefs[KEY_MONTH_END_DAYS]  ?: 3,
            reportReadyAlert     = prefs[KEY_REPORT_READY]    ?: false,
            emailOnSave          = prefs[KEY_EMAIL_ON_SAVE]   ?: false,
        )
    }

    suspend fun update(block: NotificationSettings.() -> NotificationSettings) {
        context.notifDataStore.edit { prefs ->
            val current = NotificationSettings(
                dailyCaptureReminder = prefs[KEY_DAILY_CAPTURE]   ?: true,
                reminderHour         = prefs[KEY_REMINDER_HOUR]   ?: 9,
                reminderMinute       = prefs[KEY_REMINDER_MINUTE] ?: 0,
                monthEndAlert        = prefs[KEY_MONTH_END_ALERT] ?: true,
                monthEndDaysBefore   = prefs[KEY_MONTH_END_DAYS]  ?: 3,
                reportReadyAlert     = prefs[KEY_REPORT_READY]    ?: false,
                emailOnSave          = prefs[KEY_EMAIL_ON_SAVE]   ?: false,
            )
            val updated = current.block()
            prefs[KEY_DAILY_CAPTURE]   = updated.dailyCaptureReminder
            prefs[KEY_REMINDER_HOUR]   = updated.reminderHour
            prefs[KEY_REMINDER_MINUTE] = updated.reminderMinute
            prefs[KEY_MONTH_END_ALERT] = updated.monthEndAlert
            prefs[KEY_MONTH_END_DAYS]  = updated.monthEndDaysBefore
            prefs[KEY_REPORT_READY]    = updated.reportReadyAlert
            prefs[KEY_EMAIL_ON_SAVE]   = updated.emailOnSave
        }
    }
}
