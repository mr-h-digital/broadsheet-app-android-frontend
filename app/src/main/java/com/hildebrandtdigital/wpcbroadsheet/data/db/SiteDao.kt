package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    /** Live list of active sites — drives every site-selector dropdown. */
    @Query("SELECT * FROM sites WHERE isActive = 1 ORDER BY name")
    fun observeActiveSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: SiteEntity)

    /** Soft-delete. */
    @Query("UPDATE sites SET isActive = 0, lastModifiedAt = :now, lastModifiedBy = :actor WHERE id = :id")
    suspend fun deactivate(id: String, now: Long, actor: String)
}
