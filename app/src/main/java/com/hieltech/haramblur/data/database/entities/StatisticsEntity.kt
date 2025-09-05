package com.hieltech.haramblur.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity for storing daily statistics
 */
@Entity(tableName = "daily_statistics")
data class StatisticsEntity(
    @PrimaryKey
    val date: LocalDate,
    val facesDetected: Int = 0,
    val sitesBlocked: Int = 0,
    val detectionAccuracy: Float = 0.0f,
    val averageProcessingTime: Long = 0L, // milliseconds
    val totalScreenTime: Long = 0L, // milliseconds
    val protectionActiveTime: Long = 0L, // milliseconds
    val batteryUsage: Float = 0.0f, // percentage
    val memoryPeakUsage: Float = 0.0f, // percentage
    val networkRequests: Int = 0,
    val errorsEncountered: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Calculate total detections for the day
     */
    fun getTotalDetections(): Int = facesDetected + sitesBlocked
    
    /**
     * Calculate detection rate per hour
     */
    fun getDetectionRatePerHour(): Float {
        val hoursActive = protectionActiveTime / (1000 * 60 * 60) // Convert to hours
        return if (hoursActive > 0) getTotalDetections().toFloat() / hoursActive else 0.0f
    }
    
    /**
     * Check if statistics are complete for the day
     */
    fun isComplete(): Boolean {
        return lastUpdated > 0 && 
               (facesDetected > 0 || sitesBlocked > 0 || protectionActiveTime > 0)
    }
    
    /**
     * Get performance score (0.0 to 1.0)
     */
    fun getPerformanceScore(): Float {
        var score = 1.0f
        
        // Deduct for high processing time
        if (averageProcessingTime > 1000) score -= 0.2f // > 1 second
        
        // Deduct for high battery usage
        if (batteryUsage > 10.0f) score -= 0.2f // > 10%
        
        // Deduct for high memory usage
        if (memoryPeakUsage > 0.8f) score -= 0.2f // > 80%
        
        // Deduct for errors
        if (errorsEncountered > 0) score -= errorsEncountered * 0.1f
        
        // Bonus for good accuracy
        if (detectionAccuracy > 0.9f) score += 0.1f
        
        return score.coerceIn(0.0f, 1.0f)
    }
}
