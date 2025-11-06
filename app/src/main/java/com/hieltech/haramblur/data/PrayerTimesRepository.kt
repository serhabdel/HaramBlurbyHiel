package com.hieltech.haramblur.data

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.api.AladhanApiService
import com.hieltech.haramblur.data.prayer.*
import com.hieltech.haramblur.utils.LocationHelper
import com.hieltech.haramblur.utils.LocalPrayerCalculator
import com.hieltech.haramblur.utils.MoroccanLocationHelper
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
    private val localPrayerCalculator: LocalPrayerCalculator,
    private val moroccanLocationHelper: MoroccanLocationHelper,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PrayerTimesRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private var cachedPrayerData: PrayerData? = null
    private var cacheTimestamp: Long = 0
    private var lastLocationKey: String? = null

    /**
     * Get current prayer times for user's location
     */
    suspend fun getPrayerTimes(): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                if (isCacheValid()) {
                    cachedPrayerData?.let { 
                        Log.d(TAG, "✅ Using cached prayer times")
                        return@withContext Result.success(it) 
                    }
                }

                val settings = settingsRepository.settings.value
                Log.d(TAG, "🔍 Fetching prayer times - localCalc: ${settings.enableLocalCalculations}, preferLocal: ${settings.preferLocalOverApi}")
                
                // Check if local calculations are enabled and preferred
                if (settings.enableLocalCalculations) {
                    if (settings.preferLocalOverApi) {
                        // Try local first, then fallback to API
                        Log.d(TAG, "🏠 Trying local calculation first")
                        val localResult = getPrayerTimesLocally()
                        if (localResult.isSuccess) {
                            Log.i(TAG, "✅ Local calculation successful")
                            return@withContext localResult
                        } else {
                            Log.w(TAG, "⚠️ Local calculation failed, falling back to API: ${localResult.exceptionOrNull()?.message}")
                            // Fall through to API fallback
                        }
                    } else {
                        // Try API first, then fallback to local
                        Log.d(TAG, "🌐 Trying API first")
                        val apiResult = getPrayerTimesFromAPI()
                        if (apiResult.isSuccess) {
                            Log.i(TAG, "✅ API successful")
                            return@withContext apiResult
                        } else {
                            Log.w(TAG, "⚠️ API failed, falling back to local calculation: ${apiResult.exceptionOrNull()?.message}")
                            val localResult = getPrayerTimesLocally()
                            if (localResult.isSuccess) {
                                Log.i(TAG, "✅ Local calculation fallback successful")
                                return@withContext localResult
                            } else {
                                Log.e(TAG, "❌ Both API and local calculation failed")
                                return@withContext localResult
                            }
                        }
                    }
                }
                
                // Default: API only mode
                Log.d(TAG, "🌐 Using API only mode")
                getPrayerTimesFromAPI()
            } catch (e: Exception) {
                Log.e(TAG, "💥 Critical error fetching prayer times", e)
                // Try emergency local calculation as last resort
                if (settingsRepository.settings.value.enableLocalCalculations) {
                    Log.w(TAG, "🆘 Attempting emergency local calculation")
                    getPrayerTimesLocally()
                } else {
                    Result.failure(e)
                }
            }
        }
    }
    
    /**
     * Get prayer times from API (helper method)
     */
    private suspend fun getPrayerTimesFromAPI(): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value
                val tz = TimeZone.getDefault().id

                // Branch by location method
                if (settings.locationMethod == LocationMethod.MANUAL_CITY) {
                    // Prefer coordinates if enabled and available
                    if (settings.preferStoredCoordinates &&
                        settings.selectedLatitude != null && settings.selectedLongitude != null) {
                        val timestamp = System.currentTimeMillis() / 1000
                        val method = settings.prayerCalculationMethod
                        Log.d(TAG, "Fetching prayer times for selected coordinates: ${settings.selectedLatitude}, ${settings.selectedLongitude}")
                        val response = apiService.getPrayerTimes(
                            timestamp = timestamp,
                            latitude = settings.selectedLatitude!!,
                            longitude = settings.selectedLongitude!!,
                            method = method,
                            school = 0,
                            timezonestring = tz
                        )
                        return@withContext if (response.code == 200) {
                            lastLocationKey = buildLocationKey(
                                lat = settings.selectedLatitude!!,
                                lon = settings.selectedLongitude!!,
                                method = method,
                                tz = tz
                            )
                            cachedPrayerData = response.data
                            cacheTimestamp = System.currentTimeMillis()
                            Result.success(response.data)
                        } else {
                            Log.e(TAG, "API Error: ${response.status}")
                            Result.failure(Exception("API Error: ${response.status}"))
                        }
                    }

                    // Else prefer selected city/country if present
                    if (!settings.selectedCityName.isNullOrEmpty() && !settings.selectedCountry.isNullOrEmpty()) {
                        return@withContext getPrayerTimesByCity(
                            settings.selectedCityName!!,
                            settings.selectedCountry!!
                        )
                    }

                    // Legacy preferred city/country
                    if (!settings.preferredCity.isNullOrEmpty() && !settings.preferredCountry.isNullOrEmpty()) {
                        return@withContext getPrayerTimesByCity(
                            settings.preferredCity!!,
                            settings.preferredCountry!!
                        )
                    }
                }

                // When using GPS, try city-based request if cached in settings
                if (!settings.locationCity.isNullOrEmpty() && !settings.locationCountry.isNullOrEmpty()) {
                    return@withContext getPrayerTimesByCity(
                        settings.locationCity!!,
                        settings.locationCountry!!
                    )
                }

                // Fallback to coordinate-based request
                val location = getCurrentLocation()
                val method = settings.prayerCalculationMethod

                val timestamp = System.currentTimeMillis() / 1000
                val response = apiService.getPrayerTimes(
                    timestamp = timestamp,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    method = method,
                    school = 0, // Shafi method
                    timezonestring = tz
                )

                if (response.code == 200) {
                    lastLocationKey = buildLocationKey(lat = location.latitude, lon = location.longitude, method = method, tz = tz)
                    cachedPrayerData = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    Result.success(response.data)
                } else {
                    Log.e(TAG, "API Error: ${response.status}")
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prayer times from API", e)
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
                val settings = settingsRepository.settings.value
                val method = settings.prayerCalculationMethod
                val tz = TimeZone.getDefault().id
                
                // Compute cache key for the requested city/country
                val requestedKey = buildLocationKey(city = city, country = country, method = method, tz = tz)
                
                // Check cache first - compare against the requested key and TTL
                if (cachedPrayerData != null &&
                    (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS &&
                    requestedKey == lastLocationKey) {
                    return@withContext Result.success(cachedPrayerData!!)
                }

                Log.d(TAG, "Fetching prayer times for city: $city, country: $country")

                val response = apiService.getPrayerTimesByCity(
                    city = city,
                    country = country,
                    method = method,
                    school = 0, // Shafi method
                    timezonestring = tz
                )

                if (response.code == 200) {
                    lastLocationKey = requestedKey
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
     * Get prayer times using local calculation (offline)
     */
    suspend fun getPrayerTimesLocally(): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value
                val location = getCurrentLocation()
                
                Log.d(TAG, "Calculating prayer times locally for: ${location.latitude}, ${location.longitude}")
                
                // Determine calculation method
                val calculationMethod = if (settings.moroccoSpecificAdjustments &&
                    moroccanLocationHelper.isInMorocco(location.latitude, location.longitude)) {
                    PrayerCalculationMethod.MOROCCO_MINISTRY
                } else {
                    PrayerCalculationMethod.values().find { it.id == settings.prayerCalculationMethod }
                        ?: PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE
                }
                
                // Get city adjustments if in Morocco
                val cityAdjustments = if (settings.moroccoSpecificAdjustments &&
                    moroccanLocationHelper.isInMorocco(location.latitude, location.longitude)) {
                    localPrayerCalculator.getMoroccanAdjustmentsForCoordinates(location.latitude, location.longitude)
                } else {
                    emptyMap()
                }
                
                // Calculate prayer times
                val calendar = Calendar.getInstance()
                val tzOffsetHours = TimeZone.getDefault().rawOffset / (1000 * 60 * 60).toDouble()
                
                val localPrayerTimes = if (calculationMethod == PrayerCalculationMethod.MOROCCO_MINISTRY) {
                    localPrayerCalculator.computeForMorocco(
                        calendar = calendar,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        asrFactor = 1,
                        adjustmentsMinutes = cityAdjustments
                    )
                } else {
                    localPrayerCalculator.compute(
                        calendar = calendar,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        tzOffsetHours = tzOffsetHours,
                        method = calculationMethod.id,
                        asrFactor = 1,
                        adjustmentsMinutes = cityAdjustments
                    )
                }
                
                // Convert to PrayerData format
                val prayerData = PrayerData(
                    timings = PrayerTimings(
                        Fajr = localPrayerTimes.Fajr,
                        Sunrise = localPrayerTimes.Sunrise,
                        Dhuhr = localPrayerTimes.Dhuhr,
                        Asr = localPrayerTimes.Asr,
                        Maghrib = localPrayerTimes.Maghrib,
                        Isha = localPrayerTimes.Isha,
                        Sunset = localPrayerTimes.Sunset,
                        Imsak = localPrayerTimes.Imsak ?: "00:00",
                        Midnight = localPrayerTimes.Midnight ?: "00:00",
                        Firstthird = localPrayerTimes.Firstthird ?: "00:00",
                        Lastthird = localPrayerTimes.Lastthird ?: "00:00"
                    ),
                    date = HijriDate(
                        hijri = getHijriCalendarApprox(),
                        gregorian = getGregorianForToday()
                    ),
                    meta = MetaData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timezone = TimeZone.getDefault().id,
                        method = MethodInfo(
                            id = calculationMethod.id,
                            name = calculationMethod.displayName,
                            params = MethodParams(
                                Fajr = getMethodAngle(calculationMethod.id, "Fajr"),
                                Isha = getMethodAngle(calculationMethod.id, "Isha")
                            ),
                            location = LocationInfo(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        ),
                        latitudeAdjustmentMethod = "ANGLE_BASED",
                        midnightMode = "STANDARD",
                        school = "STANDARD",
                        offset = mapOf(
                            "Imsak" to 0,
                            "Fajr" to settings.fajrOffsetMinutes,
                            "Sunrise" to settings.sunriseOffsetMinutes,
                            "Dhuhr" to settings.dhuhrOffsetMinutes,
                            "Asr" to settings.asrOffsetMinutes,
                            "Maghrib" to settings.maghribOffsetMinutes,
                            "Isha" to settings.ishaOffsetMinutes
                        )
                    )
                )
                
                // Update cache
                lastLocationKey = buildLocationKey(lat = location.latitude, lon = location.longitude, method = calculationMethod.id, tz = TimeZone.getDefault().id)
                cachedPrayerData = prayerData
                cacheTimestamp = System.currentTimeMillis()
                
                Result.success(prayerData)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating prayer times locally", e)
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
                
                // Check if local calculations are enabled and preferred
                if (settings.enableLocalCalculations) {
                    if (settings.preferLocalOverApi) {
                        // Try local first, then fallback to API
                        val localResult = getIslamicCalendarLocally()
                        if (localResult.isSuccess) {
                            return@withContext localResult
                        } else {
                            Log.w(TAG, "Local Islamic calendar calculation failed, falling back to API: ${localResult.exceptionOrNull()?.message}")
                            // Fall through to API fallback
                        }
                    } else {
                        // Try API first, then fallback to local
                        val apiResult = getIslamicCalendarFromAPI()
                        if (apiResult.isSuccess) {
                            return@withContext apiResult
                        } else {
                            Log.w(TAG, "API failed for Islamic calendar, falling back to local calculation: ${apiResult.exceptionOrNull()?.message}")
                            return@withContext getIslamicCalendarLocally()
                        }
                    }
                }
                
                // Default: API only mode
                getIslamicCalendarFromAPI()
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
                val gregorianYear = calendar.get(Calendar.YEAR)
                val gregorianMonth = calendar.get(Calendar.MONTH) + 1
                val tz = TimeZone.getDefault().id

                Log.d(TAG, "Fetching Islamic calendar for city: $city, country: $country ($gregorianYear/$gregorianMonth Gregorian)")

                val response = apiService.getIslamicCalendarByCity(
                    year = gregorianYear,
                    month = gregorianMonth,
                    city = city,
                    country = country,
                    timezonestring = tz
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
     * Get Islamic calendar from API (helper method)
     */
    private suspend fun getIslamicCalendarFromAPI(): Result<List<CalendarDay>> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value
                val tz = TimeZone.getDefault().id

                // Branch by location method
                if (settings.locationMethod == LocationMethod.MANUAL_CITY) {
                    // Prefer coordinates if enabled and available
                    if (settings.preferStoredCoordinates &&
                        settings.selectedLatitude != null && settings.selectedLongitude != null) {
                        val calendar = Calendar.getInstance()
                        val gregorianYear = calendar.get(Calendar.YEAR)
                        val gregorianMonth = calendar.get(Calendar.MONTH) + 1
                        val response = apiService.getIslamicCalendar(
                            year = gregorianYear,
                            month = gregorianMonth,
                            latitude = settings.selectedLatitude!!,
                            longitude = settings.selectedLongitude!!,
                            timezonestring = tz
                        )
                        return@withContext if (response.code == 200) {
                            Result.success(response.data)
                        } else {
                            Result.failure(Exception("API Error: ${response.status}"))
                        }
                    }

                    // Else prefer selected city/country if present
                    if (!settings.selectedCityName.isNullOrEmpty() && !settings.selectedCountry.isNullOrEmpty()) {
                        return@withContext getIslamicCalendarByCity(
                            settings.selectedCityName!!,
                            settings.selectedCountry!!
                        )
                    }

                    // Legacy preferred city/country
                    if (!settings.preferredCity.isNullOrEmpty() && !settings.preferredCountry.isNullOrEmpty()) {
                        return@withContext getIslamicCalendarByCity(
                            settings.preferredCity!!,
                            settings.preferredCountry!!
                        )
                    }
                }

                // When GPS, try city-based request first if cached
                if (!settings.locationCity.isNullOrEmpty() && !settings.locationCountry.isNullOrEmpty()) {
                    return@withContext getIslamicCalendarByCity(
                        settings.locationCity!!,
                        settings.locationCountry!!
                    )
                }

                // Fallback to coordinate-based request
                val location = getCurrentLocation()
                val calendar = Calendar.getInstance()
                val gregorianYear = calendar.get(Calendar.YEAR)
                val gregorianMonth = calendar.get(Calendar.MONTH) + 1

                Log.d(TAG, "Fetching Islamic calendar for $gregorianYear/$gregorianMonth (Gregorian)")

                val response = apiService.getIslamicCalendar(
                    year = gregorianYear,
                    month = gregorianMonth,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timezonestring = tz
                )

                if (response.code == 200) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Islamic calendar from API", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get Islamic calendar using local calculation (offline)
     */
    suspend fun getIslamicCalendarLocally(): Result<List<CalendarDay>> {
        return withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val gregorianYear = calendar.get(Calendar.YEAR)
                val gregorianMonth = calendar.get(Calendar.MONTH) + 1
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                val calendarDays = mutableListOf<CalendarDay>()
                
                for (day in 1..daysInMonth) {
                    // Create a simple date object for each day
                    val dateCalendar = Calendar.getInstance()
                    dateCalendar.set(Calendar.YEAR, gregorianYear)
                    dateCalendar.set(Calendar.MONTH, gregorianMonth - 1)
                    dateCalendar.set(Calendar.DAY_OF_MONTH, day)
                    
                    // Generate approximate Hijri date (simplified calculation)
                    val hijriDay = day
                    val hijriMonth = getHijriMonth()
                    val hijriYear = getHijriYear()
                    
                    // Calculate prayer times for this day
                    dateCalendar.set(Calendar.HOUR_OF_DAY, 12)
                    dateCalendar.set(Calendar.MINUTE, 0)
                    dateCalendar.set(Calendar.SECOND, 0)
                    
                    val location = getCurrentLocation()
                    val tzOffsetHours = TimeZone.getDefault().rawOffset / (1000 * 60 * 60).toDouble()
                    val settings = settingsRepository.settings.value
                    
                    val calculationMethod = if (settings.moroccoSpecificAdjustments &&
                        moroccanLocationHelper.isInMorocco(location.latitude, location.longitude)) {
                        PrayerCalculationMethod.MOROCCO_MINISTRY
                    } else {
                        PrayerCalculationMethod.values().find { it.id == settings.prayerCalculationMethod }
                            ?: PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE
                    }
                    
                    val cityAdjustments = if (settings.moroccoSpecificAdjustments &&
                        moroccanLocationHelper.isInMorocco(location.latitude, location.longitude)) {
                        localPrayerCalculator.getMoroccanAdjustmentsForCoordinates(location.latitude, location.longitude)
                    } else {
                        emptyMap()
                    }
                    
                    val prayerTimings = if (calculationMethod == PrayerCalculationMethod.MOROCCO_MINISTRY) {
                        localPrayerCalculator.computeForMorocco(
                            calendar = dateCalendar,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            asrFactor = 1,
                            adjustmentsMinutes = cityAdjustments
                        )
                    } else {
                        localPrayerCalculator.compute(
                            calendar = dateCalendar,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            tzOffsetHours = tzOffsetHours,
                            method = calculationMethod.id,
                            asrFactor = 1,
                            adjustmentsMinutes = cityAdjustments
                        )
                    }
                    
                    val calendarDay = CalendarDay(
                        timings = prayerTimings,
                        date = HijriDate(
                            hijri = HijriCalendar(
                                date = String.format(Locale.US, "%02d-%02d-%d", hijriDay, hijriMonth, hijriYear),
                                format = "DD-MM-YYYY",
                                day = String.format(Locale.US, "%02d", hijriDay),
                                weekday = HijriWeekday(en = "", ar = ""),
                                month = HijriMonth(
                                    number = hijriMonth,
                                    en = "Hijri Month $hijriMonth",
                                    ar = "Hijri Month $hijriMonth"
                                ),
                                year = hijriYear.toString(),
                                designation = Designation(abbreviated = "AH", expanded = "Anno Hegirae"),
                                holidays = emptyList()
                            ),
                            gregorian = com.hieltech.haramblur.data.prayer.GregorianCalendar(
                                date = String.format(Locale.US, "%02d-%02d-%d", day, gregorianMonth, gregorianYear),
                                format = "DD-MM-YYYY",
                                day = String.format(Locale.US, "%02d", day),
                                weekday = GregorianWeekday(
                                    en = dateCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH) ?: ""
                                ),
                                month = GregorianMonth(
                                    number = gregorianMonth,
                                    en = dateCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: ""
                                ),
                                year = gregorianYear.toString(),
                                designation = Designation(abbreviated = "AD", expanded = "Anno Domini")
                            )
                        ),
                        meta = MetaData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timezone = TimeZone.getDefault().id,
                            method = MethodInfo(
                                id = calculationMethod.id,
                                name = calculationMethod.displayName,
                                params = MethodParams(
                                    Fajr = getMethodAngle(calculationMethod.id, "Fajr"),
                                    Isha = getMethodAngle(calculationMethod.id, "Isha")
                                ),
                                location = LocationInfo(
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            ),
                            latitudeAdjustmentMethod = "ANGLE_BASED",
                            midnightMode = "STANDARD",
                            school = "STANDARD",
                            offset = mapOf(
                                "Imsak" to 0,
                                "Fajr" to settings.fajrOffsetMinutes,
                                "Sunrise" to settings.sunriseOffsetMinutes,
                                "Dhuhr" to settings.dhuhrOffsetMinutes,
                                "Asr" to settings.asrOffsetMinutes,
                                "Maghrib" to settings.maghribOffsetMinutes,
                                "Isha" to settings.ishaOffsetMinutes
                            )
                        )
                    )
                    
                    calendarDays.add(calendarDay)
                }
                
                Result.success(calendarDays)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating Islamic calendar locally", e)
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

    // Helpers to construct simple date structs when using local calculation
    private fun getGregorianForToday(): com.hieltech.haramblur.data.prayer.GregorianCalendar {
        val cal = Calendar.getInstance()
        val day = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val monthNum = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR).toString()
        return com.hieltech.haramblur.data.prayer.GregorianCalendar(
            date = "$day-$monthNum-$year",
            format = "DD-MM-YYYY",
            day = day,
            weekday = com.hieltech.haramblur.data.prayer.GregorianWeekday(en = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH) ?: ""),
            month = com.hieltech.haramblur.data.prayer.GregorianMonth(number = monthNum, en = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: ""),
            year = year,
            designation = com.hieltech.haramblur.data.prayer.Designation(abbreviated = "AD", expanded = "Anno Domini")
        )
    }

    private fun getHijriCalendarApprox(): com.hieltech.haramblur.data.prayer.HijriCalendar {
        val day = "--"
        val year = getHijriYear().toString()
        val monthNum = getHijriMonth()
        val monthName = "Hijri"
        return com.hieltech.haramblur.data.prayer.HijriCalendar(
            date = "$day-$monthNum-$year",
            format = "DD-MM-YYYY",
            day = day,
            weekday = com.hieltech.haramblur.data.prayer.HijriWeekday(en = "", ar = ""),
            month = com.hieltech.haramblur.data.prayer.HijriMonth(number = monthNum, en = monthName, ar = monthName),
            year = year,
            designation = com.hieltech.haramblur.data.prayer.Designation(abbreviated = "AH", expanded = "Anno Hegirae"),
            holidays = emptyList()
        )
    }

    /**
     * Get current location with fallback options
     */
    private suspend fun getCurrentLocation(): LocationData {
        return withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.value
                // MANUAL_CITY: return selected or preferred entries
                if (settings.locationMethod == LocationMethod.MANUAL_CITY) {
                    if (settings.preferStoredCoordinates &&
                        settings.selectedLatitude != null && settings.selectedLongitude != null) {
                        return@withContext LocationData(
                            latitude = settings.selectedLatitude!!,
                            longitude = settings.selectedLongitude!!,
                            city = settings.selectedCityName ?: settings.preferredCity,
                            country = settings.selectedCountry ?: settings.preferredCountry
                        )
                    }
                    if (!settings.selectedCityName.isNullOrEmpty() && !settings.selectedCountry.isNullOrEmpty()) {
                        return@withContext LocationData(
                            latitude = 0.0,
                            longitude = 0.0,
                            city = settings.selectedCityName,
                            country = settings.selectedCountry
                        )
                    }
                    if (!settings.preferredCity.isNullOrEmpty() && !settings.preferredCountry.isNullOrEmpty()) {
                        return@withContext LocationData(
                            latitude = 0.0,
                            longitude = 0.0,
                            city = settings.preferredCity,
                            country = settings.preferredCountry
                        )
                    }
                }

                // GPS path
                // 1) Prefer stored coordinates from settings if present
                if (settings.locationLatitude != null && settings.locationLongitude != null) {
                    Log.d(TAG, "Using stored GPS coordinates from settings")
                    return@withContext LocationData(
                        latitude = settings.locationLatitude!!,
                        longitude = settings.locationLongitude!!,
                        city = settings.locationCity,
                        country = settings.locationCountry
                    )
                }

                // 2) Try in-memory cached location from helper for fast path
                locationHelper.getCachedLocation()?.let {
                    Log.d(TAG, "Using cached in-memory location")
                    return@withContext it
                }

                // 3) If permission is not granted, avoid fresh request and fallback
                if (!locationHelper.hasLocationPermission()) {
                    Log.w(TAG, "Location permission not granted; falling back")
                    // Optionally use last known even without runtime grant; but many devices need permission
                    locationHelper.getLastKnownLocation()?.let { return@withContext it }
                    return@withContext LocationData(
                        latitude = 21.4225,
                        longitude = 39.8262,
                        city = "Mecca",
                        country = "Saudi Arabia"
                    )
                }

                // 4) Try enhanced best location, then last known
                locationHelper.getBestLocation()?.let { return@withContext it }
                locationHelper.getLastKnownLocation()?.let { return@withContext it }

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
        val currentKey = computeCurrentLocationKey()
        return cachedPrayerData != null &&
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS &&
               currentKey != null && currentKey == lastLocationKey
    }

    /**
     * Clear cache (useful for testing or manual refresh)
     */
    fun clearCache() {
        cachedPrayerData = null
        cacheTimestamp = 0
        lastLocationKey = null
    }

    /**
     * Explicitly invalidate prayer times cache (to be called on settings/location changes)
     */
    fun invalidateCache() = clearCache()

    /**
     * Get prayer times as a Flow for reactive updates
     */
    fun getPrayerTimesFlow(): Flow<Result<PrayerData>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getPrayerTimes())
            delay(30 * 60 * 1000L) // Refresh every 30 minutes
        }
    }

    /**
     * Trigger immediate refresh of prayer times (fire-and-forget)
     */
    fun triggerRefresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getPrayerTimes()
            } catch (e: Exception) {
                Log.e(TAG, "Error during triggered refresh", e)
            }
        }
    }

    /**
     * Build a key representing the current location context for cache validation
     */
    private fun buildLocationKey(
        city: String? = null,
        country: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        method: Int,
        tz: String
    ): String {
        return if (!city.isNullOrEmpty() && !country.isNullOrEmpty()) {
            "city:$city|$country|m:$method|tz:$tz"
        } else if (lat != null && lon != null) {
            val rlat = String.format(Locale.US, "%.3f", lat)
            val rlon = String.format(Locale.US, "%.3f", lon)
            "geo:$rlat,$rlon|m:$method|tz:$tz"
        } else {
            "unknown|m:$method|tz:$tz"
        }
    }

    private fun computeCurrentLocationKey(): String? {
        return try {
            val settings = settingsRepository.settings.value
            val method = settings.prayerCalculationMethod
            val tz = TimeZone.getDefault().id
            when {
                settings.locationMethod == LocationMethod.MANUAL_CITY &&
                        settings.preferStoredCoordinates &&
                        settings.selectedLatitude != null && settings.selectedLongitude != null ->
                    buildLocationKey(lat = settings.selectedLatitude, lon = settings.selectedLongitude, method = method, tz = tz)

                settings.locationMethod == LocationMethod.MANUAL_CITY &&
                        !settings.selectedCityName.isNullOrEmpty() && !settings.selectedCountry.isNullOrEmpty() ->
                    buildLocationKey(city = settings.selectedCityName, country = settings.selectedCountry, method = method, tz = tz)

                !settings.preferredCity.isNullOrEmpty() && !settings.preferredCountry.isNullOrEmpty() ->
                    buildLocationKey(city = settings.preferredCity, country = settings.preferredCountry, method = method, tz = tz)

                !settings.locationCity.isNullOrEmpty() && !settings.locationCountry.isNullOrEmpty() ->
                    buildLocationKey(city = settings.locationCity, country = settings.locationCountry, method = method, tz = tz)

                settings.locationLatitude != null && settings.locationLongitude != null ->
                    buildLocationKey(lat = settings.locationLatitude, lon = settings.locationLongitude, method = method, tz = tz)

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper method to get prayer calculation method angles
     */
    private fun getMethodAngle(methodId: Int, prayerType: String): Double {
        return when (methodId) {
            1 -> when (prayerType) { // Karachi
                "Fajr" -> 18.0
                "Isha" -> 18.0
                else -> 18.0
            }
            2 -> when (prayerType) { // ISNA
                "Fajr" -> 15.0
                "Isha" -> 15.0
                else -> 15.0
            }
            3 -> when (prayerType) { // Muslim World League
                "Fajr" -> 18.0
                "Isha" -> 17.0
                else -> 18.0
            }
            4 -> when (prayerType) { // Umm Al-Qura
                "Fajr" -> 18.5
                "Isha" -> 0.0 // 90 minutes interval
                else -> 18.5
            }
            5 -> when (prayerType) { // Egypt
                "Fajr" -> 19.5
                "Isha" -> 17.5
                else -> 19.5
            }
            7 -> when (prayerType) { // Tehran
                "Fajr" -> 17.7
                "Isha" -> 14.0
                else -> 17.7
            }
            13 -> when (prayerType) { // Diyanet
                "Fajr" -> 18.0
                "Isha" -> 17.0
                else -> 18.0
            }
            15 -> when (prayerType) { // Morocco Ministry
                "Fajr" -> 18.0
                "Isha" -> 17.0
                else -> 18.0
            }
            else -> when (prayerType) { // Default
                "Fajr" -> 18.0
                "Isha" -> 17.0
                else -> 18.0
            }
        }
    }
}
