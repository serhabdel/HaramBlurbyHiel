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
import com.hieltech.haramblur.data.QuranicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class HaramBlurAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "HaramBlurService"
        private const val NOTIFICATION_ID = 1001
        private var instance: HaramBlurAccessibilityService? = null
        
        fun getInstance(): HaramBlurAccessibilityService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }
    
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
    private var lastProcessedTime = 0L
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
        if (!isProcessingActive) return
        
        // Check if real-time processing is enabled
        val currentSettings = settingsRepository.getCurrentSettings()
        if (!currentSettings.enableRealTimeProcessing) {
            return // Real-time processing disabled
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Get dynamic processing cooldown from settings
        val processingCooldown = currentSettings.processingSpeed.intervalMs
        
        if (currentTime - lastProcessedTime < processingCooldown) {
            return // Skip processing to prevent overload
        }
        lastProcessedTime = currentTime
        
        try {
            Log.d(TAG, "Processing screen content: ${bitmap.width}x${bitmap.height} (speed: ${currentSettings.processingSpeed})")
            
            // Analyze content using detection engine with user settings
            val analysisResult = contentDetectionEngine.analyzeContent(
                bitmap,
                currentSettings
            )
            
            if (analysisResult.isSuccessful()) {
                handleAnalysisResult(analysisResult, currentSettings)
            } else {
                Log.w(TAG, "Content analysis failed: ${analysisResult.error}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing screen content", e)
        }
    }
    
    private fun handleAnalysisResult(result: ContentDetectionEngine.ContentAnalysisResult, settings: AppSettings) {
        Log.d(TAG, "Analysis result: shouldBlur=${result.shouldBlur}, regions=${result.blurRegions.size}")
        
        if (result.shouldBlur && result.blurRegions.isNotEmpty()) {
            // Show blur overlay with user-selected intensity
            blurOverlayManager.showBlurOverlay(result.blurRegions, settings.blurIntensity)
            
            Log.d(TAG, "Blur overlay activated for ${result.blurRegions.size} regions with intensity: ${settings.blurIntensity}")
        } else {
            // Hide blur overlay if no inappropriate content detected
            blurOverlayManager.hideBlurOverlay()
        }
        
        // Log detection details
        result.faceDetectionResult?.let { faceResult ->
            if (faceResult.facesDetected > 0) {
                Log.d(TAG, "Faces detected: ${faceResult.facesDetected}")
            }
        }
        
        result.nsfwDetectionResult?.let { nsfwResult ->
            if (nsfwResult.isNSFW) {
                Log.d(TAG, "NSFW content detected with confidence: ${nsfwResult.confidence}")
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
                        lastProcessedTime = 0L
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
    
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            isServiceRunning = true,
            isProcessingActive = isProcessingActive,
            isCapturingActive = screenCaptureManager.isCapturingActive(),
            isOverlayActive = blurOverlayManager.isOverlayActive(),
            captureStats = screenCaptureManager.getCaptureStats(),
            currentUrl = currentUrl,
            isShowingBlockedSiteOverlay = isShowingBlockedSiteOverlay
        )
    }
    
    data class ServiceStatus(
        val isServiceRunning: Boolean,
        val isProcessingActive: Boolean,
        val isCapturingActive: Boolean,
        val isOverlayActive: Boolean,
        val captureStats: ScreenCaptureManager.CaptureStats,
        val currentUrl: String? = null,
        val isShowingBlockedSiteOverlay: Boolean = false
    )
    
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
                    Log.d(TAG, "URL changed to: $extractedUrl")
                    
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