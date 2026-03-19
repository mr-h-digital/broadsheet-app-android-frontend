package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentAuditDao {

    /**
     * Full event history for a specific resident, newest first.
     * Used in the audit trail UI.
     */
    @Query("""
        SELECT * FROM resident_audit
        WHERE siteId = :siteId AND unitNumber = :unitNumber
        ORDER BY at DESC
    """)
    fun observeAudit(siteId: String, unitNumber: String): Flow<List<ResidentAuditEntity>>

    /**
     * All relocation events across all sites — lets the Ops Manager see
     * the full movement history of residents between villages.
     * [fromSiteId] and [toSiteId] are guaranteed non-null for RELOCATED rows.
     */
    @Query("""
        SELECT * FROM resident_audit
        WHERE action = 'RELOCATED'
        ORDER BY at DESC
    """)
    fun observeAllRelocations(): Flow<List<ResidentAuditEntity>>

    /**
     * Relocation events involving a specific site (either as origin or destination).
     * Useful for a site's own movement report.
     */
    @Query("""
        SELECT * FROM resident_audit
        WHERE action = 'RELOCATED'
          AND (fromSiteId = :siteId OR toSiteId = :siteId)
        ORDER BY at DESC
    """)
    fun observeRelocationsForSite(siteId: String): Flow<List<ResidentAuditEntity>>

    /**
     * All audit events across an entire site — useful for Ops Manager overview.
     */
    @Query("""
        SELECT * FROM resident_audit
        WHERE siteId = :siteId
        ORDER BY at DESC
    """)
    fun observeAuditForSite(siteId: String): Flow<List<ResidentAuditEntity>>

    /** Append a new audit event. Never update existing rows. */
    @Insert
    suspend fun insert(event: ResidentAuditEntity)
}
