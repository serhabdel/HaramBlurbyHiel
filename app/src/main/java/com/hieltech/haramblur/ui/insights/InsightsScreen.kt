package com.hieltech.haramblur.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ml.MLModelManager

/**
 * Rich Insights/Stats screen with comprehensive app statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.insightsState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            InsightsHeader()
        }
        
        // System Health Overview
        item {
            SystemHealthSection(
                mlStatus = state.mlStatus,
                performanceState = state.performanceState.name
            )
        }
        
        // ML Model Health Details (NEW)
        item {
            MLModelHealthSection(mlStatus = state.mlStatus)
        }
        
        // Detection Trends Chart (NEW)
        item {
            DetectionTrendsChart(trends = state.detectionTrends)
        }
        
        // Gender Confidence Quality (NEW)
        item {
            GenderConfidenceSection(confidenceStats = state.genderConfidenceStats)
        }
        
        // Feature Status
        item {
            FeatureStatusSection(features = state.features)
        }
        
        // Detection Breakdown (NEW)
        item {
            DetectionBreakdownSection(detailedStats = state.detailedStats)
        }
        
        // Protected Categories
        item {
            ProtectedCategoriesSection(
                categories = state.protectedCategories,
                totalAppsProtected = state.protectedAppsCount
            )
        }
        
        // Stats Summary
        item {
            StatsSummarySection(
                detectionsToday = state.totalDetectionsToday,
                detectionsWeek = state.totalDetectionsWeek,
                avgProcessingTime = state.avgProcessingTimeMs,
                successRate = state.successRate,
                blockedApps = state.blockedAppsCount,
                blockedSites = state.blockedSitesCount
            )
        }
        
        // Settings Quick View
        item {
            SettingsQuickViewSection(settings = state.currentSettings)
        }
        
        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InsightsHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📊",
            fontSize = 40.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Insights & Stats",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Monitor your protection status",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SystemHealthSection(
    mlStatus: MLModelManager.MLStatus,
    performanceState: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ML Status
                HealthStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "🤖",
                    title = "ML Models",
                    status = mlStatus.overallHealth.name.replace("_", " "),
                    isHealthy = mlStatus.overallHealth == MLModelManager.MLHealth.HEALTHY ||
                               mlStatus.overallHealth == MLModelManager.MLHealth.REDUCED_PERFORMANCE,
                    statusColor = when (mlStatus.overallHealth) {
                        MLModelManager.MLHealth.HEALTHY -> Color(0xFF4CAF50)
                        MLModelManager.MLHealth.REDUCED_PERFORMANCE -> Color(0xFFFFC107)
                        MLModelManager.MLHealth.DEGRADED -> Color(0xFFFF9800)
                        MLModelManager.MLHealth.CRITICAL -> Color(0xFFF44336)
                        MLModelManager.MLHealth.NOT_INITIALIZED -> Color(0xFF9E9E9E)
                    }
                )
                
                // GPU Status
                HealthStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    title = "GPU",
                    status = if (mlStatus.gpuAccelerationActive) "Active" else "CPU Mode",
                    isHealthy = mlStatus.gpuAccelerationActive,
                    statusColor = if (mlStatus.gpuAccelerationActive) Color(0xFF4CAF50) else Color(0xFFFFC107)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NSFW Model
                HealthStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔍",
                    title = "NSFW Model",
                    status = if (mlStatus.nsfwModelAvailable) "Ready" else "Unavailable",
                    isHealthy = mlStatus.nsfwModelAvailable,
                    statusColor = if (mlStatus.nsfwModelAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                
                // Gender Model
                HealthStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "👤",
                    title = "Gender Model",
                    status = if (mlStatus.genderModelAvailable) "Ready" else "Unavailable",
                    isHealthy = mlStatus.genderModelAvailable,
                    statusColor = if (mlStatus.genderModelAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            // Status message
            if (mlStatus.statusMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = mlStatus.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthStatusCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    status: String,
    isHealthy: Boolean,
    statusColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun FeatureStatusSection(features: InsightsViewModel.FeatureStatusList) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Feature Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            val featureList = listOf(
                features.faceDetection,
                features.nsfwDetection,
                features.gpuAcceleration,
                features.edgeRefinement,
                features.realTimeProcessing,
                features.appSpecificProtection,
                features.smoothAnimations,
                features.hardwareBlur
            )
            
            featureList.chunked(2).forEach { rowFeatures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowFeatures.forEach { feature ->
                        FeatureStatusItem(
                            modifier = Modifier.weight(1f),
                            name = feature.name,
                            isEnabled = feature.isEnabled,
                            description = feature.description
                        )
                    }
                    // Add empty space if odd number of items
                    if (rowFeatures.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureStatusItem(
    modifier: Modifier = Modifier,
    name: String,
    isEnabled: Boolean,
    description: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (isEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetectionBreakdownSection(detailedStats: InsightsViewModel.DetailedDetectionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "👤 Detection Breakdown (Today)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Face detection stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FaceStatCard(
                    modifier = Modifier.weight(1f),
                    icon = "👩",
                    label = "Female",
                    count = detailedStats.femaleFaceCount,
                    color = Color(0xFFE91E63)
                )
                FaceStatCard(
                    modifier = Modifier.weight(1f),
                    icon = "👨",
                    label = "Male",
                    count = detailedStats.maleFaceCount,
                    color = Color(0xFF2196F3)
                )
                FaceStatCard(
                    modifier = Modifier.weight(1f),
                    icon = "❓",
                    label = "Unknown",
                    count = detailedStats.unknownFaceCount,
                    color = Color(0xFF9E9E9E)
                )
            }
            
            // Blur action stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlurActionCard(
                    modifier = Modifier.weight(1f),
                    icon = "🌫️",
                    label = "Blurred",
                    count = detailedStats.facesBlurred,
                    color = Color(0xFF4CAF50)
                )
                BlurActionCard(
                    modifier = Modifier.weight(1f),
                    icon = "⏭️",
                    label = "Skipped",
                    count = detailedStats.facesSkipped,
                    color = Color(0xFFFF9800)
                )
            }
            
            // Blur rate progress
            if (detailedStats.totalFacesDetected > 0) {
                val blurRate = detailedStats.facesBlurred.toFloat() / detailedStats.totalFacesDetected
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Blur Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(blurRate * 100).toInt()}% of faces blurred",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val animatedProgress by animateFloatAsState(
                        targetValue = blurRate,
                        animationSpec = tween(1000),
                        label = "blurProgress"
                    )
                    
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            // NSFW stats
            if (detailedStats.nsfwDetections > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🔞", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "${detailedStats.nsfwDetections} NSFW Detections",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "Avg confidence: ${(detailedStats.avgNsfwConfidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Performance info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📊", fontSize = 18.sp)
                        Text(
                            text = "${detailedStats.framesProcessed}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Frames",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⚡", fontSize = 18.sp)
                        Text(
                            text = "${detailedStats.avgProcessingTime.toInt()}ms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Avg Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🏃", fontSize = 18.sp)
                        Text(
                            text = "${detailedStats.minProcessingTime}ms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🐢", fontSize = 18.sp)
                        Text(
                            text = "${detailedStats.maxProcessingTime}ms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Max",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlurActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProtectedCategoriesSection(
    categories: List<InsightsViewModel.CategoryStatus>,
    totalAppsProtected: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protected Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$totalAppsProtected apps",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        name = category.category.displayName,
                        isProtected = category.isProtected,
                        appCount = category.appCount,
                        icon = when (category.category.name) {
                            "SOCIAL_MEDIA" -> "📱"
                            "BROWSERS" -> "🌐"
                            "MESSAGING" -> "💬"
                            "ENTERTAINMENT" -> "🎬"
                            "DATING" -> "💕"
                            else -> "📦"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    isProtected: Boolean,
    appCount: Int,
    icon: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isProtected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isProtected) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isProtected) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$appCount apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsSummarySection(
    detectionsToday: Int,
    detectionsWeek: Int,
    avgProcessingTime: Double,
    successRate: Float,
    blockedApps: Int,
    blockedSites: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "📅",
                    value = detectionsToday.toString(),
                    label = "Today",
                    color = Color(0xFF2196F3)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "📆",
                    value = detectionsWeek.toString(),
                    label = "This Week",
                    color = Color(0xFF9C27B0)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    value = "${avgProcessingTime.toInt()}ms",
                    label = "Avg Speed",
                    color = Color(0xFF4CAF50)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "✅",
                    value = "${successRate.toInt()}%",
                    label = "Success Rate",
                    color = Color(0xFFFF9800)
                )
            }
            
            // Blocked stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🚫",
                    value = blockedApps.toString(),
                    label = "Blocked Apps",
                    color = Color(0xFFF44336)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🌐",
                    value = blockedSites.toString(),
                    label = "Blocked Sites",
                    color = Color(0xFF607D8B)
                )
            }
            
            // Success rate progress bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Detection Success Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${successRate.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            successRate >= 90f -> Color(0xFF4CAF50)
                            successRate >= 70f -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                
                val animatedProgress by animateFloatAsState(
                    targetValue = successRate / 100f,
                    animationSpec = tween(1000),
                    label = "progress"
                )
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        successRate >= 90f -> Color(0xFF4CAF50)
                        successRate >= 70f -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsQuickViewSection(settings: InsightsViewModel.SettingsSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingQuickItem(
                    modifier = Modifier.weight(1f),
                    icon = "🎯",
                    label = "Quality Mode",
                    value = settings.qualityMode
                )
                SettingQuickItem(
                    modifier = Modifier.weight(1f),
                    icon = "🌫️",
                    label = "Blur Intensity",
                    value = settings.blurIntensity
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingQuickItem(
                    modifier = Modifier.weight(1f),
                    icon = "📡",
                    label = "Sensitivity",
                    value = "${(settings.detectionSensitivity * 100).toInt()}%"
                )
                SettingQuickItem(
                    modifier = Modifier.weight(1f),
                    icon = "⚙️",
                    label = "Processing",
                    value = settings.processingSpeed
                )
            }
            
            // Image downscale indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Image Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(settings.imageDownscaleRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                LinearProgressIndicator(
                    progress = { settings.imageDownscaleRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingQuickItem(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 18.sp)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// NEW SECTIONS: ML Health, Trends Chart, Gender Confidence
// ============================================================

@Composable
private fun MLModelHealthSection(mlStatus: MLModelManager.MLStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ML Model Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        mlStatus.nsfwUsingHeuristics && mlStatus.genderUsingHeuristics -> Color(0xFFFF9800)
                        mlStatus.nsfwUsingHeuristics || mlStatus.genderUsingHeuristics -> Color(0xFFFFC107)
                        else -> Color(0xFF4CAF50)
                    }.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = mlStatus.detectionMode,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when {
                            mlStatus.nsfwUsingHeuristics && mlStatus.genderUsingHeuristics -> Color(0xFFFF9800)
                            mlStatus.nsfwUsingHeuristics || mlStatus.genderUsingHeuristics -> Color(0xFFFFC107)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }
            }
            
            // Model status grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔞",
                    title = "NSFW",
                    isLoaded = mlStatus.nsfwModelAvailable,
                    usingHeuristic = mlStatus.nsfwUsingHeuristics
                )
                ModelStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "👤",
                    title = "Gender",
                    isLoaded = mlStatus.genderModelAvailable,
                    usingHeuristic = mlStatus.genderUsingHeuristics
                )
                ModelStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "👁️",
                    title = "Face",
                    isLoaded = mlStatus.faceDetectionVerified,
                    usingHeuristic = !mlStatus.faceDetectionHealthy && mlStatus.consecutiveEmptyFaceResults > 5,
                    alternateLabels = if (mlStatus.faceDetectionVerified) {
                        if (mlStatus.faceDetectionHealthy) Pair("Working", "Issues") else Pair("Degraded", "Not Working")
                    } else {
                        Pair("Verifying", "Not Verified")
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    title = "GPU",
                    isLoaded = mlStatus.gpuAccelerationActive,
                    usingHeuristic = false,
                    alternateLabels = Pair("Active", "CPU Only")
                )
                // Cache info
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💾", fontSize = 20.sp)
                        Text(
                            text = "${mlStatus.nsfwCacheSize + mlStatus.genderCacheSize}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Warning message if using heuristics
            if (mlStatus.nsfwUsingHeuristics || mlStatus.genderUsingHeuristics) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 16.sp)
                        Text(
                            text = buildString {
                                if (mlStatus.nsfwUsingHeuristics) append("NSFW using skin-tone heuristics. ")
                                if (mlStatus.genderUsingHeuristics) append("Gender using facial heuristics.")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            // Critical warning for face detection issues
            if (mlStatus.consecutiveEmptyFaceResults > 10 || (!mlStatus.faceDetectionVerified && mlStatus.faceDetectionAvailable)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🚨", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (mlStatus.consecutiveEmptyFaceResults > 10) {
                                    "Face detection returning 0 faces (${mlStatus.consecutiveEmptyFaceResults} consecutive)"
                                } else {
                                    "Face detection not yet verified working"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "ML Kit may not be functioning correctly. Try restarting the app.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF44336).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatusCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    isLoaded: Boolean,
    usingHeuristic: Boolean,
    alternateLabels: Pair<String, String>? = null
) {
    val statusText = when {
        alternateLabels != null -> if (isLoaded) alternateLabels.first else alternateLabels.second
        usingHeuristic -> "Heuristic"
        isLoaded -> "Loaded"
        else -> "Unavailable"
    }
    
    val statusColor = when {
        isLoaded && !usingHeuristic -> Color(0xFF4CAF50)
        usingHeuristic -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetectionTrendsChart(trends: List<InsightsViewModel.DetectionTrendPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Detection Trends (24h)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (trends.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📊", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No data yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Bar chart
                val maxDetections = trends.maxOfOrNull { it.totalDetections } ?: 1
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val barWidth = size.width / (trends.size.coerceAtLeast(1) * 1.5f)
                    val spacing = barWidth * 0.5f
                    
                    trends.forEachIndexed { index, point ->
                        val barHeight = if (maxDetections > 0) {
                            (point.totalDetections.toFloat() / maxDetections) * size.height * 0.9f
                        } else 0f
                        
                        val x = index * (barWidth + spacing)
                        val y = size.height - barHeight
                        
                        // Draw bar
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
                
                // Time labels
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayTrends = if (trends.size > 6) {
                        trends.filterIndexed { index, _ -> index % (trends.size / 6) == 0 }
                    } else trends
                    
                    items(displayTrends) { point ->
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val totalDetections = trends.sumOf { it.totalDetections }
                    val avgTime = trends.map { it.avgProcessingTime }.average()
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalDetections.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${avgTime.toInt()}ms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Avg Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trends.maxOfOrNull { it.totalDetections }?.toString() ?: "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Peak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderConfidenceSection(confidenceStats: InsightsViewModel.GenderConfidenceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Gender Detection Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (confidenceStats.totalAnalyzed == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎯", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No faces analyzed yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Confidence distribution bars
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceBar(
                        label = "High (>70%)",
                        percent = confidenceStats.highConfidencePercent,
                        count = confidenceStats.highConfidenceCount,
                        color = Color(0xFF4CAF50)
                    )
                    ConfidenceBar(
                        label = "Medium (40-70%)",
                        percent = confidenceStats.mediumConfidencePercent,
                        count = confidenceStats.mediumConfidenceCount,
                        color = Color(0xFFFFC107)
                    )
                    ConfidenceBar(
                        label = "Low (<40%)",
                        percent = confidenceStats.lowConfidencePercent,
                        count = confidenceStats.lowConfidenceCount,
                        color = Color(0xFFF44336)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Average confidence by gender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ConfidenceChip(
                        icon = "👩",
                        label = "Female",
                        confidence = confidenceStats.avgFemaleConfidence,
                        color = Color(0xFFE91E63)
                    )
                    ConfidenceChip(
                        icon = "👨",
                        label = "Male",
                        confidence = confidenceStats.avgMaleConfidence,
                        color = Color(0xFF2196F3)
                    )
                    ConfidenceChip(
                        icon = "❓",
                        label = "Unknown",
                        confidence = confidenceStats.avgUnknownConfidence,
                        color = Color(0xFF9E9E9E)
                    )
                }
                
                // Total analyzed
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔍 ", fontSize = 16.sp)
                        Text(
                            text = "${confidenceStats.totalAnalyzed} faces analyzed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBar(
    label: String,
    percent: Int,
    count: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percent% ($count)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        val animatedProgress by animateFloatAsState(
            targetValue = percent / 100f,
            animationSpec = tween(1000),
            label = "confidenceProgress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun ConfidenceChip(
    icon: String,
    label: String,
    confidence: Float,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 18.sp)
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
