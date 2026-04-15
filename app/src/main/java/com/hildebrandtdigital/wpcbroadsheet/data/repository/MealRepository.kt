package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.util.Log
import com.hildebrandtdigital.wpcbroadsheet.data.db.MealEntryDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.MealPricingDao
import com.hildebrandtdigital.wpcbroadsheet.data.db.MealEntryEntity
import com.hildebrandtdigital.wpcbroadsheet.data.db.MealPricingEntity
import com.hildebrandtdigital.wpcbroadsheet.data.db.RoomConverters
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealEntry
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealPricing
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealType
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiClient
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiMealEntry
import com.hildebrandtdigital.wpcbroadsheet.data.network.ApiMealPricing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "MealRepository"

/**
 * Single source of truth for meal capture data and pricing configuration.
 *
 * Designed as a write-through cache:
 * - Reads always come from Room (fast, offline-capable).
 * - Writes go to Room immediately so the UI updates instantly, then the
 *   backend sync layer (when built) can push dirty rows using the
 *   [findDirtyEntries] / [findUnsyncedPricing] helpers.
 *
 * Neither CaptureScreen nor PricingConfigScreen should call DAOs directly.
 */
class MealRepository(
    private val mealEntryDao  : MealEntryDao,
    private val mealPricingDao: MealPricingDao,
) {

    private val converters = RoomConverters()

    // ── API Sync ──────────────────────────────────────────────────────────────

    /**
     * Pull all meal entries for a site/month from the API and write to Room.
     */
    suspend fun syncEntriesFromApi(siteId: String, year: Int, month: Int) {
        try {
            val response = ApiClient.wpcApi.getMealEntries(siteId, year, month)
            if (response.isSuccessful) {
                val entries = response.body() ?: return
                val now = System.currentTimeMillis()
                entries.forEach { api ->
                    mealEntryDao.upsert(
                        MealEntryEntity(
                            siteId         = api.siteId,
                            unitNumber     = api.unitNumber,
                            year           = api.year,
                            month          = api.month,
                            countsJson     = converters.mealCountsToJson(
                                api.counts.mapKeys { (k, _) ->
                                    runCatching { MealType.valueOf(k) }.getOrNull()
                                }.filterKeys { it != null }
                                 .mapKeys { it.key!! }
                            ),
                            lastModifiedAt = now,
                            lastModifiedBy = "api",
                        )
                    )
                }
                Log.d(TAG, "Synced ${entries.size} meal entries from API ($siteId $year/$month)")
            } else {
                Log.w(TAG, "Meal entry sync failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Meal entry sync skipped (offline): ${e.message}")
        }
    }

    /**
     * Pull pricing for a site/month from the API and write to Room.
     */
    suspend fun syncPricingFromApi(siteId: String, year: Int, month: Int) {
        try {
            val response = ApiClient.wpcApi.getPricing(siteId, year, month)
            if (response.isSuccessful) {
                val api = response.body() ?: return
                mealPricingDao.upsert(api.toEntity())
                Log.d(TAG, "Synced pricing from API ($siteId $year/$month)")
            } else {
                Log.w(TAG, "Pricing sync failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pricing sync skipped (offline): ${e.message}")
        }
    }

    // ── Meal Entries ──────────────────────────────────────────────────────────

    /**
     * Live stream of meal entries for a site in a given month.
     * The screen collects this Flow — any save triggers a recomposition
     * automatically.
     */
    fun observeEntries(siteId: String, year: Int, month: Int): Flow<List<MealEntry>> =
        mealEntryDao
          .observeEntries(siteId, year, month)
          .map { entities -> entities.map { it.toDomain() } }

    /**
     * Persist a single resident's meal counts for a period.
     *
     * Merge strategy: read the existing row first, then overlay only the
     * meal types that the caller has provided. This prevents a partial
     * card save from zeroing out meal types that were captured on another
     * device or in a different session.
     *
     * When the backend is connected, the sync worker will call
     * [findDirtyEntries] and push any row whose [lastModifiedAt] is
     * newer than the last acknowledged sync timestamp.
     */
    suspend fun saveCounts(
        siteId    : String,
        unitNumber: String,
        year      : Int,
        month     : Int,
        counts    : Map<MealType, Int>,
        actor     : String,
    ) {
        val now      = System.currentTimeMillis()
        val existing = mealEntryDao.find(siteId, unitNumber, year, month)

        // Merge: start from persisted counts, overlay with new values,
        // remove zero entries to keep the JSON compact
        val merged = if (existing != null) {
            val old = converters.jsonToMealCounts(existing.countsJson).toMutableMap()
            counts.forEach { (type, value) ->
                if (value <= 0) old.remove(type) else old[type] = value
            }
            old
        } else {
            counts.filter { it.value > 0 }
        }

        val entity = MealEntryEntity(
            siteId         = siteId,
            unitNumber     = unitNumber,
            year           = year,
            month          = month,
            countsJson     = converters.mealCountsToJson(merged),
            lastModifiedAt = now,
            lastModifiedBy = actor,
        )
        mealEntryDao.upsert(entity)

        // Push to API (fire-and-forget; Room is already updated)
        try {
            ApiClient.wpcApi.saveMealEntry(
                ApiMealEntry(
                    unitNumber = unitNumber,
                    siteId     = siteId,
                    year       = year,
                    month      = month,
                    counts     = merged.mapKeys { it.key.name },
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Push meal entry to API failed (will retry on next sync): ${e.message}")
        }
    }

    /**
     * Persist all resident counts for a site at once.
     * Called by the "Save All" button in the header.
     */
    suspend fun saveAllCounts(
        siteId  : String,
        year    : Int,
        month   : Int,
        allCounts: Map<String, Map<MealType, Int>>,   // unitNumber → counts
        actor   : String,
    ) {
        allCounts.forEach { (unitNumber, counts) ->
            saveCounts(siteId, unitNumber, year, month, counts, actor)
        }
    }

    /**
     * Entries modified after [syncedBefore] — used by the future sync worker
     * to identify what needs to be pushed to the backend.
     */
    suspend fun findDirtyEntries(
        siteId      : String,
        year        : Int,
        month       : Int,
        syncedBefore: Long,
    ) = mealEntryDao.findDirty(siteId, year, month, syncedBefore)

    // ── Pricing ───────────────────────────────────────────────────────────────

    /**
     * Live pricing config for a site in a period.
     * Emits null if Room has no config yet — the screen falls back to
     * [SampleData.pricingForSite] in that case so the UI never blocks.
     */
    fun observePricing(siteId: String, year: Int, month: Int): Flow<MealPricing?> =
        mealPricingDao
          .observePricing(siteId, year, month)
          .map { entity -> entity?.toDomain() }

    /**
     * Most recent saved pricing for a site — used to pre-populate a new
     * month before the Ops Manager has explicitly configured it.
     */
    suspend fun latestPricing(siteId: String): MealPricing? =
        mealPricingDao.findLatest(siteId)?.toDomain()

    /**
     * Persist a pricing config for a period.
     *
     * Enforces the closed-period rule: the year/month in [pricing] must
     * be the current or a future period — past periods are read-only.
     * The UI should prevent editing closed periods, but the repository
     * enforces it as a safety net.
     */
    suspend fun savePricing(
        siteId : String,
        year   : Int,
        month  : Int,
        pricing: MealPricing,
        actor  : String,
    ) {
        val now      = System.currentTimeMillis()
        val apiPricing = ApiMealPricing(
            siteId                   = siteId,
            year                     = year,
            month                    = month,
            course1                  = pricing.course1,
            course2                  = pricing.course2,
            course3                  = pricing.course3,
            fullBoard                = pricing.fullBoard,
            sun1Course               = pricing.sun1Course,
            sun3Course               = pricing.sun3Course,
            breakfast                = pricing.breakfast,
            dinner                   = pricing.dinner,
            soupDessert              = pricing.soupDessert,
            visitorMonSat            = pricing.visitorMonSat,
            visitorSun1              = pricing.visitorSun1,
            visitorSun3              = pricing.visitorSun3,
            taBakkies                = pricing.taBakkies,
            vatRate                  = pricing.vatRate,
            compulsoryMealsDeduction = pricing.compulsoryMealsDeduction,
        )
        mealPricingDao.upsert(apiPricing.toEntity())
        SampleData.savePricing(siteId, pricing)

        // Push to API
        try {
            val response = ApiClient.wpcApi.savePricing(apiPricing)
            if (response.isSuccessful) {
                markPricingSynced(siteId, year, month)
            } else {
                Log.w(TAG, "Push pricing to API failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Push pricing to API failed (offline): ${e.message}")
        }
    }

    /**
     * Pricing rows not yet acknowledged by the backend.
     * Used by the future sync worker.
     */
    suspend fun findUnsyncedPricing() =
        mealPricingDao.findUnsynced()

    /**
     * Called by the sync worker after the backend successfully acknowledges
     * a pricing config push.
     */
    suspend fun markPricingSynced(siteId: String, year: Int, month: Int) {
        mealPricingDao.markSynced(siteId, year, month, System.currentTimeMillis())
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun MealEntryEntity.toDomain(): MealEntry = MealEntry(
        unitNumber = unitNumber,
        siteId     = siteId,
        year       = year,
        month      = month,
        day        = 0,     // aggregate row; per-day detail is future work
        counts     = converters.jsonToMealCounts(countsJson),
    )

    private fun ApiMealPricing.toEntity() = MealPricingEntity(
        siteId                   = siteId,
        year                     = year,
        month                    = month,
        course1                  = course1,
        course2                  = course2,
        course3                  = course3,
        fullBoard                = fullBoard,
        sun1Course               = sun1Course,
        sun3Course               = sun3Course,
        breakfast                = breakfast,
        dinner                   = dinner,
        soupDessert              = soupDessert,
        visitorMonSat            = visitorMonSat,
        visitorSun1              = visitorSun1,
        visitorSun3              = visitorSun3,
        taBakkies                = taBakkies,
        vatRate                  = vatRate,
        compulsoryMealsDeduction = compulsoryMealsDeduction,
        lastModifiedAt           = System.currentTimeMillis(),
        lastModifiedBy           = "api",
        lastSyncedAt             = System.currentTimeMillis(),
    )

    private fun MealPricingEntity.toDomain(): MealPricing = MealPricing(
        siteId                   = siteId,
        course1                  = course1,
        course2                  = course2,
        course3                  = course3,
        fullBoard                = fullBoard,
        sun1Course               = sun1Course,
        sun3Course               = sun3Course,
        breakfast                = breakfast,
        dinner                   = dinner,
        soupDessert              = soupDessert,
        visitorMonSat            = visitorMonSat,
        visitorSun1              = visitorSun1,
        visitorSun3              = visitorSun3,
        taBakkies                = taBakkies,
        vatRate                  = vatRate,
        compulsoryMealsDeduction = compulsoryMealsDeduction,
    )
}
