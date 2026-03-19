package com.hildebrandtdigital.wpcbroadsheet.data.repository

import com.hildebrandtdigital.wpcbroadsheet.data.db.SiteDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.SiteEntity
import com.hildebrandtdigital.wpcbroadsheet.data.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SiteRepository(private val dao: SiteDao) {

    /** Live list of active sites for dropdowns / dashboards. */
    fun observeActiveSites(): Flow<List<Site>> =
        dao.observeActiveSites().map { list -> list.map { it.toDomain() } }

    suspend fun findById(id: String): Site? = dao.findById(id)?.toDomain()

    /** Create a new site. Returns the created [Site]. */
    suspend fun createSite(name: String, actor: String): Site {
        val now = System.currentTimeMillis()
        // Derive a URL-safe id from the name, append random suffix to avoid collision
        val id  = name.trim().lowercase()
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
        val existing = dao.findById(id) ?: return
        dao.upsert(
            existing.copy(
                name           = newName.trim(),
                lastModifiedAt = System.currentTimeMillis(),
                lastModifiedBy = actor,
            )
        )
    }

    /** Soft-delete a site. */
    suspend fun deactivateSite(id: String, actor: String) =
        dao.deactivate(id, System.currentTimeMillis(), actor)

    private fun SiteEntity.toDomain() = Site(
        id                 = id,
        name               = name,
        unitManagerName    = "",   // resolved separately from UserRepository
        unitManagerEmail   = "",
    )
}
