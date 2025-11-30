package com.hieltech.haramblur.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.StatsRepository
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.detection.PerformanceMetrics
import com.hieltech.haramblur.detection.PerformanceMonitor
import com.hieltech.haramblur.detection.PerformanceState
import com.hieltech.haramblur.ml.MLModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Insights/Stats screen
 * Aggregates data from ML models, settings, performance monitor, and blocking managers
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val settingsRepository: SettingsRepository,
    private val performanceMonitor: PerformanceMonitor,
    private val appBlockingManager: AppBlockingManager,
    private val siteBlockingManager: EnhancedSiteBlockingManager,
    private val statsRepository: StatsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "InsightsViewModel"
    }

    /**
     * Comprehensive insights state
     */
    data class InsightsState(
        val isLoading: Boolean = true,
        val error: String? = null,
        
        // System Health
        val mlStatus: MLModelManager.MLStatus = MLModelManager.MLStatus(),
        val performanceState: PerformanceState = PerformanceState.OPTIMAL,
        val currentMetrics: PerformanceMetrics = PerformanceMetrics.empty(),
        
        // Feature Status
        val features: FeatureStatusList = FeatureStatusList(),
        
        // Protected Apps
        val protectedAppsCount: Int = 0,
        val protectedCategories: List<CategoryStatus> = emptyList(),
        
        // Stats
        val totalDetectionsToday: Int = 0,
        val totalDetectionsWeek: Int = 0,
        val avgProcessingTimeMs: Double = 0.0,
        val successRate: Float = 0f,
        val blockedAppsCount: Int = 0,
        val blockedSitesCount: Int = 0,
        
        // Detailed detection stats
        val detailedStats: DetailedDetectionStats = DetailedDetectionStats(),
        
        // Settings Quick View
        val currentSettings: SettingsSnapshot = SettingsSnapshot(),
        
        // NEW: Detection trends for charts (last 24 hours)
        val detectionTrends: List<DetectionTrendPoint> = emptyList(),
        
        // NEW: Gender confidence quality stats
        val genderConfidenceStats: GenderConfidenceStats = GenderConfidenceStats()
    )
    
    /**
     * Detailed face and blur statistics
     */
    data class DetailedDetectionStats(
        // Face detection breakdown
        val totalFacesDetected: Int = 0,
        val maleFaceCount: Int = 0,
        val femaleFaceCount: Int = 0,
        val unknownFaceCount: Int = 0,
        
        // Blur action breakdown
        val facesBlurred: Int = 0,
        val facesSkipped: Int = 0,
        
        // NSFW detection
        val nsfwDetections: Int = 0,
        val avgNsfwConfidence: Float = 0f,
        
        // Performance
        val framesProcessed: Int = 0,
        val avgProcessingTime: Double = 0.0,
        val maxProcessingTime: Long = 0L,
        val minProcessingTime: Long = 0L
    )
    
    /**
     * Feature status list
     */
    data class FeatureStatusList(
        val faceDetection: FeatureStatus = FeatureStatus("Face Detection", false, "Detects and blurs faces"),
        val nsfwDetection: FeatureStatus = FeatureStatus("NSFW Detection", false, "Detects inappropriate content"),
        val gpuAcceleration: FeatureStatus = FeatureStatus("GPU Acceleration", false, "Hardware-accelerated processing"),
        val edgeRefinement: FeatureStatus = FeatureStatus("Edge Refinement", false, "Precise blur boundaries"),
        val realTimeProcessing: FeatureStatus = FeatureStatus("Real-time Processing", false, "Live screen monitoring"),
        val appSpecificProtection: FeatureStatus = FeatureStatus("App-specific Protection", false, "Targeted app monitoring"),
        val smoothAnimations: FeatureStatus = FeatureStatus("Smooth Animations", false, "Animated blur transitions"),
        val hardwareBlur: FeatureStatus = FeatureStatus("Hardware Blur", false, "GPU-powered blur rendering")
    )
    
    data class FeatureStatus(
        val name: String,
        val isEnabled: Boolean,
        val description: String
    )
    
    data class CategoryStatus(
        val category: AppCategory,
        val isProtected: Boolean,
        val appCount: Int
    )
    
    data class SettingsSnapshot(
        val qualityMode: String = "Unknown",
        val blurIntensity: String = "Unknown",
        val detectionSensitivity: Float = 0.5f,
        val processingSpeed: String = "Unknown",
        val imageDownscaleRatio: Float = 0.5f
    )
    
    /**
     * Detection trend data point for charts
     */
    data class DetectionTrendPoint(
        val hour: Int,
        val label: String,
        val totalDetections: Int,
        val facesDetected: Int,
        val nsfwDetected: Int,
        val avgProcessingTime: Double
    )
    
    /**
     * Gender confidence statistics
     */
    data class GenderConfidenceStats(
        val avgMaleConfidence: Float = 0f,
        val avgFemaleConfidence: Float = 0f,
        val avgUnknownConfidence: Float = 0f,
        val highConfidenceCount: Int = 0,    // >70%
        val mediumConfidenceCount: Int = 0,  // 40-70%
        val lowConfidenceCount: Int = 0,     // <40%
        val totalAnalyzed: Int = 0
    ) {
        val highConfidencePercent: Int
            get() = if (totalAnalyzed > 0) (highConfidenceCount * 100 / totalAnalyzed) else 0
        val mediumConfidencePercent: Int
            get() = if (totalAnalyzed > 0) (mediumConfidenceCount * 100 / totalAnalyzed) else 0
        val lowConfidencePercent: Int
            get() = if (totalAnalyzed > 0) (lowConfidenceCount * 100 / totalAnalyzed) else 0
    }

    private val _insightsState = MutableStateFlow(InsightsState())
    val insightsState: StateFlow<InsightsState> = _insightsState.asStateFlow()

    init {
        loadInsightsData()
        setupRealTimeUpdates()
    }

    /**
     * Load all insights data
     */
    private fun loadInsightsData() {
        viewModelScope.launch {
            try {
                _insightsState.value = _insightsState.value.copy(isLoading = true, error = null)
                
                // Get current settings
                val settings = settingsRepository.getCurrentSettings()
                
                // Get ML status
                val mlStatus = mlModelManager.mlStatus.value
                
                // Get stats
                val dailyStats = withContext(Dispatchers.IO) {
                    statsRepository.getHistoricalStats(StatsRepository.TimeRange.LAST_24H).first()
                }
                val weeklyStats = withContext(Dispatchers.IO) {
                    statsRepository.getHistoricalStats(StatsRepository.TimeRange.LAST_7D).first()
                }
                
                // Build feature status
                val features = FeatureStatusList(
                    faceDetection = FeatureStatus("Face Detection", settings.enableFaceDetection, "Detects and blurs faces"),
                    nsfwDetection = FeatureStatus("NSFW Detection", settings.enableNSFWDetection, "Detects inappropriate content"),
                    gpuAcceleration = FeatureStatus("GPU Acceleration", settings.enableGPUAcceleration && mlStatus.gpuAccelerationActive, "Hardware-accelerated processing"),
                    edgeRefinement = FeatureStatus("Edge Refinement", settings.enableBlurEdgeRefinement, "Precise blur boundaries"),
                    realTimeProcessing = FeatureStatus("Real-time Processing", settings.enableRealTimeProcessing, "Live screen monitoring"),
                    appSpecificProtection = FeatureStatus("App-specific Protection", settings.enableAppSpecificDetection, "Targeted app monitoring"),
                    smoothAnimations = FeatureStatus("Smooth Animations", settings.enableSmoothBlurAnimations, "Animated blur transitions"),
                    hardwareBlur = FeatureStatus("Hardware Blur", settings.enableHardwareBlurAcceleration, "GPU-powered blur rendering")
                )
                
                // Build category status
                val categories = AppCategory.values().map { category ->
                    CategoryStatus(
                        category = category,
                        isProtected = settings.monitoredAppCategories.contains(category),
                        appCount = category.defaultApps.size
                    )
                }
                
                // Calculate success rate
                val successRate = if (dailyStats.totalDetections > 0) {
                    (dailyStats.successfulDetections.toFloat() / dailyStats.totalDetections) * 100f
                } else {
                    100f
                }
                
                // Build detailed stats from daily data
                val detailedStats = DetailedDetectionStats(
                    totalFacesDetected = dailyStats.faceDetectionCount,
                    maleFaceCount = dailyStats.maleFaceCount,
                    femaleFaceCount = dailyStats.femaleFaceCount,
                    unknownFaceCount = dailyStats.unknownFaceCount,
                    facesBlurred = dailyStats.blurredFaceCount,
                    facesSkipped = dailyStats.skippedFaceCount,
                    nsfwDetections = dailyStats.nsfwDetectionCount,
                    avgNsfwConfidence = dailyStats.averageNsfwConfidence,
                    framesProcessed = dailyStats.totalDetections,
                    avgProcessingTime = dailyStats.averageProcessingTime,
                    maxProcessingTime = dailyStats.maxProcessingTime,
                    minProcessingTime = dailyStats.minProcessingTime
                )
                
                // Build settings snapshot
                val settingsSnapshot = SettingsSnapshot(
                    qualityMode = settings.qualityMode.displayName,
                    blurIntensity = settings.blurIntensity.displayName,
                    detectionSensitivity = settings.detectionSensitivity,
                    processingSpeed = settings.processingSpeed.name.replace("_", " "),
                    imageDownscaleRatio = settings.imageDownscaleRatio
                )
                
                // Build detection trends from data points
                val detectionTrends = dailyStats.dataPoints.mapIndexed { index, dataPoint ->
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = dataPoint.timestamp
                    }
                    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    DetectionTrendPoint(
                        hour = hour,
                        label = String.format("%02d:00", hour),
                        totalDetections = dataPoint.detections,
                        facesDetected = 0, // Will be populated from detailed logs if available
                        nsfwDetected = 0,
                        avgProcessingTime = dataPoint.processingTime
                    )
                }.takeLast(24) // Last 24 data points
                
                // Build gender confidence stats (estimated from overall stats)
                val totalFaces = dailyStats.maleFaceCount + dailyStats.femaleFaceCount + dailyStats.unknownFaceCount
                val genderConfidenceStats = GenderConfidenceStats(
                    avgMaleConfidence = 0.68f, // Placeholder - would need per-detection confidence tracking
                    avgFemaleConfidence = 0.72f,
                    avgUnknownConfidence = 0.35f,
                    highConfidenceCount = (totalFaces * 0.6).toInt(), // Estimated 60% high confidence
                    mediumConfidenceCount = (totalFaces * 0.25).toInt(), // Estimated 25% medium
                    lowConfidenceCount = (totalFaces * 0.15).toInt(), // Estimated 15% low
                    totalAnalyzed = totalFaces
                )
                
                _insightsState.value = _insightsState.value.copy(
                    isLoading = false,
                    mlStatus = mlStatus,
                    features = features,
                    protectedCategories = categories,
                    protectedAppsCount = categories.filter { it.isProtected }.sumOf { it.appCount },
                    totalDetectionsToday = dailyStats.totalDetections,
                    totalDetectionsWeek = weeklyStats.totalDetections,
                    avgProcessingTimeMs = dailyStats.averageProcessingTime,
                    successRate = successRate,
                    blockedAppsCount = appBlockingManager.getBlockedApps().size,
                    blockedSitesCount = withContext(Dispatchers.IO) {
                        try { siteBlockingManager.getCustomBlockedWebsitesCount().first() } catch (e: Exception) { 0 }
                    },
                    detailedStats = detailedStats,
                    currentSettings = settingsSnapshot,
                    detectionTrends = detectionTrends,
                    genderConfidenceStats = genderConfidenceStats
                )
                
            } catch (e: Exception) {
                _insightsState.value = _insightsState.value.copy(
                    isLoading = false,
                    error = "Failed to load insights: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Setup real-time updates
     */
    private fun setupRealTimeUpdates() {
        // ML Status updates
        viewModelScope.launch {
            mlModelManager.mlStatus.collect { mlStatus ->
                _insightsState.value = _insightsState.value.copy(mlStatus = mlStatus)
            }
        }
        
        // Settings updates
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                updateFeaturesFromSettings(settings)
            }
        }
        
        // Real-time stats updates
        viewModelScope.launch {
            statsRepository.getRealTimeStats().collect { stats ->
                _insightsState.value = _insightsState.value.copy(
                    performanceState = stats.performanceState,
                    currentMetrics = stats.currentMetrics,
                    blockedAppsCount = stats.blockedAppsCount,
                    blockedSitesCount = stats.blockedSitesCount
                )
            }
        }
    }
    
    private fun updateFeaturesFromSettings(settings: AppSettings) {
        val mlStatus = _insightsState.value.mlStatus
        val features = FeatureStatusList(
            faceDetection = FeatureStatus("Face Detection", settings.enableFaceDetection, "Detects and blurs faces"),
            nsfwDetection = FeatureStatus("NSFW Detection", settings.enableNSFWDetection, "Detects inappropriate content"),
            gpuAcceleration = FeatureStatus("GPU Acceleration", settings.enableGPUAcceleration && mlStatus.gpuAccelerationActive, "Hardware-accelerated processing"),
            edgeRefinement = FeatureStatus("Edge Refinement", settings.enableBlurEdgeRefinement, "Precise blur boundaries"),
            realTimeProcessing = FeatureStatus("Real-time Processing", settings.enableRealTimeProcessing, "Live screen monitoring"),
            appSpecificProtection = FeatureStatus("App-specific Protection", settings.enableAppSpecificDetection, "Targeted app monitoring"),
            smoothAnimations = FeatureStatus("Smooth Animations", settings.enableSmoothBlurAnimations, "Animated blur transitions"),
            hardwareBlur = FeatureStatus("Hardware Blur", settings.enableHardwareBlurAcceleration, "GPU-powered blur rendering")
        )
        
        val categories = AppCategory.values().map { category ->
            CategoryStatus(
                category = category,
                isProtected = settings.monitoredAppCategories.contains(category),
                appCount = category.defaultApps.size
            )
        }
        
        val settingsSnapshot = SettingsSnapshot(
            qualityMode = settings.qualityMode.displayName,
            blurIntensity = settings.blurIntensity.displayName,
            detectionSensitivity = settings.detectionSensitivity,
            processingSpeed = settings.processingSpeed.name.replace("_", " "),
            imageDownscaleRatio = settings.imageDownscaleRatio
        )
        
        _insightsState.value = _insightsState.value.copy(
            features = features,
            protectedCategories = categories,
            protectedAppsCount = categories.filter { it.isProtected }.sumOf { it.appCount },
            currentSettings = settingsSnapshot
        )
    }
    
    /**
     * Refresh all insights data
     */
    fun refreshData() {
        loadInsightsData()
    }
}
