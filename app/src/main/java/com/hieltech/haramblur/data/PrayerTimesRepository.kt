package com.hieltech.haramblur.data

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.api.AladhanApiService
import com.hieltech.haramblur.data.prayer.*
import com.hieltech.haramblur.utils.LocationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Islamic prayer times and calendar data
 * Handles API calls, caching, and location services
 */
@Singleton
class PrayerTimesRepository @Inject constructor(
    private val apiService: AladhanApiService,
    private val settingsRepository: SettingsRepository,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PrayerTimesRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private var cachedPrayerData: PrayerData? = null
    private var cacheTimestamp: Long = 0

    /**
     * Get current prayer times for user's location
     */
    suspend fun getPrayerTimes(): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                if (isCacheValid()) {
                    cachedPrayerData?.let { return@withContext Result.success(it) }
                }

                val settings = settingsRepository.settings.value

                // Try city-based request first if city/country are available
                if (!settings.locationCity.isNullOrEmpty() && !settings.locationCountry.isNullOrEmpty()) {
                    return@withContext getPrayerTimesByCity(
                        settings.locationCity!!,
                        settings.locationCountry!!
                    )
                }

                // Fallback to coordinate-based request
                val location = getCurrentLocation()
                val timestamp = System.currentTimeMillis() / 1000
                val method = settings.prayerCalculationMethod

                Log.d(TAG, "Fetching prayer times for location: ${location.latitude}, ${location.longitude}")

                val response = apiService.getPrayerTimes(
                    timestamp = timestamp,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    method = method,
                    school = 0 // Shafi method
                )

                if (response.code == 200) {
                    cachedPrayerData = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    Result.success(response.data)
                } else {
                    Log.e(TAG, "API Error: ${response.status}")
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prayer times", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get prayer times by city and country (more accurate)
     */
    suspend fun getPrayerTimesByCity(city: String, country: String): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                if (isCacheValid()) {
                    cachedPrayerData?.let { return@withContext Result.success(it) }
                }

                val settings = settingsRepository.settings.value
                val method = settings.prayerCalculationMethod

                Log.d(TAG, "Fetching prayer times for city: $city, country: $country")

                val response = apiService.getPrayerTimesByCity(
                    city = city,
                    country = country,
                    method = method,
                    school = 0 // Shafi method
                )

                if (response.code == 200) {
                    cachedPrayerData = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    Result.success(response.data)
                } else {
                    Log.e(TAG, "API Error: ${response.status}")
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prayer times by city", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get Islamic calendar for current month
     */
    suspend fun getIslamicCalendar(): Result<List<CalendarDay>> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value

                // Try city-based request first if city/country are available
                if (!settings.locationCity.isNullOrEmpty() && !settings.locationCountry.isNullOrEmpty()) {
                    return@withContext getIslamicCalendarByCity(
                        settings.locationCity!!,
                        settings.locationCountry!!
                    )
                }

                // Fallback to coordinate-based request
                val location = getCurrentLocation()
                val calendar = Calendar.getInstance()
                val hijriYear = getHijriYear()
                val hijriMonth = getHijriMonth()

                Log.d(TAG, "Fetching Islamic calendar for $hijriYear/$hijriMonth")

                val response = apiService.getIslamicCalendar(
                    year = hijriYear,
                    month = hijriMonth,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                if (response.code == 200) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Islamic calendar", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get Islamic calendar by city and country (more accurate)
     */
    suspend fun getIslamicCalendarByCity(city: String, country: String): Result<List<CalendarDay>> {
        return withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val hijriYear = getHijriYear()
                val hijriMonth = getHijriMonth()

                Log.d(TAG, "Fetching Islamic calendar for city: $city, country: $country ($hijriYear/$hijriMonth)")

                val response = apiService.getIslamicCalendarByCity(
                    year = hijriYear,
                    month = hijriMonth,
                    city = city,
                    country = country
                )

                if (response.code == 200) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Islamic calendar by city", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get Qibla direction from current location
     */
    suspend fun getQiblaDirection(): Result<Double> {
        return withContext(Dispatchers.IO) {
            try {
                val location = getCurrentLocation()

                val response = apiService.getQiblaDirection(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                if (response.code == 200) {
                    Result.success(response.data.direction)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Qibla direction", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get next prayer information
     */
    suspend fun getNextPrayer(): Result<NextPrayerInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val prayerData = getPrayerTimes().getOrThrow()
                val nextPrayer = calculateNextPrayer(prayerData)

                if (nextPrayer != null) {
                    Result.success(nextPrayer)
                } else {
                    Result.failure(Exception("No upcoming prayer found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating next prayer", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get current location with fallback options
     */
    private suspend fun getCurrentLocation(): LocationData {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value

                // If auto-detect is disabled and user has set preferred location, use that
                if (!settings.autoDetectLocation &&
                    !settings.preferredCity.isNullOrEmpty() &&
                    !settings.preferredCountry.isNullOrEmpty()) {
                    return@withContext LocationData(
                        latitude = 0.0, // Not needed for city-based requests
                        longitude = 0.0, // Not needed for city-based requests
                        city = settings.preferredCity,
                        country = settings.preferredCountry
                    )
                }

                // Try to get location from cached settings first
                if (settings.locationLatitude != null && settings.locationLongitude != null) {
                    return@withContext LocationData(
                        latitude = settings.locationLatitude!!,
                        longitude = settings.locationLongitude!!,
                        city = settings.locationCity,
                        country = settings.locationCountry
                    )
                }

                // Try to get current location
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    return@withContext location
                }

                // Try last known location as fallback
                val lastLocation = locationHelper.getLastKnownLocation()
                if (lastLocation != null) {
                    return@withContext lastLocation
                }

                // Fallback to default location (Mecca)
                Log.w(TAG, "Using fallback location (Mecca)")
                LocationData(
                    latitude = 21.4225,
                    longitude = 39.8262,
                    city = "Mecca",
                    country = "Saudi Arabia"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                // Fallback to Mecca
                LocationData(
                    latitude = 21.4225,
                    longitude = 39.8262,
                    city = "Mecca",
                    country = "Saudi Arabia"
                )
            }
        }
    }

    /**
     * Calculate next prayer from current prayer times
     */
    private fun calculateNextPrayer(prayerData: PrayerData): NextPrayerInfo? {
        try {
            val currentTime = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val prayers = listOf(
                "Fajr" to prayerData.timings.Fajr,
                "Dhuhr" to prayerData.timings.Dhuhr,
                "Asr" to prayerData.timings.Asr,
                "Maghrib" to prayerData.timings.Maghrib,
                "Isha" to prayerData.timings.Isha
            )

            for ((prayerName, prayerTime) in prayers) {
                val prayerTimestamp = parsePrayerTime(prayerTime)
                if (prayerTimestamp > currentTime) {
                    val timeUntil = formatTimeUntil(prayerTimestamp - currentTime)
                    return NextPrayerInfo(
                        name = prayerName,
                        time = prayerTime,
                        timeUntil = timeUntil,
                        timestamp = prayerTimestamp
                    )
                }
            }

            // If no prayer found today, get tomorrow's Fajr
            val tomorrowFajr = parsePrayerTime(prayerData.timings.Fajr, addDays = 1)
            val timeUntil = formatTimeUntil(tomorrowFajr - currentTime)
            return NextPrayerInfo(
                name = "Fajr",
                time = prayerData.timings.Fajr,
                timeUntil = timeUntil,
                timestamp = tomorrowFajr
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating next prayer", e)
            return null
        }
    }

    /**
     * Parse prayer time string to timestamp
     */
    private fun parsePrayerTime(timeString: String, addDays: Int = 0): Long {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.parse(timeString)

            val calendar = Calendar.getInstance()
            calendar.time = time ?: Date()
            calendar.add(Calendar.DAY_OF_MONTH, addDays)

            // Set current date but with prayer time
            val now = Calendar.getInstance()
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH) + addDays)

            return calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing prayer time: $timeString", e)
            return System.currentTimeMillis()
        }
    }

    /**
     * Format time until prayer in human readable format
     */
    private fun formatTimeUntil(millisUntil: Long): String {
        val hours = millisUntil / (1000 * 60 * 60)
        val minutes = (millisUntil % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "Now"
        }
    }

    /**
     * Get current Hijri year (approximate)
     */
    private fun getHijriYear(): Int {
        val calendar = Calendar.getInstance()
        val gregorianYear = calendar.get(Calendar.YEAR)
        // Approximate conversion (Hijri year is about 11 days shorter)
        return gregorianYear - 579
    }

    /**
     * Get current Hijri month (approximate)
     */
    private fun getHijriMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) + 1
    }

    /**
     * Check if cached data is still valid
     */
    private fun isCacheValid(): Boolean {
        return cachedPrayerData != null &&
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS
    }

    /**
     * Clear cache (useful for testing or manual refresh)
     */
    fun clearCache() {
        cachedPrayerData = null
        cacheTimestamp = 0
    }

    /**
     * Get prayer times as a Flow for reactive updates
     */
    fun getPrayerTimesFlow(): Flow<Result<PrayerData>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getPrayerTimes())
            delay(30 * 60 * 1000L) // Refresh every 30 minutes
        }
    }
}