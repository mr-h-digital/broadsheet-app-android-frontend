package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentDao {

    /**
     * Live list of residents for a site (or all sites when siteId is null).
     * Pass includeInactive = 1 to include soft-deleted residents.
     */
    @Query("""
        SELECT * FROM residents
        WHERE (:siteId IS NULL OR siteId = :siteId)
          AND (:includeInactive = 1 OR isActive = 1)
        ORDER BY unitNumber
    """)
    fun observeResidents(siteId: String?, includeInactive: Int = 0): Flow<List<ResidentEntity>>

    /** One-shot list of active residents for a site — used by the notification worker. */
    @Query("SELECT * FROM residents WHERE siteId = :siteId AND isActive = 1")
    suspend fun getForSite(siteId: String): List<ResidentEntity>

    /** Count active residents for a specific site — used by ProfileScreen stat. */
    @Query("SELECT COUNT(*) FROM residents WHERE siteId = :siteId AND isActive = 1")
    suspend fun countBySite(siteId: String): Int

    /** Count all active residents across all sites. */
    @Query("SELECT COUNT(*) FROM residents WHERE isActive = 1")
    suspend fun countAll(): Int

    /**
     * Single resident lookup — used to verify existence before relocation.
     */
    @Query("""
        SELECT * FROM residents
        WHERE siteId = :siteId AND unitNumber = :unitNumber
        LIMIT 1
    """)
    suspend fun find(siteId: String, unitNumber: String): ResidentEntity?

    /**
     * Insert or replace a resident row.
     * Used for both creates and updates — the composite PK (siteId, unitNumber)
     * ensures the correct row is targeted.
     * For relocation: the old row is deleted first via [delete], then this
     * inserts the new row with the updated siteId.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resident: ResidentEntity)

    /**
     * Hard-delete a specific resident row.
     * Only called during relocation to remove the old (siteId, unitNumber) row
     * before inserting the new one — not for user-initiated deactivation.
     */
    @Query("""
        DELETE FROM residents
        WHERE siteId = :siteId AND unitNumber = :unitNumber
    """)
    suspend fun delete(siteId: String, unitNumber: String)

    /**
     * Soft-deactivate or reactivate a resident in-place.
     * Billing history depends on this row — never hard-delete for this purpose.
     */
    @Query("""
        UPDATE residents
        SET isActive       = :active,
            lastModifiedBy = :actor,
            lastModifiedAt = :at,
            deactivatedBy  = :deactivatedBy,
            deactivatedAt  = :deactivatedAt
        WHERE siteId = :siteId AND unitNumber = :unitNumber
    """)
    suspend fun setActive(
        siteId        : String,
        unitNumber    : String,
        active        : Boolean,
        actor         : String,
        at            : Long,
        deactivatedBy : String?,
        deactivatedAt : Long?,
    )
}
