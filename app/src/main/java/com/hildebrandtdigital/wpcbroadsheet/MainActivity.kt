package com.hildebrandtdigital.wpcbroadsheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.hildebrandtdigital.wpcbroadsheet.data.db.AppDatabase
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppThemePreference
import com.hildebrandtdigital.wpcbroadsheet.data.repository.MealRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.NotificationPreferences
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ResidentRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SiteRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ThemePreferences
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiClient
import com.hildebrandtdigital.wpcbroadsheet.data.network.AuthTokenStore
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.UserRepository
import com.hildebrandtdigital.wpcbroadsheet.navigation.WpcNavHost
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.WPCBroadsheetTheme
import com.hildebrandtdigital.wpcbroadsheet.workers.CaptureReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Initialise Room and repositories ──────────────────────────────────
        val db                 = AppDatabase.get(applicationContext)
        val tokenStore         = AuthTokenStore.getInstance(applicationContext)
        val userRepository     = UserRepository(db.userDao(), tokenStore)

        // Wire the token provider so ApiClient always sends the current JWT
        ApiClient.setTokenProvider { kotlinx.coroutines.runBlocking { tokenStore.getToken() } }

        // Restore session: if a JWT was persisted from a previous launch, call
        // /api/auth/me to re-validate it and repopulate AppSession without
        // forcing the user to log in again.
        lifecycleScope.launch(Dispatchers.IO) {
            val token = tokenStore.getToken()
            if (token != null && !AppSession.isLoggedIn) {
                try {
                    val response = ApiClient.wpcApi.me()
                    if (response.isSuccessful) {
                        val apiUser = response.body()!!
                        val user = com.hildebrandtdigital.wpcbroadsheet.data.model.User(
                            id     = apiUser.id,
                            name   = apiUser.name,
                            email  = apiUser.email,
                            role   = com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole.valueOf(apiUser.role),
                            phone  = apiUser.phone,
                            siteId = apiUser.siteId,
                        )
                        AppSession.login(user)
                    } else {
                        // Token rejected — clear it so the user is prompted to log in
                        tokenStore.clearToken()
                    }
                } catch (_: Exception) {
                    // Network unavailable — session stays unauthenticated; login screen handles it
                }
            }
        }
        val siteRepository     = SiteRepository(db.siteDao())
        val residentRepository = ResidentRepository(db.residentDao(), db.residentAuditDao())
        val mealRepository     = MealRepository(db.mealEntryDao(), db.mealPricingDao())
        val themePreferences   = ThemePreferences(applicationContext)

        // ── Background work: seed DB + reschedule notifications ───────────────
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.ensureSeeded(applicationContext)

            val notifPrefs = NotificationPreferences(applicationContext)
            val settings   = notifPrefs.settings.first()
            if (settings.dailyCaptureReminder) {
                CaptureReminderWorker.schedule(
                    applicationContext,
                    settings.reminderHour,
                    settings.reminderMinute,
                )
            }
        }

        setContent {
            // Observe theme preference — recomposes WPCBroadsheetTheme on change
            val themePreference by themePreferences.themePreference
                .collectAsStateWithLifecycle(initialValue = AppThemePreference.DARK)

            WPCBroadsheetTheme(themePreference = themePreference) {
                val navController = rememberNavController()
                WpcNavHost(
                    navController      = navController,
                    userRepository     = userRepository,
                    siteRepository     = siteRepository,
                    residentRepository = residentRepository,
                    mealRepository     = mealRepository,
                )
            }
        }
    }
}