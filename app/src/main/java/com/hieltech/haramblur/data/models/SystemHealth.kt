package com.hieltech.haramblur.data.models

/**
 * Represents the overall system health status
 */
data class SystemHealth(
    val batteryOptimized: Boolean = false,
    val accessibilityServiceActive: Boolean = false,
    val permissionsGranted: Map<String, Boolean> = emptyMap(),
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val criticalIssues: List<HealthIssue> = emptyList(),
    val warnings: List<HealthIssue> = emptyList(),
    val lastChecked: Long = System.currentTimeMillis()
) {
    /**
     * Get overall health status as a string
     */
    fun getHealthStatus(): String {
        return when {
            criticalIssues.isNotEmpty() -> "Critical"
            warnings.isNotEmpty() -> "Warning"
            else -> "Good"
        }
    }

    /**
     * Check if system has critical issues
     */
    fun hasCriticalIssues(): Boolean {
        return criticalIssues.isNotEmpty()
    }

    /**
     * Check if system has warnings
     */
    fun hasWarnings(): Boolean {
        return warnings.isNotEmpty()
    }

    /**
     * Get health score (0.0 to 1.0)
     */
    fun getOverallHealthScore(): Float {
        var score = 1.0f

        // Deduct for critical issues
        score -= criticalIssues.size * 0.2f

        // Deduct for warnings
        score -= warnings.size * 0.1f

        // Add bonus for good metrics
        if (!batteryOptimized) score += 0.1f
        if (accessibilityServiceActive) score += 0.1f

        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Get list of current issues
     */
    fun getIssues(): List<HealthIssue> {
        return criticalIssues + warnings
    }
}
