package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // ── Observe ──────────────────────────────────────────────────────────────

    /** Live list of all active users — for Admin user-management screen. */
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY role, name")
    fun observeActiveUsers(): Flow<List<UserEntity>>

    /** Live list of active Unit Managers assigned to a specific site. */
    @Query("SELECT * FROM users WHERE isActive = 1 AND role = 'UNIT_MANAGER' AND siteId = :siteId ORDER BY name")
    fun observeUnitManagersForSite(siteId: String): Flow<List<UserEntity>>

    /** Live list of ALL active Unit Managers (used by Ops Manager). */
    @Query("SELECT * FROM users WHERE isActive = 1 AND role = 'UNIT_MANAGER' ORDER BY name")
    fun observeAllUnitManagers(): Flow<List<UserEntity>>

    // ── Lookup ───────────────────────────────────────────────────────────────

    /** Find a single active user by email — used for login. */
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1 LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    /** Find by primary key. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    /** Count all users — used to detect whether seed data exists. */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Long

    // ── Write ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    /** Soft-delete: mark inactive rather than physically deleting. */
    @Query("UPDATE users SET isActive = 0, lastModifiedAt = :now, lastModifiedBy = :actor WHERE id = :id")
    suspend fun deactivate(id: String, now: Long, actor: String)

    /** Reassign a Unit Manager to a different site. */
    @Query("UPDATE users SET siteId = :newSiteId, lastModifiedAt = :now, lastModifiedBy = :actor WHERE id = :id")
    suspend fun relocate(id: String, newSiteId: String, now: Long, actor: String)
}
