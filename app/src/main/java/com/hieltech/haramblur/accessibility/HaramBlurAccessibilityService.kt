package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.detection.SiteBlockingManager
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.ContentAction
import com.hieltech.haramblur.detection.ForegroundAppMonitor
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.models.DetectionScope
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.AppFilteringManager
import com.hieltech.haramblur.data.AppCategoryDetector
import com.hieltech.haramblur.utils.UrlUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class HaramBlurAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "HaramBlurAccessibilityService"
        private var instance: HaramBlurAccessibilityService? = null
        private const val EMERGENCY_RESET_ACTION = "com.hieltech.haramblur.EMERGENCY_RESET"

        fun getInstance(): HaramBlurAccessibilityService? = instance

        fun isServiceRunning(): Boolean = instance != null

        /**
         * Send emergency reset broadcast to force hide stuck overlays
         * Usage from ADB: adb shell am broadcast -a com.hieltech.haramblur.EMERGENCY_RESET
         */
        fun sendEmergencyResetBroadcast(context: Context) {
            val intent = Intent(EMERGENCY_RESET_ACTION)
            context.sendBroadcast(intent)
            Log.w(TAG, "Emergency reset broadcast sent")
        }
    }
    
    data class ServiceStatus(
        val isServiceRunning: Boolean = false,
        val isProcessingActive: Boolean = false,
        val isCapturingActive: Boolean = false,
        val isOverlayActive: Boolean = false,
        val lastProcessingTime: Long = 0L,
        val totalFramesProcessed: Long = 0L,
        val totalFramesSkipped: Long = 0L,
        val averageProcessingTime: Float = 0f,
        val lastError: String = ""
    )
    
    @Inject
    lateinit var screenCaptureManager: ScreenCaptureManager
    
    @Inject 
    lateinit var blurOverlayManager: BlurOverlayManager
    
    @Inject
    lateinit var contentDetectionEngine: ContentDetectionEngine
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var siteBlockingManager: SiteBlockingManager
    
    @Inject
    lateinit var quranicRepository: QuranicRepository

    @Inject
    lateinit var serviceLifecycleManager: com.hieltech.haramblur.di.ServiceLifecycleManager

    @Inject
    lateinit var foregroundAppMonitor: ForegroundAppMonitor
    
    @Inject
    lateinit var dhikrManager: com.hieltech.haramblur.services.DhikrManager

    @Inject
    lateinit var appFilteringManager: AppFilteringManager

    @Inject
    lateinit var appCategoryDetector: AppCategoryDetector



    // TODO: Behavioral action components temporarily disabled
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessingActive = false
    private var lastProcessingTime: Long = 0
    private var frameCount = 0

    // Current app tracking for app-specific filtering
    private var currentAppPackage: String? = null

    // Action throttling to prevent crashes
    private var lastActionTime: Long = 0
    private var isActionInProgress = false
    private var totalFramesProcessed: Long = 0
    private var totalFramesSkipped: Long = 0
    private var processingTimes = mutableListOf<Long>()
    private var lastServiceError: String = ""
    // Single-flight guards and coalescing
    private val pornClosureMutex = Mutex()
    private var pornClosureJob: Job? = null
    private val lastDomainBlockTimestamps = mutableMapOf<String, Long>()
    private val domainBlockCoalesceWindowMs = 10_000L // 10s coalescing to avoid thrash

    // Emergency reset broadcast receiver
    private val emergencyResetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EMERGENCY_RESET_ACTION) {
                Log.w(TAG, "Received emergency reset broadcast")
                emergencyReset()
            }
        }
    }
    
    // Blur stability tracking with detection caching
    private var recentNSFWDetections = mutableListOf<Pair<Long, Float>>()
    private var lastBlurStartTime: Long = 0
    private var isCurrentlyBlurred = false
    private var minBlurDuration = 2000L // Minimum 2 seconds of blur
    
    // Detection caching for stability
    private var detectionCache = mutableMapOf<String, Pair<Long, Boolean>>()
    private val cacheExpirationMs = 5000L // Cache results for 5 seconds
    private var lastBitmapHash: String? = null
    private var consecutiveNSFWCount = 0
    private var consecutiveCleanCount = 0
    private val requiredConsecutiveDetections = 1 // Immediate response for safety
    
    // Adaptive learning system
    private var adaptiveNSFWThreshold = 0.4f // Start lower, adapt based on content
    private var adaptiveGenderThreshold = 0.4f // Start lower for better female detection
    private var detectionHistory = mutableListOf<Pair<Long, Boolean>>() // Track success/failure
    private var lastAdaptationTime = 0L
    private val adaptationIntervalMs = 30000L // Adapt every 30 seconds
    private var currentUrl: String? = null
    private var lastUrlCheckTime = 0L
    private var isShowingBlockedSiteOverlay = false
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "HaramBlur Accessibility Service Created")

        // Register emergency reset broadcast receiver
        val filter = IntentFilter(EMERGENCY_RESET_ACTION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyResetReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(emergencyResetReceiver, filter)
        }
        Log.d(TAG, "Emergency reset broadcast receiver registered")

        // App launch interceptor is registered in manifest
        Log.d(TAG, "App launch interceptor ready")

        // Initialize components
        serviceScope.launch {
            initializeComponents()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()

        // Unregister emergency reset broadcast receiver
        try {
            unregisterReceiver(emergencyResetReceiver)
            Log.d(TAG, "Emergency reset broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering emergency reset receiver", e)
        }

        // App launch interceptor is managed by system
        Log.d(TAG, "App launch interceptor cleanup complete")

        // Stop app blocking monitor
        try {
            foregroundAppMonitor.stopMonitoring()
            Log.d(TAG, "ForegroundAppMonitor stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping ForegroundAppMonitor: ${e.message}")
        }

        // Clean up all components
        cleanupComponents()
        
        // Clean up dhikr manager
        try {
            dhikrManager.cleanup()
            Log.d(TAG, "DhikrManager cleaned up")
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up DhikrManager: ${e.message}")
        }

        cancelPendingPornClosure("service destroyed")
        serviceScope.cancel()
        instance = null
        Log.d(TAG, "HaramBlur Accessibility Service Destroyed")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "HaramBlur Accessibility Service Connected")
        
        serviceScope.launch {
            startContentMonitoring()

            // Start app blocking monitor
            try {
                foregroundAppMonitor.startMonitoring()
                Log.d(TAG, "ForegroundAppMonitor started successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start ForegroundAppMonitor: ${e.message}")
            }
        }

        // Observe dynamic settings changes for app filtering
        serviceScope.launch {
            appFilteringManager.detectionScopeFlow.collect { newScope ->
                Log.d(TAG, "App filtering settings updated: mode=${newScope.mode}, categories=${newScope.monitoredCategories.size}, custom=${newScope.customIncludedApps.size}")

                // Check if current app should still be monitored based on new scope
                val shouldMonitorCurrent = appFilteringManager.shouldMonitorApp(currentAppPackage)
                if (!shouldMonitorCurrent && screenCaptureManager.isCapturingActive()) {
                    Log.d(TAG, "Stopping screen capture due to settings change for app: $currentAppPackage")
                    screenCaptureManager.stopCapturing()
                } else if (shouldMonitorCurrent && !screenCaptureManager.isCapturingActive()) {
                    Log.d(TAG, "Starting screen capture due to settings change for app: $currentAppPackage")
                    // Note: This would need proper initialization with onScreenCaptured callback
                    // For now, we'll just log that it should start
                    Log.i(TAG, "Screen capture should be started but requires proper callback setup")
                }
            }
        }
    }
    
    private suspend fun initializeComponents() {
        try {
            Log.d(TAG, "Initializing HaramBlur components...")

            // Initialize all enhanced detection services
            serviceLifecycleManager.initializeServices()

            // Initialize detection engine
            val detectionInitialized = contentDetectionEngine.initialize(this@HaramBlurAccessibilityService)
            if (!detectionInitialized) {
                Log.w(TAG, "Content detection initialization failed")
            }

            // Initialize overlay manager
            blurOverlayManager.initialize(this@HaramBlurAccessibilityService)
            
            // Initialize dhikr manager
            dhikrManager.initialize(this@HaramBlurAccessibilityService)

            // Behavioral action components temporarily disabled for build

            Log.d(TAG, "HaramBlur components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
        }
    }



    private fun startContentMonitoring() {
        if (isProcessingActive) {
            Log.w(TAG, "Content monitoring already active")
            return
        }
        
        Log.d(TAG, "Starting content monitoring...")
        isProcessingActive = true
        
        // Set up warning dialog action handler for full screen warnings
        blurOverlayManager.onWarningAction = { action ->
            serviceScope.launch {
                handleWarningDialogAction(action)
            }
        }
        
        // Set up automatic navigation callback for when 6+ NSFW regions detected
        blurOverlayManager.onNavigateAwayAction = {
            serviceScope.launch {
                navigateAwayFromInappropriateContent()
            }
        }

        // Start screen capture with content analysis
        screenCaptureManager.startCapturing { bitmap ->
            serviceScope.launch {
                processScreenContent(bitmap)
            }
        }
    }
    
    private fun stopContentMonitoring() {
        Log.d(TAG, "Stopping content monitoring...")
        isProcessingActive = false
        
        screenCaptureManager.stopCapturing()
        blurOverlayManager.hideBlurOverlay()
    }
    
    private suspend fun processScreenContent(bitmap: Bitmap) {
        if (!isProcessingActive) {
            Log.d(TAG, "🚫 Processing not active, skipping screen content analysis")
            return
        }
        
        Log.d(TAG, "📸 Processing screen content - Size: ${bitmap.width}x${bitmap.height}")
        
        val currentTime = System.currentTimeMillis()
        
        // Generate bitmap hash for caching
        val bitmapHash = generateBitmapHash(bitmap)
        
        // Check cache first
        val cachedResult = detectionCache[bitmapHash]
        if (cachedResult != null && (currentTime - cachedResult.first) < cacheExpirationMs) {
            Log.d(TAG, "💾 Using cached detection result: ${cachedResult.second}")
            handleCachedResult(cachedResult.second)
            return
        }
        
        // Clean old detection history and cache
        recentNSFWDetections.removeAll { (timestamp, _) -> 
            currentTime - timestamp > 10000L 
        }
        detectionCache.entries.removeAll { (_, value) ->
            currentTime - value.first > cacheExpirationMs
        }
        
        // Check if real-time processing is enabled and service is not paused
        val currentSettings = settingsRepository.getCurrentSettings()
        if (!currentSettings.enableRealTimeProcessing) {
            Log.d(TAG, "⏸️ Real-time processing disabled in settings")
            return
        }
        
        if (currentSettings.isServicePaused) {
            Log.d(TAG, "⏸️ Service is paused - skipping all processing")
            // Hide any existing blur overlay when paused
            blurOverlayManager.hideBlurOverlay()
            return
        }
        
        Log.d(TAG, "⚙️ Processing with settings: Female blur=${currentSettings.blurFemaleFaces}, Male blur=${currentSettings.blurMaleFaces}, NSFW=${currentSettings.enableNSFWDetection}, GPU=${currentSettings.enableGPUAcceleration}")
        
        // Get dynamic processing cooldown from settings
        val interval = getProcessingInterval(currentSettings)
        if (currentTime - lastProcessingTime < interval) {
            Log.v(TAG, "⏱️ Throttling: Skipping processing (interval: ${interval}ms)")
            totalFramesSkipped++
            return
        }
        lastProcessingTime = currentTime
        Log.d(TAG, "🔄 Processing interval OK, proceeding with analysis")
        
        try {
            Log.d(TAG, "Processing screen content: ${bitmap.width}x${bitmap.height} (speed: ${currentSettings.processingSpeed})")
            
            // Check app filtering first - early exit if app should not be monitored
            val shouldMonitor = shouldMonitorCurrentApp()
            if (!shouldMonitor) {
                Log.d(TAG, "Content monitoring skipped for app: $currentAppPackage (not in monitored categories)")
                // Hide any existing overlays when switching to unmonitored app
                blurOverlayManager.hideBlurOverlay()
                isCurrentlyBlurred = false
                return
            }

            // Analyze content using detection engine with user settings
            Log.d(TAG, "🧠 Starting content analysis with detection engine...")
            val analysisResult = contentDetectionEngine.analyzeContent(bitmap, currentSettings, currentAppPackage)
            
            if (analysisResult.isSuccessful()) {
                Log.d(TAG, "✅ Content analysis successful, handling results...")

                // Behavioral actions temporarily disabled for build

                // Handle traditional blur overlay based on action results
                val shouldBlur = handleAnalysisResultWithStability(analysisResult, currentSettings)
                detectionCache[bitmapHash] = Pair(currentTime, shouldBlur)
                lastBitmapHash = bitmapHash

            } else {
                Log.w(TAG, "❌ Content analysis failed: ${analysisResult.error}")
                // Don't immediately hide blur on failure - maintain current state
                if (!isCurrentlyBlurred || (currentTime - lastBlurStartTime) > minBlurDuration) {
                    blurOverlayManager.hideBlurOverlay()
                    isCurrentlyBlurred = false
                }
            }
        } catch (e: Exception) {
            lastServiceError = "${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "💥 Critical error processing screen content", e)
            Log.e(TAG, "   • Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   • Error message: ${e.message}")
            blurOverlayManager.hideBlurOverlay()
        } finally {
            totalFramesProcessed++
            // Track processing time
            val processingTime = System.currentTimeMillis() - currentTime
            processingTimes.add(processingTime)
            if (processingTimes.size > 50) {
                processingTimes.removeAt(0) // Keep only last 50 measurements
            }
        }
    }

    /**
     * Emergency method to force hide all overlays
     * Can be called from debug screen or when overlays get stuck
     */
    fun emergencyHideAllOverlays() {
        try {
            Log.w(TAG, "EMERGENCY: Force hiding all overlays from accessibility service")
            blurOverlayManager.emergencyHideAllOverlays()
            isCurrentlyBlurred = false
            lastBlurStartTime = 0
            Log.w(TAG, "EMERGENCY: All overlays hidden and blur state reset")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency hide overlays", e)
        }
    }
    
    /**
     * Get current service status for diagnostics
     */
    fun getServiceStatus(): ServiceStatus {
        val averageTime = if (processingTimes.isNotEmpty()) {
            processingTimes.average().toFloat()
        } else 0f
        
        return ServiceStatus(
            isServiceRunning = isServiceRunning(),
            isProcessingActive = isProcessingActive,
            isCapturingActive = screenCaptureManager.isCapturingActive(),
            isOverlayActive = isCurrentlyBlurred,
            lastProcessingTime = lastProcessingTime,
            totalFramesProcessed = totalFramesProcessed,
            totalFramesSkipped = totalFramesSkipped,
            averageProcessingTime = averageTime,
            lastError = lastServiceError
        )
    }
    
    private fun handleAnalysisResultWithStability(result: ContentDetectionEngine.ContentAnalysisResult, settings: AppSettings): Boolean {
        val currentTime = System.currentTimeMillis()
        
        Log.d(TAG, "📊 Analysis result: shouldBlur=${result.shouldBlur}, regions=${result.blurRegions.size}")
        
        // Adapt thresholds based on learning
        adaptThresholds(currentTime)
        
        // Use adaptive thresholds for better detection
        val nsfwThreshold = minOf(adaptiveNSFWThreshold, settings.nsfwConfidenceThreshold)
        val genderThreshold = minOf(adaptiveGenderThreshold, settings.genderConfidenceThreshold)
        
        Log.d(TAG, "🧠 Using adaptive thresholds: NSFW=$nsfwThreshold, Gender=$genderThreshold")
        
        // ULTRA-PRECISE FEMALE-ONLY DETECTION
        val hasFemaleFaces = if (settings.blurFemaleFaces) {
            result.faceDetectionResult?.detectedFaces?.any { face ->
                val isConfidentFemale = face.genderConfidence > genderThreshold && 
                                       face.estimatedGender.toString().contains("FEMALE", ignoreCase = true)
                val isPossibleFemale = face.genderConfidence < 0.7f && 
                                     !face.estimatedGender.toString().contains("MALE", ignoreCase = true)
                
                Log.d(TAG, "👩 Female analysis: confidence=${face.genderConfidence}, gender=${face.estimatedGender}, confident=$isConfidentFemale, possible=$isPossibleFemale")
                
                // STRICT: Only blur if confident female OR uncertain (but not confident male)
                isConfidentFemale || (isPossibleFemale && settings.detectionSensitivity > 0.6f)
            } ?: false
        } else {
            Log.d(TAG, "👩 Female face detection disabled in settings")
            false
        }
        
        // More sensitive NSFW detection  
        val hasNSFWContent = result.nsfwDetectionResult?.let { nsfwResult ->
            val isHighConfidenceNSFW = nsfwResult.isNSFW && nsfwResult.confidence > nsfwThreshold
            val isMediumConfidenceNSFW = nsfwResult.confidence > (nsfwThreshold * 0.7f) // 70% of threshold
            val isAnyNSFWIndicator = nsfwResult.confidence > 0.2f // Very low threshold for any indication
            
            Log.d(TAG, "🔞 NSFW analysis: confidence=${nsfwResult.confidence}, isNSFW=${nsfwResult.isNSFW}, threshold=$nsfwThreshold")
            Log.d(TAG, "🔞 NSFW levels: high=$isHighConfidenceNSFW, medium=$isMediumConfidenceNSFW, any=$isAnyNSFWIndicator")
            
            // Blur for various levels based on sensitivity
            when {
                isHighConfidenceNSFW -> true
                isMediumConfidenceNSFW && settings.detectionSensitivity > 0.6f -> true
                isAnyNSFWIndicator && settings.detectionSensitivity > 0.8f -> true
                else -> false
            }
        } ?: false
        
        Log.d(TAG, "🔍 Detection summary: Female faces=$hasFemaleFaces, NSFW=$hasNSFWContent")
        Log.d(TAG, "⚙️ Settings: blurFemaleFaces=${settings.blurFemaleFaces}, blurMaleFaces=${settings.blurMaleFaces}, enableNSFW=${settings.enableNSFWDetection}, sensitivity=${settings.detectionSensitivity}")
        
        // Record detection for learning
        val detectedInappropriate = hasFemaleFaces || hasNSFWContent
        detectionHistory.add(Pair(currentTime, detectedInappropriate))
        
        // Determine if blur should be shown based on content type
        val shouldBlurBasedOnContent = when {
            hasFemaleFaces && settings.blurFemaleFaces -> {
                Log.d(TAG, "👩 ❗ Female face detected - TRIGGERING BLUR")
                true
            }
            hasNSFWContent && settings.enableNSFWDetection -> {
                Log.d(TAG, "🔞 ❗ NSFW content detected - TRIGGERING BLUR")
                true
            }
            // Emergency fallback - if any blur regions exist, blur them
            result.blurRegions.isNotEmpty() -> {
                Log.d(TAG, "⚠️ Fallback - blur regions detected, applying blur for safety")
                true
            }
            else -> {
                Log.d(TAG, "✅ No inappropriate content detected")
                false
            }
        }
        
        // Apply consecutive detection logic to prevent false positives
        if (shouldBlurBasedOnContent) {
            consecutiveNSFWCount++
            consecutiveCleanCount = 0
            Log.d(TAG, "🔴 Inappropriate content count: $consecutiveNSFWCount")
        } else {
            consecutiveCleanCount++
            consecutiveNSFWCount = 0
            Log.d(TAG, "✅ Clean content count: $consecutiveCleanCount")
        }
        
        // Immediate blur for safety - no consecutive requirements for unsafe content
        val shouldShowBlur = when {
            // IMMEDIATE blur for any inappropriate content
            shouldBlurBasedOnContent -> {
                if (!isCurrentlyBlurred) {
                    Log.d(TAG, "🛑 ⚡ IMMEDIATE BLUR TRIGGERED - Safety first!")
                    isCurrentlyBlurred = true
                    lastBlurStartTime = currentTime
                }
                true
            }
            // Continue blur if within minimum duration
            isCurrentlyBlurred -> {
                val timeSinceBlurStart = currentTime - lastBlurStartTime
                if (timeSinceBlurStart < minBlurDuration) {
                    Log.d(TAG, "⏰ Maintaining blur (min duration: ${timeSinceBlurStart}ms)")
                    true
                } else {
                    Log.d(TAG, "🔓 Stopping blur - content appears clean")
                    isCurrentlyBlurred = false
                    false
                }
            }
            else -> false
        }
        


        // Handle AUTOMATIC ACTIONS for 6+ NSFW regions (RULE-BASED APPROACH)
        if (result.fullScreenBlurDecision?.recommendedAction != null) {
            val action = result.fullScreenBlurDecision.recommendedAction
            when (action) {
                ContentAction.SCROLL_AWAY -> {
                    Log.d(TAG, "🔄 SCROLL_AWAY action triggered for ${result.nsfwRegionCount} regions")
                    performScrollAwayAction()
                    return false // Don't show blur overlay
                }
                ContentAction.NAVIGATE_BACK -> {
                    Log.d(TAG, "⬅️ NAVIGATE_BACK action triggered for ${result.nsfwRegionCount} regions")
                    performNavigateBackAction()
                    return false // Don't show blur overlay
                }
                ContentAction.AUTO_CLOSE_APP -> {
                    Log.d(TAG, "🚫 AUTO_CLOSE_APP action triggered for ${result.nsfwRegionCount} regions")
                    performAutoCloseAppAction()
                    return false // Don't show blur overlay
                }
                ContentAction.GENTLE_REDIRECT -> {
                    Log.d(TAG, "🔄 GENTLE_REDIRECT action triggered for ${result.nsfwRegionCount} regions")
                    performGentleRedirectAction()
                    return false // Don't show blur overlay
                }
                else -> {
                    // Fall through to traditional handling
                }
            }
        }

        // Handle FULL SCREEN WARNING - only for traditional density-based triggers
        if (result.requiresFullScreenWarning && shouldShowBlur && 
            result.fullScreenBlurDecision?.recommendedAction !in listOf(
                ContentAction.SCROLL_AWAY, ContentAction.NAVIGATE_BACK, 
                ContentAction.AUTO_CLOSE_APP, ContentAction.GENTLE_REDIRECT)) {
            
            Log.d(TAG, "🚨 FULL SCREEN WARNING TRIGGERED - showing warning dialog")

            try {
                // Show full screen warning with region-based information
                blurOverlayManager.showFullScreenWarning(
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    customMessage = "Multiple inappropriate content regions detected",
                    reflectionTimeSeconds = result.fullScreenBlurDecision?.reflectionTimeSeconds ?: 30,
                    nsfwRegionCount = result.nsfwRegionCount,
                    maxNsfwConfidence = result.maxNsfwConfidence,
                    triggeredByRegionCount = result.triggeredByRegionCount
                )

                isCurrentlyBlurred = true
                lastBlurStartTime = currentTime
                Log.d(TAG, "🚨 Full screen warning displayed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ CRITICAL ERROR: Failed to show full screen warning", e)

                // Emergency fallback - try to hide any existing overlays
                try {
                    blurOverlayManager.emergencyHideAllOverlays()
                    Log.w(TAG, "EMERGENCY: All overlays hidden after full screen warning failure")
                } catch (emergencyError: Exception) {
                    Log.e(TAG, "❌ CRITICAL: Emergency overlay hide also failed", emergencyError)
                }

                // Reset state to prevent stuck overlays
                isCurrentlyBlurred = false
                return false
            }

            return true
        }

        // Apply selective blur decision with PRECISION TARGETING ONLY
        if (shouldShowBlur && result.blurRegions.isNotEmpty()) {
            // Get display metrics for proper scaling
            val displayMetrics = this.resources?.displayMetrics
            val screenWidth = displayMetrics?.widthPixels ?: 1080
            val screenHeight = displayMetrics?.heightPixels ?: 2400

            // Scale and validate blur regions to screen resolution
            val preciseRegions = result.blurRegions.mapNotNull { region ->
                // Ensure region is within screen bounds
                val scaledRegion = android.graphics.Rect(
                    maxOf(0, region.left),
                    maxOf(0, region.top),
                    minOf(screenWidth, region.right),
                    minOf(screenHeight, region.bottom)
                )

                // Only include regions that are valid and not too small
                if (scaledRegion.width() > 20 && scaledRegion.height() > 20) {
                    scaledRegion
                } else null
            }

            if (preciseRegions.isNotEmpty()) {
                blurOverlayManager.showBlurOverlay(preciseRegions, settings.blurIntensity)
                Log.d(TAG, "🎯 PRECISION BLUR: ${preciseRegions.size} targeted regions (screen: ${screenWidth}x${screenHeight})")
            } else {
                Log.d(TAG, "⚠️ No valid precision regions - blur skipped")
                blurOverlayManager.hideBlurOverlay()
            }
        } else {
            blurOverlayManager.hideBlurOverlay()
            Log.d(TAG, "🔓 Blur hidden - no content detected or no precise regions")
        }
        
        // Log detailed results
        logDetailedResults(result)
        
        return shouldShowBlur
    }
    
    private fun adaptThresholds(currentTime: Long) {
        // Adapt thresholds every 30 seconds based on detection history
        if (currentTime - lastAdaptationTime > adaptationIntervalMs) {
            lastAdaptationTime = currentTime
            
            // Clean old history (keep last 10 minutes)
            detectionHistory.removeAll { (timestamp, _) -> 
                currentTime - timestamp > 600000L 
            }
            
            val recentDetections = detectionHistory.takeLast(20)
            if (recentDetections.size >= 5) {
                val inappropriateRatio = recentDetections.count { it.second }.toFloat() / recentDetections.size
                
                Log.d(TAG, "🧠 Learning: ${recentDetections.size} recent detections, $inappropriateRatio inappropriate ratio")
                
                // Adapt NSFW threshold based on detection patterns
                adaptiveNSFWThreshold = when {
                    inappropriateRatio < 0.1f -> {
                        // Too few detections, lower threshold for better sensitivity
                        maxOf(0.2f, adaptiveNSFWThreshold - 0.05f)
                    }
                    inappropriateRatio > 0.8f -> {
                        // Too many false positives, raise threshold slightly
                        minOf(0.7f, adaptiveNSFWThreshold + 0.02f)
                    }
                    else -> adaptiveNSFWThreshold // Keep current
                }
                
                // Adapt gender threshold similarly
                adaptiveGenderThreshold = when {
                    inappropriateRatio < 0.1f -> {
                        maxOf(0.3f, adaptiveGenderThreshold - 0.03f)
                    }
                    inappropriateRatio > 0.8f -> {
                        minOf(0.6f, adaptiveGenderThreshold + 0.02f)
                    }
                    else -> adaptiveGenderThreshold
                }
                
                Log.d(TAG, "🎯 Adapted thresholds: NSFW=$adaptiveNSFWThreshold, Gender=$adaptiveGenderThreshold")
            }
        }
    }
    
    private fun generateBitmapHash(bitmap: Bitmap): String {
        // Generate a simple hash based on bitmap properties and sample pixels
        val width = bitmap.width
        val height = bitmap.height
        val centerPixel = if (width > 0 && height > 0) {
            bitmap.getPixel(width / 2, height / 2)
        } else 0
        val cornerPixel = if (width > 10 && height > 10) {
            bitmap.getPixel(width / 10, height / 10)
        } else 0
        
        return "${width}x${height}_${centerPixel}_${cornerPixel}_${System.currentTimeMillis() / 1000}"
    }
    
    private fun handleCachedResult(shouldBlur: Boolean) {
        if (shouldBlur && !isCurrentlyBlurred) {
            // PRECISION CACHED RESULT - NO FULL SCREEN
            Log.d(TAG, "⚠️ Cached result shows blur needed but no precise regions available - skipping")
        } else if (!shouldBlur && isCurrentlyBlurred) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastBlurStartTime) > minBlurDuration) {
                isCurrentlyBlurred = false
                blurOverlayManager.hideBlurOverlay()
                Log.d(TAG, "🔓 Removed blur based on cached result")
            }
        }
    }
    
    private fun logDetailedResults(result: ContentDetectionEngine.ContentAnalysisResult) {
        result.faceDetectionResult?.let { faceResult ->
            if (faceResult.facesDetected > 0) {
                Log.d(TAG, "👤 Faces detected: ${faceResult.facesDetected}")
            }
        }
        
        result.nsfwDetectionResult?.let { nsfwResult ->
            if (nsfwResult.isNSFW) {
                Log.d(TAG, "🔞 NSFW content detected with confidence: ${nsfwResult.confidence}")
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val newPackageName = event.packageName?.toString()

                // Update current app package tracking
                if (currentAppPackage != newPackageName) {
                    val oldApp = currentAppPackage
                    currentAppPackage = newPackageName
                    Log.d(TAG, "App changed from: $oldApp to: $currentAppPackage")
                }

                Log.d(TAG, "Window state changed: ${event.packageName}")

                // Check app filtering and control screen capture
                serviceScope.launch {
                    try {
                        val shouldMonitor = appFilteringManager.shouldMonitorApp(currentAppPackage)
                        Log.d(TAG, "App changed to $currentAppPackage, should monitor: $shouldMonitor")
                        
                        if (!shouldMonitor) {
                            // Stop screen capture for non-monitored apps
                            if (screenCaptureManager.isCapturingActive()) {
                                Log.d(TAG, "Stopping screen capture for non-monitored app: $currentAppPackage")
                                screenCaptureManager.stopCapturing()
                                blurOverlayManager.hideBlurOverlay()
                                isCurrentlyBlurred = false
                            }
                        } else {
                            // Start screen capture for monitored apps (if needed and service is active)
                            if (!screenCaptureManager.isCapturingActive() && isProcessingActive) {
                                Log.d(TAG, "Starting screen capture for monitored app: $currentAppPackage")
                                startContentMonitoring()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling app change for filtering", e)
                        // Continue with default behavior on error
                    }
                }

                // Check for stuck overlays when app changes
                blurOverlayManager.checkForStuckOverlays(event.packageName?.toString())

                // Check for URL changes in browser apps
                serviceScope.launch {
                    checkForUrlChanges(event)
                }

                // Trigger immediate content analysis when window changes
                serviceScope.launch {
                    delay(500) // Small delay to let window settle
                    if (isProcessingActive) {
                        // Force immediate processing for window changes
                        lastProcessingTime = 0L
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Check for URL changes in content updates
                serviceScope.launch {
                    checkForUrlChanges(event)
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                Log.d(TAG, "Windows changed")
                
                // Multiple windows changed - ensure we're monitoring correctly
                if (isProcessingActive && !screenCaptureManager.isCapturingActive()) {
                    startContentMonitoring()
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "HaramBlur Accessibility Service Interrupted")
        stopContentMonitoring()
    }
    
    private fun cleanupComponents() {
        try {
            stopContentMonitoring()
            contentDetectionEngine.cleanup()
            // blurOverlayManager cleanup is handled in stopContentMonitoring
            
            // Cleanup all enhanced detection services
            serviceLifecycleManager.cleanupServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    // Public methods for external control
    fun pauseProcessing() {
        Log.d(TAG, "Processing paused by user")
        stopContentMonitoring()
    }

    fun resumeProcessing() {
        Log.d(TAG, "Processing resumed by user")
        serviceScope.launch {
            startContentMonitoring()
        }
    }

    /**
     * Emergency method to force hide all overlays and reset state
     * Call this if overlays get stuck (e.g., via ADB)
     */
    fun emergencyReset() {
        Log.w(TAG, "EMERGENCY RESET: Force hiding all overlays and resetting state")

        serviceScope.launch {
            try {
                // Force stop processing
                isProcessingActive = false
                isCurrentlyBlurred = false
                consecutiveNSFWCount = 0
                consecutiveCleanCount = 0

                // Clear all caches
                detectionCache.clear()
                recentNSFWDetections.clear()

                // Emergency hide all overlays
                blurOverlayManager.emergencyHideAllOverlays()

                // Stop screen capture
                screenCaptureManager.stopCapturing()

                Log.w(TAG, "EMERGENCY RESET: All overlays hidden and state reset successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ CRITICAL ERROR: Emergency reset failed", e)

                // Last resort - try to reset state even if overlay hide fails
                isCurrentlyBlurred = false
                isProcessingActive = false
                detectionCache.clear()
                recentNSFWDetections.clear()
            }
        }
    }

    
    private fun getProcessingInterval(settings: AppSettings): Long {
        return when (settings.processingSpeed) {
            ProcessingSpeed.FAST -> 50L  // Faster for immediate response
            ProcessingSpeed.BALANCED -> 100L  // Reduced for better responsiveness 
            ProcessingSpeed.BATTERY_SAVER -> 300L  // Still faster than before
            ProcessingSpeed.ULTRA_FAST -> 25L  // Super fast for real-time
        }
    }
    
    /**
     * Check for URL changes in accessibility events with enhanced porn detection
     */
    private suspend fun checkForUrlChanges(event: AccessibilityEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()

        // Throttle URL checking to avoid excessive processing
        if (currentTime - lastUrlCheckTime < 500) { // Reduced from 1000ms for faster detection
            return
        }
        lastUrlCheckTime = currentTime

        try {
            val packageName = event.packageName?.toString()

            // Check app filtering first - skip if app should not be monitored
            try {
                if (!appFilteringManager.shouldMonitorApp(packageName)) {
                    Log.d(TAG, "URL checking skipped for non-monitored app: $packageName")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking app monitoring for URL check: $packageName", e)
                // Continue with URL check on error to prevent missing blocks
            }

            // Only check URLs for browser apps and web-based apps
            if (isBrowserApp(packageName)) {
                val extractedUrl = extractUrlFromAccessibilityEvent(event)
                if (extractedUrl != null && extractedUrl != currentUrl) {
                    currentUrl = extractedUrl
                    cancelPendingPornClosure("new URL detected: $extractedUrl")

                    // Detect if likely in private mode for logging
                    val isPrivateMode = isLikelyPrivateMode(packageName, extractedUrl)
                    val modeIndicator = if (isPrivateMode) "🔒 PRIVATE" else "🌐 NORMAL"

                    Log.d(TAG, "$modeIndicator URL detected: $currentUrl (Browser: $packageName)")

                    // Enhanced porn detection before full blocking check
                    if (isLikelyPornUrl(extractedUrl)) {
                        Log.w(TAG, "🚨 LIKELY PORN URL DETECTED: $extractedUrl")
                        handleLikelyPornUrl(extractedUrl, packageName)
                    } else {
                        // Check if the URL should be blocked
                        checkAndBlockUrl(extractedUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL changes", e)
        }
    }

    /**
     * Quick check if URL is likely porn content
     */
    private fun isLikelyPornUrl(url: String): Boolean {
        val lowercaseUrl = url.lowercase()

        // Quick porn keywords for immediate detection
        val quickPornKeywords = listOf(
            "porn", "sex", "xxx", "nude", "naked", "fuck", "pussy", "dick",
            "boobs", "tits", "ass", "cum", "blowjob", "handjob", "anal",
            "milf", "teen", "gay", "lesbian", "hentai", "onlyfans"
        )

        // Check for porn TLDs
        val pornTlds = listOf(".porn", ".sex", ".xxx", ".cam", ".tube", ".video")
        for (tld in pornTlds) {
            if (lowercaseUrl.contains(tld)) {
                return true
            }
        }

        // Check for porn keywords
        for (keyword in quickPornKeywords) {
            if (lowercaseUrl.contains(keyword)) {
                return true
            }
        }

        // Check for suspicious patterns
        if (lowercaseUrl.contains("%") && lowercaseUrl.length > 50) {
            return true // Likely encoded porn content
        }

        return false
    }

    /**
     * Handle likely porn URL with immediate action
     */
    private suspend fun handleLikelyPornUrl(url: String, packageName: String?) {
        try {
            Log.w(TAG, "🚨 Handling likely porn URL: $url")

            // Create immediate blocking result
            val immediateBlockingResult = com.hieltech.haramblur.detection.SiteBlockingResult(
                isBlocked = true,
                category = BlockingCategory.EXPLICIT_CONTENT,
                confidence = 0.95f,
                quranicVerse = null,
                reflectionTimeSeconds = 30,
                matchedPattern = "immediate_porn_detection",
                blockingReason = "Immediate porn content detection"
            )

            // Immediate action for porn URLs
            handlePornSiteBlocking(url, immediateBlockingResult)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling likely porn URL", e)
            // Fallback to standard blocking
            checkAndBlockUrl(url)
        }
    }
    
    /**
     * Extract URL from accessibility node info
     */
    private fun extractUrlFromAccessibilityEvent(event: AccessibilityEvent?): String? {
        if (event?.source == null) return null
        
        return try {
            // Try to find URL in various ways
            extractUrlFromNodeInfo(event.source) ?: 
            extractUrlFromText(event.text?.toString()) ?: 
            extractUrlFromContentDescription(event.contentDescription?.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from accessibility event", e)
            null
        }
    }
    
    /**
     * Extract URL from accessibility node info recursively
     */
    private fun extractUrlFromNodeInfo(nodeInfo: AccessibilityNodeInfo?): String? {
        if (nodeInfo == null) return null
        
        try {
            // First try browser-specific extraction
            val packageName = nodeInfo.packageName?.toString()
            val browserSpecificUrl = extractUrlFromBrowserSpecific(packageName, nodeInfo)
            if (browserSpecificUrl != null) return browserSpecificUrl
            
            // Fallback to generic extraction
            val url = extractUrlFromText(nodeInfo.text?.toString()) ?:
                     extractUrlFromText(nodeInfo.contentDescription?.toString()) ?:
                     extractUrlFromText(nodeInfo.viewIdResourceName)
            
            if (url != null) return url
            
            // Recursively check child nodes (limit depth to avoid performance issues)
            for (i in 0 until minOf(nodeInfo.childCount, 10)) {
                val child = nodeInfo.getChild(i)
                val childUrl = extractUrlFromNodeInfo(child)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from node info", e)
            return null
        }
    }
    
    /**
     * Extract URL from text using pattern matching
     */
    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        
        // Common URL patterns
        val urlPatterns = listOf(
            Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE),
            Regex("www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE),
            Regex("[\\w\\-._~]+\\.[a-zA-Z]{2,}[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in urlPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                var url = match.value
                
                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                
                return url
            }
        }
        
        return null
    }
    
    /**
     * Extract URL from content description
     */
    private fun extractUrlFromContentDescription(description: String?): String? {
        return extractUrlFromText(description)
    }
    
    /**
     * Check if the package is a browser or web-based app
     */
    private fun isBrowserApp(packageName: String?): Boolean {
        if (packageName == null) return false
        return appCategoryDetector.isBrowserApp(packageName)
    }

    /**
     * Detect if browser is likely in private/incognito mode
     * Note: This is a heuristic and may not be 100% accurate
     */
    private fun isLikelyPrivateMode(packageName: String?, url: String?): Boolean {
        if (packageName == null) return false

        // Firefox Focus is always private mode
        if (packageName == "org.mozilla.focus") {
            return true
        }

        // DuckDuckGo private mode indicators
        if (packageName.contains("duckduckgo") && url?.contains("duckduckgo.com") == true) {
            // Could be private search, but not definitive
        }

        // Chrome private mode - harder to detect via accessibility
        // Would need to look for specific UI indicators

        // For now, we assume the detection works the same in private mode
        // The main difference would be in URL bar resource IDs or UI structure
        return false // Conservative approach
    }
    
    /**
     * Check URL against site blocking manager and block if necessary
     * Enhanced with immediate porn site blocking
     */
    private suspend fun checkAndBlockUrl(url: String) {
        try {
            val blockingResult = siteBlockingManager.checkUrl(url)

            if (blockingResult.isBlocked) {
                Log.d(TAG, "Blocking URL: $url (Category: ${blockingResult.category})")

                // Enhanced blocking for porn sites
                if (isPornCategory(blockingResult.category)) {
                    handlePornSiteBlocking(url, blockingResult)
                } else {
                    // Standard blocking for other categories
                    showBlockedSiteOverlay(blockingResult)

                    // Optionally navigate away from the blocked site
                    if (shouldNavigateAwayFromBlockedSite(blockingResult)) {
                        navigateAwayFromBlockedSite()
                    }
                }
            } else {
                // Hide any existing blocked site overlay
                hideBlockedSiteOverlay()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL for blocking", e)
        }
    }

    /**
     * Check if the blocking category is related to porn/adult content
     */
    private fun isPornCategory(category: BlockingCategory?): Boolean {
        return category in listOf(
            BlockingCategory.EXPLICIT_CONTENT,
            BlockingCategory.ADULT_ENTERTAINMENT,
            BlockingCategory.INAPPROPRIATE_IMAGERY
        )
    }

    private fun cancelPendingPornClosure(reason: String? = null) {
        val job = pornClosureJob ?: return

        if (!job.isActive) {
            pornClosureJob = null
            return
        }

        if (pornClosureMutex.isLocked) {
            val suffix = reason?.let { ": $it" } ?: ""
            Log.d(TAG, "Porn closure already running; skipping cancellation$suffix")
            return
        }

        val suffix = reason?.let { ": $it" } ?: ""
        Log.d(TAG, "Cancelling pending porn closure$suffix")
        job.cancel()
        pornClosureJob = null
    }

    private suspend fun runSerializedPornClosure(block: suspend () -> Unit) {
        pornClosureMutex.withLock {
            block()
        }
    }

    /**
     * Handle porn site blocking with enhanced measures and automatic actions
     */
    private suspend fun handlePornSiteBlocking(url: String, blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        val startTime = System.currentTimeMillis()
        Log.w(TAG, "🚫 PORN SITE DETECTED: $url (Category: ${blockingResult.category})")

        try {
            // Debounce per-domain to avoid re-entrant overlays/closures
            val domain = UrlUtils.extractDomain(url)
            val lastTs = lastDomainBlockTimestamps[domain] ?: 0L
            if (System.currentTimeMillis() - lastTs < domainBlockCoalesceWindowMs) {
                Log.d(TAG, "Coalesced porn block for domain=$domain; recently handled")
                return
            }
            lastDomainBlockTimestamps[domain] = System.currentTimeMillis()

            // Always show reflection overlay for porn/adult categories
            showPornBlockingOverlay(blockingResult)
            logPornBlockingEvent(url, blockingResult, "overlay_displayed")

            // Schedule automatic closure after reflection time + small buffer
            val reflectionSeconds = when (blockingResult.category) {
                BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT ->
                    blockingResult.reflectionTimeSeconds.coerceAtLeast(10)
                else -> blockingResult.reflectionTimeSeconds
            }

            val bufferMs = 1500L
            val totalWait = (reflectionSeconds * 1000L) + bufferMs
            cancelPendingPornClosure("scheduling auto-closure for $url")
            val job = serviceScope.launch {
                try {
                    Log.d(TAG, "Scheduling aggressive close in ${totalWait}ms for porn category")
                    delay(totalWait)
                    runSerializedPornClosure {
                        if (!isShowingBlockedSiteOverlay) {
                            Log.d(TAG, "Skipping porn auto-closure because overlay is no longer visible")
                            return@runSerializedPornClosure
                        }

                        logPornBlockingEvent(url, blockingResult, "auto_closure_initiated")
                        executeAggressivePornSiteClosure()
                        logPornBlockingEvent(
                            url,
                            blockingResult,
                            "auto_closure_completed",
                            System.currentTimeMillis() - startTime
                        )
                    }
                } catch (cancellation: CancellationException) {
                    Log.d(TAG, "Porn auto-closure job cancelled before execution")
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing porn auto-closure job", e)
                } finally {
                    val currentJob = coroutineContext[Job]
                    if (pornClosureJob === currentJob) {
                        pornClosureJob = null
                    }
                }
            }
            pornClosureJob = job
        } catch (e: Exception) {
            Log.e(TAG, "Error handling porn site blocking", e)
            logPornBlockingEvent(url, blockingResult, "error_occurred", error = e.message)
            // Fallback to standard blocking
            try { showBlockedSiteOverlay(blockingResult) } catch (_: Exception) {}
        }
    }

    /**
     * Log porn blocking events for analytics and accountability
     */
    private fun logPornBlockingEvent(
        url: String,
        blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult,
        eventType: String,
        duration: Long = 0,
        error: String? = null
    ) {
        try {
            val timestamp = System.currentTimeMillis()
            val domain = UrlUtils.extractDomain(url)

            // Create analytics log entry
            val analyticsEntry = PornBlockingAnalytics(
                timestamp = timestamp,
                url = url,
                domain = domain,
                category = blockingResult.category?.name ?: "UNKNOWN",
                confidence = blockingResult.confidence,
                matchedPattern = blockingResult.matchedPattern ?: "",
                eventType = eventType,
                duration = duration,
                error = error
            )

            // Log to system log for now (could be enhanced to save to database)
            Log.i(TAG, "📊 PORN_BLOCKING_ANALYTICS: ${analyticsEntry.toLogString()}")

            // TODO: Save to analytics database for long-term tracking
            // savePornBlockingAnalytics(analyticsEntry)

        } catch (e: Exception) {
            Log.w(TAG, "Error logging porn blocking event", e)
        }
    }

    /**
     * Data class for porn blocking analytics
     */
    data class PornBlockingAnalytics(
        val timestamp: Long,
        val url: String,
        val domain: String,
        val category: String,
        val confidence: Float,
        val matchedPattern: String,
        val eventType: String,
        val duration: Long = 0,
        val error: String? = null
    ) {
        fun toLogString(): String {
            return "ts=$timestamp,domain=$domain,cat=$category,conf=$confidence,pattern=$matchedPattern,event=$eventType,duration=${duration}ms${error?.let { ",error=$it" } ?: ""}"
        }
    }

    /**
     * Perform immediate closure for high-confidence porn sites
     */
    private suspend fun performImmediatePornSiteClosure(url: String, blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        try {
            Log.w(TAG, "🚨 IMMEDIATE PORN SITE CLOSURE: $url")

            cancelPendingPornClosure("immediate porn closure triggered")

            // Quick Quranic flash (1 second)
            showQuickQuranicWarning(blockingResult)

            // Immediate closure
            delay(1000) // Brief pause for warning visibility
            performAggressivePornSiteClosure()

            // Log for accountability
            Log.w(TAG, "✅ Immediate porn site closure completed for: $url")

        } catch (e: Exception) {
            Log.e(TAG, "Error in immediate porn site closure", e)
            // Fallback to aggressive closure
            performAggressivePornSiteClosure()
        }
    }

    /**
     * Show quick Quranic warning for immediate closures
     */
    private suspend fun showQuickQuranicWarning(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        try {
            // Show a brief full-screen warning with Quranic verse
            blurOverlayManager.showFullScreenWarning(
                category = blockingResult.category ?: BlockingCategory.EXPLICIT_CONTENT,
                customMessage = "Haram content blocked immediately. Allah protects the believers.",
                reflectionTimeSeconds = 1, // Very brief
                nsfwRegionCount = 1,
                maxNsfwConfidence = blockingResult.confidence
            )

            Log.d(TAG, "Quick Quranic warning displayed")
        } catch (e: Exception) {
            Log.w(TAG, "Error showing quick Quranic warning", e)
        }
    }

    /**
     * Show enhanced porn blocking overlay with full-screen Quranic verse
     */
    private suspend fun showPornBlockingOverlay(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        if (isShowingBlockedSiteOverlay) {
            Log.d(TAG, "Porn blocking overlay already showing")
            return
        }

        try {
            isShowingBlockedSiteOverlay = true
            Log.d(TAG, "🎯 Showing enhanced porn blocking overlay for category: ${blockingResult.category}")

            // Get Islamic guidance for porn blocking
            val guidance = blockingResult.category?.let { category ->
                quranicRepository.getGuidanceForCategory(category)
            }

            // Show enhanced porn blocking overlay
            blurOverlayManager.showPornBlockingOverlay(
                blockingResult = blockingResult,
                guidance = guidance,
                onAction = { action ->
                    serviceScope.launch {
                        try {
                            handlePornBlockingAction(action, blockingResult)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in porn blocking action handler", e)
                            hideBlockedSiteOverlay()
                        }
                    }
                }
            )

            Log.d(TAG, "✅ Enhanced porn blocking overlay shown")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing porn blocking overlay", e)
            isShowingBlockedSiteOverlay = false
        }
    }

    /**
     * Handle user actions from porn blocking overlay
     */
    private suspend fun handlePornBlockingAction(
        action: com.hieltech.haramblur.data.WarningDialogAction,
        blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult
    ) {
        try {
            when (action) {
                is com.hieltech.haramblur.data.WarningDialogAction.Close -> {
                    Log.d(TAG, "🚫 User chose to close from porn blocking overlay")
                    cancelPendingPornClosure("user requested porn closure")
                    performAggressivePornSiteClosure()
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Continue -> {
                    Log.w(TAG, "⚠️ User chose to continue despite porn blocking warning")
                    // For porn sites, add extra delay and warning
                    delay(2000)
                    hideBlockedSiteOverlay()
                    // Could add logging for accountability
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Dismiss -> {
                    Log.d(TAG, "👋 User dismissed porn blocking overlay")
                    hideBlockedSiteOverlay()
                }
                else -> {
                    Log.d(TAG, "❓ Unknown action from porn blocking overlay: $action")
                    hideBlockedSiteOverlay()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling porn blocking action", e)
            try {
                hideBlockedSiteOverlay()
            } catch (hideError: Exception) {
                Log.e(TAG, "❌ Error hiding overlay after action error", hideError)
            }
        }
    }

    /**
     * Perform aggressive porn site closure with multiple strategies
     */
    private suspend fun performAggressivePornSiteClosure() {
        runSerializedPornClosure {
            executeAggressivePornSiteClosure()
        }
    }

    private suspend fun executeAggressivePornSiteClosure() {
        try {
            Log.w(TAG, "🚫 Performing aggressive porn site closure")

            // Strategy 1: Close current tab
            val closeSuccess = closeCurrentBrowserTab()
            if (closeSuccess) {
                Log.d(TAG, "✅ Successfully closed porn tab")
                delay(1000)
                openSafePageAfterBlocking()
                return
            }

            // Strategy 2: Force back navigation multiple times
            Log.d(TAG, "🔄 Porn tab close failed, trying back navigation")
            repeat(3) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(500)
            }

            // Strategy 3: Go to home screen
            delay(1000)
            val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)
            if (homeSuccess) {
                Log.d(TAG, "✅ Forced home screen from porn site")
            }

            // Clear URL to prevent re-blocking
            currentUrl = null

            // Hide overlay
            delay(1000)
            hideBlockedSiteOverlay()

        } catch (cancellation: CancellationException) {
            Log.d(TAG, "Porn site closure coroutine cancelled before completion")
            throw cancellation
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in aggressive porn site closure", e)
            // Emergency fallback
            try {
                performGlobalAction(GLOBAL_ACTION_HOME)
                hideBlockedSiteOverlay()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Emergency fallback also failed", fallbackError)
            }
        }
    }
    
    /**
     * Show blocked site overlay with Quranic verse
     */
    private suspend fun showBlockedSiteOverlay(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        if (isShowingBlockedSiteOverlay) {
            Log.d(TAG, "Blocked site overlay already showing, ignoring duplicate request")
            return
        }

        try {
            isShowingBlockedSiteOverlay = true
            Log.d(TAG, "🎯 Showing blocked site overlay for: $currentUrl (Category: ${blockingResult.category})")

            // Get Islamic guidance for the blocking category
            val guidance = blockingResult.category?.let { category ->
                quranicRepository.getGuidanceForCategory(category)
            }

            // Show overlay using BlurOverlayManager with site blocking mode
            blurOverlayManager.showBlockedSiteOverlay(
                blockingResult = blockingResult,
                guidance = guidance,
                onAction = { action ->
                    serviceScope.launch {
                        try {
                            handleBlockedSiteAction(action, blockingResult)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in action handler coroutine", e)
                            // Ensure overlay is hidden even if action handler fails
                            hideBlockedSiteOverlay()
                        }
                    }
                }
            )

            Log.d(TAG, "✅ Blocked site overlay shown for category: ${blockingResult.category}")

            // Add timeout mechanism to prevent stuck overlays (30 seconds)
            serviceScope.launch {
                delay(30000) // 30 seconds timeout
                if (isShowingBlockedSiteOverlay) {
                    Log.w(TAG, "⚠️ Blocked site overlay timeout - forcing hide")
                    hideBlockedSiteOverlay()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing blocked site overlay", e)
            isShowingBlockedSiteOverlay = false
        }
    }
    
    /**
     * Hide blocked site overlay
     */
    private fun hideBlockedSiteOverlay() {
        if (!isShowingBlockedSiteOverlay) {
            Log.d(TAG, "Blocked site overlay already hidden")
            return
        }

        try {
            Log.d(TAG, "🔒 Hiding blocked site overlay")
            blurOverlayManager.hideBlockedSiteOverlay()
            isShowingBlockedSiteOverlay = false
            Log.d(TAG, "✅ Blocked site overlay hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error hiding blocked site overlay", e)
            // Force state reset even if hiding fails
            isShowingBlockedSiteOverlay = false
        }
    }
    
    /**
     * Handle user action from blocked site overlay
     */
    private suspend fun handleBlockedSiteAction(
        action: com.hieltech.haramblur.data.WarningDialogAction,
        blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult
    ) {
        try {
            when (action) {
                is com.hieltech.haramblur.data.WarningDialogAction.Close -> {
                    Log.d(TAG, "🚫 User chose to close from blocked site")
                    // Don't hide overlay immediately - wait for navigation to complete
                    navigateAwayFromBlockedSite()
                    // Overlay will be hidden by navigateAwayFromBlockedSite after navigation
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Continue -> {
                    Log.d(TAG, "⚠️ User chose to continue despite blocked site warning")
                    // Add small delay for user to see the choice was acknowledged
                    delay(500)
                    hideBlockedSiteOverlay()
                    // Note: In a real implementation, you might want to add this to a temporary whitelist
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Dismiss -> {
                    Log.d(TAG, "👋 User dismissed blocked site overlay")
                    hideBlockedSiteOverlay()
                }
                else -> {
                    Log.d(TAG, "❓ Unknown action from blocked site overlay: $action")
                    hideBlockedSiteOverlay()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling blocked site action", e)
            // Always hide overlay on error to prevent stuck state
            try {
                hideBlockedSiteOverlay()
            } catch (hideError: Exception) {
                Log.e(TAG, "❌ Error hiding overlay after action error", hideError)
            }
        }
    }
    
    /**
     * Handle user actions from full screen warning dialogs
     */
    private suspend fun handleWarningDialogAction(action: com.hieltech.haramblur.data.WarningDialogAction) {
        try {
            Log.d(TAG, "Handling full screen warning action: $action")

            when (action) {
                is com.hieltech.haramblur.data.WarningDialogAction.Close -> {
                    Log.d(TAG, "🚫 User chose to close from full screen warning")
                    // Navigate away from the inappropriate content
                    navigateAwayFromInappropriateContent()
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Continue -> {
                    Log.d(TAG, "⚠️ User chose to continue despite full screen warning")
                    // Hide the warning but keep monitoring
                    try {
                        blurOverlayManager.hideFullScreenWarning()
                        isCurrentlyBlurred = false
                        Log.d(TAG, "Full screen warning dismissed by user")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error hiding full screen warning", e)
                        // Emergency fallback
                        blurOverlayManager.emergencyHideAllOverlays()
                    }
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Dismiss -> {
                    Log.d(TAG, "👋 User dismissed full screen warning")
                    // Hide the warning
                    try {
                        blurOverlayManager.hideFullScreenWarning()
                        isCurrentlyBlurred = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing full screen warning", e)
                        // Emergency fallback
                        blurOverlayManager.emergencyHideAllOverlays()
                    }
                }
                else -> {
                    Log.d(TAG, "❓ Unknown action from full screen warning: $action")
                    // Default to dismiss
                    try {
                        blurOverlayManager.hideFullScreenWarning()
                        isCurrentlyBlurred = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling unknown action", e)
                        blurOverlayManager.emergencyHideAllOverlays()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL ERROR: Failed to handle full screen warning action", e)

            // Emergency cleanup to prevent stuck overlays
            try {
                blurOverlayManager.emergencyHideAllOverlays()
                isCurrentlyBlurred = false
                Log.w(TAG, "EMERGENCY: All overlays hidden after action handler error")
            } catch (emergencyError: Exception) {
                Log.e(TAG, "❌ CRITICAL: Emergency cleanup also failed", emergencyError)
            }
        }
    }

    /**
     * Navigate away from inappropriate content
     */
    private fun navigateAwayFromInappropriateContent() {
        try {
            // Try to go back to previous screen
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "Navigated back from inappropriate content")

            // Schedule overlay hiding after navigation
            serviceScope.launch {
                delay(1000) // Wait for navigation to complete
                try {
                    blurOverlayManager.hideFullScreenWarning()
                    isCurrentlyBlurred = false
                    Log.d(TAG, "Full screen warning hidden after navigation")
                } catch (e: Exception) {
                    Log.e(TAG, "Error hiding overlay after navigation", e)
                    blurOverlayManager.emergencyHideAllOverlays()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating away from inappropriate content", e)
            // Fallback to just hiding the overlay
            try {
                blurOverlayManager.hideFullScreenWarning()
                isCurrentlyBlurred = false
            } catch (hideError: Exception) {
                Log.e(TAG, "Error in fallback overlay hide", hideError)
                blurOverlayManager.emergencyHideAllOverlays()
            }
        }
    }

    /**
     * Determine if we should automatically navigate away from blocked site
     */
    private fun shouldNavigateAwayFromBlockedSite(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult): Boolean {
        // Navigate away for high-severity categories
        return blockingResult.category?.severity ?: 0 >= 4
    }
    
    /**
     * Navigate away from blocked site with multiple fallback strategies
     */
    private fun navigateAwayFromBlockedSite() {
        serviceScope.launch {
            try {
                // Check if an action is already in progress
                if (isActionInProgress) {
                    Log.d(TAG, "⚠️ Action already in progress, skipping navigation")
                    return@launch
                }

                // Throttle actions to prevent crashes (minimum 2 seconds between actions)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastActionTime < 2000) {
                    Log.d(TAG, "⚠️ Action throttled - too soon after last action")
                    delay(2000 - (currentTime - lastActionTime))
                }

                isActionInProgress = true
                lastActionTime = System.currentTimeMillis()

                Log.d(TAG, "🚫 Attempting to navigate away from blocked site")

                // Add delay to prevent rapid successive actions
                delay(1000)

                // Strategy 1: Close the current tab (more aggressive for porn sites)
                Log.d(TAG, "🔄 Strategy 1: Attempting to close current tab")
                val closeTabSuccess = closeCurrentBrowserTab()
                if (closeTabSuccess) {
                    Log.d(TAG, "✅ Successfully closed browser tab")
                    delay(1000) // Increased delay
                    // Check if we need to open a safe page
                    openSafePageAfterBlocking()
                    return@launch // Use return@launch for coroutine scope
                }

                // Strategy 2: Try to go back in browser history
                Log.d(TAG, "🔄 Strategy 2: Attempting to navigate back in history")
                try {
                    val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
                    if (backSuccess) {
                        Log.d(TAG, "✅ Successfully navigated back in browser history")
                        delay(1500) // Increased delay for stability

                        // Check if we're still on the same URL after going back
                        if (currentUrl != null) {
                            // If still on blocked site, try additional strategies
                            navigateToSafeLocation()
                        } else {
                            // Successfully navigated away
                            openSafePageAfterBlocking()
                        }
                    } else {
                        Log.w(TAG, "⚠️ Global back action failed, trying safe location")
                        // Strategy 3: Go directly to safe location
                        navigateToSafeLocation()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in back navigation strategy", e)
                    // Fallback to safe location
                    navigateToSafeLocation()
                }

                                // Clear current URL to prevent repeated blocking
                currentUrl = null

                // Hide overlay after navigation completes successfully
                delay(1000) // Give time for navigation to complete
                hideBlockedSiteOverlay()

                isActionInProgress = false // Reset flag on success

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error navigating away from blocked site", e)
                // Hide overlay even on error to prevent stuck state
                try {
                    hideBlockedSiteOverlay()
                } catch (hideError: Exception) {
                    Log.e(TAG, "❌ Error hiding overlay after navigation error", hideError)
                }

                // Fallback: Force go to home screen (with error handling)
                try {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } catch (homeError: Exception) {
                    Log.e(TAG, "❌ Error performing home action", homeError)
                }

                isActionInProgress = false // Reset flag on error
            }
        }
    }

    /**
     * Attempt to close the current browser tab with enhanced multi-browser support
     */
    private fun closeCurrentBrowserTab(): Boolean {
        return try {
            Log.d(TAG, "🔍 Attempting to close current browser tab")

            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No root node available for tab closing")
                return false
            }

            val packageName = rootNode.packageName?.toString()
            Log.d(TAG, "Detected browser package: $packageName")

            try {
                val closeSuccess = when {
                    packageName?.contains("firefox", ignoreCase = true) == true ->
                        closeFirefoxTab(rootNode)
                    packageName?.contains("chrome", ignoreCase = true) == true ->
                        closeChromeTab(rootNode)
                    packageName?.contains("edge", ignoreCase = true) == true ->
                        closeEdgeTab(rootNode)
                    packageName?.contains("samsung", ignoreCase = true) == true ->
                        closeSamsungBrowserTab(rootNode)
                    packageName?.contains("opera", ignoreCase = true) == true ->
                        closeOperaTab(rootNode)
                    packageName?.contains("brave", ignoreCase = true) == true ->
                        closeBraveTab(rootNode)
                    packageName?.contains("duckduckgo", ignoreCase = true) == true ->
                        closeDuckDuckGoTab(rootNode)
                    else -> closeGenericBrowserTab(rootNode)
                }

                if (closeSuccess) {
                    Log.d(TAG, "✅ Successfully closed tab in $packageName")
                    return true
                }
            } finally {
                rootNode.recycle()
            }

            Log.d(TAG, "⚠️ Browser-specific close failed, using fallback method")

            // Enhanced fallback methods
            return closeTabFallback()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error closing browser tab", e)
            return false
        }
    }

    /**
     * Close Firefox tab with multiple strategies
     */
    private fun closeFirefoxTab(rootNode: AccessibilityNodeInfo): Boolean {
        // Firefox resource IDs for different versions
        val firefoxCloseIds = listOf(
            "org.mozilla.firefox:id/tab_close_button",
            "org.mozilla.firefox:id/close_tab_button",
            "org.mozilla.firefox:id/tab_close",
            "mozac_browser_tab_close_button"
        )

        return tryCloseWithIds(rootNode, firefoxCloseIds, "Firefox")
    }

    /**
     * Close Chrome tab
     */
    private fun closeChromeTab(rootNode: AccessibilityNodeInfo): Boolean {
        val chromeCloseIds = listOf(
            "com.android.chrome:id/tab_close_button",
            "com.android.chrome:id/close_button",
            "com.chrome.beta:id/tab_close_button",
            "com.chrome.canary:id/tab_close_button"
        )

        return tryCloseWithIds(rootNode, chromeCloseIds, "Chrome")
    }

    /**
     * Close Edge tab
     */
    private fun closeEdgeTab(rootNode: AccessibilityNodeInfo): Boolean {
        val edgeCloseIds = listOf(
            "com.microsoft.emmx:id/tab_close_button",
            "com.microsoft.emmx:id/close_tab_button"
        )

        return tryCloseWithIds(rootNode, edgeCloseIds, "Edge")
    }

    /**
     * Close Samsung Browser tab
     */
    private fun closeSamsungBrowserTab(rootNode: AccessibilityNodeInfo): Boolean {
        val samsungCloseIds = listOf(
            "com.sec.android.app.sbrowser:id/tab_close_button",
            "com.sec.android.app.sbrowser:id/close_tab_btn"
        )

        return tryCloseWithIds(rootNode, samsungCloseIds, "Samsung Browser")
    }

    /**
     * Close Opera tab
     */
    private fun closeOperaTab(rootNode: AccessibilityNodeInfo): Boolean {
        val operaCloseIds = listOf(
            "com.opera.browser:id/tab_close_button",
            "com.opera.browser:id/close_button"
        )

        return tryCloseWithIds(rootNode, operaCloseIds, "Opera")
    }

    /**
     * Close Brave tab
     */
    private fun closeBraveTab(rootNode: AccessibilityNodeInfo): Boolean {
        val braveCloseIds = listOf(
            "com.brave.browser:id/tab_close_button",
            "com.brave.browser:id/close_tab_button"
        )

        return tryCloseWithIds(rootNode, braveCloseIds, "Brave")
    }

    /**
     * Close DuckDuckGo tab
     */
    private fun closeDuckDuckGoTab(rootNode: AccessibilityNodeInfo): Boolean {
        val ddGoCloseIds = listOf(
            "com.duckduckgo.mobile.android:id/tab_close_button",
            "com.duckduckgo.mobile.android:id/close_button"
        )

        return tryCloseWithIds(rootNode, ddGoCloseIds, "DuckDuckGo")
    }

    /**
     * Generic browser tab closing using common patterns
     */
    private fun closeGenericBrowserTab(rootNode: AccessibilityNodeInfo): Boolean {
        // Try common close button patterns
        val genericCloseIds = listOf(
            "tab_close_button",
            "close_tab_button",
            "close_button",
            "tab_close"
        )

        return tryCloseWithIds(rootNode, genericCloseIds, "Generic Browser")
    }

    /**
     * Try to close tab using a list of resource IDs
     */
    private fun tryCloseWithIds(rootNode: AccessibilityNodeInfo, ids: List<String>, browserName: String): Boolean {
        for (id in ids) {
            try {
                val closeButtons = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (closeButtons.isNotEmpty()) {
                    val closeButton = closeButtons[0]
                    if (closeButton.isClickable && closeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.d(TAG, "✅ Successfully clicked $browserName close button: $id")
                        closeButtons.forEach { it.recycle() }
                        return true
                    }
                    closeButtons.forEach { it.recycle() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error trying $browserName ID $id: ${e.message}")
            }
        }
        return false
    }

    /**
     * Enhanced fallback methods for closing tabs
     */
    private fun closeTabFallback(): Boolean {
        try {
            // Method 1: Try to find close button by text/description
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                try {
                    val closeSuccess = findAndClickCloseButton(rootNode)
                    if (closeSuccess) {
                        Log.d(TAG, "✅ Found and clicked close button by text")
                        return true
                    }
                } finally {
                    rootNode.recycle()
                }
            }

            // Method 2: Enhanced back navigation with multiple attempts
            Log.d(TAG, "🔄 Using enhanced back navigation fallback")
            repeat(2) {
                val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
                if (backSuccess) {
                    Log.d(TAG, "✅ Successfully performed back action (attempt ${it + 1})")
                    Thread.sleep(500) // Brief pause between actions
                } else {
                    Log.w(TAG, "Back action failed (attempt ${it + 1})")
                }
            }

            // Method 3: Try to close app entirely (last resort)
            Log.d(TAG, "🔄 Attempting to close entire browser app")
            val recentAppsSuccess = performGlobalAction(GLOBAL_ACTION_RECENTS)
            if (recentAppsSuccess) {
                Thread.sleep(1000)
                // Try to swipe away the browser app
                // Note: This is difficult to implement reliably across devices
                Log.d(TAG, "✅ Opened recent apps, attempting swipe to close")
            }

            return false

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in tab closing fallback", e)
            return false
        }
    }

    /**
     * Find and click close button by searching for text or content description
     */
    private fun findAndClickCloseButton(rootNode: AccessibilityNodeInfo): Boolean {
        val closeKeywords = listOf(
            "close", "tab", "x", "×", "✕",
            "close tab", "tab close", "dismiss"
        )

        fun searchNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
            if (depth > 8) return false // Limit search depth

            try {
                // Check current node
                val text = node.text?.toString()?.lowercase()
                val description = node.contentDescription?.toString()?.lowercase()

                for (keyword in closeKeywords) {
                    if ((text?.contains(keyword) == true || description?.contains(keyword) == true) &&
                        node.isClickable) {
                        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.d(TAG, "Found close button by text: '$text' or '$description'")
                            return true
                        }
                    }
                }

                // Search child nodes
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        val found = searchNode(child, depth + 1)
                        child.recycle()
                        if (found) return true
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error searching node at depth $depth: ${e.message}")
            }

            return false
        }

        return searchNode(rootNode)
    }

    /**
     * Open a safe Islamic page after blocking with multiple options
     */
    private suspend fun openSafePageAfterBlocking() {
        try {
            Log.d(TAG, "🕌 Opening safe Islamic page after blocking")

            // Wait for navigation to complete
            delay(2000)

            // List of safe Islamic websites to choose from
            val safeUrls = listOf(
                "https://quran.com" to "Quran.com - Read and listen to the Quran",
                "https://muslimpro.com" to "Muslim Pro - Prayer times and Quran",
                "https://islamicfinder.org" to "Islamic Finder - Prayer times and Qibla",
                "https://sunnah.com" to "Sunnah.com - Hadith collection",
                "https://bayyinah.com" to "Bayyinah Institute - Islamic education",
                "https://yaqeeninstitute.org" to "Yaqeen Institute - Islamic research",
                "https://productivemuslim.com" to "Productive Muslim - Islamic lifestyle",
                "https://muslim.sg" to "Muslim.sg - Islamic articles and resources"
            )

            // Select a random safe URL to prevent predictability
            val (selectedUrl, description) = safeUrls.random()

            Log.d(TAG, "Selected safe page: $description")

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            startActivity(intent)
            Log.d(TAG, "✅ Successfully opened safe Islamic page: $selectedUrl")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening safe page", e)
            // Fallback: Try to open Islamic content in available apps
            openFallbackIslamicContent()
        }
    }

    /**
     * Fallback method to open Islamic content if web browsing fails
     */
    private suspend fun openFallbackIslamicContent() {
        try {
            Log.d(TAG, "🔄 Attempting fallback Islamic content")

            // Try to open Islamic apps if available
            val islamicApps = listOf(
                "com.andi.alquran.en", // Al Quran Indonesia
                "com.quran.labs.androidquran", // Quran Android
                "com.metinkale.prayer", // Prayer Times
                "com.duosecurity.duomobile", // Muslim Pro (if available)
                "com.juvodroid.app" // Islamic apps
            )

            for (packageName in islamicApps) {
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        Log.d(TAG, "✅ Opened Islamic app: $packageName")
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Islamic app not available: $packageName")
                }
            }

            // If no Islamic apps available, try to open general browser with Islamic search
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("query", "Islamic guidance and Quran")
            }

            startActivity(searchIntent)
            Log.d(TAG, "✅ Opened web search for Islamic content")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in fallback Islamic content", e)
        }
    }
    
    /**
     * Navigate to a safe location (home screen or safe website)
     */
    private suspend fun navigateToSafeLocation() {
        try {
            // Strategy 1: Try to go to home screen
            val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)
            if (homeSuccess) {
                Log.d(TAG, "Navigated to home screen")
                return
            }
            
            // Strategy 2: Try to close current app/tab
            delay(500)
            val closeSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
            if (closeSuccess) {
                Log.d(TAG, "Closed current app/tab")
                delay(500)
                
                // Try home again
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
            
            // Strategy 3: Try to navigate to a safe URL (if in browser)
            navigateToSafeUrl()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to safe location", e)
        }
    }
    
    /**
     * Attempt to navigate to a safe URL in browser
     */
    private suspend fun navigateToSafeUrl() {
        try {
            // This is a more advanced strategy that would require
            // interacting with the browser's address bar
            // For now, we'll just log the attempt
            Log.d(TAG, "Attempting to navigate to safe URL")
            
            // In a real implementation, this could:
            // 1. Find the address bar accessibility node
            // 2. Clear it and type a safe URL
            // 3. Trigger navigation
            
            // For now, just go back to home
            performGlobalAction(GLOBAL_ACTION_HOME)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to safe URL", e)
        }
    }
    
    /**
     * Enhanced URL extraction with better browser support
     */
    private fun extractUrlFromBrowserSpecific(packageName: String?, rootNode: AccessibilityNodeInfo?): String? {
        if (packageName == null || rootNode == null) return null
        
        return when {
            packageName.contains("chrome") -> extractUrlFromChrome(rootNode)
            packageName.contains("firefox") -> extractUrlFromFirefox(rootNode)
            packageName.contains("edge") -> extractUrlFromEdge(rootNode)
            packageName.contains("samsung") -> extractUrlFromSamsungBrowser(rootNode)
            else -> extractUrlFromGenericBrowser(rootNode)
        }
    }
    
    /**
     * Extract URL from Chrome browser
     */
    private fun extractUrlFromChrome(rootNode: AccessibilityNodeInfo): String? {
        try {
            // Chrome typically has the URL in a node with specific resource IDs
            val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
            if (urlNodes.isNotEmpty()) {
                val urlText = urlNodes[0].text?.toString()
                urlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }
            
            // Fallback: look for nodes with URL-like content
            return findUrlInNodeHierarchy(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from Chrome", e)
            return null
        }
    }
    
    /**
     * Extract URL from Firefox browser
     */
    private fun extractUrlFromFirefox(rootNode: AccessibilityNodeInfo): String? {
        try {
            // Firefox URL bar resource ID (works in both normal and private mode)
            val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/url_bar_title")
            if (urlNodes.isNotEmpty()) {
                val urlText = urlNodes[0].text?.toString()
                urlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }

            // Alternative Firefox private mode URL bar (if different)
            val privateUrlNodes = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/mozac_browser_toolbar_url_view")
            if (privateUrlNodes.isNotEmpty()) {
                val urlText = privateUrlNodes[0].text?.toString()
                privateUrlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }

            return findUrlInNodeHierarchy(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from Firefox (normal/private mode)", e)
            return null
        }
    }
    
    /**
     * Extract URL from Edge browser
     */
    private fun extractUrlFromEdge(rootNode: AccessibilityNodeInfo): String? {
        try {
            // Edge URL bar
            val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar")
            if (urlNodes.isNotEmpty()) {
                val urlText = urlNodes[0].text?.toString()
                urlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }
            
            return findUrlInNodeHierarchy(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from Edge", e)
            return null
        }
    }
    
    /**
     * Extract URL from Samsung Browser
     */
    private fun extractUrlFromSamsungBrowser(rootNode: AccessibilityNodeInfo): String? {
        try {
            // Samsung Browser URL bar
            val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("com.sec.android.app.sbrowser:id/location_bar_edit_text")
            if (urlNodes.isNotEmpty()) {
                val urlText = urlNodes[0].text?.toString()
                urlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }
            
            return findUrlInNodeHierarchy(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from Samsung Browser", e)
            return null
        }
    }
    
    /**
     * Generic URL extraction for unknown browsers
     */
    private fun extractUrlFromGenericBrowser(rootNode: AccessibilityNodeInfo): String? {
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Find URL in node hierarchy by searching for URL-like patterns
     */
    private fun findUrlInNodeHierarchy(node: AccessibilityNodeInfo?, depth: Int = 0): String? {
        if (node == null || depth > 5) return null // Limit recursion depth
        
        try {
            // Check current node
            val nodeText = node.text?.toString()
            val nodeDescription = node.contentDescription?.toString()
            
            extractUrlFromText(nodeText)?.let { return it }
            extractUrlFromText(nodeDescription)?.let { return it }
            
            // Check child nodes
            for (i in 0 until minOf(node.childCount, 20)) { // Limit children to check
                val child = node.getChild(i)
                val childUrl = findUrlInNodeHierarchy(child, depth + 1)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error finding URL in node hierarchy", e)
            return null
        }
    }
    
    // ==================== NEW ACTION METHODS ====================
    
    /**
     * Perform scroll away action to move inappropriate content out of view
     */
    private fun performScrollAwayAction() {
        serviceScope.launch {
            try {
                Log.d(TAG, "🔄 Performing SCROLL_AWAY action")
                
                // Check if action is already in progress
                if (isActionInProgress) {
                    Log.d(TAG, "⚠️ Action already in progress, skipping scroll")
                    return@launch
                }
                
                isActionInProgress = true
                
                // Strategy 1: Try scrolling down to move content out of view
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    try {
                        // Find scrollable view
                        val scrollableNode = findScrollableNode(rootNode)
                        if (scrollableNode != null) {
                            Log.d(TAG, "📱 Found scrollable node, performing scroll")
                            val scrollSuccess = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            if (scrollSuccess) {
                                Log.d(TAG, "✅ Successfully scrolled content")
                                delay(1000) // Give time for scroll to complete
                                
                                // Try scrolling again to ensure content is moved
                                scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            }
                            scrollableNode.recycle()
                        } else {
                            Log.d(TAG, "⚠️ No scrollable node found, using gesture")
                            // Fallback: Perform scroll gesture
                            performScrollGesture()
                        }
                    } finally {
                        rootNode.recycle()
                    }
                } else {
                    Log.d(TAG, "⚠️ No root node, performing gesture scroll")
                    performScrollGesture()
                }
                
                Log.d(TAG, "✅ SCROLL_AWAY action completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error performing scroll away action", e)
            } finally {
                isActionInProgress = false
            }
        }
    }
    
    /**
     * Perform navigate back action
     */
    private fun performNavigateBackAction() {
        serviceScope.launch {
            try {
                Log.d(TAG, "⬅️ Performing NAVIGATE_BACK action")
                
                if (isActionInProgress) {
                    Log.d(TAG, "⚠️ Action already in progress, skipping navigation")
                    return@launch
                }
                
                isActionInProgress = true
                
                val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
                if (backSuccess) {
                    Log.d(TAG, "✅ Successfully navigated back")
                    delay(1000) // Give time for navigation
                } else {
                    Log.w(TAG, "❌ Back navigation failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error performing navigate back action", e)
            } finally {
                isActionInProgress = false
            }
        }
    }
    
    /**
     * Perform auto close app action
     */
    private fun performAutoCloseAppAction() {
        serviceScope.launch {
            try {
                Log.d(TAG, "🚫 Performing AUTO_CLOSE_APP action")
                
                if (isActionInProgress) {
                    Log.d(TAG, "⚠️ Action already in progress, skipping app close")
                    return@launch
                }
                
                isActionInProgress = true
                
                // Strategy 1: Try to go to home screen
                val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)
                if (homeSuccess) {
                    Log.d(TAG, "✅ Successfully closed app (went to home)")
                } else {
                    // Strategy 2: Try back button multiple times
                    Log.d(TAG, "🔄 Home failed, trying back navigation")
                    repeat(3) {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        delay(500)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error performing auto close app action", e)
            } finally {
                isActionInProgress = false
            }
        }
    }
    
    /**
     * Perform gentle redirect action with short warning
     */
    private fun performGentleRedirectAction() {
        serviceScope.launch {
            try {
                Log.d(TAG, "🔄 Performing GENTLE_REDIRECT action")
                
                if (isActionInProgress) {
                    Log.d(TAG, "⚠️ Action already in progress, skipping redirect")
                    return@launch
                }
                
                isActionInProgress = true
                
                // Show a brief warning overlay
                try {
                    blurOverlayManager.showFullScreenWarning(
                        category = BlockingCategory.EXPLICIT_CONTENT,
                        customMessage = "Inappropriate content detected - redirecting...",
                        reflectionTimeSeconds = 3 // Very short warning
                    )
                    
                    // Wait for warning to be seen
                    delay(3000)
                    
                    // Hide warning and navigate back
                    blurOverlayManager.hideFullScreenWarning()
                    
                    // Navigate away
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    
                    Log.d(TAG, "✅ Gentle redirect completed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error showing gentle redirect warning", e)
                    // Fallback to just navigation
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error performing gentle redirect action", e)
            } finally {
                isActionInProgress = false
            }
        }
    }
    
    /**
     * Find a scrollable node in the node hierarchy
     */
    private fun findScrollableNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if root node itself is scrollable
        if (rootNode.isScrollable) {
            return rootNode
        }
        
        // Search children for scrollable nodes
        fun searchScrollable(node: AccessibilityNodeInfo, depth: Int = 0): AccessibilityNodeInfo? {
            if (depth > 5) return null // Limit search depth
            
            if (node.isScrollable) {
                return node
            }
            
            for (i in 0 until minOf(node.childCount, 10)) {
                val child = node.getChild(i)
                if (child != null) {
                    val scrollable = searchScrollable(child, depth + 1)
                    if (scrollable != null) {
                        child.recycle()
                        return scrollable
                    }
                    child.recycle()
                }
            }
            return null
        }
        
        return searchScrollable(rootNode)
    }
    
    /**
     * Perform scroll gesture using accessibility service
     */
    private fun performScrollGesture() {
        try {
            // This would require gesture dispatch which is available in API 24+
            // For now, just log the attempt
            Log.d(TAG, "🖱️ Attempting scroll gesture (fallback)")
            
            // In a real implementation, you could use:
            // dispatchGesture() for API 24+
            // For now, we'll just try the back action as fallback
            performGlobalAction(GLOBAL_ACTION_BACK)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error performing scroll gesture", e)
        }
    }
    
    /**
     * Get the current app package name for context-aware LLM decisions
     */
    private fun getCurrentAppPackage(): String {
        return try {
            val rootNode = rootInActiveWindow
            val packageName = rootNode?.packageName?.toString() ?: "unknown"
            rootNode?.recycle()

            // Update currentAppPackage tracking variable
            if (currentAppPackage != packageName) {
                currentAppPackage = packageName
            }

            // Simplify package name for better LLM context
            when {
                packageName.contains("firefox", ignoreCase = true) -> "firefox_browser"
                packageName.contains("chrome", ignoreCase = true) -> "chrome_browser"
                packageName.contains("edge", ignoreCase = true) -> "edge_browser"
                packageName.contains("browser", ignoreCase = true) -> "browser"
                packageName.contains("youtube", ignoreCase = true) -> "youtube"
                packageName.contains("instagram", ignoreCase = true) -> "instagram"
                packageName.contains("tiktok", ignoreCase = true) -> "tiktok"
                packageName.contains("twitter", ignoreCase = true) -> "twitter"
                packageName.contains("facebook", ignoreCase = true) -> "facebook"
                else -> "mobile_app"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current app package", e)
            "unknown_app"
        }
    }

    /**
     * Check if the current app should be monitored based on app filtering settings
     */
    private suspend fun shouldMonitorCurrentApp(): Boolean {
        return try {
            val result = appFilteringManager.shouldMonitorApp(currentAppPackage)
            Log.v(TAG, "App monitoring check: $currentAppPackage = $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app should be monitored: $currentAppPackage", e)
            // Default to monitoring all apps on error to prevent service disruption
            true
        }
    }
}
