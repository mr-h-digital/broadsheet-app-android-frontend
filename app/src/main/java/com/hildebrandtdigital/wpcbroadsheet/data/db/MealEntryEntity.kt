package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Persisted monthly meal aggregate per resident per site.
 *
 * Primary key is (siteId, unitNumber, year, month) — one row per
 * resident per billing period. Captures are additive within a month:
 * the UI reads the current counts, the user adjusts them, and the
 * repository upserts the whole row.
 *
 * [countsJson] stores the Map<MealType, Int> as a JSON object,
 * converted by [RoomConverters]. Example:
 *   {"COURSE_1":12,"COURSE_2":4,"TA_BAKKIES":3}
 *
 * When the backend is connected, this table becomes a write-through
 * cache: the repository writes here immediately (optimistic local update)
 * and syncs to the API in the background. On a fresh sync the row is
 * replaced wholesale via upsert.
 */
@Entity(
    tableName  = "meal_entries",
    primaryKeys = ["siteId", "unitNumber", "year", "month"],
    indices     = [
        Index("siteId", "year", "month"),   // fast monthly report queries
        Index("unitNumber"),                 // fast per-resident history
    ]
)
data class MealEntryEntity(
    val siteId      : String,
    val unitNumber  : String,
    val year        : Int,
    val month       : Int,      // 1–12

    /** JSON-serialised Map<MealType,Int> — see RoomConverters */
    val countsJson  : String,

    /** Epoch millis of last write — used for cache invalidation / sync */
    val lastModifiedAt : Long,
    val lastModifiedBy : String,
)
