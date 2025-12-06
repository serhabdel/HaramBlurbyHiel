package com.hieltech.haramblur.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.ui.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Simplified location permission step - requests permission AND fetches location
 * FIXED: Now actually calls refreshLocation() to get GPS coordinates
 */
@Composable
fun SimpleLocationPermissionStep(
    viewModel: PermissionWizardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsState()
    
    var permissionGranted by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationFetched by remember { mutableStateOf(false) }

    // Derive city name from settings
    val detectedCity = remember(settings.locationLatitude, settings.selectedCityName, settings.locationCity) {
        settings.selectedCityName ?: settings.locationCity ?: 
            if (settings.locationLatitude != null) "Location detected" else null
    }

    val scope = rememberCoroutineScope()
    
    // Check initial permission state and fetch location if already granted
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED

        permissionGranted = fineLocation || coarseLocation
        
        // If permission already granted, fetch location immediately
        if (permissionGranted && settings.locationLatitude == null) {
            isLoadingLocation = true
            settingsViewModel.syncLocationPermissionStatus()
            // Use the suspend version that actually waits!
            val success = settingsViewModel.refreshLocationAndWait()
            isLoadingLocation = false
            locationFetched = success
        } else if (permissionGranted && settings.locationLatitude != null) {
            locationFetched = true
        }
        
        if (permissionGranted) {
            onPermissionResult(true)
        }
    }
    
    // Watch for location updates
    LaunchedEffect(settings.locationLatitude) {
        if (settings.locationLatitude != null) {
            isLoadingLocation = false
            locationFetched = true
        }
    }

    // Permission launcher - FETCH LOCATION IMMEDIATELY when granted
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        permissionGranted = granted
        
        if (granted) {
            // CRITICAL: Actually fetch the location coordinates!
            isLoadingLocation = true
            settingsViewModel.syncLocationPermissionStatus()
            
            // Launch coroutine to wait for location
            scope.launch {
                val success = settingsViewModel.refreshLocationAndWait()
                isLoadingLocation = false
                locationFetched = success
            }
        }
        
        onPermissionResult(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "📍 Location Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "We need location access to provide accurate prayer times for your area.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!permissionGranted) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Location Access")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { onPermissionResult(false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        } else {
            // Permission granted - show status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (locationFetched && settings.locationLatitude != null)
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "📍 Detecting your location...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Please wait a moment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (settings.locationLatitude != null) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Location detected!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        if (detectedCity != null) {
                            Text(
                                text = detectedCity,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Text(
                            text = "Prayer times will be accurate for your location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Permission granted",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Location will be detected automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
