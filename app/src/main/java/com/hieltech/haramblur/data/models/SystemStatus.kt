package com.hieltech.haramblur.data.models

import java.time.LocalDateTime

/**
 * Represents the overall system status including protection state,
 * detection statistics, and service health indicators.
 */
data class SystemStatus(
    val protectionEnabled: Boolean = false,
    val dailyFacesDetected: Int = 0,
    val dailySitesBlocked: Int = 0,
    val serviceHealthScore: Float = 0.0f,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val batteryOptimized: Boolean = false,
    val accessibilityServiceActive: Boolean = false,
    val criticalIssues: List<String> = emptyList()
) {
    /**
     * Calculate the detection rate per hour based on current time
     */
    fun getDetectionRate(): Float {
        val hoursSinceMidnight = LocalDateTime.now().hour + (LocalDateTime.now().minute / 60.0f)
        return if (hoursSinceMidnight > 0) {
            (dailyFacesDetected + dailySitesBlocked) / hoursSinceMidnight
        } else 0.0f
    }
    
    /**
     * Get overall system health score (0.0 to 1.0)
     */
    fun getOverallHealthScore(): Float {
        var score = serviceHealthScore
        
        // Deduct points for critical issues
        score -= criticalIssues.size * 0.1f
        
        // Add bonus for optimized battery
        if (batteryOptimized) score += 0.1f
        
        // Add bonus for active accessibility service
        if (accessibilityServiceActive) score += 0.1f
        
        return score.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Check if system needs attention
     */
    fun needsAttention(): Boolean {
        return criticalIssues.isNotEmpty() || 
               !accessibilityServiceActive || 
               getOverallHealthScore() < 0.7f
    }
    
    /**
     * Get status summary for display
     */
    fun getStatusSummary(): String {
        return when {
            criticalIssues.isNotEmpty() -> "Issues Detected"
            !protectionEnabled -> "Protection Disabled"
            !accessibilityServiceActive -> "Service Inactive"
            else -> "All Systems Operational"
        }
    }
}
