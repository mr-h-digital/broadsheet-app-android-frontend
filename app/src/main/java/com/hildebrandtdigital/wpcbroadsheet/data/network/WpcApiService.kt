package com.hildebrandtdigital.wpcbroadsheet.data.network

import retrofit2.Response
import retrofit2.http.*

// ════════════════════════════════════════════════════════════════════════════
//  Auth DTOs
// ════════════════════════════════════════════════════════════════════════════

data class LoginRequest(
    val email   : String,
    val password: String,   // SHA-256 hex hash
)

data class ApiUser(
    val id       : String,
    val name     : String,
    val email    : String,
    val role     : String,
    val phone    : String,
    val siteId   : String?,
    val avatarUrl: String?,
)

data class LoginResponse(
    val token: String,
    val user : ApiUser,
)

// ════════════════════════════════════════════════════════════════════════════
//  Sites DTOs
// ════════════════════════════════════════════════════════════════════════════

data class ApiSite(
    val id               : String,
    val name             : String,
    val unitManagerName  : String = "",
    val unitManagerEmail : String = "",
    val isActive         : Boolean = true,
)

data class CreateSiteRequest(val name: String)
data class UpdateSiteRequest(val name: String)

// ════════════════════════════════════════════════════════════════════════════
//  Residents DTOs
// ════════════════════════════════════════════════════════════════════════════

data class ApiResident(
    val unitNumber     : String,
    val clientName     : String,
    val totalOccupants : Int,
    val residentType   : String,   // "RENTAL" | "OWNER" | "OTP"
    val siteId         : String,
    val isActive       : Boolean = true,
    val createdBy      : String  = "system",
    val createdAt      : String  = "",
    val lastModifiedBy : String  = "system",
    val lastModifiedAt : String  = "",
    val deactivatedBy  : String? = null,
    val deactivatedAt  : String? = null,
)

data class ResidentRequest(
    val unitNumber     : String,
    val clientName     : String,
    val totalOccupants : Int,
    val residentType   : String,
    val siteId         : String,
)

data class RelocateRequest(val toSiteId: String)

// ════════════════════════════════════════════════════════════════════════════
//  Meal Entries DTOs
// ════════════════════════════════════════════════════════════════════════════

data class ApiMealEntry(
    val unitNumber : String,
    val siteId     : String,
    val year       : Int,
    val month      : Int,
    val day        : Int    = 0,
    val counts     : Map<String, Int> = emptyMap(),   // MealType.name → count
)

// ════════════════════════════════════════════════════════════════════════════
//  Pricing DTOs
// ════════════════════════════════════════════════════════════════════════════

data class ApiMealPricing(
    val siteId                   : String,
    val year                     : Int,
    val month                    : Int,
    val course1                  : Double,
    val course2                  : Double,
    val course3                  : Double,
    val fullBoard                : Double,
    val sun1Course               : Double,
    val sun3Course               : Double,
    val breakfast                : Double,
    val dinner                   : Double,
    val soupDessert              : Double,
    val visitorMonSat            : Double,
    val visitorSun1              : Double,
    val visitorSun3              : Double,
    val taBakkies                : Double,
    val vatRate                  : Double,
    val compulsoryMealsDeduction : Double,
)

// ════════════════════════════════════════════════════════════════════════════
//  Reports DTOs
// ════════════════════════════════════════════════════════════════════════════

data class ApiResidentBilling(
    val unitNumber          : String,
    val clientName          : String,
    val totalMealCounts     : Map<String, Int>,
    val totalMeals          : Int,
    val subtotalExclVat     : Double,
    val vat                 : Double,
    val taBakkiesTotal      : Double,
    val compulsoryDeduction : Double,
    val finalTotal          : Double,
    val isCredit            : Boolean,
)

data class ApiSiteReport(
    val siteId           : String,
    val siteName         : String,
    val year             : Int,
    val month            : Int,
    val residentBillings : List<ApiResidentBilling>,
    val grandTotalMeals  : Int,
    val grandSubtotal    : Double,
    val grandVat         : Double,
    val grandTaBakkies   : Double,
    val grandTotal       : Double,
)

data class ApiConsolidatedReport(
    val year        : Int,
    val month       : Int,
    val siteReports : List<ApiSiteReport>,
    val grandTotal  : Double,
)

// ════════════════════════════════════════════════════════════════════════════
//  API Service
// ════════════════════════════════════════════════════════════════════════════

interface WpcApiService {

    // ── Health ────────────────────────────────────────────────────────────────

    @GET("health")
    suspend fun health(): Response<Unit>

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<ApiUser>

    // ── Sites ─────────────────────────────────────────────────────────────────

    /** Returns sites scoped to the authenticated user's access level. */
    @GET("api/sites")
    suspend fun getSites(): Response<List<ApiSite>>

    /** Returns all sites regardless of role — ADMIN / OPS_MANAGER only. */
    @GET("api/sites/all")
    suspend fun getAllSites(): Response<List<ApiSite>>

    @GET("api/sites/{id}")
    suspend fun getSite(@Path("id") id: String): Response<ApiSite>

    @POST("api/sites")
    suspend fun createSite(@Body request: CreateSiteRequest): Response<ApiSite>

    @PUT("api/sites/{id}")
    suspend fun updateSite(
        @Path("id") id     : String,
        @Body  request     : UpdateSiteRequest,
    ): Response<ApiSite>

    @DELETE("api/sites/{id}")
    suspend fun deleteSite(@Path("id") id: String): Response<Unit>

    // ── Residents ─────────────────────────────────────────────────────────────

    @GET("api/residents")
    suspend fun getResidents(
        @Query("siteId")          siteId         : String?,
        @Query("includeInactive") includeInactive: Boolean = false,
    ): Response<List<ApiResident>>

    @GET("api/residents/{siteId}/{unitNumber}")
    suspend fun getResident(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
    ): Response<ApiResident>

    @GET("api/residents/{siteId}/{unitNumber}/audit")
    suspend fun getResidentAudit(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
    ): Response<List<Map<String, Any>>>

    @POST("api/residents")
    suspend fun createResident(@Body request: ResidentRequest): Response<ApiResident>

    @PUT("api/residents/{siteId}/{unitNumber}")
    suspend fun updateResident(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
        @Body  request                : ResidentRequest,
    ): Response<ApiResident>

    @POST("api/residents/{siteId}/{unitNumber}/deactivate")
    suspend fun deactivateResident(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
    ): Response<Unit>

    @POST("api/residents/{siteId}/{unitNumber}/reactivate")
    suspend fun reactivateResident(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
    ): Response<Unit>

    @POST("api/residents/{siteId}/{unitNumber}/relocate")
    suspend fun relocateResident(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
        @Body  request                : RelocateRequest,
    ): Response<Unit>

    // ── Meal Entries ──────────────────────────────────────────────────────────

    @GET("api/meal-entries/{siteId}/{year}/{month}")
    suspend fun getMealEntries(
        @Path("siteId") siteId: String,
        @Path("year")   year  : Int,
        @Path("month")  month : Int,
    ): Response<List<ApiMealEntry>>

    @GET("api/meal-entries/{siteId}/{unitNumber}/{year}/{month}")
    suspend fun getMealEntry(
        @Path("siteId")     siteId    : String,
        @Path("unitNumber") unitNumber: String,
        @Path("year")       year      : Int,
        @Path("month")      month     : Int,
    ): Response<ApiMealEntry>

    /** Merge/upsert — only supplied counts are updated. */
    @POST("api/meal-entries")
    suspend fun saveMealEntry(@Body entry: ApiMealEntry): Response<ApiMealEntry>

    /** Full replace — overwrites all counts for the entry. */
    @PUT("api/meal-entries")
    suspend fun replaceMealEntry(@Body entry: ApiMealEntry): Response<ApiMealEntry>

    // ── Pricing ───────────────────────────────────────────────────────────────

    @GET("api/pricing/{siteId}/{year}/{month}")
    suspend fun getPricing(
        @Path("siteId") siteId: String,
        @Path("year")   year  : Int,
        @Path("month")  month : Int,
    ): Response<ApiMealPricing>

    @GET("api/pricing/{siteId}/history")
    suspend fun getPricingHistory(
        @Path("siteId") siteId: String,
    ): Response<List<ApiMealPricing>>

    @POST("api/pricing")
    suspend fun savePricing(@Body pricing: ApiMealPricing): Response<ApiMealPricing>

    // ── Reports ───────────────────────────────────────────────────────────────

    @GET("api/reports/{siteId}/{year}/{month}")
    suspend fun getSiteReport(
        @Path("siteId") siteId: String,
        @Path("year")   year  : Int,
        @Path("month")  month : Int,
    ): Response<ApiSiteReport>

    @GET("api/reports/all/{year}/{month}")
    suspend fun getConsolidatedReport(
        @Path("year")  year : Int,
        @Path("month") month: Int,
    ): Response<ApiConsolidatedReport>
}
