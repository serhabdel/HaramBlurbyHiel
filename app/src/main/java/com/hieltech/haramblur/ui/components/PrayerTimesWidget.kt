package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.prayer.*

/**
 * Prayer Times Widget for displaying Islamic prayer times
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesWidget(
    prayerData: PrayerData?,
    nextPrayer: NextPrayerInfo?,
    modifier: Modifier = Modifier
) {
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
                Text(
                    text = "🕌 Prayer Times",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

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
                                    text = "Next Prayer: ${prayer.name}",
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

            // All prayer times grid
            prayerData?.timings?.let { timings ->
                val prayers = listOf(
                    "Fajr" to timings.Fajr,
                    "Sunrise" to timings.Sunrise,
                    "Dhuhr" to timings.Dhuhr,
                    "Asr" to timings.Asr,
                    "Maghrib" to timings.Maghrib,
                    "Sunset" to timings.Sunset,
                    "Isha" to timings.Isha
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(prayers) { (name, time) ->
                        AnimatedFadeIn(visible = true) {
                            PrayerTimeChip(name = name, time = time)
                        }
                    }
                }
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
                text = "📅 Islamic Calendar",
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
                                    text = "Special Day",
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
                text = "🧭 Qibla Direction",
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
private fun PrayerTimeChip(name: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Helper function to get direction text from degrees
 */
private fun getDirectionText(degrees: Double): String {
    return when {
        degrees >= 337.5 || degrees < 22.5 -> "North"
        degrees >= 22.5 && degrees < 67.5 -> "Northeast"
        degrees >= 67.5 && degrees < 112.5 -> "East"
        degrees >= 112.5 && degrees < 157.5 -> "Southeast"
        degrees >= 157.5 && degrees < 202.5 -> "South"
        degrees >= 202.5 && degrees < 247.5 -> "Southwest"
        degrees >= 247.5 && degrees < 292.5 -> "West"
        degrees >= 292.5 && degrees < 337.5 -> "Northwest"
        else -> "Unknown"
    }
}