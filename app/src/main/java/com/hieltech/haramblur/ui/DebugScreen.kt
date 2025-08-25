package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val debugState by viewModel.debugState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startDebugging()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 Debug & System Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            
            // Quick Actions
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                }
            }
            
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
            
            // Performance Metrics
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚡ Performance Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = debugState.performance.cpuUsage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("CPU Usage: ${(debugState.performance.cpuUsage * 100).toInt()}%")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = debugState.performance.memoryUsage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Memory Usage: ${(debugState.performance.memoryUsage * 100).toInt()}%")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Frames Processed: ${debugState.performance.framesProcessed}")
                    Text("Frames Skipped: ${debugState.performance.framesSkipped}")
                    Text("Average Processing Time: ${debugState.performance.averageProcessingTime}ms")
                }
            }
            
            // Recent Logs
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📝 Recent Debug Logs",
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
            
            Spacer(modifier = Modifier.height(32.dp))
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
    val recentLogs: List<DebugLog> = emptyList()
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
