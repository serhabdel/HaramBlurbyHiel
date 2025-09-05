package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.LocationAccuracy
import com.hieltech.haramblur.data.LocationMethod
import com.hieltech.haramblur.data.LocationPermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationStatusCard(
    method: LocationMethod,
    permissionStatus: LocationPermissionStatus,
    statusText: String,
    accuracyTier: LocationAccuracy,
    onSwitchToGPS: () -> Unit,
    onSwitchToManual: () -> Unit,
    onRequestPermission: (() -> Unit)? = null,
    onRefreshLocation: (() -> Unit)? = null,
    onClearLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Method chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = method == LocationMethod.GPS,
                    onClick = onSwitchToGPS,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    label = { Text("GPS") }
                )
                FilterChip(
                    selected = method == LocationMethod.MANUAL_CITY,
                    onClick = onSwitchToManual,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    label = { Text("Select City") }
                )
            }

            // Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusIcon: ImageVector
                val tint: Color
                when (permissionStatus) {
                    LocationPermissionStatus.GRANTED -> {
                        statusIcon = Icons.Default.LocationOn
                        tint = MaterialTheme.colorScheme.primary
                    }
                    LocationPermissionStatus.DENIED -> {
                        statusIcon = Icons.Default.Warning
                        tint = MaterialTheme.colorScheme.error
                    }
                    LocationPermissionStatus.UNKNOWN -> {
                        statusIcon = Icons.Default.Warning
                        tint = MaterialTheme.colorScheme.tertiary
                    }
                }
                Icon(statusIcon, contentDescription = null, tint = tint)
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }

            // Accuracy pill for GPS
            if (method == LocationMethod.GPS) {
                val accLabel = when (accuracyTier) {
                    LocationAccuracy.HIGH -> "High accuracy"
                    LocationAccuracy.MEDIUM -> "Medium accuracy"
                    LocationAccuracy.LOW -> "Low accuracy"
                    LocationAccuracy.UNKNOWN -> "Accuracy unknown"
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(accLabel) }
                )
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (method == LocationMethod.GPS) {
                    if (permissionStatus != LocationPermissionStatus.GRANTED && onRequestPermission != null) {
                        OutlinedButton(onClick = onRequestPermission) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Permission")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (onRefreshLocation != null) {
                        Button(onClick = onRefreshLocation) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (onClearLocation != null) {
                        TextButton(onClick = onClearLocation) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}
