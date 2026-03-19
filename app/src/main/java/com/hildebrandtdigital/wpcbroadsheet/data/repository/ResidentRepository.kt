package com.hildebrandtdigital.wpcbroadsheet.data.repository

import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentAuditDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.AuditAction
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentAuditEntity
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentEntity
import com.hildebrandtdigital.wpcbroadsheet.data.model.Resident
import com.hildebrandtdigital.wpcbroadsheet.data.model.ResidentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Single source of truth for resident data.
 *
 * All mutations go through here so the audit trail is always complete.
 * The UI layer should never call DAOs directly.
 *
 * Every write method requires an [actor] — the current user's name or email
 * from [AppSession] — so the audit log always records who made each change.
 *
 * Mapping convention:
 *   DB entity  (ResidentEntity)  ←→  Domain model (Resident)
 *   Long epoch millis            ←→  java.time.Instant
 */
class ResidentRepository(
    private val residentDao  : ResidentDao,
    private val auditDao     : ResidentAuditDao,
) {

    // ── Observe ──────────────────────────────────────────────────────────────

    /**
     * Live stream of active residents for a site.
     * Pass null for [siteId] to observe all sites (Ops Manager view).
     */
    fun observeResidents(siteId: String?): Flow<List<Resident>> =
        residentDao
          .observeResidents(siteId = siteId, includeInactive = 0)
          .map { entities -> entities.map { it.toDomain() } }

    /**
     * Live stream including soft-deleted residents.
     * Useful for the Ops Manager audit / history view.
     */
    fun observeAllResidents(siteId: String?): Flow<List<Resident>> =
        residentDao
          .observeResidents(siteId = siteId, includeInactive = 1)
          .map { entities -> entities.map { it.toDomain() } }

    /**
     * Live audit trail for a single resident, newest event first.
     */
    fun observeAudit(
        siteId    : String,
        unitNumber: String,
    ): Flow<List<ResidentAuditEntity>> =
        auditDao.observeAudit(siteId, unitNumber)

    /**
     * Live stream of all relocation events across all sites.
     */
    fun observeAllRelocations(): Flow<List<ResidentAuditEntity>> =
        auditDao.observeAllRelocations()

    /**
     * Live stream of relocations involving a specific site
     * (as origin or destination).
     */
    fun observeRelocationsForSite(siteId: String): Flow<List<ResidentAuditEntity>> =
        auditDao.observeRelocationsForSite(siteId)

    // ── Mutations ────────────────────────────────────────────────────────────

    /**
     * Add a brand-new resident.
     * Writes the resident row and a CREATED audit entry atomically.
     */
    suspend fun addResident(resident: Resident, actor: String) {
        val now = System.currentTimeMillis()
        residentDao.upsert(resident.toEntity(createdBy = actor, createdAt = now, modifiedBy = actor, modifiedAt = now))
        auditDao.insert(
            ResidentAuditEntity(
                siteId     = resident.siteId,
                unitNumber = resident.unitNumber,
                action     = AuditAction.CREATED.name,
                actor      = actor,
                at         = now,
                note       = "Resident added",
            )
        )
    }

    /**
     * Update an existing resident's details (name, type, occupant count).
     * Does NOT change siteId — use [relocateResident] for that.
     */
    suspend fun updateResident(updated: Resident, actor: String) {
        val now      = System.currentTimeMillis()
        val existing = residentDao.find(updated.siteId, updated.unitNumber)
        residentDao.upsert(
            updated.toEntity(
                createdBy  = existing?.createdBy  ?: actor,
                createdAt  = existing?.createdAt  ?: now,
                modifiedBy = actor,
                modifiedAt = now,
            )
        )
        auditDao.insert(
            ResidentAuditEntity(
                siteId     = updated.siteId,
                unitNumber = updated.unitNumber,
                action     = AuditAction.UPDATED.name,
                actor      = actor,
                at         = now,
            )
        )
    }

    /**
     * Move a resident from one site to another.
     *
     * What happens under the hood:
     * 1. The old (fromSiteId, unitNumber) row is deleted.
     * 2. A new (toSiteId, unitNumber) row is inserted, preserving createdBy/At.
     * 3. A RELOCATED audit entry is written against the FROM site so the
     *    origin site's history records the departure.
     *
     * Past meal entries remain tagged to [fromSiteId] — billing history is
     * unaffected. Future captures and reports will use [toSiteId].
     */
    suspend fun relocateResident(
        resident  : Resident,
        fromSiteId: String,
        toSiteId  : String,
        actor     : String,
        note      : String? = null,
    ) {
        val now      = System.currentTimeMillis()
        val existing = residentDao.find(fromSiteId, resident.unitNumber)

        // 1. Remove the old row
        residentDao.delete(fromSiteId, resident.unitNumber)

        // 2. Insert under the new siteId, preserving original creation metadata
        residentDao.upsert(
            resident.copy(siteId = toSiteId).toEntity(
                createdBy  = existing?.createdBy ?: actor,
                createdAt  = existing?.createdAt ?: now,
                modifiedBy = actor,
                modifiedAt = now,
            )
        )

        // 3. Audit entry against FROM site — fromSiteId/toSiteId are now queryable
        auditDao.insert(
            ResidentAuditEntity(
                siteId     = fromSiteId,
                unitNumber = resident.unitNumber,
                action     = AuditAction.RELOCATED.name,
                actor      = actor,
                at         = now,
                fromSiteId = fromSiteId,
                toSiteId   = toSiteId,
                note       = note,
            )
        )
    }

    /**
     * Soft-deactivate a resident.
     * The row is kept for billing history; isActive = false hides them
     * from capture and reports.
     */
    suspend fun deactivateResident(
        siteId    : String,
        unitNumber: String,
        actor     : String,
        reason    : String? = null,
    ) {
        val now = System.currentTimeMillis()
        residentDao.setActive(
            siteId        = siteId,
            unitNumber    = unitNumber,
            active        = false,
            actor         = actor,
            at            = now,
            deactivatedBy = actor,
            deactivatedAt = now,
        )
        auditDao.insert(
            ResidentAuditEntity(
                siteId     = siteId,
                unitNumber = unitNumber,
                action     = AuditAction.DEACTIVATED.name,
                actor      = actor,
                at         = now,
                note       = reason,
            )
        )
    }

    /**
     * Reactivate a previously deactivated resident.
     */
    suspend fun reactivateResident(
        siteId    : String,
        unitNumber: String,
        actor     : String,
    ) {
        val now = System.currentTimeMillis()
        residentDao.setActive(
            siteId        = siteId,
            unitNumber    = unitNumber,
            active        = true,
            actor         = actor,
            at            = now,
            deactivatedBy = null,
            deactivatedAt = null,
        )
        auditDao.insert(
            ResidentAuditEntity(
                siteId     = siteId,
                unitNumber = unitNumber,
                action     = AuditAction.REACTIVATED.name,
                actor      = actor,
                at         = now,
            )
        )
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun ResidentEntity.toDomain(): Resident = Resident(
        unitNumber     = unitNumber,
        clientName     = clientName,
        totalOccupants = totalOccupants,
        residentType   = residentType,
        siteId         = siteId,
        isActive       = isActive,
        createdBy      = createdBy,
        createdAt      = Instant.ofEpochMilli(createdAt),
        lastModifiedBy = lastModifiedBy,
        lastModifiedAt = Instant.ofEpochMilli(lastModifiedAt),
        deactivatedBy  = deactivatedBy,
        deactivatedAt  = deactivatedAt?.let { Instant.ofEpochMilli(it) },
    )

    /** Count active residents at a specific site — for ProfileScreen stat. */
    suspend fun countForSite(siteId: String): Int = residentDao.countBySite(siteId)

    /** Count all active residents across all sites — for ProfileScreen stat. */
    suspend fun countAll(): Int = residentDao.countAll()

    private fun Resident.toEntity(
        createdBy : String,
        createdAt : Long,
        modifiedBy: String,
        modifiedAt: Long,
    ): ResidentEntity = ResidentEntity(
        siteId         = siteId,
        unitNumber     = unitNumber,
        clientName     = clientName,
        totalOccupants = totalOccupants,
        residentType   = residentType,
        isActive       = isActive,
        createdBy      = createdBy,
        createdAt      = createdAt,
        lastModifiedBy = modifiedBy,
        lastModifiedAt = modifiedAt,
        deactivatedBy  = deactivatedBy,
        deactivatedAt  = deactivatedAt?.toEpochMilli(),
    )
}
