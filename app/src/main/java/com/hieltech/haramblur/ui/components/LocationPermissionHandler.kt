package com.hieltech.haramblur.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/**
 * Location permission handler for Islamic features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }

    // Check initial permission state
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
        if (permissionGranted) {
            onPermissionGranted()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionGranted = allGranted

        if (allGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
            showPermissionDialog = true
        }
    }

    // Show content if permission granted, otherwise show permission request
    if (permissionGranted) {
        content()
    } else {
        LocationPermissionRequestCard(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onSkip = {
                onPermissionDenied()
                // Still show content but with limited functionality
                permissionGranted = true
            }
        )
    }

    // Show rationale dialog if permission denied
    if (showPermissionDialog) {
        LocationPermissionRationaleDialog(
            onRequestAgain = {
                showPermissionDialog = false
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = {
                showPermissionDialog = false
                onPermissionDenied()
                // Allow app to continue with limited functionality
                permissionGranted = true
            }
        )
    }
}

/**
 * Location permission request card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionRequestCard(
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "To provide accurate prayer times and Islamic calendar for your location, we need access to your location. This information stays on your device and is never shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip for now")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

/**
 * Location permission rationale dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionRationaleDialog(
    onRequestAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Location Access Needed",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Accurate prayer times depend on knowing your location. Without location access, prayer times will be calculated for Mecca. You can grant permission later in Settings > Islamic.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onRequestAgain) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Use Default Location")
            }
        }
    )
}

/**
 * Islamic features with location permission wrapper
 */
@Composable
fun IslamicFeaturesWithPermission(
    content: @Composable () -> Unit
) {
    LocationPermissionHandler(
        onPermissionGranted = {
            // Permission granted - full functionality
        },
        onPermissionDenied = {
            // Permission denied - limited functionality
        }
    ) {
        content()
    }
}