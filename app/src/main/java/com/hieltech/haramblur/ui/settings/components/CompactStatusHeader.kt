package com.hieltech.haramblur.ui.settings.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hieltech.haramblur.data.models.SystemStatus
import com.hieltech.haramblur.ui.components.HapticFeedback

/**
 * Compact status header that replaces the large settings card
 * Displays real-time protection status and detection statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactStatusHeader(
    systemStatus: SystemStatus,
    onStatusClick: () -> Unit,
    onProtectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                HapticFeedback.performLightFeedback(context)
                onStatusClick() 
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with protection status and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Protection status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (systemStatus.protectionEnabled) 
                                    Color(0xFF4CAF50) 
                                else Color(0xFFF44336)
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = if (systemStatus.protectionEnabled) "Protection ON" else "Protection OFF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (systemStatus.protectionEnabled) 
                                Color(0xFF4CAF50) 
                            else Color(0xFFF44336)
                        )
                        Text(
                            text = systemStatus.getStatusSummary(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Protection toggle button
                IconButton(
                    onClick = {
                        HapticFeedback.performMediumFeedback(context)
                        onProtectionToggle()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Toggle Protection",
                        tint = if (systemStatus.protectionEnabled) 
                            Color(0xFF4CAF50) 
                        else Color(0xFFF44336)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Faces detected
                StatisticItem(
                    icon = Icons.Default.Face,
                    value = systemStatus.dailyFacesDetected.toString(),
                    label = "Faces",
                    color = Color(0xFF2196F3)
                )
                
                // Sites blocked
                StatisticItem(
                    icon = Icons.Default.Close,
                    value = systemStatus.dailySitesBlocked.toString(),
                    label = "Sites",
                    color = Color(0xFFFF9800)
                )
                
                // Detection rate
                StatisticItem(
                    icon = Icons.Default.Info,
                    value = String.format("%.1f", systemStatus.getDetectionRate()),
                    label = "Rate/hr",
                    color = Color(0xFF9C27B0)
                )
                
                // Health score
                StatisticItem(
                    icon = Icons.Default.Favorite,
                    value = "${(systemStatus.getOverallHealthScore() * 100).toInt()}%",
                    label = "Health",
                    color = if (systemStatus.getOverallHealthScore() > 0.7f) 
                        Color(0xFF4CAF50) 
                    else Color(0xFFFF9800)
                )
            }
            
            // Health indicators (if there are issues)
            if (systemStatus.needsAttention()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "System needs attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Individual statistic item component
 */
@Composable
private fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Detailed statistics dialog
 */
@Composable
fun DetailedStatisticsDialog(
    systemStatus: SystemStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "System Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Protection status
                StatisticRow(
                    label = "Protection Status",
                    value = if (systemStatus.protectionEnabled) "Active" else "Inactive",
                    color = if (systemStatus.protectionEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                
                // Daily statistics
                StatisticRow(
                    label = "Faces Detected Today",
                    value = systemStatus.dailyFacesDetected.toString(),
                    color = Color(0xFF2196F3)
                )
                
                StatisticRow(
                    label = "Sites Blocked Today",
                    value = systemStatus.dailySitesBlocked.toString(),
                    color = Color(0xFFFF9800)
                )
                
                StatisticRow(
                    label = "Detection Rate",
                    value = "${String.format("%.1f", systemStatus.getDetectionRate())} per hour",
                    color = Color(0xFF9C27B0)
                )
                
                StatisticRow(
                    label = "System Health",
                    value = "${(systemStatus.getOverallHealthScore() * 100).toInt()}%",
                    color = if (systemStatus.getOverallHealthScore() > 0.7f) 
                        Color(0xFF4CAF50) 
                    else Color(0xFFFF9800)
                )
                
                // Service status
                StatisticRow(
                    label = "Accessibility Service",
                    value = if (systemStatus.accessibilityServiceActive) "Active" else "Inactive",
                    color = if (systemStatus.accessibilityServiceActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                
                StatisticRow(
                    label = "Battery Optimized",
                    value = if (systemStatus.batteryOptimized) "Yes" else "No",
                    color = if (systemStatus.batteryOptimized) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                
                // Critical issues
                if (systemStatus.criticalIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Critical Issues:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    systemStatus.criticalIssues.forEach { issue ->
                        Text(
                            text = "• $issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

/**
 * Statistic row for dialog
 */
@Composable
private fun StatisticRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
