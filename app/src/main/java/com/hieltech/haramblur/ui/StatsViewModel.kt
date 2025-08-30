package com.hieltech.haramblur.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.LogRepository.LogCategory
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.detection.PerformanceMonitor
import com.hieltech.haramblur.detection.PerformanceMetrics
import com.hieltech.haramblur.detection.PerformanceState
import com.hieltech.haramblur.data.StatsRepository
import com.hieltech.haramblur.data.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Comprehensive ViewModel for the stats dashboard
 * Aggregates data from PerformanceMonitor, LogRepository, and blocking managers
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val logRepository: LogRepository,
    private val appBlockingManager: AppBlockingManager,
    private val siteBlockingManager: EnhancedSiteBlockingManager,
    private val statsRepository: StatsRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
        private const val HOURS_24 = 24L * 60 * 60 * 1000
        private const val HOURS_7 = 7L * 24 * 60 * 60 * 1000
        private const val HOURS_30 = 30L * 24 * 60 * 60 * 1000
    }

    /**
     * Dashboard state containing all aggregated metrics
     */
    data class DashboardState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val performanceState: PerformanceState = PerformanceState.OPTIMAL,
        val currentMetrics: PerformanceMetrics = PerformanceMetrics.empty(),
        val dailySummary: LogRepository.DetectionSummary = LogRepository.DetectionSummary.empty(),
        val weeklySummary: LogRepository.DetectionSummary = LogRepository.DetectionSummary.empty(),
        val timelineData: List<LogRepository.TimelinePoint> = emptyList(),
        val blockedAppsCount: Int = 0,
        val blockedSitesCount: Int = 0,
        val performanceTrends: PerformanceTrends = PerformanceTrends.empty(),
        // Islamic features
        val prayerTimes: com.hieltech.haramblur.data.prayer.PrayerData? = null,
        val hijriDate: com.hieltech.haramblur.data.prayer.HijriCalendar? = null,
        val nextPrayer: com.hieltech.haramblur.data.prayer.NextPrayerInfo? = null,
        val qiblaDirection: Double? = null,
        val isIslamicFeaturesEnabled: Boolean = true
    )

    // Using LogRepository data classes to avoid duplication

    /**
     * Performance trends data class
     */
    data class PerformanceTrends(
        val processingTimeChange: Float = 0f,
        val violationRateChange: Float = 0f,
        val detectionRateChange: Float = 0f,
        val trendDirection: TrendDirection = TrendDirection.STABLE
    ) {
        enum class TrendDirection {
            IMPROVING, DECLINING, STABLE
        }

        companion object {
            fun empty() = PerformanceTrends()
        }
    }

    /**
     * Time range enum for filtering data
     */
    enum class TimeRange(val displayName: String, val hours: Int) {
        LAST_24H("24h", 24),
        LAST_7D("7d", 7 * 24),
        LAST_30D("30d", 30 * 24)
    }

    /**
     * Timeline type enum for switching between detections and blocks
     */
    enum class TimelineType(val displayName: String) {
        DETECTIONS("Detections"),
        BLOCKS("Blocks")
    }

    // StateFlow for dashboard data
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    // Selected time range for timeline
    private val _selectedTimeRange = MutableStateFlow(TimeRange.LAST_24H)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    // Selected timeline type (detections vs blocks)
    private val _selectedTimelineType = MutableStateFlow(TimelineType.DETECTIONS)
    val selectedTimelineType: StateFlow<TimelineType> = _selectedTimelineType.asStateFlow()

    init {
        loadDashboardData()
        setupRealTimeUpdates()
        setupTimelineUpdates()
        // Load Islamic data after a short delay to avoid blocking UI
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            loadIslamicData()
        }
    }

    /**
     * Load all dashboard data
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)

                // Load all data in parallel using StatsRepository - convert types
                val dailyData = statsRepository.getHistoricalStats(StatsRepository.TimeRange.LAST_24H).first()
                val weeklyData = statsRepository.getHistoricalStats(StatsRepository.TimeRange.LAST_7D).first()
                val dailySummary = LogRepository.DetectionSummary(
                    totalDetections = dailyData.totalDetections,
                    successfulDetections = dailyData.successfulDetections,
                    failedDetections = dailyData.failedDetections,
                    averageProcessingTime = dailyData.averageProcessingTime
                )
                val weeklySummary = LogRepository.DetectionSummary(
                    totalDetections = weeklyData.totalDetections,
                    successfulDetections = weeklyData.successfulDetections,
                    failedDetections = weeklyData.failedDetections,
                    averageProcessingTime = weeklyData.averageProcessingTime
                )
                val timelineData = getTimelineData(24)
                val performanceTrendsData = statsRepository.getPerformanceTrends().first()
                val performanceTrends = PerformanceTrends(
                    processingTimeChange = 0f,
                    violationRateChange = 0f,
                    detectionRateChange = 0f,
                    trendDirection = PerformanceTrends.TrendDirection.STABLE
                )

                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    dailySummary = dailySummary,
                    weeklySummary = weeklySummary,
                    timelineData = timelineData,
                    performanceTrends = performanceTrends
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    /**
     * Setup real-time updates from StatsRepository
     */
    private fun setupRealTimeUpdates() {
        viewModelScope.launch {
            // Collect from StatsRepository real-time stats
            statsRepository.getRealTimeStats().collect { realTimeStats ->
                _dashboardState.value = _dashboardState.value.copy(
                    performanceState = realTimeStats.performanceState,
                    currentMetrics = realTimeStats.currentMetrics,
                    blockedAppsCount = realTimeStats.blockedAppsCount,
                    blockedSitesCount = realTimeStats.blockedSitesCount
                )
            }
        }
    }

    /**
     * Setup timeline updates when time range changes
     */
    private fun setupTimelineUpdates() {
        viewModelScope.launch {
            selectedTimeRange.collect { timeRange ->
                updateTimelineData(timeRange.hours)
            }
        }

        // Periodic refresh every 60 seconds
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(60000) // 60 seconds
                val currentRange = _selectedTimeRange.value
                updateTimelineData(currentRange.hours)
            }
        }
    }

    /**
     * Set selected time range for timeline
     */
    fun setSelectedTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
    }

    /**
     * Set selected timeline type (detections vs blocks)
     */
    fun setSelectedTimelineType(timelineType: TimelineType) {
        _selectedTimelineType.value = timelineType
        // Immediately update timeline data with new type
        viewModelScope.launch {
            updateTimelineData(_selectedTimeRange.value.hours)
        }
    }

    /**
     * Update timeline data for the given hours
     */
    private suspend fun updateTimelineData(hours: Int) {
        try {
            val timelineData = getTimelineData(hours)
            _dashboardState.value = _dashboardState.value.copy(
                timelineData = timelineData
            )
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    /**
     * Get daily detection summary
     */
    suspend fun getDailySummary(): LogRepository.DetectionSummary {
        return try {
            logRepository.getDailyDetectionSummary(System.currentTimeMillis()).first()
        } catch (e: Exception) {
            LogRepository.DetectionSummary.empty()
        }
    }

    /**
     * Get weekly detection summary
     */
    suspend fun getWeeklySummary(): LogRepository.DetectionSummary {
        return try {
            logRepository.getWeeklyDetectionSummary(System.currentTimeMillis()).first()
        } catch (e: Exception) {
            LogRepository.DetectionSummary.empty()
        }
    }

    /**
     * Get timeline data for chart visualization based on selected type
     */
    suspend fun getTimelineData(hours: Int): List<LogRepository.TimelinePoint> {
        return try {
            val timelineFlow = when (_selectedTimelineType.value) {
                TimelineType.DETECTIONS -> logRepository.getDetectionTimeline(hours)
                TimelineType.BLOCKS -> logRepository.getBlockedTimeline(hours)
            }

            timelineFlow.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get blocked apps count
     */
    suspend fun getBlockedAppsCount(): Int {
        return try {
            appBlockingManager.getBlockedApps().size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get blocked sites count
     */
    suspend fun getBlockedSitesCount(): Int {
        return try {
            siteBlockingManager.getCustomBlockedWebsitesCount().first()
        } catch (e: Exception) {
            0
        }
    }



    /**
     * Load Islamic prayer times and calendar data
     */
    private fun loadIslamicData() {
        viewModelScope.launch {
            try {
                // Load prayer times
                prayerTimesRepository.getPrayerTimes().onSuccess { prayerData ->
                    _dashboardState.value = _dashboardState.value.copy(
                        prayerTimes = prayerData,
                        hijriDate = prayerData.date.hijri
                    )
                }.onFailure { error ->
                    // Log error but don't show to user unless critical
                    println("Error loading prayer times: ${error.message}")
                }

                // Load next prayer
                prayerTimesRepository.getNextPrayer().onSuccess { nextPrayer ->
                    _dashboardState.value = _dashboardState.value.copy(
                        nextPrayer = nextPrayer
                    )
                }.onFailure { error ->
                    println("Error loading next prayer: ${error.message}")
                }

                // Load Qibla direction
                prayerTimesRepository.getQiblaDirection().onSuccess { direction ->
                    _dashboardState.value = _dashboardState.value.copy(
                        qiblaDirection = direction
                    )
                }.onFailure { error ->
                    println("Error loading Qibla direction: ${error.message}")
                }
            } catch (e: Exception) {
                println("Error in loadIslamicData: ${e.message}")
            }
        }
    }

    /**
     * Refresh Islamic data
     */
    fun refreshIslamicData() {
        viewModelScope.launch {
            loadIslamicData()
        }
    }

    /**
     * Refresh all dashboard data
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                statsRepository.refreshStats()
                loadDashboardData()
                loadIslamicData()
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    error = "Failed to refresh data: ${e.message}"
                )
            }
        }
    }

    /**
     * Calculate percentage change between two values
     */
    private fun calculateChange(oldValue: Float, newValue: Float): Float {
        if (oldValue == 0f) return 0f
        return ((newValue - oldValue) / oldValue) * 100f
    }

    private fun calculateChange(oldValue: Double, newValue: Double): Float {
        if (oldValue == 0.0) return 0f
        return ((newValue - oldValue) / oldValue * 100).toFloat()
    }


}
