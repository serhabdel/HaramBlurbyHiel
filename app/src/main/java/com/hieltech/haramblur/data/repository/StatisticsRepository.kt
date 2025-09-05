package com.hieltech.haramblur.data.repository

import com.hieltech.haramblur.data.database.StatisticsDao
import com.hieltech.haramblur.data.database.entities.StatisticsEntity
import com.hieltech.haramblur.data.models.SystemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing detection statistics and usage data
 */
@Singleton
class StatisticsRepository @Inject constructor(
    private val statisticsDao: StatisticsDao
) {
    
    /**
     * Get today's statistics as Flow
     */
    fun getTodayStatisticsFlow(): Flow<StatisticsEntity?> {
        return statisticsDao.getTodayStatisticsFlow(LocalDate.now())
    }
    
    /**
     * Get recent statistics for the last N days
     */
    fun getRecentStatisticsFlow(days: Int = 7): Flow<List<StatisticsEntity>> {
        return statisticsDao.getRecentStatisticsFlow(days)
    }
    
    /**
     * Get recent statistics for the last N days (suspend version)
     */
    suspend fun getRecentStatistics(days: Int = 7): List<StatisticsEntity> {
        return statisticsDao.getRecentStatistics(days)
    }
    
    /**
     * Get statistics for a specific date
     */
    suspend fun getStatisticsForDate(date: LocalDate): StatisticsEntity? {
        return statisticsDao.getStatisticsForDate(date)
    }
    
    /**
     * Get statistics for a date range
     */
    suspend fun getStatisticsForDateRange(startDate: LocalDate, endDate: LocalDate): List<StatisticsEntity> {
        return statisticsDao.getStatisticsForDateRange(startDate, endDate)
    }
    
    /**
     * Get current system status based on today's statistics
     */
    suspend fun getCurrentSystemStatus(): SystemStatus {
        val todayStats = statisticsDao.getTodayStatistics(LocalDate.now())
        val recentStats = statisticsDao.getRecentStatistics(7)
        
        return SystemStatus(
            protectionEnabled = todayStats?.protectionActiveTime ?: 0L > 0,
            dailyFacesDetected = todayStats?.facesDetected ?: 0,
            dailySitesBlocked = todayStats?.sitesBlocked ?: 0,
            serviceHealthScore = calculateServiceHealthScore(recentStats),
            lastUpdate = java.time.LocalDateTime.now(),
            batteryOptimized = todayStats?.batteryUsage ?: 0f < 5.0f,
            accessibilityServiceActive = todayStats?.protectionActiveTime ?: 0L > 0,
            criticalIssues = identifyCriticalIssues(todayStats, recentStats)
        )
    }
    
    /**
     * Get system status as Flow
     */
    fun getSystemStatusFlow(): Flow<SystemStatus> {
        return getTodayStatisticsFlow().map { todayStats ->
            SystemStatus(
                protectionEnabled = todayStats?.protectionActiveTime ?: 0L > 0,
                dailyFacesDetected = todayStats?.facesDetected ?: 0,
                dailySitesBlocked = todayStats?.sitesBlocked ?: 0,
                serviceHealthScore = 0.8f, // Default value, would be calculated from recent stats
                lastUpdate = java.time.LocalDateTime.now(),
                batteryOptimized = todayStats?.batteryUsage ?: 0f < 5.0f,
                accessibilityServiceActive = todayStats?.protectionActiveTime ?: 0L > 0,
                criticalIssues = emptyList()
            )
        }
    }
    
    /**
     * Record a face detection
     */
    suspend fun recordFaceDetection() {
        val today = LocalDate.now()
        val existingStats = statisticsDao.getTodayStatistics(today)
        
        if (existingStats != null) {
            statisticsDao.incrementDetections(today, 1, 0, System.currentTimeMillis())
        } else {
            val newStats = StatisticsEntity(
                date = today,
                facesDetected = 1,
                lastUpdated = System.currentTimeMillis()
            )
            statisticsDao.insertStatistics(newStats)
        }
    }
    
    /**
     * Record a site block
     */
    suspend fun recordSiteBlock() {
        val today = LocalDate.now()
        val existingStats = statisticsDao.getTodayStatistics(today)
        
        if (existingStats != null) {
            statisticsDao.incrementDetections(today, 0, 1, System.currentTimeMillis())
        } else {
            val newStats = StatisticsEntity(
                date = today,
                sitesBlocked = 1,
                lastUpdated = System.currentTimeMillis()
            )
            statisticsDao.insertStatistics(newStats)
        }
    }
    
    /**
     * Record protection active time
     */
    suspend fun recordProtectionTime(timeIncrement: Long) {
        val today = LocalDate.now()
        val existingStats = statisticsDao.getTodayStatistics(today)
        
        if (existingStats != null) {
            statisticsDao.incrementProtectionTime(today, timeIncrement, System.currentTimeMillis())
        } else {
            val newStats = StatisticsEntity(
                date = today,
                protectionActiveTime = timeIncrement,
                lastUpdated = System.currentTimeMillis()
            )
            statisticsDao.insertStatistics(newStats)
        }
    }
    
    /**
     * Update performance metrics
     */
    suspend fun updatePerformanceMetrics(
        processingTime: Long,
        batteryUsage: Float,
        memoryUsage: Float,
        errors: Int = 0
    ) {
        val today = LocalDate.now()
        val existingStats = statisticsDao.getTodayStatistics(today)
        
        if (existingStats != null) {
            val updatedStats = existingStats.copy(
                averageProcessingTime = (existingStats.averageProcessingTime + processingTime) / 2,
                batteryUsage = (existingStats.batteryUsage + batteryUsage) / 2,
                memoryPeakUsage = maxOf(existingStats.memoryPeakUsage, memoryUsage),
                errorsEncountered = existingStats.errorsEncountered + errors,
                lastUpdated = System.currentTimeMillis()
            )
            statisticsDao.insertStatistics(updatedStats)
        } else {
            val newStats = StatisticsEntity(
                date = today,
                averageProcessingTime = processingTime,
                batteryUsage = batteryUsage,
                memoryPeakUsage = memoryUsage,
                errorsEncountered = errors,
                lastUpdated = System.currentTimeMillis()
            )
            statisticsDao.insertStatistics(newStats)
        }
    }
    
    /**
     * Get detection trends for the last N days
     */
    suspend fun getDetectionTrends(days: Int = 7): List<Pair<LocalDate, Int>> {
        val recentStats = statisticsDao.getRecentStatistics(days)
        return recentStats.map { stats ->
            stats.date to stats.getTotalDetections()
        }
    }
    
    /**
     * Get performance trends
     */
    suspend fun getPerformanceTrends(days: Int = 7): List<com.hieltech.haramblur.data.database.PerformanceTrend> {
        return statisticsDao.getPerformanceTrends(days)
    }
    
    /**
     * Export statistics data
     */
    suspend fun exportStatistics(startDate: LocalDate, endDate: LocalDate): List<StatisticsEntity> {
        return statisticsDao.getStatisticsForDateRange(startDate, endDate)
    }
    
    /**
     * Clean up old statistics
     */
    suspend fun cleanupOldStatistics(daysToKeep: Int = 90) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong())
        statisticsDao.deleteOldStatistics(cutoffDate)
    }
    
    /**
     * Calculate service health score based on recent statistics
     */
    private fun calculateServiceHealthScore(recentStats: List<StatisticsEntity>): Float {
        if (recentStats.isEmpty()) return 0.5f
        
        var totalScore = 0.0f
        var validDays = 0
        
        recentStats.forEach { stats ->
            if (stats.isComplete()) {
                totalScore += stats.getPerformanceScore()
                validDays++
            }
        }
        
        return if (validDays > 0) totalScore / validDays else 0.5f
    }
    
    /**
     * Identify critical issues from statistics
     */
    private fun identifyCriticalIssues(
        todayStats: StatisticsEntity?,
        recentStats: List<StatisticsEntity>
    ): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for high error rate
        if (todayStats?.errorsEncountered ?: 0 > 5) {
            issues.add("High error rate detected")
        }
        
        // Check for performance issues
        if (todayStats?.averageProcessingTime ?: 0L > 2000) {
            issues.add("Slow processing detected")
        }
        
        // Check for high battery usage
        if (todayStats?.batteryUsage ?: 0f > 15.0f) {
            issues.add("High battery usage")
        }
        
        // Check for memory issues
        if (todayStats?.memoryPeakUsage ?: 0f > 0.9f) {
            issues.add("High memory usage")
        }
        
        // Check for declining performance trend
        if (recentStats.size >= 3) {
            val recentPerformance = recentStats.take(3).map { it.getPerformanceScore() }
            if (recentPerformance.isSortedDescending()) {
                issues.add("Performance declining")
            }
        }
        
        return issues
    }
    
    /**
     * Check if list is sorted in descending order
     */
    private fun List<Float>.isSortedDescending(): Boolean {
        for (i in 1 until size) {
            if (this[i] > this[i - 1]) return false
        }
        return true
    }
}
