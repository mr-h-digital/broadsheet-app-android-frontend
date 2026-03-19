package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.util.Log
import com.hildebrandtdigital.wpcbroadsheet.data.db.UserDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.UserEntity
import com.hildebrandtdigital.wpcbroadsheet.data.model.User
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiClient
import com.hildebrandtdigital.wpcbroadsheet.data.network.AuthTokenStore
import com.hildebrandtdigital.wpcbroadsheet.data.network.LoginRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "UserRepository"

class UserRepository(
    private val dao            : UserDao,
    private val tokenStore     : AuthTokenStore,
) {

    // ── Observe ──────────────────────────────────────────────────────────────

    fun observeActiveUsers(): Flow<List<User>> =
        dao.observeActiveUsers().map { list -> list.map { it.toDomain() } }

    fun observeUnitManagersForSite(siteId: String): Flow<List<User>> =
        dao.observeUnitManagersForSite(siteId).map { list -> list.map { it.toDomain() } }

    fun observeAllUnitManagers(): Flow<List<User>> =
        dao.observeAllUnitManagers().map { list -> list.map { it.toDomain() } }

    // ── Auth ─────────────────────────────────────────────────────────────────

    /**
     * Attempts login.
     *
     * Strategy:
     * 1. Try the remote API (requires network).
     *    On success → cache the user in Room, persist JWT token.
     * 2. If the network call fails for any reason → fall back to Room
     *    (offline mode, last-known credentials).
     *
     * Returns the matching [User] on success, null on failure.
     */
    suspend fun login(email: String, password: String): User? {
        val hash = sha256(password)

        // ── 1. Try API ────────────────────────────────────────────────────────
        try {
            val response = ApiClient.wpcApi.login(
                LoginRequest(email = email.trim().lowercase(), password = hash)
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                // Persist JWT so it survives process death
                tokenStore.saveToken(body.token)
                // Cache user in Room for offline access
                val entity = UserEntity(
                    id             = body.user.id,
                    name           = body.user.name,
                    email          = body.user.email,
                    passwordHash   = hash,
                    role           = body.user.role,
                    phone          = body.user.phone,
                    siteId         = body.user.siteId,
                    isActive       = true,
                    createdAt      = System.currentTimeMillis(),
                    createdBy      = "api",
                    lastModifiedAt = System.currentTimeMillis(),
                    lastModifiedBy = "api",
                )
                dao.upsert(entity)
                Log.d(TAG, "Login via API succeeded for ${body.user.email}")
                return entity.toDomain()
            } else {
                Log.w(TAG, "API login rejected: HTTP ${response.code()}")
                // 401 = wrong credentials — don't fall back to Room
                if (response.code() == 401) return null
            }
        } catch (e: Exception) {
            Log.w(TAG, "API unreachable, falling back to Room: ${e.message}")
        }

        // ── 2. Fall back to Room (offline) ────────────────────────────────────
        val entity = dao.findByEmail(email.trim().lowercase()) ?: return null
        return if (entity.passwordHash == hash) {
            Log.d(TAG, "Login via Room (offline) succeeded for ${entity.email}")
            entity.toDomain()
        } else null
    }

    /**
     * Logs out: clears the JWT token and session.
     */
    suspend fun logout() {
        tokenStore.clearToken()
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun createUser(
        name      : String,
        email     : String,
        password  : String,
        role      : UserRole,
        phone     : String  = "",
        siteId    : String? = null,
        actor     : String,
    ): User {
        val now    = System.currentTimeMillis()
        val entity = UserEntity(
            id             = UUID.randomUUID().toString(),
            name           = name.trim(),
            email          = email.trim().lowercase(),
            passwordHash   = sha256(password),
            role           = role.name,
            phone          = phone.trim(),
            siteId         = if (role == UserRole.UNIT_MANAGER) siteId else null,
            isActive       = true,
            createdAt      = now,
            createdBy      = actor,
            lastModifiedAt = now,
            lastModifiedBy = actor,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun updateUser(
        id          : String,
        name        : String,
        email       : String,
        role        : UserRole,
        phone       : String  = "",
        siteId      : String? = null,
        newPassword : String? = null,
        actor       : String,
    ) {
        val existing = dao.findById(id) ?: return
        val now      = System.currentTimeMillis()
        val hash     = if (!newPassword.isNullOrBlank()) sha256(newPassword) else existing.passwordHash
        dao.upsert(
            existing.copy(
                name           = name.trim(),
                email          = email.trim().lowercase(),
                passwordHash   = hash,
                role           = role.name,
                phone          = phone.trim(),
                siteId         = if (role == UserRole.UNIT_MANAGER) siteId else null,
                lastModifiedAt = now,
                lastModifiedBy = actor,
            )
        )
    }

    suspend fun deactivateUser(id: String, actor: String) =
        dao.deactivate(id, System.currentTimeMillis(), actor)

    suspend fun relocateUnitManager(id: String, newSiteId: String, actor: String) =
        dao.relocate(id, newSiteId, System.currentTimeMillis(), actor)

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun UserEntity.toDomain() = User(
        id     = id,
        name   = name,
        email  = email,
        role   = UserRole.valueOf(role),
        phone  = phone,
        siteId = siteId,
    )
}