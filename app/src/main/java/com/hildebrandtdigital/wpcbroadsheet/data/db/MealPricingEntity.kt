package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Persisted pricing configuration per site per billing period.
 *
 * Primary key is (siteId, year, month) so pricing history is preserved
 * when rates change — a January report will always re-calculate correctly
 * using January's prices, not whatever the current config is.
 *
 * [effectiveFrom] is the billing period this config applies to.
 * When a price is updated mid-month, the new config takes effect from
 * the NEXT month — the repository enforces this by always writing to
 * the next period's row, never touching a closed period.
 *
 * All prices stored EXCLUSIVE of VAT, matching [MealPricing].
 *
 * Cache strategy: same as MealEntryEntity — local write-through, sync
 * to backend in background. [lastSyncedAt] is null until first successful
 * backend round-trip, letting the repository know it needs to be pushed.
 */
@Entity(
    tableName   = "meal_pricing",
    primaryKeys = ["siteId", "year", "month"],
    indices     = [Index("siteId")]
)
data class MealPricingEntity(
    val siteId      : String,
    val year        : Int,
    val month       : Int,      // 1–12

    // ── Meal prices excl. VAT ─────────────────────────────────────────────
    val course1                  : Double,
    val course2                  : Double,
    val course3                  : Double,
    val fullBoard                : Double,
    val sun1Course               : Double,
    val sun3Course               : Double,
    val breakfast                : Double,
    val dinner                   : Double,
    val soupDessert              : Double,
    val visitorMonSat            : Double,
    val visitorSun1              : Double,
    val visitorSun3              : Double,
    val taBakkies                : Double,
    val vatRate                  : Double,
    val compulsoryMealsDeduction : Double,

    // ── Audit ─────────────────────────────────────────────────────────────
    val lastModifiedAt : Long,
    val lastModifiedBy : String,

    /**
     * Null until this row has been successfully acknowledged by the backend.
     * The sync layer checks for rows where lastSyncedAt IS NULL to find
     * dirty configs that need pushing.
     */
    val lastSyncedAt   : Long? = null,
)
