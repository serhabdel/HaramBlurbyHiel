package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.data.cities.CitySelection
import com.hieltech.haramblur.data.LocationMethod
import com.hieltech.haramblur.data.LocationPermissionStatus
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import com.hieltech.haramblur.utils.MoroccanLocationHelper
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Islamic Features Onboarding Step
 * Allows users to configure Islamic features during initial setup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicOnboardingStep(
    viewModel: PermissionWizardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsState()
    var enableIslamicFeatures by remember { mutableStateOf(true) }
    var locationMethod by remember { mutableStateOf(settings.locationMethod) }
    var selectedSelection by remember { mutableStateOf<CitySelection?>(null) }
    var showCitySelector by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val moroccanLocationHelper = remember {
        EntryPointAccessors.fromApplication(context, MoroccanLocationHelperEntryPoint::class.java)
            .getMoroccanLocationHelper()
    }

    // Morocco detection
    val isInMorocco = remember(settings.locationLatitude, settings.locationLongitude) {
        settings.locationLatitude?.let { lat ->
            settings.locationLongitude?.let { lon ->
                moroccanLocationHelper.isInMorocco(lat, lon)
            }
        } ?: false
    }

    // Suggested Moroccan cities
    val suggestedMoroccanCities = remember(settings.locationLatitude, settings.locationLongitude) {
        if (isInMorocco && settings.locationLatitude != null && settings.locationLongitude != null) {
            moroccanLocationHelper.getSuggestedMoroccanCities(
                settings.locationLatitude!!,
                settings.locationLongitude!!
            )
        } else {
            emptyList()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Sync permission and try to refresh location
        settingsViewModel.syncLocationPermissionStatus()
        settingsViewModel.refreshLocation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "🕌 Islamic Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Enhance your experience with Islamic prayer times, calendar, and spiritual features",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Islamic Features Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Islamic Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Prayer times, Islamic calendar, and spiritual reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enableIslamicFeatures,
                    onCheckedChange = { enableIslamicFeatures = it }
                )
            }
        }

        // Islamic Features Details (when enabled)
        if (enableIslamicFeatures) {
            // Prayer Times Feature
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🕌", style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prayer Times",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Accurate prayer times for your location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Enabled",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Islamic Calendar Feature
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📅", style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Islamic Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hijri dates and Islamic events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Enabled",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Qibla Direction Feature
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🧭", style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Qibla Direction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Find direction to Mecca for prayer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Enabled",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Morocco-specific section
            if (isInMorocco) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🇲🇦", style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Morocco Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Using Muslim World League method (18° Fajr, 17° Isha) - same as Morocco's official calculation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (suggestedMoroccanCities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Suggested Cities:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestedMoroccanCities.take(3).forEach { city ->
                                    AssistChip(
                                        onClick = {
                                            selectedSelection = CitySelection(city.name, "Morocco", "", 0.0, 0.0)
                                            locationMethod = LocationMethod.MANUAL_CITY
                                        },
                                        label = { Text(city.name, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Location Setup
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Location Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Configure how prayer times are calculated for your location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Location Method Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = locationMethod == LocationMethod.GPS,
                            onClick = { locationMethod = LocationMethod.GPS },
                            label = { Text("Use GPS") }
                        )
                        FilterChip(
                            selected = locationMethod == LocationMethod.MANUAL_CITY,
                            onClick = { locationMethod = LocationMethod.MANUAL_CITY },
                            label = { Text("Select City Manually") }
                        )
                    }

                    if (locationMethod == LocationMethod.GPS) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }) {
                                Text("Grant Location Permission")
                            }
                        }
                    }

                    // Manual City Selection (when Method is Manual City)
                    if (locationMethod == LocationMethod.MANUAL_CITY) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Select Your City",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Choose your city for accurate prayer times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // City Selector
                        CitySelector(
                            selectedCity = selectedSelection?.name,
                            selectedCountry = selectedSelection?.country,
                            onCitySelected = { selection ->
                                selectedSelection = selection
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Calculation Method
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Prayer Calculation Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Choose the calculation method for prayer times",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Simple method selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Select ISNA */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ISNA")
                        }

                        OutlinedButton(
                            onClick = { /* Select Muslim World League */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Muslim League")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Complete Button
        Button(
            onClick = {
                // Save Islamic settings
                if (enableIslamicFeatures) {
                    settingsViewModel.updatePrayerTimesEnabled(true)
                    settingsViewModel.updateIslamicCalendarEnabled(true)
                    settingsViewModel.updateQiblaDirectionEnabled(true)

                    // Set Muslim World League method if user is in Morocco (same as Morocco's official method)
                    if (isInMorocco) {
                        settingsViewModel.updateCalculationMethod(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE.id)
                    }

                    // Save location settings using LocationMethod
                    settingsViewModel.updateLocationMethod(locationMethod)
                    settingsViewModel.syncLocationPermissionStatus()
                    if (locationMethod == LocationMethod.GPS) {
                        settingsViewModel.refreshLocation()
                    } else if (selectedSelection != null) {
                        settingsViewModel.updateSelectedCity(selectedSelection!!)
                    }
                } else {
                    settingsViewModel.updatePrayerTimesEnabled(false)
                    settingsViewModel.updateIslamicCalendarEnabled(false)
                    settingsViewModel.updateQiblaDirectionEnabled(false)
                }
                
                // Mark the Islamic features configuration as completed
                viewModel.completeIslamicFeaturesConfiguration()
                onNext()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Complete")
        }
        
    }

    // City Selector Dialog
    if (showCitySelector) {
        CitySelectorDialog(
            onCitySelected = { selection ->
                selectedSelection = selection
                showCitySelector = false
            },
            onDismiss = { showCitySelector = false }
        )
    }
}