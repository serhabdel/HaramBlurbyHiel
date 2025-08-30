package com.hieltech.haramblur.data.api

import com.hieltech.haramblur.data.prayer.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Aladhan API Service Interface
 * Provides Islamic prayer times, calendar, and Qibla direction services
 */
interface AladhanApiService {

    /**
     * Get prayer times for a specific timestamp and location
     * @param timestamp Unix timestamp
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param method Calculation method (default: ISNA)
     * @param school Asr calculation method (0: Shafi, 1: Hanafi)
     * @param midnightMode Midnight calculation method
     * @param latitudeAdjustmentMethod Latitude adjustment method
     * @param tune Prayer time adjustments in minutes
     */
    @GET("timings/{timestamp}")
    suspend fun getPrayerTimes(
        @Path("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2, // ISNA method
        @Query("school") school: Int = 0, // Shafi
        @Query("midnightMode") midnightMode: Int = 0,
        @Query("latitudeAdjustmentMethod") latitudeAdjustmentMethod: Int = 3,
        @Query("tune") tune: String? = null
    ): PrayerTimesResponse

    /**
     * Get Islamic calendar for a specific month
     * @param year Hijri year
     * @param month Hijri month (1-12)
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param method Calculation method
     * @param school Asr calculation method
     */
    @GET("calendar/{year}/{month}")
    suspend fun getIslamicCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2,
        @Query("school") school: Int = 0
    ): IslamicCalendarResponse

    /**
     * Get Qibla direction from a location
     * @param latitude Location latitude
     * @param longitude Location longitude
     */
    @GET("qibla/{latitude}/{longitude}")
    suspend fun getQiblaDirection(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): QiblaResponse

    /**
     * Get prayer times by city and country
     * @param city City name
     * @param country Country name
     * @param method Calculation method
     * @param school Asr calculation method
     * @param month Optional month for calendar
     * @param year Optional year for calendar
     */
    @GET("timingsByCity")
    suspend fun getPrayerTimesByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 2,
        @Query("school") school: Int = 0,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): PrayerTimesResponse

    /**
     * Get Islamic calendar by city and country
     * @param city City name
     * @param country Country name
     * @param month Hijri month
     * @param year Hijri year
     * @param method Calculation method
     * @param school Asr calculation method
     */
    @GET("calendarByCity/{year}/{month}")
    suspend fun getIslamicCalendarByCity(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 2,
        @Query("school") school: Int = 0
    ): IslamicCalendarResponse
}