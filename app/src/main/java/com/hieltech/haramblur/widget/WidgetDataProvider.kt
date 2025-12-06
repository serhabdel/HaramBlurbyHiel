package com.hieltech.haramblur.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.hieltech.haramblur.data.prayer.NextPrayerInfo
import com.hieltech.haramblur.data.prayer.PrayerData
import com.hieltech.haramblur.data.prayer.PrayerTimings
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides data for widgets by reading from SharedPreferences
 * This allows widgets to access prayer times and dhikr data without direct app access
 */
object WidgetDataProvider {
    
    private const val TAG = "WidgetDataProvider"
    private const val PREFS_NAME = "widget_data_prefs"
    private const val KEY_PRAYER_TIMES = "prayer_times"
    private const val KEY_NEXT_PRAYER = "next_prayer"
    private const val KEY_HIJRI_DATE = "hijri_date"
    private const val KEY_GREGORIAN_DATE = "gregorian_date"
    private const val KEY_TASBIH_COUNT = "tasbih_count"
    private const val KEY_TASBIH_INDEX = "tasbih_index"
    private const val KEY_TASBIH_DATE = "tasbih_date"
    private const val KEY_LOCATION_NAME = "location_name"
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Prayer Times Data
    fun savePrayerTimes(context: Context, timings: PrayerTimings) {
        Log.d(TAG, "Saving prayer times: Fajr=${timings.Fajr}, Dhuhr=${timings.Dhuhr}")
        getPrefs(context).edit()
            .putString(KEY_PRAYER_TIMES, gson.toJson(timings))
            .apply()
    }
    
    fun getPrayerTimes(context: Context): PrayerTimings? {
        val json = getPrefs(context).getString(KEY_PRAYER_TIMES, null)
        Log.d(TAG, "Getting prayer times, json exists: ${json != null}")
        if (json == null) return null
        return try {
            gson.fromJson(json, PrayerTimings::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing prayer times", e)
            null
        }
    }
    
    fun saveNextPrayer(context: Context, nextPrayer: NextPrayerInfo) {
        getPrefs(context).edit()
            .putString(KEY_NEXT_PRAYER, gson.toJson(nextPrayer))
            .apply()
    }
    
    fun getNextPrayer(context: Context): NextPrayerInfo? {
        val json = getPrefs(context).getString(KEY_NEXT_PRAYER, null) ?: return null
        return try {
            gson.fromJson(json, NextPrayerInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Data class for dynamic next prayer info
     */
    data class DynamicNextPrayer(
        val name: String,
        val time: String,
        val countdown: String
    )
    
    /**
     * Get the actual next prayer based on current time
     * This dynamically determines which prayer is next
     */
    fun getDynamicNextPrayer(context: Context): DynamicNextPrayer? {
        val prayerTimes = getPrayerTimes(context) ?: return null
        
        try {
            val now = java.util.Calendar.getInstance()
            val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            
            // Build list of prayers with their times in minutes
            val prayers = listOf(
                Triple("Fajr", prayerTimes.Fajr, parseTimeToMinutes(prayerTimes.Fajr)),
                Triple("Dhuhr", prayerTimes.Dhuhr, parseTimeToMinutes(prayerTimes.Dhuhr)),
                Triple("Asr", prayerTimes.Asr, parseTimeToMinutes(prayerTimes.Asr)),
                Triple("Maghrib", prayerTimes.Maghrib, parseTimeToMinutes(prayerTimes.Maghrib)),
                Triple("Isha", prayerTimes.Isha, parseTimeToMinutes(prayerTimes.Isha))
            ).filter { it.third >= 0 } // Filter out invalid times
            
            if (prayers.isEmpty()) return null
            
            // Find the next prayer (first prayer after current time)
            var nextPrayer = prayers.firstOrNull { it.third > currentMinutes }
            
            // If no prayer found today, next prayer is Fajr tomorrow
            val isTomorrow = nextPrayer == null
            if (nextPrayer == null) {
                nextPrayer = prayers.first() // Fajr
            }
            
            // Calculate minutes until next prayer
            var minutesUntil = nextPrayer.third - currentMinutes
            if (minutesUntil <= 0) {
                minutesUntil += 24 * 60 // Add 24 hours if prayer is tomorrow
            }
            
            // Format countdown
            val countdown = when {
                minutesUntil < 1 -> "now"
                minutesUntil < 60 -> "in ${minutesUntil}m"
                else -> {
                    val hours = minutesUntil / 60
                    val mins = minutesUntil % 60
                    if (mins > 0) "in ${hours}h ${mins}m" else "in ${hours}h"
                }
            }
            
            Log.d(TAG, "Dynamic next prayer: ${nextPrayer.first} at ${nextPrayer.second}, $countdown")
            
            return DynamicNextPrayer(
                name = nextPrayer.first,
                time = nextPrayer.second,
                countdown = countdown
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating dynamic next prayer", e)
            return null
        }
    }
    
    /**
     * Parse time string (HH:mm) to minutes since midnight
     */
    private fun parseTimeToMinutes(timeString: String): Int {
        return try {
            val cleanTime = timeString.split(" ").first().trim()
            val parts = cleanTime.split(":")
            if (parts.size != 2) return -1
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Get the countdown string for the next prayer (e.g., "in 16m" or "in 1h 23m")
     * This calculates dynamically based on current time
     */
    fun getNextPrayerCountdown(context: Context): String {
        return getDynamicNextPrayer(context)?.countdown ?: ""
    }
    
    // Hijri Date
    fun saveHijriDate(context: Context, hijriDate: String) {
        getPrefs(context).edit()
            .putString(KEY_HIJRI_DATE, hijriDate)
            .apply()
    }
    
    fun getHijriDate(context: Context): String {
        return getPrefs(context).getString(KEY_HIJRI_DATE, "") ?: ""
    }
    
    fun saveGregorianDate(context: Context, date: String) {
        getPrefs(context).edit()
            .putString(KEY_GREGORIAN_DATE, date)
            .apply()
    }
    
    fun getGregorianDate(context: Context): String {
        return getPrefs(context).getString(KEY_GREGORIAN_DATE, "") ?: ""
    }
    
    // Location
    fun saveLocationName(context: Context, name: String) {
        getPrefs(context).edit()
            .putString(KEY_LOCATION_NAME, name)
            .apply()
    }
    
    fun getLocationName(context: Context): String {
        return getPrefs(context).getString(KEY_LOCATION_NAME, "Unknown") ?: "Unknown"
    }
    
    // Tasbih Counter
    fun getTasbihCount(context: Context): Int {
        checkAndResetDailyTasbih(context)
        return getPrefs(context).getInt(KEY_TASBIH_COUNT, 0)
    }
    
    fun getTasbihIndex(context: Context): Int {
        checkAndResetDailyTasbih(context)
        return getPrefs(context).getInt(KEY_TASBIH_INDEX, 0)
    }
    
    fun incrementTasbih(context: Context): Pair<Int, Int> {
        checkAndResetDailyTasbih(context)
        val prefs = getPrefs(context)
        var count = prefs.getInt(KEY_TASBIH_COUNT, 0)
        var index = prefs.getInt(KEY_TASBIH_INDEX, 0)
        
        count++
        if (count >= 33) {
            count = 0
            index = (index + 1) % 3
        }
        
        // Use commit() for synchronous write - ensures data is saved before widget update
        prefs.edit()
            .putInt(KEY_TASBIH_COUNT, count)
            .putInt(KEY_TASBIH_INDEX, index)
            .commit()
        
        Log.d(TAG, "Tasbih incremented: count=$count, index=$index")
        return Pair(count, index)
    }
    
    fun resetTasbih(context: Context) {
        // Use commit() for synchronous write
        getPrefs(context).edit()
            .putInt(KEY_TASBIH_COUNT, 0)
            .commit()
        Log.d(TAG, "Tasbih reset to 0")
    }
    
    private fun checkAndResetDailyTasbih(context: Context) {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString(KEY_TASBIH_DATE, "")
        
        if (savedDate != today) {
            prefs.edit()
                .putInt(KEY_TASBIH_COUNT, 0)
                .putInt(KEY_TASBIH_INDEX, 0)
                .putString(KEY_TASBIH_DATE, today)
                .apply()
        }
    }
    
    // Tasbih texts
    val tasbihTexts = listOf(
        TasbihData("سُبْحَانَ اللهِ", "Subḥān Allāh", "Glory be to Allah"),
        TasbihData("الْحَمْدُ للهِ", "Al-ḥamdu lillāh", "All praise is for Allah"),
        TasbihData("اللهُ أَكْبَرُ", "Allāhu Akbar", "Allah is the Greatest")
    )
    
    data class TasbihData(
        val arabic: String,
        val transliteration: String,
        val english: String
    )
    
    // Calculate time until next prayer
    fun calculateTimeUntil(prayerTime: String): String {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()
            val prayerCal = Calendar.getInstance()
            
            val parsed = timeFormat.parse(prayerTime.take(5)) ?: return ""
            prayerCal.time = parsed
            prayerCal.set(Calendar.YEAR, now.get(Calendar.YEAR))
            prayerCal.set(Calendar.MONTH, now.get(Calendar.MONTH))
            prayerCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            
            if (prayerCal.before(now)) {
                prayerCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val diffMs = prayerCal.timeInMillis - now.timeInMillis
            val hours = diffMs / (1000 * 60 * 60)
            val minutes = (diffMs / (1000 * 60)) % 60
            
            when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "Now"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
