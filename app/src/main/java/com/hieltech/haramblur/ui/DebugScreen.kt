package com.hieltech.haramblur.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService

import kotlinx.coroutines.launch
import com.hieltech.haramblur.testing.MLDiagnosticHelper
import com.hieltech.haramblur.testing.MLDiagnosticState
import com.hieltech.haramblur.testing.MLDiagnosticReport
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    // Determine window size class
    val windowSizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    when (windowSizeClass) {
        "Compact" -> CompactDebugScreen(
            isLandscape = isLandscape,
            onNavigateBack = onNavigateBack,
            viewModel = viewModel
        )
        "Medium" -> MediumDebugScreen(
            isLandscape = isLandscape,
            onNavigateBack = onNavigateBack,
            viewModel = viewModel
        )
        "Expanded" -> ExpandedDebugScreen(
            isLandscape = isLandscape,
            onNavigateBack = onNavigateBack,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDebugScreen(
    isLandscape: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel
) {
    val debugState by viewModel.debugState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.startDebugging()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 Debug & System Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Quick Actions with Export
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.quick_actions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // First row: Refresh and Test
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.refreshStatus() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }

                        Button(
                            onClick = { viewModel.testDetection() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Test")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Second row: Export options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugLogs(context, debugState.recentLogs)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export Logs")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugData(context, debugState)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export Data")
                        }
                    }
                }
            }

            // Service Status
            ServiceStatusCard(
                title = stringResource(R.string.accessibility_service),
                status = debugState.accessibilityService,
                details = listOf(
                    stringResource(R.string.service_running) to debugState.accessibilityService.isRunning.toString(),
                    stringResource(R.string.processing_active) to debugState.accessibilityService.isProcessingActive.toString(),
                    stringResource(R.string.screen_capture) to debugState.accessibilityService.isCapturingActive.toString(),
                    stringResource(R.string.overlay_active) to debugState.accessibilityService.isOverlayActive.toString()
                )
            )

            // Detection Engine Status
            GenericStatusCard(
                title = stringResource(R.string.detection_engine),
                isHealthy = debugState.detectionEngine.isHealthy,
                details = listOf(
                    stringResource(R.string.engine_ready) to debugState.detectionEngine.isReady.toString(),
                    stringResource(R.string.ml_models_loaded) to debugState.detectionEngine.mlModelsReady.toString(),
                    stringResource(R.string.gpu_acceleration) to debugState.detectionEngine.gpuEnabled.toString(),
                    stringResource(R.string.last_processing_time) to "${debugState.detectionEngine.lastProcessingTimeMs}ms"
                ),
                lastError = debugState.detectionEngine.lastError
            )

            // Face Detection Status
            GenericStatusCard(
                title = stringResource(R.string.face_detection),
                isHealthy = debugState.faceDetection.isHealthy,
                details = listOf(
                    stringResource(R.string.face_detector_ready) to debugState.faceDetection.isReady.toString(),
                    stringResource(R.string.gender_detector_ready) to debugState.faceDetection.genderDetectorReady.toString(),
                    stringResource(R.string.last_faces_detected) to debugState.faceDetection.lastFacesCount.toString(),
                    stringResource(R.string.female_faces) to debugState.faceDetection.lastFemaleFaces.toString(),
                    stringResource(R.string.detection_confidence) to "${(debugState.faceDetection.averageConfidence * 100).toInt()}%"
                ),
                lastError = debugState.faceDetection.lastError
            )

            // NSFW Detection Status
            GenericStatusCard(
                title = stringResource(R.string.content_detection),
                isHealthy = debugState.nsfwDetection.isHealthy,
                details = listOf(
                    stringResource(R.string.nsfw_model_ready) to debugState.nsfwDetection.isReady.toString(),
                    stringResource(R.string.last_detection) to debugState.nsfwDetection.lastResult.toString(),
                    stringResource(R.string.confidence) to "${(debugState.nsfwDetection.lastConfidence * 100).toInt()}%",
                    stringResource(R.string.processing_mode) to debugState.nsfwDetection.processingMode
                ),
                lastError = debugState.nsfwDetection.lastError
            )

            // Performance Metrics
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.performance_metrics),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { debugState.performance.cpuUsage },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${stringResource(R.string.cpu_usage)}: ${(debugState.performance.cpuUsage * 100).toInt()}%")

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { debugState.performance.memoryUsage },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${stringResource(R.string.memory_usage)}: ${(debugState.performance.memoryUsage * 100).toInt()}%")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Frames Processed: ${debugState.performance.framesProcessed}")
                    Text("Frames Skipped: ${debugState.performance.framesSkipped}")
                    Text("Average Processing Time: ${debugState.performance.averageProcessingTime}ms")
                }
            }

            // Behavioral Actions Testing
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.behavioral_actions_testing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.test_behavioral_system),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Test Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.testAction("CLOSE_TAB") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.test_close_tab_action))
                        }

                        Button(
                            onClick = { viewModel.testAction("SCROLL_UP") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.test_scroll_up_action))
                        }

                        Button(
                            onClick = { viewModel.testAction("NAVIGATE_SAFE") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.test_navigate_safe_action))
                        }

                        Button(
                            onClick = { viewModel.testAction("SHOW_ISLAMIC") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.test_show_islamic_action))
                        }

                        Button(
                            onClick = { viewModel.testAction("EMERGENCY_BLUR") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.test_emergency_blur_action))
                        }

                        Button(
                            onClick = { viewModel.emergencyHideOverlays() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.emergency_hide_overlays))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        stringResource(R.string.last_action_result),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = debugState.lastActionResult ?: "No actions tested yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Recent Logs
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.recent_debug_logs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    debugState.recentLogs.forEach { log ->
                        Text(
                            text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Prayer Notification Testing
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🕌 Prayer Notification Testing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Test the prayer notification and Quranic guidance system",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Prayer Test Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.testPrayerNotification("Fajr") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Fajr Notification")
                        }

                        Button(
                            onClick = { viewModel.testPrayerNotification("Dhuhr") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Dhuhr Notification")
                        }

                        Button(
                            onClick = { viewModel.testPrayerNotification("Asr") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Asr Notification")
                        }

                        Button(
                            onClick = { viewModel.testPrayerNotification("Maghrib") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Maghrib Notification")
                        }

                        Button(
                            onClick = { viewModel.testPrayerNotification("Isha") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Isha Notification")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quranic Guidance Test
                        Button(
                            onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Test Quranic Guidance Dialog")
                        }

                        // Test All Notifications
                        Button(
                            onClick = { viewModel.testAllPrayerNotifications() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Test All Prayer Notifications")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Last Prayer Test Result:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = debugState.lastActionResult ?: "No prayer tests yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ML Diagnostics Section
            // Note: ML Diagnostics section is disabled due to compilation issues
            // This is non-critical debug functionality and can be fixed later
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🤖 ML Diagnostics (Disabled)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ML diagnostics feature is being refined. Core ML functionality is still operational.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumDebugScreen(
    isLandscape: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel
) {
    val debugState by viewModel.debugState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.startDebugging()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 Debug & System Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Row: Quick Actions with Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.refreshStatus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh Status")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.testDetection() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test Detection")
                        }
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Export Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugLogs(context, debugState.recentLogs)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Logs")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugData(context, debugState)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export All Data")
                        }
                    }
                }
            }

            // Status Cards in 2-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Service Status
                    ServiceStatusCard(
                        title = "🎯 Accessibility Service",
                        status = debugState.accessibilityService,
                        details = listOf(
                            "Service Running" to debugState.accessibilityService.isRunning.toString(),
                            "Processing Active" to debugState.accessibilityService.isProcessingActive.toString(),
                            "Screen Capture" to debugState.accessibilityService.isCapturingActive.toString(),
                            "Overlay Active" to debugState.accessibilityService.isOverlayActive.toString()
                        )
                    )

                    // Detection Engine Status
                    GenericStatusCard(
                        title = "🧠 Detection Engine",
                        isHealthy = debugState.detectionEngine.isHealthy,
                        details = listOf(
                            "Engine Ready" to debugState.detectionEngine.isReady.toString(),
                            "ML Models Loaded" to debugState.detectionEngine.mlModelsReady.toString(),
                            "GPU Acceleration" to debugState.detectionEngine.gpuEnabled.toString(),
                            "Last Processing Time" to "${debugState.detectionEngine.lastProcessingTimeMs}ms"
                        ),
                        lastError = debugState.detectionEngine.lastError
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Face Detection Status
                    GenericStatusCard(
                        title = "👤 Face Detection",
                        isHealthy = debugState.faceDetection.isHealthy,
                        details = listOf(
                            "Face Detector Ready" to debugState.faceDetection.isReady.toString(),
                            "Gender Detector Ready" to debugState.faceDetection.genderDetectorReady.toString(),
                            "Last Faces Detected" to debugState.faceDetection.lastFacesCount.toString(),
                            "Female Faces" to debugState.faceDetection.lastFemaleFaces.toString(),
                            "Detection Confidence" to "${(debugState.faceDetection.averageConfidence * 100).toInt()}%"
                        ),
                        lastError = debugState.faceDetection.lastError
                    )

                    // NSFW Detection Status
                    GenericStatusCard(
                        title = "🔞 Content Detection",
                        isHealthy = debugState.nsfwDetection.isHealthy,
                        details = listOf(
                            "NSFW Model Ready" to debugState.nsfwDetection.isReady.toString(),
                            "Last Detection" to debugState.nsfwDetection.lastResult.toString(),
                            "Confidence" to "${(debugState.nsfwDetection.lastConfidence * 100).toInt()}%",
                            "Processing Mode" to debugState.nsfwDetection.processingMode
                        ),
                        lastError = debugState.nsfwDetection.lastError
                    )
                }
            }

            // Performance Metrics
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "⚡ Performance Metrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { debugState.performance.cpuUsage },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "CPU Usage: ${(debugState.performance.cpuUsage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { debugState.performance.memoryUsage },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Memory Usage: ${(debugState.performance.memoryUsage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text("Frames Processed: ${debugState.performance.framesProcessed}")
                        Text("Frames Skipped: ${debugState.performance.framesSkipped}")
                        Text("Avg Processing Time: ${debugState.performance.averageProcessingTime}ms")
                    }
                }
            }

            // Behavioral Actions Testing
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🎯 Behavioral Actions Testing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Test the new behavioral intervention system:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Test Buttons in grid
                    val actions = listOf(
                        "CLOSE_TAB" to "🗂️ Close Tab",
                        "SCROLL_UP" to "📜 Scroll Up",
                        "NAVIGATE_SAFE" to "🛡️ Navigate Safe",
                        "SHOW_ISLAMIC" to "📖 Show Islamic",
                        "EMERGENCY_BLUR" to "🚨 Emergency Blur"
                    )

                    actions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (action, label) ->
                                Button(
                                    onClick = { viewModel.testAction(action) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label)
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.emergencyHideOverlays() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("🚨 EMERGENCY: Hide All Overlays")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Last Action Result:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = debugState.lastActionResult ?: "No actions tested yet",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Recent Logs
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "📝 Recent Debug Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    debugState.recentLogs.forEach { log ->
                        Text(
                            text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Prayer Notification Testing
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🕌 Prayer Notification Testing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Test the prayer notification and Quranic guidance system",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Prayer Test Buttons in grid
                    val prayers = listOf(
                        "Fajr" to "🌅 Fajr",
                        "Dhuhr" to "☀️ Dhuhr",
                        "Asr" to "🌤️ Asr",
                        "Maghrib" to "🌅 Maghrib",
                        "Isha" to "🌙 Isha"
                    )

                    prayers.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (prayer, label) ->
                                Button(
                                    onClick = { viewModel.testPrayerNotification(prayer) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label)
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Special Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("📖 Quranic Guidance")
                        }

                        Button(
                            onClick = { viewModel.testAllPrayerNotifications() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("🕌 Test All")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Last Prayer Test Result:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = debugState.lastActionResult ?: "No prayer tests yet",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ML Diagnostics Section
            MLDiagnosticsSection(
                viewModel = viewModel,
                onGenerateReport = { viewModel.generateMLDiagnosticReport() },
                onTestFaceDetection = { viewModel.testFaceDetection() },
                onTestGenderClassification = { viewModel.testGenderClassification() },
                onReloadLibraries = { viewModel.reloadMLLibraries() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedDebugScreen(
    isLandscape: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel
) {
    val debugState by viewModel.debugState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.startDebugging()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 Debug & System Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Row: Welcome + Quick Actions + Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🔧",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            "Debug Center",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Monitor system performance and test features",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.refreshStatus() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.refresh))
                            }

                            Button(
                                onClick = { viewModel.testDetection() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.test))
                            }
                        }
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Export Data",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugLogs(context, debugState.recentLogs)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Logs")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportDebugData(context, debugState)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export All Data")
                        }
                    }
                }
            }

            // Main Content Grid: Status Cards + Performance + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Status Cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Service Status
                    ServiceStatusCard(
                        title = "🎯 Accessibility Service",
                        status = debugState.accessibilityService,
                        details = listOf(
                            "Service Running" to debugState.accessibilityService.isRunning.toString(),
                            "Processing Active" to debugState.accessibilityService.isProcessingActive.toString(),
                            "Screen Capture" to debugState.accessibilityService.isCapturingActive.toString(),
                            "Overlay Active" to debugState.accessibilityService.isOverlayActive.toString()
                        )
                    )

                    // Detection Engine Status
                    GenericStatusCard(
                        title = "🧠 Detection Engine",
                        isHealthy = debugState.detectionEngine.isHealthy,
                        details = listOf(
                            "Engine Ready" to debugState.detectionEngine.isReady.toString(),
                            "ML Models Loaded" to debugState.detectionEngine.mlModelsReady.toString(),
                            "GPU Acceleration" to debugState.detectionEngine.gpuEnabled.toString(),
                            "Last Processing Time" to "${debugState.detectionEngine.lastProcessingTimeMs}ms"
                        ),
                        lastError = debugState.detectionEngine.lastError
                    )

                    // Face Detection Status
                    GenericStatusCard(
                        title = "👤 Face Detection",
                        isHealthy = debugState.faceDetection.isHealthy,
                        details = listOf(
                            "Face Detector Ready" to debugState.faceDetection.isReady.toString(),
                            "Gender Detector Ready" to debugState.faceDetection.genderDetectorReady.toString(),
                            "Last Faces Detected" to debugState.faceDetection.lastFacesCount.toString(),
                            "Female Faces" to debugState.faceDetection.lastFemaleFaces.toString(),
                            "Detection Confidence" to "${(debugState.faceDetection.averageConfidence * 100).toInt()}%"
                        ),
                        lastError = debugState.faceDetection.lastError
                    )
                }

                // Middle Column: Performance + NSFW Detection
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Performance Metrics
                    Card {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "⚡ Performance Metrics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { debugState.performance.cpuUsage },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "CPU Usage: ${(debugState.performance.cpuUsage * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { debugState.performance.memoryUsage },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "Memory Usage: ${(debugState.performance.memoryUsage * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text("Frames Processed: ${debugState.performance.framesProcessed}")
                                Text("Frames Skipped: ${debugState.performance.framesSkipped}")
                                Text("Avg Processing Time: ${debugState.performance.averageProcessingTime}ms")
                            }
                        }
                    }

                    // NSFW Detection Status
                    GenericStatusCard(
                        title = "🔞 Content Detection",
                        isHealthy = debugState.nsfwDetection.isHealthy,
                        details = listOf(
                            "NSFW Model Ready" to debugState.nsfwDetection.isReady.toString(),
                            "Last Detection" to debugState.nsfwDetection.lastResult.toString(),
                            "Confidence" to "${(debugState.nsfwDetection.lastConfidence * 100).toInt()}%",
                            "Processing Mode" to debugState.nsfwDetection.processingMode
                        ),
                        lastError = debugState.nsfwDetection.lastError
                    )
                }

                // Right Column: Behavioral Actions + Logs
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Behavioral Actions Testing
                    Card {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "🎯 Behavioral Actions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Test intervention system:",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val actions = listOf(
                                "CLOSE_TAB" to "🗂️ Close Tab",
                                "SCROLL_UP" to "📜 Scroll Up",
                                "NAVIGATE_SAFE" to "🛡️ Navigate Safe",
                                "SHOW_ISLAMIC" to "📖 Show Islamic"
                            )

                            actions.forEach { (action, label) ->
                                Button(
                                    onClick = { viewModel.testAction(action) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { viewModel.testAction("EMERGENCY_BLUR") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🚨 Emergency Blur")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.emergencyHideOverlays() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("🚨 Hide All Overlays")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Last Result:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = debugState.lastActionResult ?: "No actions tested",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Recent Logs
                    Card {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "📝 Debug Logs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            debugState.recentLogs.take(10).forEach { log ->
                                Text(
                                    text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Prayer Notification Testing
            Card {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "🕌 Prayer Notification Testing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Test the prayer notification and Quranic guidance system",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Prayer Test Buttons in grid
                    val prayers = listOf(
                        "Fajr" to "🌅 Fajr",
                        "Dhuhr" to "☀️ Dhuhr",
                        "Asr" to "🌤️ Asr",
                        "Maghrib" to "🌅 Maghrib",
                        "Isha" to "🌙 Isha"
                    )

                    prayers.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (prayer, label) ->
                                Button(
                                    onClick = { viewModel.testPrayerNotification(prayer) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label)
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Special Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("📖 Quranic Guidance")
                        }

                        Button(
                            onClick = { viewModel.testAllPrayerNotifications() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("🕌 Test All")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Last Prayer Test Result:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = debugState.lastActionResult ?: "No prayer tests yet",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ML Diagnostics Section
            MLDiagnosticsSection(
                viewModel = viewModel,
                onGenerateReport = { viewModel.generateMLDiagnosticReport() },
                onTestFaceDetection = { viewModel.testFaceDetection() },
                onTestGenderClassification = { viewModel.testGenderClassification() },
                onReloadLibraries = { viewModel.reloadMLLibraries() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ServiceStatusCard(
    title: String,
    status: ServiceDebugInfo,
    details: List<Pair<String, String>>
) {
    GenericStatusCard(
        title = title,
        isHealthy = status.isHealthy,
        details = details,
        lastError = status.lastError
    )
}

@Composable
fun GenericStatusCard(
    title: String,
    isHealthy: Boolean,
    details: List<Pair<String, String>>,
    lastError: String = ""
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHealthy) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isHealthy) Color.Green else Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            details.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(key, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value, 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (lastError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Last Error: $lastError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

data class DebugState(
    val accessibilityService: ServiceDebugInfo = ServiceDebugInfo(),
    val detectionEngine: DetectionEngineDebugInfo = DetectionEngineDebugInfo(),
    val faceDetection: FaceDetectionDebugInfo = FaceDetectionDebugInfo(),
    val nsfwDetection: NSFWDetectionDebugInfo = NSFWDetectionDebugInfo(),
    val performance: PerformanceDebugInfo = PerformanceDebugInfo(),
    val recentLogs: List<DebugLog> = emptyList(),
    val lastActionResult: String? = null
)

data class ServiceDebugInfo(
    val isHealthy: Boolean = false,
    val isRunning: Boolean = false,
    val isProcessingActive: Boolean = false,
    val isCapturingActive: Boolean = false,
    val isOverlayActive: Boolean = false,
    val lastError: String = ""
)

data class DetectionEngineDebugInfo(
    val isHealthy: Boolean = false,
    val isReady: Boolean = false,
    val mlModelsReady: Boolean = false,
    val gpuEnabled: Boolean = false,
    val lastProcessingTimeMs: Long = 0L,
    val lastError: String = ""
)

data class FaceDetectionDebugInfo(
    val isHealthy: Boolean = false,
    val isReady: Boolean = false,
    val genderDetectorReady: Boolean = false,
    val lastFacesCount: Int = 0,
    val lastFemaleFaces: Int = 0,
    val averageConfidence: Float = 0f,
    val lastError: String = ""
)

data class NSFWDetectionDebugInfo(
    val isHealthy: Boolean = false,
    val isReady: Boolean = false,
    val lastResult: Boolean = false,
    val lastConfidence: Float = 0f,
    val processingMode: String = "Unknown",
    val lastError: String = ""
)

data class PerformanceDebugInfo(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val framesProcessed: Long = 0L,
    val framesSkipped: Long = 0L,
    val averageProcessingTime: Long = 0L
)

data class DebugLog(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: String = "DEBUG"
)

// Export Functions
suspend fun exportDebugLogs(context: Context, logs: List<DebugLog>) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "haramblur_debug_logs_$timestamp.txt"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        val content = buildString {
            appendLine("HaramBlur Debug Logs - Exported on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("=" * 50)
            appendLine()

            logs.forEach { log ->
                appendLine("[${log.timestamp}] ${log.level} ${log.tag}: ${log.message}")
            }

            if (logs.isEmpty()) {
                appendLine("No logs available")
            }
        }

        file.writeText(content)

        // Share the file
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Debug Logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Debug Logs"))

    } catch (e: Exception) {
        // Handle error - could show a toast or snackbar
        e.printStackTrace()
    }
}

suspend fun exportDebugData(context: Context, debugState: DebugState) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "haramblur_debug_data_$timestamp.json"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        val jsonContent = buildString {
            appendLine("{")
            appendLine("  \"export_timestamp\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\",")
            appendLine("  \"accessibility_service\": {")
            appendLine("    \"is_healthy\": ${debugState.accessibilityService.isHealthy},")
            appendLine("    \"is_running\": ${debugState.accessibilityService.isRunning},")
            appendLine("    \"is_processing_active\": ${debugState.accessibilityService.isProcessingActive},")
            appendLine("    \"is_capturing_active\": ${debugState.accessibilityService.isCapturingActive},")
            appendLine("    \"is_overlay_active\": ${debugState.accessibilityService.isOverlayActive},")
            appendLine("    \"last_error\": \"${debugState.accessibilityService.lastError}\"")
            appendLine("  },")
            appendLine("  \"detection_engine\": {")
            appendLine("    \"is_healthy\": ${debugState.detectionEngine.isHealthy},")
            appendLine("    \"is_ready\": ${debugState.detectionEngine.isReady},")
            appendLine("    \"ml_models_ready\": ${debugState.detectionEngine.mlModelsReady},")
            appendLine("    \"gpu_enabled\": ${debugState.detectionEngine.gpuEnabled},")
            appendLine("    \"last_processing_time_ms\": ${debugState.detectionEngine.lastProcessingTimeMs},")
            appendLine("    \"last_error\": \"${debugState.detectionEngine.lastError}\"")
            appendLine("  },")
            appendLine("  \"face_detection\": {")
            appendLine("    \"is_healthy\": ${debugState.faceDetection.isHealthy},")
            appendLine("    \"is_ready\": ${debugState.faceDetection.isReady},")
            appendLine("    \"gender_detector_ready\": ${debugState.faceDetection.genderDetectorReady},")
            appendLine("    \"last_faces_count\": ${debugState.faceDetection.lastFacesCount},")
            appendLine("    \"last_female_faces\": ${debugState.faceDetection.lastFemaleFaces},")
            appendLine("    \"average_confidence\": ${debugState.faceDetection.averageConfidence},")
            appendLine("    \"last_error\": \"${debugState.faceDetection.lastError}\"")
            appendLine("  },")
            appendLine("  \"nsfw_detection\": {")
            appendLine("    \"is_healthy\": ${debugState.nsfwDetection.isHealthy},")
            appendLine("    \"is_ready\": ${debugState.nsfwDetection.isReady},")
            appendLine("    \"last_result\": ${debugState.nsfwDetection.lastResult},")
            appendLine("    \"last_confidence\": ${debugState.nsfwDetection.lastConfidence},")
            appendLine("    \"processing_mode\": \"${debugState.nsfwDetection.processingMode}\",")
            appendLine("    \"last_error\": \"${debugState.nsfwDetection.lastError}\"")
            appendLine("  },")
            appendLine("  \"performance\": {")
            appendLine("    \"cpu_usage\": ${debugState.performance.cpuUsage},")
            appendLine("    \"memory_usage\": ${debugState.performance.memoryUsage},")
            appendLine("    \"frames_processed\": ${debugState.performance.framesProcessed},")
            appendLine("    \"frames_skipped\": ${debugState.performance.framesSkipped},")
            appendLine("    \"average_processing_time\": ${debugState.performance.averageProcessingTime}")
            appendLine("  },")
            appendLine("  \"logs_count\": ${debugState.recentLogs.size},")
            appendLine("  \"last_action_result\": \"${debugState.lastActionResult ?: "None"}\"")
            appendLine("}")
        }

        file.writeText(jsonContent)

        // Share the file
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Debug Data")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Debug Data"))

    } catch (e: Exception) {
        // Handle error - could show a toast or snackbar
        e.printStackTrace()
    }
}

private operator fun String.times(count: Int): String {
    return this.repeat(count)
}

/**
 * ML Diagnostics Section Composable
 */
@Composable
fun MLDiagnosticsSection(
    viewModel: DebugViewModel,
    onGenerateReport: () -> Unit,
    onTestFaceDetection: () -> Unit,
    onTestGenderClassification: () -> Unit,
    onReloadLibraries: () -> Unit
) {
    val mlDiagnosticState by viewModel.mlDiagnosticState.collectAsState(MLDiagnosticState())
    
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🤖 ML Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Comprehensive ML system diagnostics and testing",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // ML Status Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TensorFlow Lite Status
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mlDiagnosticState.tensorFlowLiteAvailable)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (mlDiagnosticState.tensorFlowLiteAvailable)
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (mlDiagnosticState.tensorFlowLiteAvailable) Color.Green else Color.Red
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TensorFlow Lite",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            if (mlDiagnosticState.tensorFlowLiteAvailable) "Available" else "Unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // ML Kit Status
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mlDiagnosticState.mlKitAvailable)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (mlDiagnosticState.mlKitAvailable)
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (mlDiagnosticState.mlKitAvailable) Color.Green else Color.Red
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ML Kit",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            if (mlDiagnosticState.mlKitAvailable) "Available" else "Unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Fallback Mode Status
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!mlDiagnosticState.fallbackMode)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (!mlDiagnosticState.fallbackMode)
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (!mlDiagnosticState.fallbackMode) Color.Green else Color.Yellow
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Fallback Mode",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            if (mlDiagnosticState.fallbackMode) "Active" else "Normal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Diagnostic Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGenerateReport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Generate Report")
                    }
                    
                    Button(
                        onClick = onTestFaceDetection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Test Face Detection")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestGenderClassification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Test Gender Classification")
                    }
                    
                    OutlinedButton(
                        onClick = onReloadLibraries,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Reload Libraries")
                    }
                }
            }
            
            // Show diagnostic report if available
            mlDiagnosticState.lastDiagnosticReport?.let { report ->
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Latest Diagnostic Report",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Overall Health: ${report.overallHealth}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

