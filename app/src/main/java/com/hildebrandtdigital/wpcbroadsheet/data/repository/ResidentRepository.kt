package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.util.Log
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentAuditDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.AuditAction
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentAuditEntity
import com.hildebrandtdigital.wpcbroadsheet.data.db.ResidentEntity
import com.hildebrandtdigital.wpcbroadsheet.data.model.Resident
import com.hildebrandtdigital.wpcbroadsheet.data.model.ResidentType
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiClient
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiResident
import com.hildebrandtdigital.wpcbroadsheet.data.network.RelocateRequest
import com.hildebrandtdigital.wpcbroadsheet.data.network.ResidentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private const val TAG = "ResidentRepository"

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

    // ── API Sync ──────────────────────────────────────────────────────────────

    /**
     * Pull residents for a site from the API and write them into Room.
     * Pass null [siteId] to sync all sites (cross-site access).
     */
    suspend fun syncFromApi(siteId: String?, includeInactive: Boolean = false) {
        try {
            val response = ApiClient.wpcApi.getResidents(siteId, includeInactive)
            if (response.isSuccessful) {
                val residents = response.body() ?: return
                val now = System.currentTimeMillis()
                residents.forEach { api ->
                    residentDao.upsert(api.toEntity(now))
                }
                Log.d(TAG, "Synced ${residents.size} residents from API (siteId=$siteId)")
            } else {
                Log.w(TAG, "Resident sync failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Resident sync skipped (offline): ${e.message}")
        }
    }

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
     * Add a brand-new resident — posts to API first, then caches in Room.
     */
    suspend fun addResident(resident: Resident, actor: String) {
        val now = System.currentTimeMillis()
        try {
            val response = ApiClient.wpcApi.createResident(resident.toRequest())
            if (response.isSuccessful) {
                response.body()?.let { residentDao.upsert(it.toEntity(now)) }
                Log.d(TAG, "Created resident via API: ${resident.unitNumber}")
            } else {
                Log.w(TAG, "Create resident API failed: HTTP ${response.code()}")
                residentDao.upsert(resident.toEntity(createdBy = actor, createdAt = now, modifiedBy = actor, modifiedAt = now))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Create resident offline fallback: ${e.message}")
            residentDao.upsert(resident.toEntity(createdBy = actor, createdAt = now, modifiedBy = actor, modifiedAt = now))
        }
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
     * Update an existing resident's details — puts to API first, then updates Room.
     */
    suspend fun updateResident(updated: Resident, actor: String) {
        val now      = System.currentTimeMillis()
        val existing = residentDao.find(updated.siteId, updated.unitNumber)
        try {
            val response = ApiClient.wpcApi.updateResident(
                updated.siteId, updated.unitNumber, updated.toRequest()
            )
            if (response.isSuccessful) {
                response.body()?.let { residentDao.upsert(it.toEntity(now)) }
                Log.d(TAG, "Updated resident via API: ${updated.unitNumber}")
            } else {
                Log.w(TAG, "Update resident API failed: HTTP ${response.code()}")
                residentDao.upsert(updated.toEntity(createdBy = existing?.createdBy ?: actor, createdAt = existing?.createdAt ?: now, modifiedBy = actor, modifiedAt = now))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update resident offline fallback: ${e.message}")
            residentDao.upsert(updated.toEntity(createdBy = existing?.createdBy ?: actor, createdAt = existing?.createdAt ?: now, modifiedBy = actor, modifiedAt = now))
        }
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
        try {
            val response = ApiClient.wpcApi.relocateResident(
                fromSiteId, resident.unitNumber, RelocateRequest(toSiteId)
            )
            if (response.isSuccessful) {
                Log.d(TAG, "Relocated resident via API: ${resident.unitNumber} → $toSiteId")
            } else {
                Log.w(TAG, "Relocate resident API failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Relocate resident offline fallback: ${e.message}")
        }
        residentDao.delete(fromSiteId, resident.unitNumber)
        residentDao.upsert(
            resident.copy(siteId = toSiteId).toEntity(
                createdBy  = existing?.createdBy ?: actor,
                createdAt  = existing?.createdAt ?: now,
                modifiedBy = actor,
                modifiedAt = now,
            )
        )
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
     */
    suspend fun deactivateResident(
        siteId    : String,
        unitNumber: String,
        actor     : String,
        reason    : String? = null,
    ) {
        val now = System.currentTimeMillis()
        try {
            val response = ApiClient.wpcApi.deactivateResident(siteId, unitNumber)
            if (!response.isSuccessful) Log.w(TAG, "Deactivate resident API failed: HTTP ${response.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "Deactivate resident offline fallback: ${e.message}")
        }
        residentDao.setActive(siteId = siteId, unitNumber = unitNumber, active = false, actor = actor, at = now, deactivatedBy = actor, deactivatedAt = now)
        auditDao.insert(ResidentAuditEntity(siteId = siteId, unitNumber = unitNumber, action = AuditAction.DEACTIVATED.name, actor = actor, at = now, note = reason))
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
        try {
            val response = ApiClient.wpcApi.reactivateResident(siteId, unitNumber)
            if (!response.isSuccessful) Log.w(TAG, "Reactivate resident API failed: HTTP ${response.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "Reactivate resident offline fallback: ${e.message}")
        }
        residentDao.setActive(siteId = siteId, unitNumber = unitNumber, active = true, actor = actor, at = now, deactivatedBy = null, deactivatedAt = null)
        auditDao.insert(ResidentAuditEntity(siteId = siteId, unitNumber = unitNumber, action = AuditAction.REACTIVATED.name, actor = actor, at = now))
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun Resident.toRequest() = ResidentRequest(
        unitNumber     = unitNumber,
        clientName     = clientName,
        totalOccupants = totalOccupants,
        residentType   = residentType.name,
        siteId         = siteId,
    )

    private fun ApiResident.toEntity(now: Long) = ResidentEntity(
        siteId         = siteId,
        unitNumber     = unitNumber,
        clientName     = clientName,
        totalOccupants = totalOccupants,
        residentType   = ResidentType.valueOf(residentType),
        isActive       = isActive,
        createdBy      = createdBy,
        createdAt      = if (createdAt.isNotBlank()) runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(now) else now,
        lastModifiedBy = lastModifiedBy,
        lastModifiedAt = if (lastModifiedAt.isNotBlank()) runCatching { Instant.parse(lastModifiedAt).toEpochMilli() }.getOrDefault(now) else now,
        deactivatedBy  = deactivatedBy,
        deactivatedAt  = deactivatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
    )

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
