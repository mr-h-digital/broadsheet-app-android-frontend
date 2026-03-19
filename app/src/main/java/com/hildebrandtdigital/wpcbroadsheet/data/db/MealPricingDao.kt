package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPricingDao {

    /**
     * Live pricing config for a site in a given period.
     * Emits null if no config has been saved yet for that period —
     * the repository falls back to SampleData defaults in that case.
     */
    @Query("""
        SELECT * FROM meal_pricing
        WHERE siteId = :siteId
          AND year   = :year
          AND month  = :month
        LIMIT 1
    """)
    fun observePricing(siteId: String, year: Int, month: Int): Flow<MealPricingEntity?>

    /**
     * Most recent saved config for a site regardless of period.
     * Used to pre-populate the pricing screen when opening a new month
     * so the user starts from the last known values.
     */
    @Query("""
        SELECT * FROM meal_pricing
        WHERE siteId = :siteId
        ORDER BY year DESC, month DESC
        LIMIT 1
    """)
    suspend fun findLatest(siteId: String): MealPricingEntity?

    /**
     * Insert or fully replace a pricing config for a period.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pricing: MealPricingEntity)

    /**
     * Pricing rows where the backend has not yet acknowledged the save
     * (lastSyncedAt IS NULL). Used by the future sync worker.
     */
    @Query("""
        SELECT * FROM meal_pricing
        WHERE lastSyncedAt IS NULL
    """)
    suspend fun findUnsynced(): List<MealPricingEntity>

    /**
     * Mark a pricing row as synced after successful backend acknowledgement.
     */
    @Query("""
        UPDATE meal_pricing
        SET lastSyncedAt = :syncedAt
        WHERE siteId = :siteId
          AND year   = :year
          AND month  = :month
    """)
    suspend fun markSynced(siteId: String, year: Int, month: Int, syncedAt: Long)
}
