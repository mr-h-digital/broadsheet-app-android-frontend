package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import com.hildebrandtdigital.wpcbroadsheet.data.model.ResidentType

/**
 * Database representation of a resident.
 *
 * Primary key is (siteId, unitNumber) — a unit number is only meaningful
 * within a site context. After relocation, the row is updated in-place with
 * the new siteId (upsert replaces the old row, new row carries new siteId).
 *
 * Rows are NEVER hard-deleted. Use isActive = false for soft deactivation
 * so billing history remains intact and auditable.
 */
@Entity(
    tableName   = "residents",
    primaryKeys = ["siteId", "unitNumber"],
)
data class ResidentEntity(
    val siteId         : String,
    val unitNumber     : String,
    val clientName     : String,
    val totalOccupants : Int,
    val residentType   : ResidentType,

    // Soft delete
    val isActive       : Boolean = true,

    // Audit summary — last-write cache (full history lives in resident_audit)
    val createdBy      : String,
    val createdAt      : Long,           // epoch millis
    val lastModifiedBy : String,
    val lastModifiedAt : Long,           // epoch millis
    val deactivatedBy  : String? = null,
    val deactivatedAt  : Long?   = null, // epoch millis
)
