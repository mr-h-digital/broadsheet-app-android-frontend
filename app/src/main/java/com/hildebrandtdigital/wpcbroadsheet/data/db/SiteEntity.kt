package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted site record. Replaces the hardcoded SampleData.sites list.
 * Soft-deleted via [isActive] so site history is never lost.
 */
@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id      : String,
    val name                : String,
    val isActive            : Boolean = true,
    val createdAt           : Long,
    val createdBy           : String,
    val lastModifiedAt      : Long,
    val lastModifiedBy      : String,
)
