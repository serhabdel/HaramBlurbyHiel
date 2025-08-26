package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.detection.SiteBlockingManager
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.QuranicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class HaramBlurAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "HaramBlurAccessibilityService"
        private var instance: HaramBlurAccessibilityService? = null
        
        fun getInstance(): HaramBlurAccessibilityService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
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
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessingActive = false
    private var lastProcessingTime: Long = 0
    private var frameCount = 0
    private var totalFramesProcessed: Long = 0
    private var totalFramesSkipped: Long = 0
    private var processingTimes = mutableListOf<Long>()
    private var lastServiceError: String = ""
    
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
        
        // Initialize components
        serviceScope.launch {
            initializeComponents()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all components
        cleanupComponents()
        
        serviceScope.cancel()
        instance = null
        Log.d(TAG, "HaramBlur Accessibility Service Destroyed")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "HaramBlur Accessibility Service Connected")
        
        serviceScope.launch {
            startContentMonitoring()
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
        
        // Check if real-time processing is enabled
        val currentSettings = settingsRepository.getCurrentSettings()
        if (!currentSettings.enableRealTimeProcessing) {
            Log.d(TAG, "⏸️ Real-time processing disabled in settings")
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
            
            // Analyze content using detection engine with user settings
            Log.d(TAG, "🧠 Starting content analysis with detection engine...")
            val analysisResult = contentDetectionEngine.analyzeContent(bitmap, currentSettings)
            
            if (analysisResult.isSuccessful()) {
                Log.d(TAG, "✅ Content analysis successful, handling results...")
                val shouldBlur = handleAnalysisResultWithStability(analysisResult, currentSettings)
                
                // Cache the result
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
     * Get current service status for debugging and UI display
     */
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            isServiceRunning = instance != null,
            isProcessingActive = isProcessingActive,
            isCapturingActive = ::screenCaptureManager.isInitialized && screenCaptureManager.isCapturingActive(),
            isOverlayActive = ::blurOverlayManager.isInitialized && blurOverlayManager.isOverlayActive(),
            lastProcessingTime = lastProcessingTime,
            totalFramesProcessed = totalFramesProcessed,
            totalFramesSkipped = totalFramesSkipped,
            averageProcessingTime = if (processingTimes.isNotEmpty()) {
                processingTimes.average().toFloat()
            } else 0f,
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
        
        // More aggressive female face detection
        val hasFemaleFaces = result.faceDetectionResult?.detectedFaces?.any { face ->
            val isFemaleLikely = face.genderConfidence > genderThreshold && 
                                face.estimatedGender.toString().contains("FEMALE", ignoreCase = true)
            val isFaceDetected = face.genderConfidence > 0.3f // Any face with decent confidence
            
            Log.d(TAG, "👩 Face analysis: confidence=${face.genderConfidence}, gender=${face.estimatedGender}, isFemaleLikely=$isFemaleLikely")
            
            // Blur if likely female OR if uncertain but settings are aggressive
            isFemaleLikely || (isFaceDetected && settings.detectionSensitivity > 0.8f)
        } ?: false
        
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
        
        // Apply blur decision
        if (shouldShowBlur) {
            val regionsToBlur = if (result.blurRegions.isNotEmpty()) {
                result.blurRegions
            } else if (shouldBlurBasedOnContent) {
                // Create full-screen blur for NSFW content without specific regions
                listOf(android.graphics.Rect(0, 0, 1080, 2400))
            } else {
                emptyList()
            }
            
            if (regionsToBlur.isNotEmpty()) {
                blurOverlayManager.showBlurOverlay(regionsToBlur, settings.blurIntensity)
                Log.d(TAG, "🛡️ Blur active: ${regionsToBlur.size} regions, intensity: ${settings.blurIntensity}")
            }
        } else {
            blurOverlayManager.hideBlurOverlay()
            Log.d(TAG, "🔓 Blur hidden")
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
            isCurrentlyBlurred = true
            lastBlurStartTime = System.currentTimeMillis()
            // Create full-screen blur for cached NSFW result
            val fullScreenRegion = listOf(android.graphics.Rect(0, 0, 1080, 2400))
            val currentSettings = settingsRepository.getCurrentSettings()
            blurOverlayManager.showBlurOverlay(fullScreenRegion, currentSettings.blurIntensity)
            Log.d(TAG, "🛡️ Applied cached blur result")
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
                Log.d(TAG, "Window state changed: ${event.packageName}")
                
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
    
    private fun getProcessingInterval(settings: AppSettings): Long {
        return when (settings.processingSpeed) {
            ProcessingSpeed.FAST -> 50L  // Faster for immediate response
            ProcessingSpeed.BALANCED -> 100L  // Reduced for better responsiveness 
            ProcessingSpeed.BATTERY_SAVER -> 300L  // Still faster than before
            ProcessingSpeed.ULTRA_FAST -> 25L  // Super fast for real-time
        }
    }
    
    /**
     * Check for URL changes in accessibility events
     */
    private suspend fun checkForUrlChanges(event: AccessibilityEvent?) {
        if (event == null) return
        
        val currentTime = System.currentTimeMillis()
        
        // Throttle URL checking to avoid excessive processing
        if (currentTime - lastUrlCheckTime < 1000) {
            return
        }
        lastUrlCheckTime = currentTime
        
        try {
            val packageName = event.packageName?.toString()
            
            // Only check URLs for browser apps and web-based apps
            if (isBrowserApp(packageName)) {
                val extractedUrl = extractUrlFromAccessibilityEvent(event)
                if (extractedUrl != null && extractedUrl != currentUrl) {
                    currentUrl = extractedUrl
                    Log.d(TAG, "Handling analysis result - URL: $currentUrl")
                    
                    // Check if the URL should be blocked
                    checkAndBlockUrl(extractedUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL changes", e)
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
        
        val browserPackages = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.samsung.android.app.sbrowser",
            "com.UCMobile.intl",
            "com.kiwibrowser.browser",
            "org.mozilla.focus",
            "com.android.browser",
            "com.sec.android.app.sbrowser"
        )
        
        // Check exact matches first
        if (browserPackages.contains(packageName)) {
            return true
        }
        
        // Check for common browser patterns
        val browserPatterns = listOf(
            "browser",
            "chrome",
            "firefox",
            "opera",
            "edge",
            "safari"
        )
        
        return browserPatterns.any { pattern ->
            packageName.lowercase().contains(pattern)
        }
    }
    
    /**
     * Check URL against site blocking manager and block if necessary
     */
    private suspend fun checkAndBlockUrl(url: String) {
        try {
            val blockingResult = siteBlockingManager.checkUrl(url)
            
            if (blockingResult.isBlocked) {
                Log.d(TAG, "Blocking URL: $url (Category: ${blockingResult.category})")
                
                // Show blocked site overlay
                showBlockedSiteOverlay(blockingResult)
                
                // Optionally navigate away from the blocked site
                if (shouldNavigateAwayFromBlockedSite(blockingResult)) {
                    navigateAwayFromBlockedSite()
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
     * Show blocked site overlay with Quranic verse
     */
    private suspend fun showBlockedSiteOverlay(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        if (isShowingBlockedSiteOverlay) return
        
        try {
            isShowingBlockedSiteOverlay = true
            
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
                        handleBlockedSiteAction(action, blockingResult)
                    }
                }
            )
            
            Log.d(TAG, "Blocked site overlay shown for category: ${blockingResult.category}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing blocked site overlay", e)
            isShowingBlockedSiteOverlay = false
        }
    }
    
    /**
     * Hide blocked site overlay
     */
    private fun hideBlockedSiteOverlay() {
        if (!isShowingBlockedSiteOverlay) return
        
        try {
            blurOverlayManager.hideBlockedSiteOverlay()
            isShowingBlockedSiteOverlay = false
            Log.d(TAG, "Blocked site overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding blocked site overlay", e)
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
                    Log.d(TAG, "User chose to close app from blocked site")
                    navigateAwayFromBlockedSite()
                    hideBlockedSiteOverlay()
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Continue -> {
                    Log.d(TAG, "User chose to continue despite blocked site warning")
                    // Allow user to continue after reflection period
                    hideBlockedSiteOverlay()
                    // Note: In a real implementation, you might want to add this to a temporary whitelist
                }
                is com.hieltech.haramblur.data.WarningDialogAction.Dismiss -> {
                    Log.d(TAG, "User dismissed blocked site overlay")
                    hideBlockedSiteOverlay()
                }
                else -> {
                    Log.d(TAG, "Unknown action from blocked site overlay: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling blocked site action", e)
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
                Log.d(TAG, "Attempting to navigate away from blocked site")
                
                // Strategy 1: Try to go back in browser history
                val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
                if (backSuccess) {
                    Log.d(TAG, "Successfully navigated back")
                    delay(1000)
                    
                    // Check if we're still on the same URL after going back
                    if (currentUrl != null) {
                        // If still on blocked site, try additional strategies
                        navigateToSafeLocation()
                    }
                } else {
                    // Strategy 2: Go directly to safe location
                    navigateToSafeLocation()
                }
                
                // Clear current URL to prevent repeated blocking
                currentUrl = null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating away from blocked site", e)
                // Fallback: Force go to home screen
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
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
            // Firefox URL bar resource ID
            val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/url_bar_title")
            if (urlNodes.isNotEmpty()) {
                val urlText = urlNodes[0].text?.toString()
                urlNodes.forEach { it.recycle() }
                return extractUrlFromText(urlText)
            }
            
            return findUrlInNodeHierarchy(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting URL from Firefox", e)
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
}