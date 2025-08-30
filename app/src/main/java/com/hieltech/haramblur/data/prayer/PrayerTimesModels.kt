package com.hieltech.haramblur.data.prayer

/**
 * Prayer Times API Response Models
 * Based on Aladhan API documentation
 */

// Main response wrapper
data class PrayerTimesResponse(
    val code: Int,
    val status: String,
    val data: PrayerData
)

// Prayer data containing timings and metadata
data class PrayerData(
    val timings: PrayerTimings,
    val date: HijriDate,
    val meta: MetaData
)

// Prayer timings for all prayers
data class PrayerTimings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String,
    val Sunrise: String,
    val Sunset: String,
    val Imsak: String? = null,
    val Midnight: String? = null,
    val Firstthird: String? = null,
    val Lastthird: String? = null
)

// Date information in both Hijri and Gregorian calendars
data class HijriDate(
    val hijri: HijriCalendar,
    val gregorian: GregorianCalendar
)

// Hijri calendar information
data class HijriCalendar(
    val date: String,
    val format: String,
    val day: String,
    val weekday: HijriWeekday,
    val month: HijriMonth,
    val year: String,
    val designation: Designation,
    val holidays: List<String>? = null
)

// Hijri weekday information
data class HijriWeekday(
    val en: String,
    val ar: String
)

// Hijri month information
data class HijriMonth(
    val number: Int,
    val en: String,
    val ar: String
)

// Designation for Islamic months
data class Designation(
    val abbreviated: String,
    val expanded: String
)

// Gregorian calendar information
data class GregorianCalendar(
    val date: String,
    val format: String,
    val day: String,
    val weekday: GregorianWeekday,
    val month: GregorianMonth,
    val year: String,
    val designation: Designation
)

// Gregorian weekday information
data class GregorianWeekday(
    val en: String
)

// Gregorian month information
data class GregorianMonth(
    val number: Int,
    val en: String
)

// Metadata about the calculation
data class MetaData(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val method: MethodInfo,
    val latitudeAdjustmentMethod: String,
    val midnightMode: String,
    val school: String,
    val offset: Map<String, Int>
)

// Calculation method information
data class MethodInfo(
    val id: Int,
    val name: String,
    val params: MethodParams,
    val location: LocationInfo
)

// Method parameters
data class MethodParams(
    val Fajr: Double,
    val Isha: Double
)

// Location information for the method
data class LocationInfo(
    val latitude: Double,
    val longitude: Double
)

// Qibla direction response
data class QiblaResponse(
    val code: Int,
    val status: String,
    val data: QiblaData
)

// Qibla data
data class QiblaData(
    val latitude: Double,
    val longitude: Double,
    val direction: Double
)

// Islamic calendar response
data class IslamicCalendarResponse(
    val code: Int,
    val status: String,
    val data: List<CalendarDay>
)

// Individual calendar day
data class CalendarDay(
    val timings: PrayerTimings,
    val date: HijriDate,
    val meta: MetaData
)

// Location data for prayer calculations
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val country: String? = null,
    val accuracy: Float? = null
)

// Next prayer information
data class NextPrayerInfo(
    val name: String,
    val time: String,
    val timeUntil: String,
    val timestamp: Long
)

// Prayer calculation methods enum
enum class PrayerCalculationMethod(val id: Int, val displayName: String) {
    UNIVERSITY_OF_KARACHI(1, "University of Islamic Sciences, Karachi"),
    ISLAMIC_SOCIETY_OF_NORTH_AMERICA(2, "Islamic Society of North America"),
    MUSLIM_WORLD_LEAGUE(3, "Muslim World League"),
    UMM_AL_QURA_UNIVERSITY(4, "Umm Al-Qura University, Makkah"),
    EGYPTIAN_GENERAL_AUTHORITY(5, "Egyptian General Authority of Survey"),
    INSTITUTE_OF_GEOPHYSICS(7, "Institute of Geophysics, University of Tehran"),
    GULF_REGION(8, "Gulf Region"),
    KUWAIT(9, "Kuwait"),
    QATAR(10, "Qatar"),
    MAJLIS_UGAMA_ISLAM_SINGAPURA(11, "Majlis Ugama Islam Singapura, Singapore"),
    UNION_ORGANIZATION_ISLAMIC_DE_COOPERATION(12, "Union Organization Islamic de Coopération"),
    DIYANET_ISLERI_BASKANLIGI_TURKEY(13, "Diyanet İşleri Başkanlığı, Turkey"),
    SPIRITUAL_ADMINISTRATION_OF_MUSLIMS_OF_RUSSIA(14, "Spiritual Administration of Muslims of Russia")
}