package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hildebrandtdigital.wpcbroadsheet.data.model.MealType
import com.hildebrandtdigital.wpcbroadsheet.data.model.ResidentType

class RoomConverters {

    private val gson = Gson()

    // ── ResidentType ──────────────────────────────────────────────────────────

    @TypeConverter
    fun residentTypeToString(type: ResidentType): String = type.name

    @TypeConverter
    fun stringToResidentType(value: String): ResidentType = ResidentType.valueOf(value)

    // ── Map<MealType, Int> ────────────────────────────────────────────────────
    // Stored as a JSON object: {"COURSE_1":12,"COURSE_2":4,"TA_BAKKIES":3}
    // Keys are MealType.name strings — forward-compatible as long as enum
    // names aren't renamed (safe; use @SerializedName on the enum if needed).

    @TypeConverter
    fun mealCountsToJson(counts: Map<MealType, Int>): String =
        gson.toJson(counts.mapKeys { it.key.name })

    @TypeConverter
    fun jsonToMealCounts(json: String): Map<MealType, Int> {
        val type    = object : TypeToken<Map<String, Int>>() {}.type
        val raw     = gson.fromJson<Map<String, Int>>(json, type)
        return raw
            .mapNotNull { (key, value) ->
                // Gracefully skip unknown keys — protects against future
                // enum removals when reading old cached data
                runCatching { MealType.valueOf(key) to value }.getOrNull()
            }
            .toMap()
    }
}
