package com.hildebrandtdigital.wpcbroadsheet.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property — one DataStore per app
private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Persists the JWT token across process death using DataStore.
 * Inject or access via [AuthTokenStore.getInstance].
 */
class AuthTokenStore private constructor(context: Context) {

    private val dataStore = context.applicationContext.authDataStore

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")

        @Volatile
        private var INSTANCE: AuthTokenStore? = null

        fun getInstance(context: Context): AuthTokenStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthTokenStore(context).also { INSTANCE = it }
            }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    val tokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    suspend fun getToken(): String? = tokenFlow.first()

    fun bearerToken(token: String) = "Bearer $token"

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun saveToken(token: String) {
        dataStore.edit { prefs -> prefs[TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        dataStore.edit { prefs -> prefs.remove(TOKEN_KEY) }
    }
}