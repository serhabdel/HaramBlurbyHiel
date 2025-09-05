package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing computed daily prayer times with metadata for caching and accuracy.
 */
@Entity(
    tableName = "prayer_times",
    indices = [
        Index(value = ["date_millis"]),
        Index(value = ["lat", "lon"]),
        Index(value = ["method"], unique = false),
        Index(value = ["date_millis", "lat", "lon", "method"], unique = true)
    ]
)
data class PrayerTimesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Day (start of day in UTC or normalized millis)
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,

    // Location
    @ColumnInfo(name = "lat")
    val latitude: Double,
    @ColumnInfo(name = "lon")
    val longitude: Double,
    @ColumnInfo(name = "tz_id")
    val timeZoneId: String,

    // Calculation parameters
    @ColumnInfo(name = "method")
    val method: String,
    @ColumnInfo(name = "asr_madhab")
    val asrMadhab: String? = null,

    // Computed prayer times (UTC millis)
    @ColumnInfo(name = "fajr")
    val fajrMillis: Long,
    @ColumnInfo(name = "sunrise")
    val sunriseMillis: Long,
    @ColumnInfo(name = "dhuhr")
    val dhuhrMillis: Long,
    @ColumnInfo(name = "asr")
    val asrMillis: Long,
    @ColumnInfo(name = "maghrib")
    val maghribMillis: Long,
    @ColumnInfo(name = "isha")
    val ishaMillis: Long,

    // Offsets applied (minutes)
    @ColumnInfo(name = "fajr_offset_min")
    val fajrOffsetMin: Int = 0,
    @ColumnInfo(name = "sunrise_offset_min")
    val sunriseOffsetMin: Int = 0,
    @ColumnInfo(name = "dhuhr_offset_min")
    val dhuhrOffsetMin: Int = 0,
    @ColumnInfo(name = "asr_offset_min")
    val asrOffsetMin: Int = 0,
    @ColumnInfo(name = "maghrib_offset_min")
    val maghribOffsetMin: Int = 0,
    @ColumnInfo(name = "isha_offset_min")
    val ishaOffsetMin: Int = 0,

    // Accuracy and source
    @ColumnInfo(name = "gps_accuracy_m")
    val gpsAccuracyMeters: Float? = null,
    @ColumnInfo(name = "location_source")
    val locationSource: String? = null, // e.g., "GPS", "Network", "Manual"

    // Metadata
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
