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
import androidx.compose.ui.platform.LocalContext
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.data.compass.CompassSize
import com.hieltech.haramblur.data.LocationMethod
import com.hieltech.haramblur.utils.MoroccanLocationHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import com.hieltech.haramblur.ui.components.responsiveSpacing
import com.hieltech.haramblur.ui.components.responsiveCardPadding
import com.hieltech.haramblur.ui.components.responsiveIconSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings

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

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with Islamic Calendar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Enhanced header with gradient background
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🕌",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.prayer_times_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            // Hijri date
                            prayerData?.date?.hijri?.let { hijri ->
                                Text(
                                    text = "${hijri.day} ${hijri.month.en} ${hijri.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Enhanced location and calculation method display
                    LocationAndMethodInfo(settings = settings)
                }

                // Location settings button
                onLocationSettingsClick?.let { onClick ->
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.location_settings_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Next prayer highlight - Enhanced design
            nextPrayer?.let { prayer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.next_prayer_label, prayer.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prayer.time,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "⏰",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = prayer.timeUntil,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
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
                    horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing()),
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
 * Individual prayer time chip - Enhanced design
 */
@Composable
private fun PrayerTimeChip(name: String, time: String, highlighted: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (highlighted) 3.dp else 1.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (highlighted) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (highlighted) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
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

/**
 * Enhanced location and calculation method info display
 */
@Composable
private fun LocationAndMethodInfo(settings: com.hieltech.haramblur.data.AppSettings) {
    val context = LocalContext.current
    val moroccanLocationHelper = remember {
        EntryPointAccessors.fromApplication(context, MoroccanLocationHelperEntryPoint::class.java)
            .getMoroccanLocationHelper()
    }

    // Location info
    val locationInfo = remember(settings) {
        when (settings.locationMethod) {
            LocationMethod.MANUAL_CITY -> {
                val city = settings.selectedCityName ?: settings.preferredCity
                val country = settings.selectedCountry ?: settings.preferredCountry

                when {
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    !city.isNullOrBlank() -> city
                    !country.isNullOrBlank() -> country
                    else -> "Manual location"
                }
            }
            LocationMethod.GPS -> {
                val city = settings.locationCity
                val country = settings.locationCountry

                when {
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    !city.isNullOrBlank() -> city
                    !country.isNullOrBlank() -> country
                    else -> "GPS location"
                }
            }
        }
    }

    // Calculation method info
    val calculationMethod = remember(settings) {
        PrayerCalculationMethod.values().find { it.id == settings.prayerCalculationMethod }?.displayName
            ?: "Unknown method"
    }

    // Morocco detection
    val isInMorocco = remember(settings.locationLatitude, settings.locationLongitude) {
        settings.locationLatitude?.let { lat ->
            settings.locationLongitude?.let { lon ->
                moroccanLocationHelper.isInMorocco(lat, lon)
            }
        } ?: false
    }

    // Calculation source info (API vs Local)
    val calculationSource = remember(settings) {
        if (!settings.enableLocalCalculations) {
            "API" to "Online"
        } else if (settings.preferLocalOverApi) {
            "Local" to "Offline"
        } else {
            "API+Local" to "Online with offline fallback"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Location row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = locationInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isInMorocco) {
                Text(
                    text = "🇲🇦",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Calculation method row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Calculation method",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = calculationMethod,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isInMorocco && settings.prayerCalculationMethod == PrayerCalculationMethod.MOROCCO_MINISTRY.id) {
                Text(
                    text = "🇲🇦",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Calculation source row (only if showCalculationMethod is enabled)
        if (settings.showCalculationMethod) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Calculation source",
                    tint = if (calculationSource.first == "Local") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = calculationSource.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (calculationSource.first == "Local") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "(${calculationSource.second})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Entry point for accessing MoroccanLocationHelper from Composable
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MoroccanLocationHelperEntryPoint {
    fun getMoroccanLocationHelper(): MoroccanLocationHelper
}
