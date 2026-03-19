package com.hildebrandtdigital.wpcbroadsheet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.MealRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.ResidentRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SiteRepository
import com.hildebrandtdigital.wpcbroadsheet.data.repository.UserRepository
import com.hildebrandtdigital.wpcbroadsheet.ui.screens.*

const val SITE_ALL = "__all__"

object Routes {
    const val SPLASH          = "splash"
    const val LOGIN           = "login"
    const val DASHBOARD       = "dashboard"
    const val CAPTURE         = "capture?siteId={siteId}"
    const val REPORTS         = "reports?siteId={siteId}"
    const val PROFILE         = "profile"
    const val RESIDENTS       = "residents?siteId={siteId}"
    const val MONTHLY_HISTORY = "monthly_history"
    const val PRICING_CONFIG  = "pricing_config"
    const val EDIT_PROFILE    = "edit_profile"
    const val NOTIFICATIONS   = "notifications"
    const val APPEARANCE      = "appearance"
    const val SITE_MANAGEMENT = "site_management?siteId={siteId}"
    const val ADMIN           = "admin"

    fun capture(siteId: String = SITE_ALL)     = "capture?siteId=$siteId"
    fun reports(siteId: String = SITE_ALL)     = "reports?siteId=$siteId"
    fun residents(siteId: String = SITE_ALL)   = "residents?siteId=$siteId"
    fun siteManagement(siteId: String? = null) =
        if (siteId != null) "site_management?siteId=$siteId" else "site_management?siteId="
}

private fun siteArg(name: String = "siteId") = navArgument(name) {
    type         = NavType.StringType
    nullable     = true
    defaultValue = SITE_ALL
}

@Composable
fun WpcNavHost(
    navController      : NavHostController,
    userRepository     : UserRepository,
    siteRepository     : SiteRepository,
    residentRepository : ResidentRepository,
    mealRepository     : MealRepository,
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                userRepository = userRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                mealRepository         = mealRepository,
                residentRepository     = residentRepository,
                onNavigateToCapture     = { siteId -> navController.navigate(Routes.capture(siteId)) },
                onNavigateToReports     = { siteId -> navController.navigate(Routes.reports(siteId)) },
                onNavigateToProfile     = { navController.navigate(Routes.PROFILE) },
                onNavigateToSiteDetails = { siteId -> navController.navigate(Routes.siteManagement(siteId)) },
            )
        }

        // ── Capture ───────────────────────────────────────────────────────────
        composable(Routes.CAPTURE, arguments = listOf(siteArg())) { back ->
            val siteId = back.arguments?.getString("siteId")?.takeIf { it.isNotBlank() } ?: SITE_ALL
            CaptureScreen(
                initialSiteId         = siteId,
                repository            = mealRepository,
                residentRepository    = residentRepository,
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToReports   = { sid -> navController.navigate(Routes.reports(sid)) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Reports ───────────────────────────────────────────────────────────
        composable(Routes.REPORTS, arguments = listOf(siteArg())) { back ->
            val siteId = back.arguments?.getString("siteId")?.takeIf { it.isNotBlank() } ?: SITE_ALL
            ReportsScreen(
                initialSiteId         = siteId,
                mealRepository        = mealRepository,
                residentRepository    = residentRepository,
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture   = { sid -> navController.navigate(Routes.capture(sid)) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                residentRepository      = residentRepository,
                onNavigateToDashboard   = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture     = { navController.navigate(Routes.capture()) },
                onNavigateToReports     = { navController.navigate(Routes.reports()) },
                onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onNavigateToSites       = { navController.navigate(Routes.siteManagement()) },
                onNavigateToResidents   = {
                    val sid = if (AppSession.hasCrossSiteAccess) SITE_ALL
                              else (AppSession.currentSiteId ?: SITE_ALL)
                    navController.navigate(Routes.residents(sid))
                },
                onNavigateToHistory       = { navController.navigate(Routes.MONTHLY_HISTORY) },
                onNavigateToPricing       = { navController.navigate(Routes.PRICING_CONFIG) },
                onNavigateToAdmin         = { navController.navigate(Routes.ADMIN) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToAppearance    = { navController.navigate(Routes.APPEARANCE) },
                onSignOut = {
                    AppSession.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Edit Profile ──────────────────────────────────────────────────────
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved        = { navController.popBackStack() },
            )
        }

        // ── Notifications ─────────────────────────────────────────────────────
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Appearance ────────────────────────────────────────────────────────
        composable(Routes.APPEARANCE) {
            AppearanceScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Residents ─────────────────────────────────────────────────────────
        composable(Routes.RESIDENTS, arguments = listOf(siteArg())) { back ->
            val siteId = back.arguments?.getString("siteId")?.takeIf { it.isNotBlank() } ?: SITE_ALL
            ResidentManagementScreen(
                initialSiteId         = siteId,
                repository            = residentRepository,
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture   = { navController.navigate(Routes.capture()) },
                onNavigateToReports   = { navController.navigate(Routes.reports()) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Monthly History ───────────────────────────────────────────────────
        composable(Routes.MONTHLY_HISTORY) {
            MonthlyHistoryScreen(
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture   = { navController.navigate(Routes.capture()) },
                onNavigateToReports   = { navController.navigate(Routes.reports()) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Pricing Config ────────────────────────────────────────────────────
        composable(Routes.PRICING_CONFIG) {
            PricingConfigScreen(
                repository            = mealRepository,
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture   = { navController.navigate(Routes.capture()) },
                onNavigateToReports   = { navController.navigate(Routes.reports()) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Site Management ───────────────────────────────────────────────────
        composable(
            route     = Routes.SITE_MANAGEMENT,
            arguments = listOf(navArgument("siteId") {
                type         = NavType.StringType
                nullable     = true
                defaultValue = null
            }),
        ) { back ->
            val siteId = back.arguments?.getString("siteId")?.takeIf { it.isNotBlank() }
            SiteManagementScreen(
                initialSiteId         = siteId,
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToCapture   = { navController.navigate(Routes.capture()) },
                onNavigateToReports   = { navController.navigate(Routes.reports()) },
                onNavigateToProfile   = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Admin Panel ───────────────────────────────────────────────────────
        composable(Routes.ADMIN) {
            AdminScreen(
                userRepository = userRepository,
                siteRepository = siteRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
