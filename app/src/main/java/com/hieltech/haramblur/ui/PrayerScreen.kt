package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.ui.components.IslamicCalendarMonthGrid
import com.hieltech.haramblur.ui.components.PrayerTimesWidget
import com.hieltech.haramblur.ui.components.QiblaCompassWidget

/**
 * Prayer screen combining Prayer Times and Qibla direction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🕌")
                        Text(
                            text = "Prayer Times",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Prayer Times Widget
            PrayerTimesSection(
                onLocationSettingsClick = onNavigateToSettings
            )

            // Islamic Calendar
            IslamicCalendarSection()

            // Qibla Compass Widget
            QiblaCompassSection()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrayerTimesSection(
    onLocationSettingsClick: () -> Unit
) {
    val prayerViewModel: PrayerTimesViewModel = hiltViewModel()
    val prayerTimes by prayerViewModel.prayerTimes.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    val isLoading by prayerViewModel.isLoading.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    prayerTimes?.date?.hijri?.let { hijri ->
                        Text(
                            text = "${hijri.day} ${hijri.month.en} ${hijri.year} AH",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(onClick = onLocationSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Next Prayer Highlight
            nextPrayer?.let { prayer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next Prayer: ${prayer.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = prayer.time,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${prayer.timeUntil}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Prayer Times List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                prayerTimes?.timings?.let { timings ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrayerTimeRow("Fajr", timings.Fajr, nextPrayer?.name == "Fajr")
                        PrayerTimeRow("Sunrise", timings.Sunrise, false)
                        PrayerTimeRow("Dhuhr", timings.Dhuhr, nextPrayer?.name == "Dhuhr")
                        PrayerTimeRow("Asr", timings.Asr, nextPrayer?.name == "Asr")
                        PrayerTimeRow("Maghrib", timings.Maghrib, nextPrayer?.name == "Maghrib")
                        PrayerTimeRow("Isha", timings.Isha, nextPrayer?.name == "Isha")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeRow(
    name: String,
    time: String,
    isNext: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isNext) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "UPCOMING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IslamicCalendarSection() {
    val prayerViewModel: PrayerTimesViewModel = hiltViewModel()
    val calendarMonth by prayerViewModel.calendarMonth.collectAsState()

    if (calendarMonth.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                IslamicCalendarMonthGrid(
                    monthDays = calendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QiblaCompassSection() {
    // Widget has its own header card internally
    QiblaCompassWidget(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}
