package com.hieltech.haramblur.data.prayer

import java.util.concurrent.TimeUnit

// Core prayer names in chronological order within a day
enum class PrayerName {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA
}

// Offsets to apply (in minutes) per prayer
data class PrayerOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

// Accuracy levels for displaying GPS/validation quality in UI
enum class PrayerAccuracyLevel { HIGH, MEDIUM, LOW }

data class PrayerAccuracy(
    val level: PrayerAccuracyLevel,
    val gpsAccuracyMeters: Float? = null,
    val message: String? = null
)

// Immutable container for a day's prayer times (UTC millis)
data class DailyPrayerTimes(
    val dateMillis: Long, // normalized start-of-day UTC
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val method: String,
    val asrMadhab: String? = null,
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val appliedOffsets: PrayerOffsets = PrayerOffsets(),
    val gpsAccuracyMeters: Float? = null,
    val locationSource: String? = null
) {
    fun withOffsets(offsets: PrayerOffsets): DailyPrayerTimes {
        fun Long.apply(mins: Int) = this + TimeUnit.MINUTES.toMillis(mins.toLong())
        return copy(
            fajr = fajr.apply(offsets.fajr),
            sunrise = sunrise.apply(offsets.sunrise),
            dhuhr = dhuhr.apply(offsets.dhuhr),
            asr = asr.apply(offsets.asr),
            maghrib = maghrib.apply(offsets.maghrib),
            isha = isha.apply(offsets.isha),
            appliedOffsets = offsets
        )
    }

    fun asList(): List<Pair<PrayerName, Long>> = listOf(
        PrayerName.FAJR to fajr,
        PrayerName.SUNRISE to sunrise,
        PrayerName.DHUHR to dhuhr,
        PrayerName.ASR to asr,
        PrayerName.MAGHRIB to maghrib,
        PrayerName.ISHA to isha
    )
}

// Computed result used by UI/Repository consumers
data class NextPrayer(
    val name: PrayerName,
    val timestamp: Long,
    val timeUntilMillis: Long
)

data class PrayerTimesResult(
    val day: DailyPrayerTimes,
    val nextPrayer: NextPrayer?,
    val accuracy: PrayerAccuracy,
    val warnings: List<String> = emptyList()
)

// Utility helpers
object PrayerTimeUtils {
    fun computeNextPrayer(nowMillis: Long, times: DailyPrayerTimes): NextPrayer? {
        val upcoming = times.asList().firstOrNull { it.second > nowMillis } ?: return null
        val delta = (upcoming.second - nowMillis).coerceAtLeast(0)
        return NextPrayer(upcoming.first, upcoming.second, delta)
    }

    fun formatDuration(millis: Long): String {
        var remaining = millis.coerceAtLeast(0)
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        remaining -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
        remaining -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining)
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }
}

// Defaults and thresholds for accuracy classification
object PrayerAccuracyDefaults {
    const val HIGH_THRESHOLD_M = 30f
    const val MEDIUM_THRESHOLD_M = 100f
    const val LOW_THRESHOLD_M = 300f

    fun classify(gpsAccuracyMeters: Float?): PrayerAccuracyLevel = when {
        gpsAccuracyMeters == null -> PrayerAccuracyLevel.MEDIUM
        gpsAccuracyMeters <= HIGH_THRESHOLD_M -> PrayerAccuracyLevel.HIGH
        gpsAccuracyMeters <= MEDIUM_THRESHOLD_M -> PrayerAccuracyLevel.MEDIUM
        else -> PrayerAccuracyLevel.LOW
    }
}
