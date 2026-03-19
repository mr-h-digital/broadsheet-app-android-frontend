package com.hildebrandtdigital.wpcbroadsheet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import java.security.MessageDigest
import java.util.Calendar

@Database(
    entities = [
        ResidentEntity::class,
        ResidentAuditEntity::class,
        MealEntryEntity::class,
        MealPricingEntity::class,
        UserEntity::class,
        SiteEntity::class,
    ],
    version      = 5,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun residentDao()     : ResidentDao
    abstract fun residentAuditDao(): ResidentAuditDao
    abstract fun mealEntryDao()    : MealEntryDao
    abstract fun mealPricingDao()  : MealPricingDao
    abstract fun userDao()         : UserDao
    abstract fun siteDao()         : SiteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context)
            }

        // ── Migration 1 → 2: meal_entries and meal_pricing ───────────────────
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_entries (
                        siteId          TEXT    NOT NULL,
                        unitNumber      TEXT    NOT NULL,
                        year            INTEGER NOT NULL,
                        month           INTEGER NOT NULL,
                        countsJson      TEXT    NOT NULL,
                        lastModifiedAt  INTEGER NOT NULL,
                        lastModifiedBy  TEXT    NOT NULL,
                        PRIMARY KEY (siteId, unitNumber, year, month)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_meal_entries_site_period ON meal_entries (siteId, year, month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_meal_entries_unit ON meal_entries (unitNumber)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_pricing (
                        siteId                    TEXT    NOT NULL,
                        year                      INTEGER NOT NULL,
                        month                     INTEGER NOT NULL,
                        course1                   REAL    NOT NULL,
                        course2                   REAL    NOT NULL,
                        course3                   REAL    NOT NULL,
                        fullBoard                 REAL    NOT NULL,
                        sun1Course                REAL    NOT NULL,
                        sun3Course                REAL    NOT NULL,
                        breakfast                 REAL    NOT NULL,
                        dinner                    REAL    NOT NULL,
                        soupDessert               REAL    NOT NULL,
                        visitorMonSat             REAL    NOT NULL,
                        visitorSun1               REAL    NOT NULL,
                        visitorSun3               REAL    NOT NULL,
                        taBakkies                 REAL    NOT NULL,
                        vatRate                   REAL    NOT NULL,
                        compulsoryMealsDeduction  REAL    NOT NULL,
                        lastModifiedAt            INTEGER NOT NULL,
                        lastModifiedBy            TEXT    NOT NULL,
                        lastSyncedAt              INTEGER,
                        PRIMARY KEY (siteId, year, month)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_meal_pricing_site ON meal_pricing (siteId)")
            }
        }

        // ── Migration 2 → 3: users and sites tables ───────────────────────────
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id              TEXT    NOT NULL PRIMARY KEY,
                        name            TEXT    NOT NULL,
                        email           TEXT    NOT NULL,
                        passwordHash    TEXT    NOT NULL,
                        role            TEXT    NOT NULL,
                        phone           TEXT    NOT NULL DEFAULT '',
                        siteId          TEXT,
                        isActive        INTEGER NOT NULL DEFAULT 1,
                        createdAt       INTEGER NOT NULL,
                        createdBy       TEXT    NOT NULL,
                        lastModifiedAt  INTEGER NOT NULL,
                        lastModifiedBy  TEXT    NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_role ON users (role)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_siteId ON users (siteId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sites (
                        id              TEXT    NOT NULL PRIMARY KEY,
                        name            TEXT    NOT NULL,
                        isActive        INTEGER NOT NULL DEFAULT 1,
                        createdAt       INTEGER NOT NULL,
                        createdBy       TEXT    NOT NULL,
                        lastModifiedAt  INTEGER NOT NULL,
                        lastModifiedBy  TEXT    NOT NULL
                    )
                """.trimIndent())
            }
        }

        // ── Migration 3 → 4: no schema change — re-seeds corrected user siteId data ─
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix seed data: update Unit Manager siteIds that were incorrectly
                // stored as null because the User() constructor used positional args.
                db.execSQL("UPDATE users SET siteId = 'lizane'  WHERE email = 'vanrooyen@wpc.co.za' AND role = 'UNIT_MANAGER'")
                db.execSQL("UPDATE users SET siteId = 'bakkies' WHERE email = 'nothnagel@wpc.co.za' AND role = 'UNIT_MANAGER'")
            }
        }


        // ── Migration 4 → 5: fix phone/siteId fields incorrectly seeded from positional args ─
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Unit Managers were seeded with siteId value in the phone field
                // (positional constructor arg bug). Clear phone AND restore siteId.
                db.execSQL("UPDATE users SET phone = '+27 72 555 0200', siteId = 'lizane'  WHERE email = 'vanrooyen@wpc.co.za'  AND role = 'UNIT_MANAGER'")
                db.execSQL("UPDATE users SET phone = '+27 72 555 0300', siteId = 'bakkies' WHERE email = 'nothnagel@wpc.co.za' AND role = 'UNIT_MANAGER'")
                // Also fix names to use full proper names
                db.execSQL("UPDATE users SET name = 'Megan van Rooyen'    WHERE email = 'vanrooyen@wpc.co.za'")
                db.execSQL("UPDATE users SET name = 'Martin Nothnagel'    WHERE email = 'nothnagel@wpc.co.za'")
                db.execSQL("UPDATE users SET name = 'Chernay Hildebrandt' WHERE email = 'chernay@wpc.co.za'")
            }
        }
        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "wpc_broadsheet.db",
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        // ── Public entry point called from MainActivity on IO dispatcher ──────
        // Checks whether users already exist before seeding, so it's safe to
        // call on every app start (idempotent).
        suspend fun ensureSeeded(context: Context) {
            val database = get(context)
            val userDao  = database.userDao()
            // Only seed if the users table is empty
            if (userDao.countUsers() == 0L) {
                seedInitialData(context)
            }
        }

        // ── Seed ─────────────────────────────────────────────────────────────

        suspend fun seedInitialData(context: Context) {
            val database    = get(context)
            val residentDao = database.residentDao()
            val auditDao    = database.residentAuditDao()
            val entryDao    = database.mealEntryDao()
            val pricingDao  = database.mealPricingDao()
            val userDao     = database.userDao()
            val siteDao     = database.siteDao()
            val now         = System.currentTimeMillis()

            // ── Sites ─────────────────────────────────────────────────────
            SampleData.sites.forEach { site ->
                siteDao.upsert(
                    SiteEntity(
                        id             = site.id,
                        name           = site.name,
                        isActive       = true,
                        createdAt      = now,
                        createdBy      = "system",
                        lastModifiedAt = now,
                        lastModifiedBy = "system",
                    )
                )
            }

            // ── Users ─────────────────────────────────────────────────────
            // Seed default accounts from SampleData.
            // Default password for all seeded accounts is "wpc2026"
            val defaultHash = sha256("wpc2026")
            // Add a system admin account
            val seedUsers = SampleData.users + com.hildebrandtdigital.wpcbroadsheet.data.model.User(
                id     = "admin1",
                name   = "System Admin",
                email  = "admin@wpc.co.za",
                role   = com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole.ADMIN,
                phone  = "",
                siteId = null,
            )
            seedUsers.forEach { u ->
                userDao.upsert(
                    UserEntity(
                        id             = u.id,
                        name           = u.name,
                        email          = u.email,
                        passwordHash   = defaultHash,
                        role           = u.role.name,
                        phone          = u.phone,
                        siteId         = u.siteId,
                        isActive       = true,
                        createdAt      = now,
                        createdBy      = "system",
                        lastModifiedAt = now,
                        lastModifiedBy = "system",
                    )
                )
            }

            // ── Residents ─────────────────────────────────────────────────
            SampleData.residents.forEach { r ->
                residentDao.upsert(
                    ResidentEntity(
                        siteId         = r.siteId,
                        unitNumber     = r.unitNumber,
                        clientName     = r.clientName,
                        totalOccupants = r.totalOccupants,
                        residentType   = r.residentType,
                        isActive       = true,
                        createdBy      = "system",
                        createdAt      = now,
                        lastModifiedBy = "system",
                        lastModifiedAt = now,
                    )
                )
                auditDao.insert(
                    ResidentAuditEntity(
                        siteId     = r.siteId,
                        unitNumber = r.unitNumber,
                        action     = AuditAction.CREATED.name,
                        actor      = "system",
                        at         = now,
                        note       = "Seeded from SampleData",
                    )
                )
            }

            // ── January 2026 meal entries ──────────────────────────────────
            val converters = RoomConverters()
            SampleData.januaryEntries.forEach { entry ->
                entryDao.upsert(
                    MealEntryEntity(
                        siteId         = entry.siteId,
                        unitNumber     = entry.unitNumber,
                        year           = entry.year,
                        month          = entry.month,
                        countsJson     = converters.mealCountsToJson(entry.counts),
                        lastModifiedAt = now,
                        lastModifiedBy = "system",
                    )
                )
            }

            // ── Pricing configs ────────────────────────────────────────────
            val cal       = Calendar.getInstance()
            val thisYear  = cal.get(Calendar.YEAR)
            val thisMonth = cal.get(Calendar.MONTH) + 1
            val nextMonth = if (thisMonth == 12) 1 else thisMonth + 1
            val nextYear  = if (thisMonth == 12) thisYear + 1 else thisYear

            listOf(thisYear to thisMonth, nextYear to nextMonth).forEach { (y, m) ->
                SampleData.sites.forEach { site ->
                    val p = SampleData.pricingForSite(site.id)
                    pricingDao.upsert(
                        MealPricingEntity(
                            siteId                   = site.id,
                            year                     = y,
                            month                    = m,
                            course1                  = p.course1,
                            course2                  = p.course2,
                            course3                  = p.course3,
                            fullBoard                = p.fullBoard,
                            sun1Course               = p.sun1Course,
                            sun3Course               = p.sun3Course,
                            breakfast                = p.breakfast,
                            dinner                   = p.dinner,
                            soupDessert              = p.soupDessert,
                            visitorMonSat            = p.visitorMonSat,
                            visitorSun1              = p.visitorSun1,
                            visitorSun3              = p.visitorSun3,
                            taBakkies                = p.taBakkies,
                            vatRate                  = p.vatRate,
                            compulsoryMealsDeduction = p.compulsoryMealsDeduction,
                            lastModifiedAt           = now,
                            lastModifiedBy           = "system",
                        )
                    )
                }
            }
        }

        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
