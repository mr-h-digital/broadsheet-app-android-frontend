package com.hildebrandtdigital.wpcbroadsheet.data.repository

import com.hildebrandtdigital.wpcbroadsheet.data.model.*
import kotlin.math.max

/**
 * Pure calculation engine — mirrors the Excel broadsheet formulas exactly.
 *
 * Logic:
 *  1. Sum monthly meal counts per meal type for each resident
 *  2. Calculate subtotal excl. VAT using per-meal prices
 *  3. Add VAT (15%)
 *  4. Add T/A Bakkies (flat R5/bakkie incl. VAT)
 *  5. Subtract 6 compulsory meals deduction (R246 fixed)
 *  6. Final = subtotal incl. VAT + bakkies − deduction
 *     (can be negative = credit for low-meal residents)
 */
object BillingCalculator {

    fun calculateResidentBilling(
        resident: Resident,
        entries: List<MealEntry>,
        pricing: MealPricing = MealPricing(),
    ): ResidentMonthlyBilling {

        // 1. Aggregate daily entries into monthly totals per meal type
        val totalCounts = mutableMapOf<MealType, Int>()
        entries
            .filter { it.unitNumber == resident.unitNumber && it.siteId == resident.siteId }
            .forEach { entry ->
                entry.counts.forEach { (type, count) ->
                    totalCounts[type] = (totalCounts[type] ?: 0) + count
                }
            }

        // 2. Subtotal excl. VAT
        val subtotalExclVat = totalCounts.entries.sumOf { (type, count) ->
            when (type) {
                MealType.COURSE_1       -> pricing.course1       * count
                MealType.COURSE_2       -> pricing.course2       * count
                MealType.COURSE_3       -> pricing.course3       * count
                MealType.FULL_BOARD     -> pricing.fullBoard     * count
                MealType.SUN_1_COURSE   -> pricing.sun1Course    * count
                MealType.SUN_3_COURSE   -> pricing.sun3Course    * count
                MealType.BREAKFAST      -> pricing.breakfast     * count
                MealType.DINNER         -> pricing.dinner        * count
                MealType.SOUP_DESSERT   -> pricing.soupDessert  * count
                MealType.VISITOR_MON_SAT-> pricing.visitorMonSat * count
                MealType.VISITOR_SUN_1  -> pricing.visitorSun1  * count
                MealType.VISITOR_SUN_3  -> pricing.visitorSun3  * count
                MealType.TA_BAKKIES     -> 0.0  // Bakkies handled separately
            }
        }

        // 3. VAT on meals only (not on bakkies)
        val vat = subtotalExclVat * pricing.vatRate

        // 4. T/A Bakkies (already incl. VAT in the R5 rate)
        val bakkiesCount = totalCounts[MealType.TA_BAKKIES] ?: 0
        val taBakkiesTotal = pricing.taBakkies * bakkiesCount

        // 5. Total meals (excluding bakkies from count)
        val totalMeals = totalCounts
            .filter { it.key != MealType.TA_BAKKIES }
            .values.sum()

        // 6. Compulsory deduction (all residents get −R246, even owners with 0 meals)
        val deduction = pricing.compulsoryMealsDeduction

        // 7. Final total (can be negative)
        val finalTotal = subtotalExclVat + vat + taBakkiesTotal - deduction

        return ResidentMonthlyBilling(
            resident            = resident,
            totalMealCounts     = totalCounts,
            totalMeals          = totalMeals,
            subtotalExclVat     = subtotalExclVat,
            vat                 = vat,
            taBakkiesTotal      = taBakkiesTotal,
            compulsoryDeduction = deduction,
            finalTotal          = finalTotal,
            isCredit            = finalTotal < 0,
        )
    }

    fun calculateSiteReport(
        site: Site,
        residents: List<Resident>,
        entries: List<MealEntry>,
        year: Int,
        month: Int,
        pricing: MealPricing = MealPricing(),
    ): SiteMonthlyReport {
        val billings = residents.map { resident ->
            calculateResidentBilling(resident, entries, pricing)
        }

        return SiteMonthlyReport(
            site             = site,
            year             = year,
            month            = month,
            residentBillings = billings,
            grandTotalMeals  = billings.sumOf { it.totalMeals },
            grandSubtotal    = billings.sumOf { it.subtotalExclVat },
            grandVat         = billings.sumOf { it.vat },
            grandTaBakkies   = billings.sumOf { it.taBakkiesTotal },
            grandTotal       = billings.sumOf { it.finalTotal },
        )
    }

    /** Quick preview for the in-card billing display during capture */
    fun previewBilling(
        counts: Map<MealType, Int>,
        pricing: MealPricing = MealPricing(),
    ): Triple<Double, Double, Double> {  // subtotalExclVat, vat, finalTotal
        val subtotal = counts.entries.sumOf { (type, count) ->
            when (type) {
                MealType.COURSE_1        -> pricing.course1       * count
                MealType.COURSE_2        -> pricing.course2       * count
                MealType.COURSE_3        -> pricing.course3       * count
                MealType.FULL_BOARD      -> pricing.fullBoard     * count
                MealType.SUN_1_COURSE    -> pricing.sun1Course    * count
                MealType.SUN_3_COURSE    -> pricing.sun3Course    * count
                MealType.BREAKFAST       -> pricing.breakfast     * count
                MealType.DINNER          -> pricing.dinner        * count
                MealType.SOUP_DESSERT    -> pricing.soupDessert  * count
                MealType.VISITOR_MON_SAT -> pricing.visitorMonSat * count
                MealType.VISITOR_SUN_1   -> pricing.visitorSun1  * count
                MealType.VISITOR_SUN_3   -> pricing.visitorSun3  * count
                MealType.TA_BAKKIES      -> 0.0
            }
        }
        val vat = subtotal * pricing.vatRate
        val bakkies = (counts[MealType.TA_BAKKIES] ?: 0) * pricing.taBakkies
        val finalTotal = subtotal + vat + bakkies - pricing.compulsoryMealsDeduction
        return Triple(subtotal, vat, finalTotal)
    }

    /** Format a Rand amount for display */
    fun formatRand(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val formatted = "R%.2f".format(abs)
        return if (amount < 0) "−$formatted" else formatted
    }

    /** Format month number to name */
    fun monthName(month: Int): String = when (month) {
        1  -> "January";  2  -> "February"; 3  -> "March"
        4  -> "April";    5  -> "May";       6  -> "June"
        7  -> "July";     8  -> "August";    9  -> "September"
        10 -> "October";  11 -> "November";  12 -> "December"
        else -> "Unknown"
    }
}
