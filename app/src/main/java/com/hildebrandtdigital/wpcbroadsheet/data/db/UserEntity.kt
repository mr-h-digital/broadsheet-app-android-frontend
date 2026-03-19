package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted user account.
 *
 * [passwordHash] stores a SHA-256 hex digest of the password. In production
 * this would be replaced with a bcrypt/Argon2 hash or a backend-issued token.
 * For the offline-first demo phase, SHA-256 is sufficient to avoid storing
 * passwords in plain text.
 *
 * [role] is stored as a String (UserRole.name) via RoomConverters.
 *
 * [siteId] is null for OPERATIONS_MANAGER and ADMIN — they have cross-site access.
 * For UNIT_MANAGER it holds the assigned site's id.
 *
 * [isActive] supports soft-delete: deactivated users cannot log in but their
 * audit history is preserved.
 */
@Entity(
    tableName = "users",
    indices   = [
        Index("email", unique = true),
        Index("role"),
        Index("siteId"),
    ]
)
data class UserEntity(
    @PrimaryKey val id             : String,
    val name                       : String,
    val email                      : String,
    val passwordHash               : String,
    val role                       : String,   // UserRole.name
    val phone                      : String = "",
    val siteId                     : String? = null,
    val isActive                   : Boolean = true,
    val createdAt                  : Long,     // epoch millis
    val createdBy                  : String,
    val lastModifiedAt             : Long,
    val lastModifiedBy             : String,
)
