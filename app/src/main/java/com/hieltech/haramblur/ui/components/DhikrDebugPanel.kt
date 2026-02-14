package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.services.DhikrManager
import com.hieltech.haramblur.services.DhikrSystemStatus
import com.hieltech.haramblur.utils.DhikrDisplayMethod
import com.hieltech.haramblur.utils.DhikrPermissionHelper
import kotlinx.coroutines.launch

/**
 * Debug panel for troubleshooting dhikr functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrDebugPanel(
    dhikrManager: DhikrManager,
    permissionHelper: DhikrPermissionHelper,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var systemStatus by remember { mutableStateOf<DhikrSystemStatus?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshStatus() {
        scope.launch {
            isRefreshing = true
            systemStatus = dhikrManager.getSystemStatus()
            isRefreshing = false
        }
    }

    // Refresh status when panel is first shown
    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dhikr Debug Panel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { refreshStatus() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Refresh")
                    }
                }
            }

            // System Status
            systemStatus?.let { status ->
                StatusSection("System Status", status)
            }

            // Permission Status
            PermissionStatusSection(permissionHelper)

            // Test Actions
            TestActionsSection(dhikrManager)

            // Troubleshooting Tips
            TroubleshootingSection()
        }
    }
}

@Composable
private fun StatusSection(title: String, status: DhikrSystemStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            StatusItem("Dhikr Enabled", status.isEnabled)
            StatusItem("Overlay Permission", status.overlayPermissionGranted)
            StatusItem("Notification Permission", status.notificationPermissionGranted)
            StatusItem("Accessibility Service", status.accessibilityServiceRunning)
            StatusItem("Overlay Visible", status.isOverlayVisible)
            StatusItem("Daily Count", status.dailyDhikrCount.toString())
            StatusItem("Current Time Window", status.currentTimeWindow.name)
            StatusItem("Recommended Method", status.recommendedDisplayMethod.name)

            val timeText = if (status.timeUntilNextDhikr > 0) {
                "${status.timeUntilNextDhikr / 1000 / 60} minutes"
            } else {
                "Ready now"
            }
            StatusItem("Time Until Next", timeText)

            Divider()

            Text(
                text = status.statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionStatusSection(permissionHelper: DhikrPermissionHelper) {
    val scope = rememberCoroutineScope()
    var permissionStatus by remember { mutableStateOf(permissionHelper.getPermissionStatus()) }

    fun refreshPermissions() {
        scope.launch {
            permissionStatus = permissionHelper.getPermissionStatus()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(onClick = { refreshPermissions() }) {
                    Text("Check")
                }
            }

            StatusItem("Overlay Permission", permissionStatus.overlayGranted)
            StatusItem("Notification Permission", permissionStatus.notificationGranted)
            StatusItem("Accessibility Enabled", permissionStatus.accessibilityEnabled)
            StatusItem("Can Show Anything", permissionStatus.canShowAnything)
            StatusItem("Preferred Method", permissionStatus.preferredMethod.name)
        }
    }
}

@Composable
private fun TestActionsSection(dhikrManager: DhikrManager) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Test Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            dhikrManager.testDhikrDisplay(DhikrDisplayMethod.OVERLAY)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Overlay")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            dhikrManager.testDhikrDisplay(DhikrDisplayMethod.NOTIFICATION)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Notification")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        dhikrManager.forceShowDhikrNow()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Force Show Dhikr Now")
            }
        }
    }
}

@Composable
private fun TroubleshootingSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Troubleshooting Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Ensure Dhikr is enabled in settings\n" +
                      "• Grant overlay permission for system overlays\n" +
                      "• Enable notification permission for fallback\n" +
                      "• Verify Accessibility Service is running\n" +
                      "• Check current time window (Morning/Evening)\n" +
                      "• Try manual trigger if automatic doesn't work",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusItem(label: String, value: Any) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        val (displayValue, color) = when (value) {
            is Boolean -> {
                if (value) "✓" to MaterialTheme.colorScheme.primary
                else "✗" to MaterialTheme.colorScheme.error
            }
            else -> value.toString() to MaterialTheme.colorScheme.onSurface
        }

        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}