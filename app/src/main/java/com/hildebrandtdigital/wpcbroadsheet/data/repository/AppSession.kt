package com.hildebrandtdigital.wpcbroadsheet.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hildebrandtdigital.wpcbroadsheet.data.model.User
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole

/**
 * In-memory session for the currently logged-in user.
 *
 * Populated from Room on login (via [UserRepository.login]).
 * In production this would survive process death via encrypted
 * SharedPreferences holding a session token.
 */
object AppSession {

    var currentUser   by mutableStateOf<User?>(null)
        private set

    // ── Convenience accessors ────────────────────────────────────────────────

    val currentRole    get() = currentUser?.role ?: UserRole.UNIT_MANAGER
    val currentSiteId  get() = currentUser?.siteId
    val currentUserId  get() = currentUser?.id   ?: "system"
    val currentUserName get() = currentUser?.name ?: "system"

    val isLoggedIn        get() = currentUser != null
    val isUnitManager     get() = currentRole == UserRole.UNIT_MANAGER
    val isOpsManager      get() = currentRole == UserRole.OPERATIONS_MANAGER
    val isAdmin           get() = currentRole == UserRole.ADMIN
    /** Ops Managers and Admins have cross-site visibility. */
    val hasCrossSiteAccess get() = currentRole != UserRole.UNIT_MANAGER

    // ── Auth ─────────────────────────────────────────────────────────────────

    fun login(user: User) {
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }

    // ── Legacy shim ──────────────────────────────────────────────────────────
    // Kept so screens that haven't been migrated yet can still call login().
    fun login(role: UserRole, siteId: String? = null, userName: String = "system") {
        currentUser = User(
            id     = "legacy",
            name   = userName,
            email  = userName,
            role   = role,
            siteId = siteId,
        )
    }
}
