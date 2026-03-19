package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences>
        by preferencesDataStore("wpc_theme")

enum class AppThemePreference(val label: String, val emoji: String) {
    DARK(   "Dark",           "🌙"),
    LIGHT(  "Light",          "☀️"),
    SYSTEM( "System Default", "📱"),
}

class ThemePreferences(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
    }

    val themePreference: Flow<AppThemePreference> = context.themeDataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            AppThemePreference.LIGHT.name  -> AppThemePreference.LIGHT
            AppThemePreference.SYSTEM.name -> AppThemePreference.SYSTEM
            else                           -> AppThemePreference.DARK  // default
        }
    }

    suspend fun setTheme(preference: AppThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = preference.name
        }
    }
}
