package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.hieltech.haramblur.data.prayer.PrayerCalculationMethod
import com.hieltech.haramblur.utils.MoroccanLocationHelper
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

/**
 * Islamic Features Onboarding Step
 * Allows users to configure Islamic features during initial setup
 * IMPROVED UX: Auto-detects location if permission granted, adds Dhikr config
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
    var showManualCitySelector by remember { mutableStateOf(false) }
    var selectedSelection by remember { mutableStateOf<CitySelection?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    // Dhikr settings
    var enableDhikrReminders by remember { mutableStateOf(settings.dhikrEnabled) }
    var dhikrIntervalMinutes by remember { mutableStateOf(settings.dhikrIntervalMinutes.toFloat()) }

    val context = LocalContext.current
    val moroccanLocationHelper = remember {
        EntryPointAccessors.fromApplication(context, MoroccanLocationHelperEntryPoint::class.java)
            .getMoroccanLocationHelper()
    }

    // Check if location permission is already granted
    val hasLocationPermission = remember(Unit) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    // Location should already be fetched in SimpleLocationPermissionStep
    // Only show loading briefly if location is still being fetched
    LaunchedEffect(settings.locationLatitude) {
        if (settings.locationLatitude != null) {
            isLoadingLocation = false
        }
    }

    // Morocco detection
    val isInMorocco = remember(settings.locationLatitude, settings.locationLongitude) {
        settings.locationLatitude?.let { lat ->
            settings.locationLongitude?.let { lon ->
                moroccanLocationHelper.isInMorocco(lat, lon)
            }
        } ?: false
    }

    // Get closest Moroccan city name
    val detectedCityName = remember(settings.locationLatitude, settings.locationLongitude, isInMorocco) {
        if (isInMorocco && settings.locationLatitude != null && settings.locationLongitude != null) {
            val cities = moroccanLocationHelper.getSuggestedMoroccanCities(
                settings.locationLatitude!!,
                settings.locationLongitude!!
            )
            cities.firstOrNull()?.name ?: "Morocco"
        } else if (settings.selectedCityName != null) {
            "${settings.selectedCityName}, ${settings.selectedCountry ?: ""}"
        } else if (settings.locationLatitude != null) {
            "Location detected"
        } else {
            null
        }
    }

    // Has valid location
    val hasValidLocation = settings.locationLatitude != null && settings.locationLongitude != null

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            isLoadingLocation = true
            settingsViewModel.syncLocationPermissionStatus()
            settingsViewModel.refreshLocation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🕌 Islamic Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Configure prayer times, dhikr reminders, and spiritual features",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ============ LOCATION STATUS ============
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasValidLocation) 
                    Color(0xFF4CAF50).copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (hasValidLocation) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasValidLocation) "📍 Your Location" else "📍 Location Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (isLoadingLocation) {
                            Text(
                                text = "Detecting your location...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (hasValidLocation && detectedCityName != null) {
                            Text(
                                text = detectedCityName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                            if (isInMorocco) {
                                Text(
                                    text = "🇲🇦 Using Morocco Ministry prayer times",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "No location set - prayer times won't be accurate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    if (isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (hasValidLocation) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Location set",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasValidLocation || !hasLocationPermission) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use GPS")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                isLoadingLocation = true
                                settingsViewModel.refreshLocation()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showManualCitySelector = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change City")
                    }
                }
            }
        }

        // ============ DHIKR REMINDERS ============
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📿", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text(
                                text = "Dhikr Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Spiritual reminders throughout the day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = enableDhikrReminders,
                        onCheckedChange = { enableDhikrReminders = it }
                    )
                }
                
                if (enableDhikrReminders) {
                    Divider()
                    
                    Text(
                        text = "Reminder Frequency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Frequency selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30, 60).forEach { minutes ->
                            FilterChip(
                                selected = dhikrIntervalMinutes.toInt() == minutes,
                                onClick = { dhikrIntervalMinutes = minutes.toFloat() },
                                label = { 
                                    Text(
                                        if (minutes == 60) "1 hour" else "$minutes min",
                                        style = MaterialTheme.typography.bodySmall
                                    ) 
                                }
                            )
                        }
                    }
                    
                    Text(
                        text = "You'll receive dhikr notifications every ${dhikrIntervalMinutes.toInt()} minutes during active hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ============ FEATURES PREVIEW ============
        if (enableIslamicFeatures) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Features Enabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("🕌 Prayer Times") })
                        AssistChip(onClick = {}, label = { Text("📅 Hijri Calendar") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("🧭 Qibla Direction") })
                        if (enableDhikrReminders) {
                            AssistChip(onClick = {}, label = { Text("📿 Dhikr") })
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
                    
                    // Save Dhikr settings
                    settingsViewModel.updateDhikrEnabled(enableDhikrReminders)
                    if (enableDhikrReminders) {
                        settingsViewModel.updateDhikrInterval(dhikrIntervalMinutes.toInt())
                    }

                    // Set Morocco Ministry method if user is in Morocco
                    if (isInMorocco) {
                        settingsViewModel.updateCalculationMethod(PrayerCalculationMethod.MOROCCO_MINISTRY.id)
                    }

                    // Use GPS if we have location, otherwise manual
                    if (hasValidLocation) {
                        settingsViewModel.updateLocationMethod(LocationMethod.GPS)
                    }
                    
                    // Save manual city if selected
                    if (selectedSelection != null) {
                        settingsViewModel.updateSelectedCity(selectedSelection!!)
                        settingsViewModel.updateLocationMethod(LocationMethod.MANUAL_CITY)
                    }
                } else {
                    settingsViewModel.updatePrayerTimesEnabled(false)
                    settingsViewModel.updateIslamicCalendarEnabled(false)
                    settingsViewModel.updateQiblaDirectionEnabled(false)
                }
                
                viewModel.completeIslamicFeaturesConfiguration()
                onNext()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasValidLocation || selectedSelection != null || !enableIslamicFeatures
        ) {
            Text(if (hasValidLocation || !enableIslamicFeatures) "Complete Setup" else "Select a Location First")
        }
        
        // Skip option
        if (!hasValidLocation && enableIslamicFeatures) {
            TextButton(onClick = {
                viewModel.completeIslamicFeaturesConfiguration()
                onSkip()
            }) {
                Text("Skip for now (configure later in Settings)")
            }
        }
    }

    // City Selector Dialog
    if (showManualCitySelector) {
        CitySelectorDialog(
            onCitySelected = { selection ->
                selectedSelection = selection
                showManualCitySelector = false
            },
            onDismiss = { showManualCitySelector = false }
        )
    }
}