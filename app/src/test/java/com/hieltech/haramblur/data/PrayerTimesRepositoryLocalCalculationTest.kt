package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.api.AladhanApiService
import com.hieltech.haramblur.utils.LocationHelper
import com.hieltech.haramblur.utils.LocalPrayerCalculator
import com.hieltech.haramblur.utils.MoroccanLocationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.content.Context

/**
 * Integration test suite for PrayerTimesRepository with local calculation support
 * Validates the integration between PrayerTimesRepository, LocalPrayerCalculator, and MoroccanLocationHelper
 */
class PrayerTimesRepositoryLocalCalculationTest {

    @Mock
    private lateinit var apiService: AladhanApiService

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var locationHelper: LocationHelper

    @Mock
    private lateinit var localPrayerCalculator: LocalPrayerCalculator

    @Mock
    private lateinit var context: Context

    private lateinit var prayerTimesRepository: PrayerTimesRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        prayerTimesRepository = PrayerTimesRepository(
            apiService = apiService,
            settingsRepository = settingsRepository,
            locationHelper = locationHelper,
            localPrayerCalculator = localPrayerCalculator,
            context = context
        )
    }

    @Test
    fun `test getPrayerTimes uses local calculation when enabled and preferred`() = runBlocking {
        // Setup mock settings
        val settings = AppSettings().copy(
            enableLocalCalculations = true,
            preferLocalOverApi = true,
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 33.5731, // Casablanca
            locationLongitude = -7.5898
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 33.5731,
            longitude = -7.5898,
            city = "Casablanca",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock local calculation result
        val mockPrayerTimings = com.hieltech.haramblur.data.prayer.PrayerTimings(
            Fajr = "05:30",
            Sunrise = "06:45",
            Dhuhr = "12:30",
            Asr = "15:45",
            Maghrib = "18:15",
            Isha = "19:30",
            Sunset = "18:15",
            Imsak = "05:20",
            Midnight = "00:00",
            Firstthird = "22:00",
            Lastthird = "01:00"
        )
        `when`(localPrayerCalculator.computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )).thenReturn(mockPrayerTimings)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        assertEquals("05:30", result.getOrThrow().timings.Fajr)
        assertEquals("12:30", result.getOrThrow().timings.Dhuhr)
        assertEquals("18:15", result.getOrThrow().timings.Maghrib)
        assertEquals("19:30", result.getOrThrow().timings.Isha)

        // Verify local calculation was called
        verify(localPrayerCalculator).computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )

        // Verify API was not called since local calculation is preferred
        verify(apiService, never()).getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )
    }

    @Test
    fun `test getPrayerTimes uses Moroccan regional adjustments`() = runBlocking {
        // Setup mock settings
        val settings = AppSettings().copy(
            enableLocalCalculations = true,
            preferLocalOverApi = true,
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 34.6814, // Oujda
            locationLongitude = -1.9086
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 34.6814,
            longitude = -1.9086,
            city = "Oujda",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock local calculation result
        val mockPrayerTimings = com.hieltech.haramblur.data.prayer.PrayerTimings(
            Fajr = "05:00",
            Sunrise = "06:15",
            Dhuhr = "12:00",
            Asr = "15:15",
            Maghrib = "17:45",
            Isha = "19:00",
            Sunset = "17:45",
            Imsak = "04:50",
            Midnight = "00:00",
            Firstthird = "21:30",
            Lastthird = "00:30"
        )
        `when`(localPrayerCalculator.computeForMorocco(
            latitude = 34.6814,
            longitude = -1.9086,
            date = any(),
            cityAdjustments = any()
        )).thenReturn(mockPrayerTimings)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        
        // Verify that the correct regional adjustments for Oujda (Oriental region) were applied
        // Oriental region has -3 minutes for Fajr and Isha, -1 for Dhuhr and Asr
        verify(localPrayerCalculator).computeForMorocco(
            latitude = 34.6814,
            longitude = -1.9086,
            date = any(),
            cityAdjustments = argThat { adjustments ->
                adjustments["Fajr"] == -3 && adjustments["Isha"] == -3 &&
                adjustments["Dhuhr"] == -1 && adjustments["Asr"] == -1
            }
        )
    }

    @Test
    fun `test getPrayerTimes uses API fallback when local calculation fails`() = runBlocking {
        // Setup mock settings
        val settings = AppSettings().copy(
            enableLocalCalculations = true,
            preferLocalOverApi = true,
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 33.5731,
            locationLongitude = -7.5898
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 33.5731,
            longitude = -7.5898,
            city = "Casablanca",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock local calculation to throw exception
        `when`(localPrayerCalculator.computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )).thenThrow(RuntimeException("Local calculation failed"))

        // Setup mock API response
        val mockApiPrayerData = PrayerData(
            timings = com.hieltech.haramblur.data.prayer.PrayerTimings(
                Fajr = "05:30",
                Sunrise = "06:45",
                Dhuhr = "12:30",
                Asr = "15:45",
                Maghrib = "18:15",
                Isha = "19:30",
                Sunset = "18:15",
                Imsak = "05:20",
                Midnight = "00:00",
                Firstthird = "22:00",
                Lastthird = "01:00"
            ),
            date = DateMeta(
                gregorian = GregorianCalendar(
                    date = "01-01-2023",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = GregorianWeekday(en = "Sunday"),
                    month = GregorianMonth(number = 1, en = "January"),
                    year = "2023",
                    designation = Designation(abbreviated = "AD", expanded = "Anno Domini")
                ),
                hijri = HijriCalendar(
                    date = "01-01-1444",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = HijriWeekday(en = "", ar = ""),
                    month = HijriMonth(number = 1, en = "Muharram", ar = "محرم"),
                    year = "1444",
                    designation = Designation(abbreviated = "AH", expanded = "Anno Hegirae"),
                    holidays = emptyList()
                )
            ),
            meta = Meta(
                latitude = 33.5731,
                longitude = -7.5898,
                timezone = "Africa/Casablanca",
                method = MetaMethod(
                    id = 15,
                    name = "Morocco Ministry of Islamic Affairs",
                    params = MetaParams(
                        Fajr = 18.0,
                        Isha = 17.0
                    )
                ),
                latitudeAdjustmentMethod = "ANGLE_BASED",
                midnightMode = "STANDARD",
                school = "STANDARD",
                offset = MetaOffset(
                    Imsak = 0,
                    Fajr = 0,
                    Sunrise = 0,
                    Dhuhr = 0,
                    Asr = 0,
                    Maghrib = 0,
                    Isha = 0
                )
            )
        )

        val mockApiResponse = com.hieltech.haramblur.data.api.ApiResponse(
            code = 200,
            status = "OK",
            data = mockApiPrayerData
        )
        `when`(apiService.getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockApiResponse)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        assertEquals("05:30", result.getOrThrow().timings.Fajr)
        assertEquals("12:30", result.getOrThrow().timings.Dhuhr)
        assertEquals("18:15", result.getOrThrow().timings.Maghrib)
        assertEquals("19:30", result.getOrThrow().timings.Isha)

        // Verify local calculation was attempted first
        verify(localPrayerCalculator).computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )

        // Verify API was called as fallback
        verify(apiService).getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )
    }

    @Test
    fun `test getPrayerTimes uses API first when local calculation is not preferred`() = runBlocking {
        // Setup mock settings
        val settings = AppSettings().copy(
            enableLocalCalculations = true,
            preferLocalOverApi = false, // API preferred
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 33.5731,
            locationLongitude = -7.5898
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 33.5731,
            longitude = -7.5898,
            city = "Casablanca",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock API response
        val mockApiPrayerData = PrayerData(
            timings = com.hieltech.haramblur.data.prayer.PrayerTimings(
                Fajr = "05:30",
                Sunrise = "06:45",
                Dhuhr = "12:30",
                Asr = "15:45",
                Maghrib = "18:15",
                Isha = "19:30",
                Sunset = "18:15",
                Imsak = "05:20",
                Midnight = "00:00",
                Firstthird = "22:00",
                Lastthird = "01:00"
            ),
            date = DateMeta(
                gregorian = GregorianCalendar(
                    date = "01-01-2023",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = GregorianWeekday(en = "Sunday"),
                    month = GregorianMonth(number = 1, en = "January"),
                    year = "2023",
                    designation = Designation(abbreviated = "AD", expanded = "Anno Domini")
                ),
                hijri = HijriCalendar(
                    date = "01-01-1444",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = HijriWeekday(en = "", ar = ""),
                    month = HijriMonth(number = 1, en = "Muharram", ar = "محرم"),
                    year = "1444",
                    designation = Designation(abbreviated = "AH", expanded = "Anno Hegirae"),
                    holidays = emptyList()
                )
            ),
            meta = Meta(
                latitude = 33.5731,
                longitude = -7.5898,
                timezone = "Africa/Casablanca",
                method = MetaMethod(
                    id = 15,
                    name = "Morocco Ministry of Islamic Affairs",
                    params = MetaParams(
                        Fajr = 18.0,
                        Isha = 17.0
                    )
                ),
                latitudeAdjustmentMethod = "ANGLE_BASED",
                midnightMode = "STANDARD",
                school = "STANDARD",
                offset = MetaOffset(
                    Imsak = 0,
                    Fajr = 0,
                    Sunrise = 0,
                    Dhuhr = 0,
                    Asr = 0,
                    Maghrib = 0,
                    Isha = 0
                )
            )
        )

        val mockApiResponse = com.hieltech.haramblur.data.api.ApiResponse(
            code = 200,
            status = "OK",
            data = mockApiPrayerData
        )
        `when`(apiService.getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockApiResponse)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        assertEquals("05:30", result.getOrThrow().timings.Fajr)
        assertEquals("12:30", result.getOrThrow().timings.Dhuhr)
        assertEquals("18:15", result.getOrThrow().timings.Maghrib)
        assertEquals("19:30", result.getOrThrow().timings.Isha)

        // Verify API was called first
        verify(apiService).getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )

        // Verify local calculation was not called since API succeeded
        verify(localPrayerCalculator, never()).computeForMorocco(
            any(), any(), any(), any()
        )
    }

    @Test
    fun `test getPrayerTimes uses local calculation when API fails and local is preferred`() = runBlocking {
        // Setup mock settings
        val settings = AppSettings().copy(
            enableLocalCalculations = true,
            preferLocalOverApi = false, // API preferred
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 33.5731,
            locationLongitude = -7.5898
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 33.5731,
            longitude = -7.5898,
            city = "Casablanca",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock API to fail
        val mockApiError = com.hieltech.haramblur.data.api.ApiResponse<PrayerData>(
            code = 500,
            status = "Internal Server Error",
            data = null
        )
        `when`(apiService.getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockApiError)

        // Setup mock local calculation result
        val mockPrayerTimings = com.hieltech.haramblur.data.prayer.PrayerTimings(
            Fajr = "05:30",
            Sunrise = "06:45",
            Dhuhr = "12:30",
            Asr = "15:45",
            Maghrib = "18:15",
            Isha = "19:30",
            Sunset = "18:15",
            Imsak = "05:20",
            Midnight = "00:00",
            Firstthird = "22:00",
            Lastthird = "01:00"
        )
        `when`(localPrayerCalculator.computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )).thenReturn(mockPrayerTimings)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        assertEquals("05:30", result.getOrThrow().timings.Fajr)
        assertEquals("12:30", result.getOrThrow().timings.Dhuhr)
        assertEquals("18:15", result.getOrThrow().timings.Maghrib)
        assertEquals("19:30", result.getOrThrow().timings.Isha)

        // Verify API was called first
        verify(apiService).getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )

        // Verify local calculation was called as fallback
        verify(localPrayerCalculator).computeForMorocco(
            latitude = 33.5731,
            longitude = -7.5898,
            date = any(),
            cityAdjustments = any()
        )
    }

    @Test
    fun `test getPrayerTimes uses API only when local calculations are disabled`() = runBlocking {
        // Setup mock settings with local calculations disabled
        val settings = AppSettings().copy(
            enableLocalCalculations = false, // Local calculations disabled
            preferLocalOverApi = false,
            moroccoSpecificAdjustments = true,
            locationMethod = LocationMethod.GPS,
            locationLatitude = 33.5731,
            locationLongitude = -7.5898
        )
        `when`(settingsRepository.settings.value).thenReturn(settings)

        // Setup mock location
        val locationData = LocationData(
            latitude = 33.5731,
            longitude = -7.5898,
            city = "Casablanca",
            country = "Morocco"
        )
        `when`(locationHelper.getCachedLocation()).thenReturn(locationData)

        // Setup mock API response
        val mockApiPrayerData = PrayerData(
            timings = com.hieltech.haramblur.data.prayer.PrayerTimings(
                Fajr = "05:30",
                Sunrise = "06:45",
                Dhuhr = "12:30",
                Asr = "15:45",
                Maghrib = "18:15",
                Isha = "19:30",
                Sunset = "18:15",
                Imsak = "05:20",
                Midnight = "00:00",
                Firstthird = "22:00",
                Lastthird = "01:00"
            ),
            date = DateMeta(
                gregorian = GregorianCalendar(
                    date = "01-01-2023",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = GregorianWeekday(en = "Sunday"),
                    month = GregorianMonth(number = 1, en = "January"),
                    year = "2023",
                    designation = Designation(abbreviated = "AD", expanded = "Anno Domini")
                ),
                hijri = HijriCalendar(
                    date = "01-01-1444",
                    format = "DD-MM-YYYY",
                    day = "01",
                    weekday = HijriWeekday(en = "", ar = ""),
                    month = HijriMonth(number = 1, en = "Muharram", ar = "محرم"),
                    year = "1444",
                    designation = Designation(abbreviated = "AH", expanded = "Anno Hegirae"),
                    holidays = emptyList()
                )
            ),
            meta = Meta(
                latitude = 33.5731,
                longitude = -7.5898,
                timezone = "Africa/Casablanca",
                method = MetaMethod(
                    id = 15,
                    name = "Morocco Ministry of Islamic Affairs",
                    params = MetaParams(
                        Fajr = 18.0,
                        Isha = 17.0
                    )
                ),
                latitudeAdjustmentMethod = "ANGLE_BASED",
                midnightMode = "STANDARD",
                school = "STANDARD",
                offset = MetaOffset(
                    Imsak = 0,
                    Fajr = 0,
                    Sunrise = 0,
                    Dhuhr = 0,
                    Asr = 0,
                    Maghrib = 0,
                    Isha = 0
                )
            )
        )

        val mockApiResponse = com.hieltech.haramblur.data.api.ApiResponse(
            code = 200,
            status = "OK",
            data = mockApiPrayerData
        )
        `when`(apiService.getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockApiResponse)

        // Execute
        val result = prayerTimesRepository.getPrayerTimes()

        // Verify
        assertTrue(result.isSuccess)
        assertEquals("05:30", result.getOrThrow().timings.Fajr)
        assertEquals("12:30", result.getOrThrow().timings.Dhuhr)
        assertEquals("18:15", result.getOrThrow().timings.Maghrib)
        assertEquals("19:30", result.getOrThrow().timings.Isha)

        // Verify API was called
        verify(apiService).getPrayerTimes(
            any(), any(), any(), any(), any(), any()
        )

        // Verify local calculation was not called since local calculations are disabled
        verify(localPrayerCalculator, never()).computeForMorocco(
            any(), any(), any(), any()
        )
    }
}