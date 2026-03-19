package com.hildebrandtdigital.wpcbroadsheet.data.repository

import com.hildebrandtdigital.wpcbroadsheet.data.model.*
/**
 * Sample data pre-seeded from the real Broadsheet_January_2026.xlsx
 * Used as initial app data until a backend / Room DB is connected.
 */
object SampleData {

    val sites = listOf(
        Site("lizane",  "Lizane Village",     "Megan van Rooyen", "vanrooyen@wpc.co.za"),
        Site("bakkies", "Bakkies Estate",     "Martin Nothnagel", "nothnagel@wpc.co.za"),
        Site("sunhill", "Sunhill Retirement", "Melissa Matheson", "matheson@wpc.co.za"),
    )

    val users = listOf(
        User("u1", "Chernay Hildebrandt", "chernay@wpc.co.za",   UserRole.OPERATIONS_MANAGER, phone = "+27 82 555 0100"),
        User("u2", "Megan van Rooyen",    "vanrooyen@wpc.co.za", UserRole.UNIT_MANAGER, phone = "+27 72 555 0200", siteId = "lizane"),
        User("u3", "Martin Nothnagel",    "nothnagel@wpc.co.za", UserRole.UNIT_MANAGER, phone = "+27 72 555 0300", siteId = "bakkies"),
    )

    // ── Per-site pricing configs ───────────────────────────────────────────────
    // Prices excl. VAT. Each site has its own menu and can charge differently.
    // VAT (15%) and compulsory deduction (R246) are regulatory and stay the same.
    val sitePricingConfigs = mutableMapOf(
        // Lizane Village — full menu, premium pricing
        "lizane" to MealPricing(
            siteId        = "lizane",
            course1       = 35.652174,
            course2       = 42.608696,
            course3       = 54.782609,
            fullBoard     = 81.739130,
            sun1Course    = 53.913043,
            sun3Course    = 68.695652,
            breakfast     = 23.913043,
            dinner        = 27.391304,
            soupDessert   = 13.043478,
            visitorMonSat = 45.217391,
            visitorSun1   = 63.478261,
            visitorSun3   = 76.521739,
            taBakkies     = 5.0,
            vatRate       = 0.15,
            compulsoryMealsDeduction = 246.0,
        ),
        // Bakkies Estate — simpler menu, slightly lower pricing
        "bakkies" to MealPricing(
            siteId        = "bakkies",
            course1       = 32.173913,
            course2       = 39.130435,
            course3       = 50.434783,
            fullBoard     = 75.652174,
            sun1Course    = 49.565217,
            sun3Course    = 63.478261,
            breakfast     = 21.739130,
            dinner        = 25.217391,
            soupDessert   = 11.739130,
            visitorMonSat = 41.304348,
            visitorSun1   = 58.260870,
            visitorSun3   = 70.434783,
            taBakkies     = 5.0,
            vatRate       = 0.15,
            compulsoryMealsDeduction = 246.0,
        ),
        // Sunhill Retirement — extended menu with higher-end pricing
        "sunhill" to MealPricing(
            siteId        = "sunhill",
            course1       = 38.260870,
            course2       = 46.086957,
            course3       = 59.130435,
            fullBoard     = 87.826087,
            sun1Course    = 57.391304,
            sun3Course    = 73.913043,
            breakfast     = 26.086957,
            dinner        = 30.434783,
            soupDessert   = 14.347826,
            visitorMonSat = 48.695652,
            visitorSun1   = 68.260870,
            visitorSun3   = 82.608696,
            taBakkies     = 5.0,
            vatRate       = 0.15,
            compulsoryMealsDeduction = 246.0,
        ),
    )

    /** Returns the pricing config for a site, falling back to Lizane defaults */
    fun pricingForSite(siteId: String): MealPricing =
        sitePricingConfigs[siteId] ?: sitePricingConfigs["lizane"]!!

    /** Persists an updated pricing config for a site */
    fun savePricing(siteId: String, pricing: MealPricing) {
        sitePricingConfigs[siteId] = pricing
    }

    val residents = listOf(
        // ── Lizane Village (23 residents) ─────────────────────────────────────
        Resident("001", "Me van Rooyen",              1, ResidentType.RENTAL, "lizane"),
        Resident("002", "Mev Pestana",                1, ResidentType.OWNER,  "lizane"),
        Resident("003", "Mnr & Me Nothnagel",         2, ResidentType.OWNER,  "lizane"),
        Resident("004", "Mrs Bloomfield",             1, ResidentType.OWNER,  "lizane"),
        Resident("005", "Gillen",                     1, ResidentType.RENTAL, "lizane"),
        Resident("006", "Mnr & Me Swanepoel",         2, ResidentType.OWNER,  "lizane"),
        Resident("007", "Me Fouche",                  1, ResidentType.RENTAL, "lizane"),
        Resident("008", "Mr & Mrs Bothe",             2, ResidentType.OWNER,  "lizane"),
        Resident("009", "Mr & Mev VD Banks",          2, ResidentType.RENTAL, "lizane"),
        Resident("010", "Mrs Mason",                  1, ResidentType.OWNER,  "lizane"),
        Resident("011", "Mrs Matheson",               1, ResidentType.RENTAL, "lizane"),
        Resident("012", "Mrs Retief",                 2, ResidentType.OWNER,  "lizane"),
        Resident("013", "Mnr Welgemoed",              1, ResidentType.OWNER,  "lizane"),
        Resident("014", "Me Klein (Brandt)",          1, ResidentType.OWNER,  "lizane"),
        Resident("015", "Mr & Mrs Calitz",            2, ResidentType.RENTAL, "lizane"),
        Resident("016", "Me Heine",                   1, ResidentType.OWNER,  "lizane"),
        Resident("017", "Me Smith",                   1, ResidentType.OTP,    "lizane"),
        Resident("018", "Mnr & Mev Wolhuter",         1, ResidentType.RENTAL, "lizane"),
        Resident("019", "Me Beylefeldt",              1, ResidentType.RENTAL, "lizane"),
        Resident("021", "Me Banister",                1, ResidentType.OWNER,  "lizane"),
        Resident("022", "Ay Mcpetrie",                1, ResidentType.OWNER,  "lizane"),
        Resident("023", "Mrs Edwards",                1, ResidentType.OWNER,  "lizane"),
        Resident("024", "Mnr J Nothnagel",            1, ResidentType.RENTAL, "lizane"),

        // ── Bakkies Estate (18 residents) ─────────────────────────────────────
        Resident("B01", "Mr & Mrs Kruger",            2, ResidentType.OWNER,  "bakkies"),
        Resident("B02", "Me Joubert",                 1, ResidentType.RENTAL, "bakkies"),
        Resident("B03", "Mnr & Mev Fourie",           2, ResidentType.OWNER,  "bakkies"),
        Resident("B04", "Mrs Pretorius",              1, ResidentType.OWNER,  "bakkies"),
        Resident("B05", "Me du Plessis",              1, ResidentType.RENTAL, "bakkies"),
        Resident("B06", "Mnr & Me Steyn",             2, ResidentType.OWNER,  "bakkies"),
        Resident("B07", "Mrs Lombard",                1, ResidentType.RENTAL, "bakkies"),
        Resident("B08", "Mr & Mrs Venter",            2, ResidentType.OWNER,  "bakkies"),
        Resident("B09", "Mev Coetzee",                1, ResidentType.OWNER,  "bakkies"),
        Resident("B10", "Mnr & Mev Nel",              2, ResidentType.RENTAL, "bakkies"),
        Resident("B11", "Me van der Merwe",           1, ResidentType.OWNER,  "bakkies"),
        Resident("B12", "Mrs Bosman",                 1, ResidentType.RENTAL, "bakkies"),
        Resident("B13", "Mnr & Me Potgieter",         2, ResidentType.OWNER,  "bakkies"),
        Resident("B14", "Me Louw",                    1, ResidentType.OTP,    "bakkies"),
        Resident("B15", "Mr & Mrs Erasmus",           2, ResidentType.OWNER,  "bakkies"),
        Resident("B16", "Mev Visser",                 1, ResidentType.RENTAL, "bakkies"),
        Resident("B17", "Me Theron",                  1, ResidentType.OWNER,  "bakkies"),
        Resident("B18", "Mnr & Mev Grobler",          2, ResidentType.OWNER,  "bakkies"),

        // ── Sunhill Retirement (31 residents) ─────────────────────────────────
        Resident("S01", "Mr & Mrs Olivier",           2, ResidentType.OWNER,  "sunhill"),
        Resident("S02", "Me Pietersen",               1, ResidentType.RENTAL, "sunhill"),
        Resident("S03", "Mnr & Mev Kotze",            2, ResidentType.OWNER,  "sunhill"),
        Resident("S04", "Mrs van Wyk",                1, ResidentType.OWNER,  "sunhill"),
        Resident("S05", "Me Boshoff",                 1, ResidentType.RENTAL, "sunhill"),
        Resident("S06", "Mnr & Me Swart",             2, ResidentType.OWNER,  "sunhill"),
        Resident("S07", "Mrs le Roux",                1, ResidentType.RENTAL, "sunhill"),
        Resident("S08", "Mr & Mrs Human",             2, ResidentType.OWNER,  "sunhill"),
        Resident("S09", "Mev Bekker",                 1, ResidentType.OWNER,  "sunhill"),
        Resident("S10", "Mnr & Mev Burger",           2, ResidentType.RENTAL, "sunhill"),
        Resident("S11", "Me de Beer",                 1, ResidentType.OWNER,  "sunhill"),
        Resident("S12", "Mrs Brits",                  1, ResidentType.RENTAL, "sunhill"),
        Resident("S13", "Mnr & Me Marais",            2, ResidentType.OWNER,  "sunhill"),
        Resident("S14", "Me Jansen",                  1, ResidentType.OTP,    "sunhill"),
        Resident("S15", "Mr & Mrs Pienaar",           2, ResidentType.OWNER,  "sunhill"),
        Resident("S16", "Mev Schoeman",               1, ResidentType.RENTAL, "sunhill"),
        Resident("S17", "Me Hugo",                    1, ResidentType.OWNER,  "sunhill"),
        Resident("S18", "Mnr & Mev Barnard",          2, ResidentType.OWNER,  "sunhill"),
        Resident("S19", "Mrs Nortje",                 1, ResidentType.RENTAL, "sunhill"),
        Resident("S20", "Me Cronje",                  1, ResidentType.OWNER,  "sunhill"),
        Resident("S21", "Mnr & Mev Smit",             2, ResidentType.OWNER,  "sunhill"),
        Resident("S22", "Me van Niekerk",             1, ResidentType.RENTAL, "sunhill"),
        Resident("S23", "Mrs Geldenhuys",             1, ResidentType.OWNER,  "sunhill"),
        Resident("S24", "Mr & Mrs Roux",              2, ResidentType.OWNER,  "sunhill"),
        Resident("S25", "Mev Liebenberg",             1, ResidentType.RENTAL, "sunhill"),
        Resident("S26", "Me Engelbrecht",             1, ResidentType.OWNER,  "sunhill"),
        Resident("S27", "Mnr & Me Viljoen",           2, ResidentType.OWNER,  "sunhill"),
        Resident("S28", "Mrs Ferreira",               1, ResidentType.RENTAL, "sunhill"),
        Resident("S29", "Me Lötter",                  1, ResidentType.OWNER,  "sunhill"),
        Resident("S30", "Mnr & Mev Scheepers",        2, ResidentType.OWNER,  "sunhill"),
        Resident("S31", "Me Rademeyer",               1, ResidentType.OTP,    "sunhill"),
    )

    /**
     * Monthly totals for January 2026 from the LIZANE PRICES sheet.
     * These are stored as single aggregate MealEntry per resident (not per-day).
     * In production, the app captures daily and sums automatically.
     */
    val januaryEntries = listOf(
        mealEntry("001", "lizane", mapOf(MealType.COURSE_1 to 4, MealType.COURSE_2 to 2, MealType.SUN_1_COURSE to 3, MealType.DINNER to 4, MealType.SOUP_DESSERT to 6, MealType.TA_BAKKIES to 8)),
        mealEntry("002", "lizane", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 7, MealType.TA_BAKKIES to 2)),
        mealEntry("003", "lizane", mapOf(MealType.COURSE_1 to 21, MealType.DINNER to 1, MealType.TA_BAKKIES to 11)),
        mealEntry("004", "lizane", mapOf()),  // 0 meals
        mealEntry("005", "lizane", mapOf(MealType.COURSE_1 to 9, MealType.SUN_1_COURSE to 2, MealType.BREAKFAST to 1)),
        mealEntry("006", "lizane", mapOf(MealType.COURSE_1 to 4, MealType.COURSE_2 to 2, MealType.SUN_1_COURSE to 2, MealType.SOUP_DESSERT to 2)),
        mealEntry("007", "lizane", mapOf(MealType.COURSE_1 to 15, MealType.SUN_1_COURSE to 4, MealType.TA_BAKKIES to 3)),
        mealEntry("008", "lizane", mapOf()),  // 0 meals
        mealEntry("009", "lizane", mapOf(MealType.COURSE_1 to 12, MealType.COURSE_2 to 7, MealType.SUN_1_COURSE to 7, MealType.SOUP_DESSERT to 7, MealType.TA_BAKKIES to 10)),
        mealEntry("010", "lizane", mapOf(MealType.COURSE_1 to 6)),
        mealEntry("011", "lizane", mapOf(MealType.COURSE_1 to 18, MealType.COURSE_2 to 4, MealType.SUN_1_COURSE to 7, MealType.SOUP_DESSERT to 2)),
        mealEntry("012", "lizane", mapOf(MealType.COURSE_1 to 7, MealType.TA_BAKKIES to 2)),
        mealEntry("013", "lizane", mapOf(MealType.COURSE_1 to 21, MealType.TA_BAKKIES to 1)),
        mealEntry("014", "lizane", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 4, MealType.BREAKFAST to 1)),
        mealEntry("015", "lizane", mapOf(MealType.COURSE_2 to 17, MealType.SUN_1_COURSE to 2, MealType.SOUP_DESSERT to 4)),
        mealEntry("016", "lizane", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 2, MealType.TA_BAKKIES to 1)),
        mealEntry("017", "lizane", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 2)),
        mealEntry("018", "lizane", mapOf(MealType.COURSE_1 to 2, MealType.SUN_1_COURSE to 2, MealType.VISITOR_MON_SAT to 1)),
        mealEntry("019", "lizane", mapOf()),
        mealEntry("021", "lizane", mapOf(MealType.COURSE_1 to 4, MealType.SUN_1_COURSE to 1, MealType.TA_BAKKIES to 1)),
        mealEntry("022", "lizane", mapOf(MealType.COURSE_1 to 5, MealType.TA_BAKKIES to 1)),
        mealEntry("023", "lizane", mapOf(MealType.COURSE_1 to 5, MealType.COURSE_2 to 2, MealType.SUN_1_COURSE to 1, MealType.BREAKFAST to 1, MealType.TA_BAKKIES to 1)),
        mealEntry("024", "lizane", mapOf()),

        // ── Bakkies Estate entries ─────────────────────────────────────────────
        mealEntry("B01", "bakkies", mapOf(MealType.COURSE_1 to 8, MealType.COURSE_2 to 4, MealType.SUN_1_COURSE to 3, MealType.TA_BAKKIES to 5)),
        mealEntry("B02", "bakkies", mapOf(MealType.COURSE_1 to 12, MealType.COURSE_2 to 3, MealType.TA_BAKKIES to 2)),
        mealEntry("B03", "bakkies", mapOf(MealType.COURSE_1 to 18, MealType.SUN_1_COURSE to 4, MealType.DINNER to 2)),
        mealEntry("B04", "bakkies", mapOf(MealType.COURSE_1 to 7, MealType.COURSE_2 to 6)),
        mealEntry("B05", "bakkies", mapOf(MealType.COURSE_1 to 10, MealType.BREAKFAST to 2, MealType.TA_BAKKIES to 3)),
        mealEntry("B06", "bakkies", mapOf(MealType.COURSE_1 to 5, MealType.COURSE_2 to 8, MealType.SUN_1_COURSE to 2, MealType.SOUP_DESSERT to 3)),
        mealEntry("B07", "bakkies", mapOf(MealType.COURSE_1 to 14, MealType.SUN_1_COURSE to 3, MealType.TA_BAKKIES to 1)),
        mealEntry("B08", "bakkies", mapOf()),  // 0 meals
        mealEntry("B09", "bakkies", mapOf(MealType.COURSE_1 to 9, MealType.COURSE_2 to 5, MealType.SUN_1_COURSE to 4, MealType.TA_BAKKIES to 6)),
        mealEntry("B10", "bakkies", mapOf(MealType.COURSE_1 to 6, MealType.DINNER to 1)),
        mealEntry("B11", "bakkies", mapOf(MealType.COURSE_1 to 16, MealType.COURSE_2 to 2, MealType.SUN_1_COURSE to 5)),
        mealEntry("B12", "bakkies", mapOf(MealType.COURSE_1 to 4, MealType.TA_BAKKIES to 1)),
        mealEntry("B13", "bakkies", mapOf(MealType.COURSE_1 to 19, MealType.SUN_1_COURSE to 3, MealType.TA_BAKKIES to 2)),
        mealEntry("B14", "bakkies", mapOf(MealType.COURSE_1 to 2, MealType.COURSE_2 to 3, MealType.BREAKFAST to 1)),
        mealEntry("B15", "bakkies", mapOf(MealType.COURSE_2 to 14, MealType.SUN_1_COURSE to 2, MealType.SOUP_DESSERT to 3)),
        mealEntry("B16", "bakkies", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 1, MealType.TA_BAKKIES to 1)),
        mealEntry("B17", "bakkies", mapOf(MealType.COURSE_1 to 11, MealType.COURSE_2 to 3)),
        mealEntry("B18", "bakkies", mapOf(MealType.COURSE_1 to 3, MealType.SUN_1_COURSE to 2, MealType.TA_BAKKIES to 1)),

        // ── Sunhill Retirement entries ─────────────────────────────────────────
        mealEntry("S01", "sunhill", mapOf(MealType.COURSE_1 to 6, MealType.COURSE_2 to 3, MealType.SUN_1_COURSE to 2, MealType.TA_BAKKIES to 4)),
        mealEntry("S02", "sunhill", mapOf(MealType.COURSE_1 to 10, MealType.COURSE_2 to 5, MealType.TA_BAKKIES to 3)),
        mealEntry("S03", "sunhill", mapOf(MealType.COURSE_1 to 15, MealType.DINNER to 2, MealType.TA_BAKKIES to 7)),
        mealEntry("S04", "sunhill", mapOf()),  // 0 meals
        mealEntry("S05", "sunhill", mapOf(MealType.COURSE_1 to 8, MealType.SUN_1_COURSE to 3, MealType.BREAKFAST to 1)),
        mealEntry("S06", "sunhill", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 4, MealType.SUN_1_COURSE to 2, MealType.SOUP_DESSERT to 2)),
        mealEntry("S07", "sunhill", mapOf(MealType.COURSE_1 to 13, MealType.SUN_1_COURSE to 5, MealType.TA_BAKKIES to 2)),
        mealEntry("S08", "sunhill", mapOf(MealType.COURSE_1 to 1)),
        mealEntry("S09", "sunhill", mapOf(MealType.COURSE_1 to 11, MealType.COURSE_2 to 6, MealType.SUN_1_COURSE to 4, MealType.SOUP_DESSERT to 5, MealType.TA_BAKKIES to 8)),
        mealEntry("S10", "sunhill", mapOf(MealType.COURSE_1 to 5, MealType.DINNER to 1)),
        mealEntry("S11", "sunhill", mapOf(MealType.COURSE_1 to 17, MealType.COURSE_2 to 3, MealType.SUN_1_COURSE to 6, MealType.SOUP_DESSERT to 1)),
        mealEntry("S12", "sunhill", mapOf(MealType.COURSE_1 to 6, MealType.TA_BAKKIES to 2)),
        mealEntry("S13", "sunhill", mapOf(MealType.COURSE_1 to 20, MealType.TA_BAKKIES to 3)),
        mealEntry("S14", "sunhill", mapOf(MealType.COURSE_1 to 2, MealType.COURSE_2 to 5, MealType.BREAKFAST to 1)),
        mealEntry("S15", "sunhill", mapOf(MealType.COURSE_2 to 15, MealType.SUN_1_COURSE to 3, MealType.SOUP_DESSERT to 5)),
        mealEntry("S16", "sunhill", mapOf(MealType.COURSE_1 to 4, MealType.COURSE_2 to 2, MealType.TA_BAKKIES to 1)),
        mealEntry("S17", "sunhill", mapOf(MealType.COURSE_1 to 4, MealType.COURSE_2 to 3)),
        mealEntry("S18", "sunhill", mapOf(MealType.COURSE_1 to 3, MealType.SUN_1_COURSE to 2, MealType.VISITOR_MON_SAT to 1)),
        mealEntry("S19", "sunhill", mapOf()),
        mealEntry("S20", "sunhill", mapOf(MealType.COURSE_1 to 5, MealType.SUN_1_COURSE to 2, MealType.TA_BAKKIES to 1)),
        mealEntry("S21", "sunhill", mapOf(MealType.COURSE_1 to 7, MealType.TA_BAKKIES to 2)),
        mealEntry("S22", "sunhill", mapOf(MealType.COURSE_1 to 6, MealType.COURSE_2 to 3, MealType.SUN_1_COURSE to 1, MealType.BREAKFAST to 1, MealType.TA_BAKKIES to 1)),
        mealEntry("S23", "sunhill", mapOf()),
        mealEntry("S24", "sunhill", mapOf(MealType.COURSE_1 to 9, MealType.COURSE_2 to 2, MealType.SUN_1_COURSE to 3)),
        mealEntry("S25", "sunhill", mapOf(MealType.COURSE_1 to 4, MealType.DINNER to 1, MealType.TA_BAKKIES to 2)),
        mealEntry("S26", "sunhill", mapOf(MealType.COURSE_1 to 8, MealType.COURSE_2 to 4)),
        mealEntry("S27", "sunhill", mapOf(MealType.COURSE_1 to 12, MealType.SUN_1_COURSE to 2, MealType.TA_BAKKIES to 3)),
        mealEntry("S28", "sunhill", mapOf(MealType.COURSE_1 to 3, MealType.COURSE_2 to 2, MealType.SOUP_DESSERT to 1)),
        mealEntry("S29", "sunhill", mapOf(MealType.COURSE_1 to 5, MealType.SUN_1_COURSE to 1)),
        mealEntry("S30", "sunhill", mapOf(MealType.COURSE_1 to 10, MealType.COURSE_2 to 3, MealType.TA_BAKKIES to 4)),
        mealEntry("S31", "sunhill", mapOf(MealType.COURSE_1 to 2, MealType.BREAKFAST to 1)),
    )

    private fun mealEntry(unitNumber: String, siteId: String, counts: Map<MealType, Int>): MealEntry =
        MealEntry(
            unitNumber = unitNumber,
            siteId     = siteId,
            year       = 2026,
            month      = 1,
            day        = 0,   // 0 = monthly aggregate
            counts     = counts,
        )
}
