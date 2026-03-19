package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {

    /**
     * Live stream of all meal entries for a site in a given month.
     * The UI collects this — any upsert will automatically push a new
     * emission and the capture screen re-renders with current counts.
     */
    @Query("""
        SELECT * FROM meal_entries
        WHERE siteId = :siteId
          AND year   = :year
          AND month  = :month
        ORDER BY unitNumber
    """)
    fun observeEntries(siteId: String, year: Int, month: Int): Flow<List<MealEntryEntity>>

    /**
     * Single entry lookup — used to read existing counts before a merge.
     */
    @Query("""
        SELECT * FROM meal_entries
        WHERE siteId     = :siteId
          AND unitNumber = :unitNumber
          AND year       = :year
          AND month      = :month
        LIMIT 1
    """)
    suspend fun find(
        siteId    : String,
        unitNumber: String,
        year      : Int,
        month     : Int,
    ): MealEntryEntity?

    /**
     * Insert or replace the full entry for a resident in a period.
     * The repository always reads → merges → upserts so no counts
     * are silently lost.
     */
    /**
     * One-shot snapshot of entries for a month — used by the notification
     * worker which can't collect a Flow inside doWork().
     */
    @Query("""
        SELECT * FROM meal_entries
        WHERE siteId = :siteId
          AND year   = :year
          AND month  = :month
    """)
    suspend fun findForMonth(siteId: String, year: Int, month: Int): List<MealEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MealEntryEntity)

    /**
     * All entries for a resident across all months — used to build
     * the monthly history view and future backend sync payloads.
     */
    @Query("""
        SELECT * FROM meal_entries
        WHERE siteId     = :siteId
          AND unitNumber = :unitNumber
        ORDER BY year DESC, month DESC
    """)
    fun observeResidentHistory(
        siteId    : String,
        unitNumber: String,
    ): Flow<List<MealEntryEntity>>

    /**
     * Entries not yet synced to the backend (lastModifiedAt newer than
     * whatever the sync layer tracks, or use a dirty flag if preferred).
     * Exposed for the future sync worker.
     */
    @Query("""
        SELECT * FROM meal_entries
        WHERE siteId = :siteId
          AND year   = :year
          AND month  = :month
          AND lastModifiedAt > :syncedBefore
    """)
    suspend fun findDirty(
        siteId      : String,
        year        : Int,
        month       : Int,
        syncedBefore: Long,
    ): List<MealEntryEntity>
}
