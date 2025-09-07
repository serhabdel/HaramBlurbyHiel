package com.hieltech.haramblur.data.models

/**
 * Represents a health issue detected in the system
 */
data class HealthIssue(
    val id: String,
    val title: String,
    val description: String,
    val severity: IssueSeverity,
    val category: IssueCategory,
    val canAutoFix: Boolean = false,
    val actionRequired: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this is a critical issue
     */
    fun isCritical(): Boolean {
        return severity == IssueSeverity.CRITICAL
    }

    /**
     * Check if this issue can be resolved automatically
     */
    fun canAutoResolve(): Boolean {
        return canAutoFix
    }

    /**
     * Get priority score for sorting (higher = more urgent)
     */
    fun getPriorityScore(): Int {
        return when (severity) {
            IssueSeverity.CRITICAL -> 100
            IssueSeverity.HIGH -> 75
            IssueSeverity.MEDIUM -> 50
            IssueSeverity.LOW -> 25
        }
    }
}
