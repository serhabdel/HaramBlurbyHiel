package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.prayer.*
import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.data.compass.CompassSize
import com.hieltech.haramblur.data.LocationMethod

/**
 * Prayer Times Widget for displaying Islamic prayer times
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesWidget(
    prayerData: PrayerData?,
    nextPrayer: NextPrayerInfo?,
    onLocationSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()

    ModernCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Islamic Calendar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prayer_times_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    // City, Country display
                    val cityCountry: String? = remember(settings) {
                        if (settings.locationMethod == LocationMethod.MANUAL_CITY) {
                            // Use selected city/country (fallback to preferred)
                            val city = settings.selectedCityName ?: settings.preferredCity
                            val country = settings.selectedCountry ?: settings.preferredCountry
                            
                            if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                                "$city, $country"
                            } else city ?: country
                        } else {
                            // GPS method - use location city/country
                            val city = settings.locationCity
                            val country = settings.locationCountry
                            
                            if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                                "$city, $country"
                            } else city ?: country
                        }
                    }
                    if (!cityCountry.isNullOrBlank()) {
                        Text(
                            text = cityCountry!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Location settings button
                    onLocationSettingsClick?.let { onClick ->
                        IconButton(
                            onClick = onClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.location_settings_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Hijri date
                    prayerData?.date?.hijri?.let { hijri ->
                        AnimatedFadeIn(visible = true) {
                            Text(
                                text = "${hijri.day} ${hijri.month.en} ${hijri.year}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Next prayer highlight
            nextPrayer?.let { prayer ->
                AnimatedFadeIn(visible = true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.next_prayer_label, prayer.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = prayer.time,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = prayer.timeUntil,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // All prayer times grid (highlight next prayer)
            prayerData?.timings?.let { timings ->
                val prayers = listOf(
                    stringResource(R.string.prayer_name_fajr) to timings.Fajr,
                    stringResource(R.string.prayer_name_sunrise) to timings.Sunrise,
                    stringResource(R.string.prayer_name_dhuhr) to timings.Dhuhr,
                    stringResource(R.string.prayer_name_asr) to timings.Asr,
                    stringResource(R.string.prayer_name_maghrib) to timings.Maghrib,
                    stringResource(R.string.prayer_name_sunset) to timings.Sunset,
                    stringResource(R.string.prayer_name_isha) to timings.Isha
                )

                val nextName = nextPrayer?.name?.lowercase()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(prayers) { (name, time) ->
                        val isNext = nextName != null && name.lowercase().contains(nextName)
                        AnimatedFadeIn(visible = true) {
                            PrayerTimeChip(name = name, time = time, highlighted = isNext)
                        }
                    }
                }
            }

            // Interactive Qibla Compass (integrated)
            if (settings.enableQiblaDirection && settings.qiblaCompassEnabled) {
                QiblaCompassWidget(
                    modifier = Modifier
                        .fillMaxWidth(),
                    showDegreeMarkings = settings.compassShowDegreeMarkings,
                    hapticOnAligned = settings.compassHapticFeedback,
                    alignmentToleranceDeg = settings.qiblaToleranceDegrees,
                    animationSpeed = settings.compassAnimationSpeed,
                    preferredSize = when (settings.compassPreferredSize) {
                        CompassSize.SMALL -> CompassSize.SMALL
                        CompassSize.MEDIUM -> CompassSize.MEDIUM
                        CompassSize.LARGE -> CompassSize.LARGE
                    }
                )
            }

            // Loading state
            if (prayerData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Islamic Calendar Widget
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarWidget(
    hijriDate: HijriCalendar?,
    modifier: Modifier = Modifier
) {
    ModernCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.islamic_calendar_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            hijriDate?.let { date ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        AnimatedFadeIn(visible = true) {
                            Text(
                                text = "${date.day} ${date.month.en}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedFadeIn(visible = true) {
                            Text(
                                text = date.year,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedFadeIn(visible = true) {
                            Text(
                                text = date.weekday.en,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        AnimatedFadeIn(visible = true) {
                            Text(
                                text = date.format,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Islamic events or special days
                if (date.holidays?.isNotEmpty() == true) {
                    AnimatedFadeIn(visible = true) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.special_day_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                date.holidays.forEach { holiday ->
                                    Text(
                                        text = "• $holiday",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading state
            if (hijriDate == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Qibla Direction Widget
 *
 * Deprecated: Replaced by the interactive `QiblaCompassWidget` which provides
 * live sensor-driven direction, accessibility semantics, and error handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Deprecated("Replaced by QiblaCompassWidget")
fun QiblaDirectionWidget(
    qiblaDirection: Double?,
    modifier: Modifier = Modifier
) {
    ModernCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.qibla_direction_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            qiblaDirection?.let { direction ->
                AnimatedFadeIn(visible = true) {
                    Text(
                        text = "${String.format("%.1f", direction)}°",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedFadeIn(visible = true) {
                    Text(
                        text = getDirectionText(direction),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compass visualization
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(60.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple compass with Qibla arrow
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Loading state
            if (qiblaDirection == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Individual prayer time chip
 */
@Composable
private fun PrayerTimeChip(name: String, time: String, highlighted: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Helper function to get direction text from degrees
 */
@Composable
private fun getDirectionText(degrees: Double): String {
    return when {
        degrees >= 337.5 || degrees < 22.5 -> stringResource(R.string.direction_north)
        degrees >= 22.5 && degrees < 67.5 -> stringResource(R.string.direction_northeast)
        degrees >= 67.5 && degrees < 112.5 -> stringResource(R.string.direction_east)
        degrees >= 112.5 && degrees < 157.5 -> stringResource(R.string.direction_southeast)
        degrees >= 157.5 && degrees < 202.5 -> stringResource(R.string.direction_south)
        degrees >= 202.5 && degrees < 247.5 -> stringResource(R.string.direction_southwest)
        degrees >= 247.5 && degrees < 292.5 -> stringResource(R.string.direction_west)
        degrees >= 292.5 && degrees < 337.5 -> stringResource(R.string.direction_northwest)
        else -> stringResource(R.string.direction_unknown)
    }
}
