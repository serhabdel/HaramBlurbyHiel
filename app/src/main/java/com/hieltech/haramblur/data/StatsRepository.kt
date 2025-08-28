package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.detection.PerformanceMonitor
import com.hieltech.haramblur.detection.PerformanceMetrics
import com.hieltech.haramblur.detection.PerformanceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

/**
 * Dedicated repository for stats and analytics data
 * Aggregates data from multiple sources and provides caching for performance
 */
@Singleton
class StatsRepository @Inject constructor(
    private val logRepository: LogRepository,
    private val performanceMonitor: PerformanceMonitor,
    private val appBlockingManager: AppBlockingManager,
    private val siteBlockingManager: EnhancedSiteBlockingManager
) {

    companion object {
        private const val TAG = "StatsRepository"
        private const val CACHE_DURATION_MS = 30000L // 30 seconds cache
        private const val HOURS_24 = 24L * 60 * 60 * 1000
        private const val HOURS_7 = 7L * 24 * 60 * 60 * 1000
        private const val HOURS_30 = 30L * 24 * 60 * 60 * 1000
    }

    /**
     * Time range enum for filtering data
     */
    enum class TimeRange(val displayName: String, val durationMs: Long) {
        LAST_24H("Last 24 Hours", HOURS_24),
        LAST_7D("Last 7 Days", HOURS_7),
        LAST_30D("Last 30 Days", HOURS_30)
    }

    /**
     * Real-time statistics combining all sources
     */
    data class RealTimeStats(
        val performanceState: PerformanceState = PerformanceState.OPTIMAL,
        val currentMetrics: PerformanceMetrics = PerformanceMetrics.empty(),
        val blockedAppsCount: Int = 0,
        val blockedSitesCount: Int = 0,
        val detectionRatePerHour: Int = 0,
        val averageProcessingTime: Double = 0.0,
        val successRate: Float = 100f,
        val lastUpdated: Long = System.currentTimeMillis(),
        val isStale: Boolean = false
    )

    /**
     * Historical statistics for time-based analysis
     */
    data class HistoricalStats(
        val timeRange: TimeRange,
        val totalDetections: Int = 0,
        val successfulDetections: Int = 0,
        val failedDetections: Int = 0,
        val averageProcessingTime: Double = 0.0,
        val faceDetectionCount: Int = 0,
        val nsfwDetectionCount: Int = 0,
        val performanceScore: Float = 100f,
        val peakHour: Int = 0,
        val mostUsedMode: String = "unknown",
        val trendDirection: TrendDirection = TrendDirection.STABLE,
        val dataPoints: List<DataPoint> = emptyList()
    ) {
        enum class TrendDirection {
            IMPROVING, DECLINING, STABLE
        }

        data class DataPoint(
            val timestamp: Long,
            val detections: Int,
            val processingTime: Double,
            val successRate: Float
        )
    }

    /**
     * Performance trends analysis
     */
    data class PerformanceTrends(
        val processingTimeChange: Float = 0f,
        val violationRateChange: Float = 0f,
        val detectionRateChange: Float = 0f,
        val overallTrend: TrendDirection = TrendDirection.STABLE,
        val recommendations: List<String> = emptyList(),
        val confidence: Float = 0f,
        val analysisPeriod: String = "",
        val lastCalculated: Long = System.currentTimeMillis()
    ) {
        enum class TrendDirection {
            IMPROVING, DECLINING, STABLE
        }
    }

    // Cache management
    private var lastCacheTime = 0L
    private var cachedRealTimeStats: RealTimeStats? = null
    private val historicalCache = mutableMapOf<TimeRange, Pair<Long, HistoricalStats>>()
    private var cachedTrends: PerformanceTrends? = null

    /**
     * Get real-time statistics with caching
     */
    fun getRealTimeStats(): Flow<RealTimeStats> = flow {
        while (currentCoroutineContext().isActive) {
            val currentTime = System.currentTimeMillis()

            // Check if cache is still valid
            if (cachedRealTimeStats != null && (currentTime - lastCacheTime) < CACHE_DURATION_MS) {
                emit(cachedRealTimeStats!!.copy(isStale = false))
            } else {
                try {
                    // Gather data from all sources
                    val performanceState = performanceMonitor.performanceState.first()
                    val currentMetrics = performanceMonitor.currentMetrics.first()
                    val blockedAppsCount = getBlockedAppsCount()
                    val blockedSitesCount = getBlockedSitesCount()

                    // Calculate detection rate from recent logs
                    val detectionStats = calculateDetectionStats(1) // Last hour
                    val detectionRatePerHour = detectionStats.totalDetections * 24 // Extrapolate to per day
                    val successRate = if (detectionStats.totalDetections > 0) {
                        (detectionStats.successfulDetections.toFloat() / detectionStats.totalDetections) * 100f
                    } else 100f

                    val realTimeStats = RealTimeStats(
                        performanceState = performanceState,
                        currentMetrics = currentMetrics,
                        blockedAppsCount = blockedAppsCount,
                        blockedSitesCount = blockedSitesCount,
                        detectionRatePerHour = detectionRatePerHour,
                        averageProcessingTime = detectionStats.averageProcessingTime,
                        successRate = successRate,
                        lastUpdated = currentTime,
                        isStale = false
                    )

                    cachedRealTimeStats = realTimeStats
                    lastCacheTime = currentTime
                    emit(realTimeStats)

                } catch (e: Exception) {
                    // Emit stale data if available, otherwise emit error state
                    val errorStats = cachedRealTimeStats?.copy(isStale = true) ?: RealTimeStats(
                        performanceState = PerformanceState.CRITICAL,
                        currentMetrics = PerformanceMetrics.empty(),
                        lastUpdated = currentTime,
                        isStale = true
                    )
                    emit(errorStats)
                }
            }

            // Update every 5 seconds
            delay(5000)
        }
    }

    /**
     * Get historical statistics for a specific time range
     */
    fun getHistoricalStats(timeRange: TimeRange): Flow<HistoricalStats> = flow {
        val currentTime = System.currentTimeMillis()

        // Check cache first
        val cached = historicalCache[timeRange]
        if (cached != null && (currentTime - cached.first) < CACHE_DURATION_MS) {
            emit(cached.second)
            return@flow
        }

        val sinceTimestamp = currentTime - timeRange.durationMs
        val detectionLogs = logRepository.getDetectionEventsSince(sinceTimestamp).first()

        // Process logs into historical stats
        val stats = processHistoricalData(detectionLogs, timeRange)

        // Cache the result
        historicalCache[timeRange] = Pair(currentTime, stats)

        emit(stats)
    }.catch { e ->
        // Return empty stats on error using catch operator
        emit(HistoricalStats(timeRange = timeRange))
    }

    /**
     * Get performance trends analysis
     */
    fun getPerformanceTrends(): Flow<PerformanceTrends> = flow {
        val currentTime = System.currentTimeMillis()

        // Check cache
        cachedTrends?.let {
            if ((currentTime - it.lastCalculated) < CACHE_DURATION_MS) {
                emit(it)
                return@flow
            }
        }

        // Get data from multiple time periods for trend analysis
        val last24h = getHistoricalStats(TimeRange.LAST_24H).first()
        val last7d = getHistoricalStats(TimeRange.LAST_7D).first()

        val trends = analyzePerformanceTrends(last24h, last7d)

        cachedTrends = trends
        emit(trends)
    }.catch { e ->
        // Return neutral trends on error using catch operator
        emit(PerformanceTrends(
            overallTrend = PerformanceTrends.TrendDirection.STABLE,
            recommendations = listOf("Unable to analyze trends due to data error"),
            analysisPeriod = "Unknown"
        ))
    }

    /**
     * Manual refresh of all cached data
     */
    suspend fun refreshStats() {
        lastCacheTime = 0L
        historicalCache.clear()
        cachedTrends = null

        // Force refresh by collecting from flows
        getRealTimeStats().first()
        getHistoricalStats(TimeRange.LAST_24H).first()
        getPerformanceTrends().first()
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        cachedRealTimeStats = null
        historicalCache.clear()
        cachedTrends = null
        lastCacheTime = 0L
    }

    // Private helper methods

    private suspend fun getBlockedAppsCount(): Int {
        return try {
            appBlockingManager.getBlockedApps().size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getBlockedSitesCount(): Int {
        return try {
            siteBlockingManager.getCustomBlockedWebsitesCount().first()
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun calculateDetectionStats(hours: Int): DetectionStats {
        return try {
            val timeline = logRepository.getDetectionTimeline(hours).first()
            val totalDetections = timeline.sumOf { it.detectionCount }
            val successfulDetections = timeline.sumOf { (it.successRate * it.detectionCount).toInt() }
            val failedDetections = totalDetections - successfulDetections
            val averageProcessingTime = timeline.map { it.averageProcessingTime }.average()

            DetectionStats(
                totalDetections = totalDetections,
                successfulDetections = successfulDetections,
                failedDetections = failedDetections,
                averageProcessingTime = averageProcessingTime
            )
        } catch (e: Exception) {
            DetectionStats.empty()
        }
    }

    private fun processHistoricalData(
        logs: List<com.hieltech.haramblur.data.database.LogEntity>,
        timeRange: TimeRange
    ): HistoricalStats {
        if (logs.isEmpty()) {
            return HistoricalStats(timeRange = timeRange)
        }

        val detectionLogs = logs.filter { it.category == LogRepository.LogCategory.DETECTION.name }
        var totalDetections = 0
        var successfulDetections = 0
        var failedDetections = 0
        var faceDetectionCount = 0
        var nsfwDetectionCount = 0
        val processingTimes = mutableListOf<Double>()
        val dataPoints = mutableListOf<HistoricalStats.DataPoint>()
        val modeCount = mutableMapOf<String, Int>()

        // Group by hour for data points
        val hourlyData = detectionLogs.groupBy { log ->
            log.timestamp / (60 * 60 * 1000) // Group by hour
        }

        hourlyData.forEach { (hourTimestamp, hourLogs) ->
            val hourDetections = hourLogs.size
            val hourMetrics = hourLogs.map { parseDetectionMetrics(it.message) }

            val hourSuccessRate = hourMetrics.count { it.success }.toFloat() / hourMetrics.size
            val hourAvgProcessingTime = hourMetrics.map { it.processingTime }.average()

            dataPoints.add(HistoricalStats.DataPoint(
                timestamp = hourTimestamp * 60 * 60 * 1000,
                detections = hourDetections,
                processingTime = hourAvgProcessingTime,
                successRate = hourSuccessRate
            ))

            // Aggregate data
            hourLogs.forEach { log ->
                totalDetections++
                val metrics = parseDetectionMetrics(log.message)

                if (metrics.success) successfulDetections++ else failedDetections++
                faceDetectionCount += metrics.faceCount
                if (metrics.isNsfw) nsfwDetectionCount++
                processingTimes.add(metrics.processingTime)
                modeCount[metrics.performanceMode] = modeCount.getOrDefault(metrics.performanceMode, 0) + 1
            }
        }

        val averageProcessingTime = processingTimes.average()
        val successRate = if (totalDetections > 0) (successfulDetections.toFloat() / totalDetections) * 100f else 100f
        val performanceScore = successRate // Simplified performance score

        // Find peak hour
        val peakHourDataPoint = dataPoints.maxByOrNull { it.detections }
        val peakHour = java.util.Calendar.getInstance().apply {
            timeInMillis = peakHourDataPoint?.timestamp ?: 0L
        }.get(java.util.Calendar.HOUR_OF_DAY)

        // Find most used mode
        val mostUsedMode = modeCount.maxByOrNull { it.value }?.key ?: "unknown"

        // Determine trend (simplified - compare first half vs second half)
        val midPoint = dataPoints.size / 2
        val firstHalf = dataPoints.take(midPoint)
        val secondHalf = dataPoints.drop(midPoint)

        val firstHalfAvg = firstHalf.map { it.detections }.average()
        val secondHalfAvg = secondHalf.map { it.detections }.average()

        val trendDirection = when {
            secondHalfAvg > firstHalfAvg * 1.1 -> HistoricalStats.TrendDirection.IMPROVING
            secondHalfAvg < firstHalfAvg * 0.9 -> HistoricalStats.TrendDirection.DECLINING
            else -> HistoricalStats.TrendDirection.STABLE
        }

        return HistoricalStats(
            timeRange = timeRange,
            totalDetections = totalDetections,
            successfulDetections = successfulDetections,
            failedDetections = failedDetections,
            averageProcessingTime = averageProcessingTime,
            faceDetectionCount = faceDetectionCount,
            nsfwDetectionCount = nsfwDetectionCount,
            performanceScore = performanceScore,
            peakHour = peakHour,
            mostUsedMode = mostUsedMode,
            trendDirection = trendDirection,
            dataPoints = dataPoints.sortedBy { it.timestamp }
        )
    }

    private fun analyzePerformanceTrends(
        recent24h: HistoricalStats,
        recent7d: HistoricalStats
    ): PerformanceTrends {
        // Simplified trend analysis
        val processingTimeChange = calculateChange(
            recent7d.averageProcessingTime,
            recent24h.averageProcessingTime
        )

        val violationRateChange = calculateChange(
            recent7d.failedDetections.toFloat() / recent7d.totalDetections,
            recent24h.failedDetections.toFloat() / recent24h.totalDetections
        )

        val detectionRateChange = calculateChange(
            recent7d.totalDetections.toFloat(),
            recent24h.totalDetections.toFloat()
        )

        val overallTrend = when {
            processingTimeChange < -10f && violationRateChange < -10f -> PerformanceTrends.TrendDirection.IMPROVING
            processingTimeChange > 10f || violationRateChange > 10f -> PerformanceTrends.TrendDirection.DECLINING
            else -> PerformanceTrends.TrendDirection.STABLE
        }

        val recommendations = generateRecommendations(
            processingTimeChange,
            violationRateChange,
            detectionRateChange,
            overallTrend
        )

        return PerformanceTrends(
            processingTimeChange = processingTimeChange,
            violationRateChange = violationRateChange,
            detectionRateChange = detectionRateChange,
            overallTrend = overallTrend,
            recommendations = recommendations,
            confidence = 0.85f, // Simplified confidence score
            analysisPeriod = "24h vs 7d average",
            lastCalculated = System.currentTimeMillis()
        )
    }

    private fun calculateChange(oldValue: Double, newValue: Double): Float {
        if (oldValue == 0.0) return 0f
        return ((newValue - oldValue) / oldValue * 100).toFloat()
    }

    private fun calculateChange(oldValue: Float, newValue: Float): Float {
        if (oldValue == 0f) return 0f
        return ((newValue - oldValue) / oldValue * 100)
    }

    private fun generateRecommendations(
        processingTimeChange: Float,
        violationRateChange: Float,
        detectionRateChange: Float,
        trend: PerformanceTrends.TrendDirection
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when (trend) {
            PerformanceTrends.TrendDirection.IMPROVING -> {
                recommendations.add("Performance is improving! Keep up the good work.")
                if (processingTimeChange < -15f) {
                    recommendations.add("Processing time has decreased significantly - excellent optimization!")
                }
            }
            PerformanceTrends.TrendDirection.DECLINING -> {
                recommendations.add("Performance is declining. Consider optimization measures.")
                if (processingTimeChange > 15f) {
                    recommendations.add("Processing time has increased - check for performance bottlenecks")
                }
                if (violationRateChange > 10f) {
                    recommendations.add("Error rate has increased - investigate recent changes")
                }
            }
            PerformanceTrends.TrendDirection.STABLE -> {
                recommendations.add("Performance is stable. Monitor for any gradual changes.")
                if (detectionRateChange > 20f) {
                    recommendations.add("Detection rate has increased - ensure system can handle the load")
                }
            }
        }

        return recommendations
    }

    private fun parseDetectionMetrics(message: String): DetectionMetrics {
        // Same parsing logic as in LogRepository
        val parts = message.split("|")
        var faceCount = 0
        var isNsfw = false
        var processingTime = 0.0
        var success = true
        var performanceMode = "unknown"

        parts.forEach { part ->
            when {
                part.startsWith("faces:") -> faceCount = part.substringAfter(":").toIntOrNull() ?: 0
                part.startsWith("nsfw:") -> isNsfw = part.substringAfter(":").toBoolean()
                part.startsWith("processing_time:") -> processingTime = part.substringAfter(":").removeSuffix("ms").toDoubleOrNull() ?: 0.0
                part.startsWith("success:") -> success = part.substringAfter(":").toBoolean()
                part.startsWith("performance_mode:") -> performanceMode = part.substringAfter(":")
                part.startsWith("error:") -> success = false
            }
        }

        return DetectionMetrics(
            faceCount = faceCount,
            isNsfw = isNsfw,
            processingTime = processingTime,
            success = success,
            performanceMode = performanceMode
        )
    }

    private data class DetectionMetrics(
        val faceCount: Int,
        val isNsfw: Boolean,
        val processingTime: Double,
        val success: Boolean,
        val performanceMode: String
    )

    private data class DetectionStats(
        val totalDetections: Int,
        val successfulDetections: Int,
        val failedDetections: Int,
        val averageProcessingTime: Double
    ) {
        companion object {
            fun empty() = DetectionStats(0, 0, 0, 0.0)
        }
    }
}
