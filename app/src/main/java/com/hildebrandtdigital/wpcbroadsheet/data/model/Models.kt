package com.hildebrandtdigital.wpcbroadsheet.data.model

import java.time.Instant

// ── User / Auth ─────────────────────────────────────────────────────────────
enum class UserRole { UNIT_MANAGER, OPERATIONS_MANAGER, ADMIN }

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val phone: String = "",
    val siteId: String? = null  // null means access to all sites (Ops Manager)
)

// ── Site ────────────────────────────────────────────────────────────────────
data class Site(
    val id: String,
    val name: String,
    val unitManagerName: String,
    val unitManagerEmail: String,
)

// ── Resident ─────────────────────────────────────────────────────────────────
enum class ResidentType { RENTAL, OWNER, OTP }

data class Resident(
    val unitNumber     : String,
    val clientName     : String,
    val totalOccupants : Int,
    val residentType   : ResidentType,
    val siteId         : String,

    // ── Lifecycle / audit ─────────────────────────────────────────────────
    // Defaulted so that SampleData and in-memory construction don't need
    // to supply them — the repository fills them properly before persistence.
    val isActive       : Boolean  = true,
    val createdBy      : String   = "system",
    val createdAt      : Instant  = Instant.EPOCH,
    val lastModifiedBy : String   = "system",
    val lastModifiedAt : Instant  = Instant.EPOCH,
    val deactivatedBy  : String?  = null,
    val deactivatedAt  : Instant? = null,
)

// ── Meal Types ───────────────────────────────────────────────────────────────
enum class MealType(val label: String, val shortLabel: String) {
    COURSE_1       ("1 Course Lunch",       "1 Course"),
    COURSE_2       ("2 Course Lunch",       "2 Course"),
    COURSE_3       ("3 Course Lunch",       "3 Course"),
    FULL_BOARD     ("Full Board",           "Full Board"),
    SUN_1_COURSE   ("Sunday 1 Course",      "Sun 1C"),
    SUN_3_COURSE   ("Sunday 3 Course",      "Sun 3C"),
    BREAKFAST      ("Breakfast",            "Breakfast"),
    DINNER         ("Dinner",               "Dinner"),
    SOUP_DESSERT   ("Soup / Dessert",       "Soup/Des"),
    VISITOR_MON_SAT("Visitor Mon–Sat 1C",  "Vis M–S"),
    VISITOR_SUN_1  ("Visitor Sunday 1C",    "Vis Sun 1"),
    VISITOR_SUN_3  ("Visitor Sunday 3C",    "Vis Sun 3"),
    TA_BAKKIES     ("T/A Bakkies",          "T/A Bak"),
}

// ── Meal Pricing ─────────────────────────────────────────────────────────────
// Prices stored EXCLUSIVE of VAT
data class MealPricing(
    val siteId                   : String = "lizane",
    val course1                  : Double = 35.652174,
    val course2                  : Double = 42.608696,
    val course3                  : Double = 54.782609,
    val fullBoard                : Double = 81.739130,
    val sun1Course               : Double = 53.913043,
    val sun3Course               : Double = 68.695652,
    val breakfast                : Double = 23.913043,
    val dinner                   : Double = 27.391304,
    val soupDessert              : Double = 13.043478,
    val visitorMonSat            : Double = 45.217391,
    val visitorSun1              : Double = 63.478261,
    val visitorSun3              : Double = 76.521739,
    val taBakkies                : Double = 5.0,
    val vatRate                  : Double = 0.15,
    val compulsoryMealsDeduction : Double = 246.0,
)

// ── Meal Entry ───────────────────────────────────────────────────────────────
data class MealEntry(
    val unitNumber : String,
    val siteId     : String,
    val year       : Int,
    val month      : Int,
    val day        : Int,
    val counts     : Map<MealType, Int> = emptyMap(),
)

// ── Monthly Summary ───────────────────────────────────────────────────────────
data class ResidentMonthlyBilling(
    val resident            : Resident,
    val totalMealCounts     : Map<MealType, Int>,
    val totalMeals          : Int,
    val subtotalExclVat     : Double,
    val vat                 : Double,
    val taBakkiesTotal      : Double,
    val compulsoryDeduction : Double,
    val finalTotal          : Double,
    val isCredit            : Boolean,
)

// ── Site Monthly Summary ─────────────────────────────────────────────────────
data class SiteMonthlyReport(
    val site              : Site,
    val year              : Int,
    val month             : Int,
    val residentBillings  : List<ResidentMonthlyBilling>,
    val grandTotalMeals   : Int,
    val grandSubtotal     : Double,
    val grandVat          : Double,
    val grandTaBakkies    : Double,
    val grandTotal        : Double,
)
