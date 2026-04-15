package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.util.Log
import com.hildebrandtdigital.wpcbroadsheet.data.db.SiteDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.SiteEntity
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiClient
import com.hildebrandtdigital.wpcbroadsheet.data.network.CreateSiteRequest
import com.hildebrandtdigital.wpcbroadsheet.data.network.UpdateSiteRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val TAG = "SiteRepository"

class SiteRepository(private val dao: SiteDao) {

    /** Live list of active sites for dropdowns / dashboards. */
    fun observeActiveSites(): Flow<List<Site>> =
        dao.observeActiveSites().map { list -> list.map { it.toDomain() } }

    suspend fun findById(id: String): Site? = dao.findById(id)?.toDomain()

    /**
     * Refresh sites from the API and cache them in Room.
     * Uses /api/sites/all for cross-site users, /api/sites otherwise.
     * Falls back to the Room cache silently on network failure.
     */
    suspend fun syncFromApi(crossSiteAccess: Boolean) {
        try {
            val response = if (crossSiteAccess) ApiClient.wpcApi.getAllSites()
                           else                  ApiClient.wpcApi.getSites()
            if (response.isSuccessful) {
                val now   = System.currentTimeMillis()
                val sites = response.body() ?: return
                sites.forEach { apiSite ->
                    dao.upsert(
                        SiteEntity(
                            id             = apiSite.id,
                            name           = apiSite.name,
                            isActive       = apiSite.isActive,
                            createdAt      = now,
                            createdBy      = "api",
                            lastModifiedAt = now,
                            lastModifiedBy = "api",
                        )
                    )
                }
                Log.d(TAG, "Synced ${sites.size} sites from API")
            } else {
                Log.w(TAG, "Site sync failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Site sync skipped (offline): ${e.message}")
        }
    }

    /** Create a new site — posts to API first, then caches in Room. */
    suspend fun createSite(name: String, actor: String): Site {
        val now = System.currentTimeMillis()
        try {
            val response = ApiClient.wpcApi.createSite(CreateSiteRequest(name.trim()))
            if (response.isSuccessful) {
                val apiSite = response.body()!!
                val entity  = apiSite.toEntity(now)
                dao.upsert(entity)
                Log.d(TAG, "Created site via API: ${apiSite.id}")
                return entity.toDomain()
            }
            Log.w(TAG, "Create site API failed: HTTP ${response.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "Create site offline fallback: ${e.message}")
        }
        // Offline fallback: generate a local id
        val id = name.trim().lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(20) + "_" + UUID.randomUUID().toString().take(4)
        val entity = SiteEntity(
            id             = id,
            name           = name.trim(),
            isActive       = true,
            createdAt      = now,
            createdBy      = actor,
            lastModifiedAt = now,
            lastModifiedBy = actor,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    /** Rename an existing site. */
    suspend fun renameSite(id: String, newName: String, actor: String) {
        try {
            val response = ApiClient.wpcApi.updateSite(id, UpdateSiteRequest(newName.trim()))
            if (response.isSuccessful) {
                response.body()?.let { dao.upsert(it.toEntity(System.currentTimeMillis())) }
                return
            }
            Log.w(TAG, "Rename site API failed: HTTP ${response.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "Rename site offline fallback: ${e.message}")
        }
        val existing = dao.findById(id) ?: return
        dao.upsert(
            existing.copy(
                name           = newName.trim(),
                lastModifiedAt = System.currentTimeMillis(),
                lastModifiedBy = actor,
            )
        )
    }

    /** Delete a site (API) / soft-deactivate (Room fallback). */
    suspend fun deactivateSite(id: String, actor: String) {
        try {
            val response = ApiClient.wpcApi.deleteSite(id)
            if (response.isSuccessful) {
                dao.deactivate(id, System.currentTimeMillis(), actor)
                return
            }
            Log.w(TAG, "Delete site API failed: HTTP ${response.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "Delete site offline fallback: ${e.message}")
        }
        dao.deactivate(id, System.currentTimeMillis(), actor)
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun SiteEntity.toDomain() = Site(
        id               = id,
        name             = name,
        unitManagerName  = "",   // resolved separately from UserRepository
        unitManagerEmail = "",
    )

    private fun com.hildebrandtdigital.wpcbroadsheet.data.network.ApiSite.toEntity(now: Long) =
        SiteEntity(
            id             = id,
            name           = name,
            isActive       = isActive,
            createdAt      = now,
            createdBy      = "api",
            lastModifiedAt = now,
            lastModifiedBy = "api",
        )
}
