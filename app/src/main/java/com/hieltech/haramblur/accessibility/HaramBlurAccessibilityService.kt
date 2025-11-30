package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.BlurIntensity
import com.hieltech.haramblur.data.BlurStyle
import com.hieltech.haramblur.data.ProcessingSpeed
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.models.DetectionScope
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.detection.SiteBlockingManager
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.ContentAction
// import com.hieltech.haramblur.services.DhikrReminder
// import com.hieltech.haramblur.services.DhikrReminderManager
// import com.hieltech.haramblur.services.DhikrReminderNotificationManager
// import com.hieltech.haramblur.ui.newsettings.ScreenTimeNotificationHelper
// import com.hieltech.haramblur.utils.AppCategoryClassifier
import com.hieltech.haramblur.utils.CoordinateMapper
import com.hieltech.haramblur.setup.FirstRunSetupManager
import com.hieltech.haramblur.testing.FirstRunValidator
import com.hieltech.haramblur.detection.ForegroundAppMonitor
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.AppFilteringManager
import com.hieltech.haramblur.data.AppCategoryDetector
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.utils.UrlUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Data class for blur regions with metadata for debounced updates
 */
data class BlurRegionWithMeta(
    val regions: List<Rect>,
    val intensity: BlurIntensity,
    val style: BlurStyle,
    val confidence: Float,
    val timestamp: Long
)

/**
 * Debouncer for blur updates to prevent excessive updates
 */
class BlurUpdateDebouncer(private val debounceMs: Long) {
    private var pendingUpdate: BlurRegionWithMeta? = null
    private var debounceJob: kotlinx.coroutines.Job? = null
    
    fun scheduleUpdate(
        blurRegionWithMeta: BlurRegionWithMeta,
        onUpdate: (BlurRegionWithMeta) -> Unit
    ) {
        // Cancel any existing debounce job
        debounceJob?.cancel()
        
        // Store the latest update
        pendingUpdate = blurRegionWithMeta
        
        // Schedule new debounced update
        debounceJob = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                delay(debounceMs)
                
                // Apply the pending update
                pendingUpdate?.let { update ->
                    onUpdate(update)
                    pendingUpdate = null
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job was cancelled, which is expected behavior
                Log.d("BlurUpdateDebouncer", "Debounced update cancelled")
            } catch (e: Exception) {
                Log.e("BlurUpdateDebouncer", "Error in debounced update", e)
            }
        }
    }
    
    fun cancelPendingUpdates() {
        debounceJob?.cancel()
        pendingUpdate = null
    }
}

@AndroidEntryPoint
class HaramBlurAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "HaramBlurAccessibilityService"
        private var instance: HaramBlurAccessibilityService? = null
        private const val EMERGENCY_RESET_ACTION = "com.hieltech.haramblur.EMERGENCY_RESET"
        private const val WATCHDOG_REASON_DESTROY = "service_destroyed"
        private const val WATCHDOG_REASON_TASK_REMOVED = "task_removed"
        private const val WATCHDOG_REASON_ACTIVE = "service_active"

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

    @Inject
    lateinit var errorRecovery: com.hieltech.haramblur.detection.ComprehensiveErrorRecovery
    
    @Inject
    lateinit var crashRecoverySystem: com.hieltech.haramblur.recovery.CrashRecoverySystem

    @Inject
    lateinit var logRepository: LogRepository

    // Note: Behavioral action components (automatic app closing, navigation) are disabled
    // This is intentional to keep the app focused on content blur overlays only
    
    // Database logging convenience methods
    private fun logToDatabase(
        message: String, 
        level: LogRepository.LogLevel = LogRepository.LogLevel.DEBUG,
        category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY
    ) {
        serviceScope.launch {
            try {
                logRepository.log(TAG, message, level, category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log to database: $message", e)
            }
        }
    }
    
    private fun logDebugToDatabase(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        logToDatabase(message, LogRepository.LogLevel.DEBUG, category)
    }
    
    private fun logInfoToDatabase(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        logToDatabase(message, LogRepository.LogLevel.INFO, category)
    }
    
    private fun logWarningToDatabase(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        logToDatabase(message, LogRepository.LogLevel.WARN, category)
    }
    
    private fun logErrorToDatabase(message: String, exception: Exception? = null, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        serviceScope.launch {
            try {
                logRepository.logError(TAG, message, exception, category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log error to database: $message", e)
            }
        }
    }
    
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
    private var pornClosureInFlight = false
    private val lastDomainBlockTimestamps = mutableMapOf<String, Long>()
    private val domainBlockCoalesceWindowMs = 3_000L // 3s coalescing for faster response
    
    // Error tracking for stability
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()

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
    private val cacheExpirationMs = 2000L // Cache results for 2 seconds - faster updates
    private var lastBitmapHash: String? = null
    private var consecutiveNSFWCount = 0
    private var consecutiveCleanCount = 0
    private val requiredConsecutiveDetections = 2 // Require 2 consecutive detections for stability
    
    // Adaptive learning system
    private var adaptiveNSFWThreshold = 0.4f // Start lower, adapt based on content
    private var adaptiveGenderThreshold = 0.4f // Start lower for better female detection
    private var detectionHistory = mutableListOf<Pair<Long, Boolean>>() // Track success/failure
    private var lastAdaptationTime = 0L
    private val adaptationIntervalMs = 30000L // Adapt every 30 seconds
    private var currentUrl: String? = null
    private var lastUrlCheckTime = 0L
    private var isShowingBlockedSiteOverlay = false

    // Tab operation safety tracking
    private val tabOperationTimeouts = mutableMapOf<String, Long>()
    private val maxConcurrentTabOperations = 3
    private val activeTabOperations = AtomicInteger(0)
    private val tabOperationHistory = mutableListOf<Pair<Long, String>>()
    
    // Debounced blur updates with metadata
    private var blurUpdateDebouncer: BlurUpdateDebouncer? = null
    private val blurUpdateDebounceMs = 50L
    
    private val knownBrowserPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.canary",
        "com.chrome.dev",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.yandex.browser",
        "org.torproject.torbrowser",
        "com.kiwibrowser.browser",
        "com.ecosia.android",
        "com.qwant.liberty",
        "com.UCMobile.intl",
        "com.puffin.browser",
        "com.adguard.browser"
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    private val browserPackageKeywords = listOf(
        "browser",
        "chrome",
        "firefox",
        "edge",
        "opera",
        "brave",
        "duckduckgo",
        "vivaldi",
        "yandex",
        "puffin",
        "samsung",
        "kiwi",
        "tor"
    )

    private fun getForegroundPackageNameSafely(): String? {
        val rootNode = rootInActiveWindow
        return try {
            rootNode?.packageName?.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Error retrieving foreground package name", e)
            null
        } finally {
            try {
                rootNode?.recycle()
            } catch (recycleError: Exception) {
                Log.w(TAG, "Error recycling root node after package lookup", recycleError)
            }
        }
    }

    private fun isBrowserPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val normalized = packageName.lowercase(Locale.ROOT)
        if (knownBrowserPackages.contains(normalized)) {
            return true
        }
        return browserPackageKeywords.any { normalized.contains(it) }
    }

    private fun isBrowserActionAllowed(foregroundPackage: String?, reason: String): Boolean {
        if (foregroundPackage.isNullOrBlank()) {
            Log.w(TAG, "Skipping global action ($reason): no foreground package detected")
            return false
        }

        if (foregroundPackage == packageName) {
            Log.w(TAG, "Skipping global action ($reason): service package ($foregroundPackage) is in the foreground")
            return false
        }

        if (!isBrowserPackage(foregroundPackage)) {
            Log.w(TAG, "Skipping global action ($reason): $foregroundPackage is not recognized as a browser")
            return false
        }

        return true
    }

    private fun describeGlobalAction(action: Int): String = when (action) {
        GLOBAL_ACTION_BACK -> "GLOBAL_ACTION_BACK"
        GLOBAL_ACTION_HOME -> "GLOBAL_ACTION_HOME"
        GLOBAL_ACTION_RECENTS -> "GLOBAL_ACTION_RECENTS"
        GLOBAL_ACTION_NOTIFICATIONS -> "GLOBAL_ACTION_NOTIFICATIONS"
        GLOBAL_ACTION_QUICK_SETTINGS -> "GLOBAL_ACTION_QUICK_SETTINGS"
        else -> "GLOBAL_ACTION_$action"
    }

    private fun performBrowserAwareGlobalAction(
        action: Int,
        reason: String,
        cachedPackageName: String? = null
    ): Boolean {
        val foregroundPackage = cachedPackageName ?: getForegroundPackageNameSafely()
        if (!isBrowserActionAllowed(foregroundPackage, reason)) {
            return false
        }

        val success = performGlobalAction(action)
        if (success) {
            Log.d(
                TAG,
                "Performed ${describeGlobalAction(action)} for $reason (foreground=$foregroundPackage)"
            )
        } else {
            Log.w(
                TAG,
                "Failed to perform ${describeGlobalAction(action)} for $reason (foreground=$foregroundPackage)"
            )
        }
        return success
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            instance = this
            Log.d(TAG, "🚀 HaramBlur Accessibility Service Created")
            logInfoToDatabase("🚀 HaramBlur Accessibility Service Created")

            AccessibilityServiceWatchdog.cancel(this, WATCHDOG_REASON_ACTIVE)

            // Register emergency reset broadcast receiver with error handling
            try {
                val filter = IntentFilter(EMERGENCY_RESET_ACTION)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(emergencyResetReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(emergencyResetReceiver, filter)
                }
                Log.d(TAG, "✅ Emergency reset broadcast receiver registered")
                logDebugToDatabase("✅ Emergency reset broadcast receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register emergency reset receiver", e)
                logErrorToDatabase("Failed to register emergency reset receiver", e)
                // Continue without receiver - not critical
            }

            // App launch interceptor is registered in manifest
            Log.d(TAG, "🚀 App launch interceptor ready")

            // Initialize blur update debouncer
            blurUpdateDebouncer = BlurUpdateDebouncer(blurUpdateDebounceMs)
            Log.d(TAG, "✅ Blur update debouncer initialized with ${blurUpdateDebounceMs}ms delay")

            // Initialize components with safety wrapper
            serviceScope.launch {
                try {
                    initializeComponents()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Critical error during service initialization", e)
                    lastServiceError = "Service init failed: ${e.message}"
                    // Try graceful recovery
                    attemptServiceRecovery()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Fatal error in service onCreate", e)
            // Emergency cleanup
            instance = null
        }
    }
    
    override fun onDestroy() {
        try {
            Log.w(TAG, "🏁 HaramBlur Accessibility Service Destroying - starting cleanup")
            
            // Mark service as stopping to prevent new operations
            isProcessingActive = false
            
            // Perform enhanced termination cleanup
            try {
                performEnhancedTerminationCleanup("onDestroy")
            } catch (e: Exception) {
                Log.e(TAG, "Error in enhanced termination cleanup", e)
            }

            // Unregister emergency reset broadcast receiver
            try {
                unregisterReceiver(emergencyResetReceiver)
                Log.d(TAG, "✅ Emergency reset broadcast receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering emergency reset receiver", e)
            }

            // App launch interceptor is managed by system
            Log.d(TAG, "🚀 App launch interceptor cleanup complete")

            // Stop app blocking monitor with timeout
            try {
                runBlocking {
                    withTimeoutOrNull(5000L) {
                        foregroundAppMonitor.stopMonitoring()
                    }
                }
                Log.d(TAG, "✅ ForegroundAppMonitor stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping ForegroundAppMonitor: ${e.message}")
            }

            // Clean up all components with error handling
            try {
                performEnhancedComponentCleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error in component cleanup", e)
            }

            // Clean up dhikr manager
            try {
                dhikrManager.stopScheduler()
                dhikrManager.cleanup()
                Log.d(TAG, "✅ DhikrManager scheduler stopped and cleaned up")
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up DhikrManager: ${e.message}")
            }

            // Emergency overlay cleanup
            try {
                blurOverlayManager.emergencyHideAllOverlays()
                isCurrentlyBlurred = false
            } catch (e: Exception) {
                Log.e(TAG, "Error in emergency overlay cleanup", e)
            }
            
            // Cancel blur update debouncer
            try {
                blurUpdateDebouncer?.cancelPendingUpdates()
                blurUpdateDebouncer = null
                Log.d(TAG, "✅ Blur update debouncer cancelled")
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling blur update debouncer", e)
            }
            
            // Shutdown crash recovery system
            try {
                crashRecoverySystem.shutdown()
                Log.d(TAG, "✅ Crash recovery system shutdown")
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down crash recovery system", e)
            }

            // Cancel service scope with timeout
            try {
                serviceScope.cancel()
                
                // Wait briefly for cancellation
                runBlocking {
                    withTimeoutOrNull(2000L) {
                        serviceScope.coroutineContext.job.join()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling service scope", e)
            }
            
            // Clear instance last
            instance = null
            Log.w(TAG, "🏁 HaramBlur Accessibility Service Destroyed successfully")

            AccessibilityServiceWatchdog.schedule(this, WATCHDOG_REASON_DESTROY)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Fatal error during service destruction", e)
            // Force cleanup
            instance = null
        } finally {
            super.onDestroy()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "HaramBlur Accessibility Service Connected")

        AccessibilityServiceWatchdog.cancel(this, WATCHDOG_REASON_ACTIVE)

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

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "HaramBlur Accessibility Service task removed")
        performEnhancedTerminationCleanup("onTaskRemoved")
        AccessibilityServiceWatchdog.schedule(this, WATCHDOG_REASON_TASK_REMOVED)
        super.onTaskRemoved(rootIntent)
    }
    
    private suspend fun initializeComponents() {
        try {
            Log.d(TAG, "Initializing HaramBlur components...")

            // Check if first-run setup is needed
            val firstRunSetupManager = FirstRunSetupManager(contentDetectionEngine, settingsRepository, FirstRunValidator(contentDetectionEngine, settingsRepository))
            
            if (firstRunSetupManager.isFirstRunNeeded(this@HaramBlurAccessibilityService)) {
                Log.i(TAG, "🚀 First-run setup needed, running comprehensive initialization...")
                
                val setupResult = firstRunSetupManager.runFirstRunSetup(this@HaramBlurAccessibilityService)
                
                if (setupResult.success) {
                    Log.i(TAG, "✅ First-run setup completed successfully in ${setupResult.setupTimeMs}ms")
                    Log.i(TAG, "📱 Device profile: ${setupResult.deviceProfile.deviceClass}, GPU: ${setupResult.deviceProfile.hasGPUAcceleration}")
                    Log.i(TAG, "⚙️ Optimized settings applied for best performance")
                    
                    if (setupResult.recommendations.isNotEmpty()) {
                        Log.i(TAG, "💡 Setup recommendations:")
                        setupResult.recommendations.forEach { recommendation ->
                            Log.i(TAG, "   • $recommendation")
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ First-run setup completed with issues:")
                    setupResult.issues.forEach { issue ->
                        Log.w(TAG, "   • $issue")
                    }
                    Log.w(TAG, "📱 Using failsafe settings for compatibility")
                }
            } else {
                Log.d(TAG, "✅ First-run setup already completed, using existing configuration")
            }

            // Initialize all enhanced detection services
            serviceLifecycleManager.initializeServices()

            // Initialize detection engine (may already be initialized by first-run setup)
            if (!contentDetectionEngine.isEngineReady()) {
                val detectionInitialized = contentDetectionEngine.initialize(this@HaramBlurAccessibilityService)
                if (!detectionInitialized) {
                    Log.w(TAG, "Content detection initialization failed")
                }
            } else {
                Log.d(TAG, "✅ Content detection engine already ready from first-run setup")
            }

            // Initialize overlay manager
            blurOverlayManager.initialize(this@HaramBlurAccessibilityService)
            
            // Initialize dhikr manager and start scheduler
            dhikrManager.initialize(this@HaramBlurAccessibilityService)
            dhikrManager.startScheduler()
            Log.d(TAG, "✅ DhikrManager initialized and scheduler started")
            
            // Initialize crash recovery system
            crashRecoverySystem.initialize()
            crashRecoverySystem.registerComponents(
                overlayManager = blurOverlayManager,
                captureManager = screenCaptureManager,
                detectionEngine = contentDetectionEngine
            )
            Log.d(TAG, "🛡️ Crash recovery system initialized")

            // Behavioral action components intentionally disabled (see note at line 181)

            Log.d(TAG, "HaramBlur components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
        }
    }



    private fun startContentMonitoring() {
        if (isProcessingActive) {
            Log.w(TAG, "Content monitoring already active")
            logWarningToDatabase("Content monitoring already active")
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
        // Enhanced safety wrapper for critical screen processing
        val processingId = System.currentTimeMillis().toString()
        
        try {
            if (!isProcessingActive) {
                Log.d(TAG, "🚫 Processing not active, skipping screen content analysis")
                return
            }
            
            // Null safety checks
            if (bitmap.isRecycled) {
                Log.w(TAG, "⚠️ Bitmap is recycled, skipping processing")
                return
            }
            
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                Log.w(TAG, "⚠️ Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                return
            }
            
            Log.d(TAG, "📸 Processing screen content [$processingId] - Size: ${bitmap.width}x${bitmap.height}")
            
            val currentTime = System.currentTimeMillis()
            
            // Timeout protection for processing
            withTimeout(30000L) {
                processScreenContentSafely(bitmap, processingId, currentTime)
            }
            
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "⏰ Screen processing timeout after 30s [$processingId]", e)
            lastServiceError = "Processing timeout: ${e.message}"
            // Force hide overlays on timeout
            try {
                blurOverlayManager.hideBlurOverlay()
                isCurrentlyBlurred = false
            } catch (overlayError: Exception) {
                Log.e(TAG, "Error hiding overlay after timeout", overlayError)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "💾 Out of memory during screen processing [$processingId]", e)
            lastServiceError = "Out of memory: ${e.message}"
            // Emergency cleanup
            emergencyMemoryCleanup()
        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Security exception during screen processing [$processingId]", e)
            lastServiceError = "Security error: ${e.message}"
            // Don't retry, likely permission revoked
        } catch (e: IllegalStateException) {
            Log.e(TAG, "⚠️ Illegal state during screen processing [$processingId]", e)
            lastServiceError = "State error: ${e.message}"
            // Try recovery
            attemptServiceRecovery()
        } catch (e: Exception) {
            lastServiceError = "${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "💥 Unexpected error during screen processing [$processingId]", e)
            
            // Report to crash recovery system
            try {
                crashRecoverySystem.reportCriticalError(
                    "screen_processing",
                    "Processing error: ${e.message}",
                    e
                )
            } catch (recoveryError: Exception) {
                Log.e(TAG, "Error reporting to crash recovery system", recoveryError)
            }
            
            // General error recovery
            handleGeneralProcessingError(e)
        } finally {
            // Always increment frame counter
            totalFramesProcessed++
        }
    }
    
    /**
     * Safe screen content processing with enhanced error handling
     */
    private suspend fun processScreenContentSafely(bitmap: Bitmap, processingId: String, currentTime: Long) {
        // Generate bitmap hash for caching
        val bitmapHash = generateBitmapHash(bitmap)
        
        // Check cache first
        val cachedResult = detectionCache[bitmapHash]
        if (cachedResult != null && (currentTime - cachedResult.first) < cacheExpirationMs) {
            Log.d(TAG, "💾 Using cached detection result: ${cachedResult.second} [$processingId]")
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
        
        // Safely get current settings with fallback
        val currentSettings = try {
            settingsRepository.getCurrentSettings()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get settings, using safe defaults [$processingId]", e)
            com.hieltech.haramblur.data.AppSettings() // Default settings
        }
        
        if (!currentSettings.enableRealTimeProcessing) {
            Log.d(TAG, "⏸️ Real-time processing disabled in settings [$processingId]")
            return
        }
        
        if (currentSettings.isServicePaused) {
            Log.d(TAG, "⏸️ Service is paused - skipping all processing [$processingId]")
            // Hide any existing blur overlay when paused
            safeHideOverlay("service_paused")
            return
        }
        
        Log.d(TAG, "⚙️ Processing with settings: Female blur=${currentSettings.blurFemaleFaces}, Male blur=${currentSettings.blurMaleFaces}, NSFW=${currentSettings.enableNSFWDetection}, GPU=${currentSettings.enableGPUAcceleration} [$processingId]")
        
        // Get dynamic processing cooldown from settings
        val processingInterval = getProcessingInterval(currentSettings)
        if (currentTime - lastProcessingTime < processingInterval) {
            Log.v(TAG, "⏱️ Throttling: Skipping processing (interval: ${processingInterval}ms) [$processingId]")
            totalFramesSkipped++
            return
        }
        lastProcessingTime = currentTime
        
        Log.d(TAG, "Processing screen content: ${bitmap.width}x${bitmap.height} (speed: ${currentSettings.processingSpeed}) [$processingId]")
        
        // Check app filtering first - early exit if app should not be monitored
        val shouldMonitor = try {
            shouldMonitorCurrentApp()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking app monitoring, defaulting to monitor [$processingId]", e)
            true // Default to monitoring for safety
        }
        
        if (!shouldMonitor) {
            Log.d(TAG, "Content monitoring skipped for app: $currentAppPackage (not in monitored categories) [$processingId]")
            // Hide any existing overlays when switching to unmonitored app
            safeHideOverlay("app_not_monitored")
            isCurrentlyBlurred = false
            return
        }

        // Analyze content using detection engine with user settings
        Log.d(TAG, "🧠 Starting content analysis with detection engine... [$processingId]")
        
        val analysisResult = try {
            // Use error recovery for critical detection
            errorRecovery.executeWithRecovery(
                operationName = "content_detection",
                operation = {
                    contentDetectionEngine.analyzeContent(bitmap, currentSettings, currentAppPackage)
                },
                fallback = {
                    Log.w(TAG, "Using fallback content detection [$processingId]")
                    // Safe fallback result - need to create a proper failure result
                    createFailureResult("Fallback detection used")
                }
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in content detection [$processingId]", e)
            createFailureResult("Detection engine failed: ${e.message}")
        }
        
        if (analysisResult.isSuccessful()) {
            Log.d(TAG, "✅ Content analysis successful, handling results... [$processingId]")

            // Behavioral actions intentionally disabled (see note at line 181)

            // Handle traditional blur overlay based on action results
            try {
                val shouldBlur = handleAnalysisResultWithStability(analysisResult, currentSettings, bitmap)
                detectionCache[bitmapHash] = Pair(currentTime, shouldBlur)
                lastBitmapHash = bitmapHash
            } catch (e: Exception) {
                Log.e(TAG, "Error handling analysis result [$processingId]", e)
                // Safe fallback - maintain current state
            }

        } else {
            Log.w(TAG, "❌ Content analysis failed: ${analysisResult.error} [$processingId]")
            // Don't immediately hide blur on failure - maintain current state for safety
            if (!isCurrentlyBlurred || (currentTime - lastBlurStartTime) > minBlurDuration) {
                safeHideOverlay("analysis_failed")
                isCurrentlyBlurred = false
            }
        }
        
        // Track processing time
        val processingTime = System.currentTimeMillis() - currentTime
        synchronized(processingTimes) {
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
     * Safe overlay hiding with error recovery
     */
    private fun safeHideOverlay(reason: String) {
        try {
            Log.d(TAG, "Safely hiding overlay: $reason")
            blurOverlayManager.hideBlurOverlay()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay ($reason), attempting emergency cleanup", e)
            try {
                blurOverlayManager.emergencyHideAllOverlays()
            } catch (emergencyError: Exception) {
                Log.e(TAG, "Emergency cleanup also failed", emergencyError)
            }
        }
    }
    
    /**
     * Emergency memory cleanup when OOM occurs
     */
    private fun emergencyMemoryCleanup() {
        try {
            Log.w(TAG, "🧹 Performing emergency memory cleanup")
            
            // Clear detection caches
            synchronized(detectionCache) {
                detectionCache.clear()
            }
            synchronized(recentNSFWDetections) {
                recentNSFWDetections.clear()
            }
            synchronized(processingTimes) {
                processingTimes.clear()
            }
            
            // Force garbage collection
            System.gc()
            
            // Hide all overlays
            safeHideOverlay("memory_cleanup")
            
            // Reset processing state
            isCurrentlyBlurred = false
            lastBlurStartTime = 0
            
            Log.w(TAG, "🧹 Emergency memory cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency memory cleanup", e)
        }
    }
    
    /**
     * Emergency reset to clear all stuck states and overlays
     */
    private fun emergencyReset() {
        try {
            Log.w(TAG, "🏥 EMERGENCY RESET: Clearing all stuck states")
            
            // Force hide all overlays
            try {
                blurOverlayManager.emergencyHideAllOverlays()
            } catch (e: Exception) {
                Log.e(TAG, "Error in overlay cleanup during emergency reset", e)
            }
            
            // Reset all state
            isCurrentlyBlurred = false
            lastBlurStartTime = 0
            isShowingBlockedSiteOverlay = false
            pornClosureInFlight = false
            
            // Clear detection caches safely
            try {
                synchronized(detectionCache) {
                    detectionCache.clear()
                }
                synchronized(recentNSFWDetections) {
                    recentNSFWDetections.clear()
                }
                synchronized(processingTimes) {
                    processingTimes.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing detection caches", e)
            }
            
            // Reset processing state
            isProcessingActive = true // Re-enable if it was disabled
            
            // Clear error tracking
            errorCounts.clear()
            
            // Force garbage collection
            System.gc()
            
            Log.w(TAG, "🏥 Emergency reset completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Critical error during emergency reset", e)
        }
    }
    
    /**
     * Attempt service recovery when in illegal state
     */
    private fun attemptServiceRecovery() {
        serviceScope.launch {
            try {
                Log.w(TAG, "🔄 Attempting service recovery")
                
                // Stop current operations
                isProcessingActive = false
                
                // Clear overlay states
                safeHideOverlay("service_recovery")
                isCurrentlyBlurred = false
                
                // Reinitialize critical components
                delay(2000) // Wait for cleanup
                
                // Restart content monitoring if needed
                val settings = settingsRepository.getCurrentSettings()
                if (settings.enableRealTimeProcessing && !settings.isServicePaused) {
                    Log.i(TAG, "🔄 Restarting content monitoring after recovery")
                    startContentMonitoring()
                }
                
                Log.w(TAG, "🔄 Service recovery completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Service recovery failed", e)
            }
        }
    }
    
    /**
     * Handle general processing errors
     */
    private fun handleGeneralProcessingError(error: Exception) {
        serviceScope.launch {
            try {
                Log.w(TAG, "🛠️ Handling general processing error: ${error.javaClass.simpleName}")
                
                when (error) {
                    is OutOfMemoryError -> {
                        emergencyMemoryCleanup()
                    }
                    is SecurityException -> {
                        Log.w(TAG, "Security error - may need permission check")
                        safeHideOverlay("security_error")
                    }
                    is IllegalStateException -> {
                        attemptServiceRecovery()
                    }
                    else -> {
                        // General error - safe cleanup
                        safeHideOverlay("general_error")
                        
                        // If too many consecutive errors, pause briefly
                        val errorCount = errorCounts.computeIfAbsent("general_processing") { AtomicInteger(0) }
                        if (errorCount.incrementAndGet() > 5) {
                            Log.w(TAG, "Too many processing errors, pausing for 10 seconds")
                            isProcessingActive = false
                            delay(10000)
                            isProcessingActive = true
                            errorCount.set(0)
                        }
                    }
                }
                
            } catch (recoveryError: Exception) {
                Log.e(TAG, "Error recovery itself failed", recoveryError)
            }
        }
    }
    
    /**
     * Create a failure result for content analysis
     */
    private fun createFailureResult(error: String): ContentDetectionEngine.ContentAnalysisResult {
        return ContentDetectionEngine.ContentAnalysisResult.failed(error)
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
    
    private fun handleAnalysisResultWithStability(result: ContentDetectionEngine.ContentAnalysisResult, settings: AppSettings, bitmap: Bitmap? = null): Boolean {
        val currentTime = System.currentTimeMillis()
        
        Log.d(TAG, "📊 Analysis result: shouldBlur=${result.shouldBlur}, regions=${result.blurRegions.size}")
        
        // Adapt thresholds based on learning
        adaptThresholds(currentTime)
        
        // Use adaptive thresholds for better detection
        val nsfwThreshold = minOf(adaptiveNSFWThreshold, settings.nsfwConfidenceThreshold)
        val genderThreshold = minOf(adaptiveGenderThreshold, settings.genderConfidenceThreshold)
        
        Log.d(TAG, "🧠 Using adaptive thresholds: NSFW=$nsfwThreshold, Gender=$genderThreshold")
        
        // ENHANCED FEMALE DETECTION WITH INCLUSIVE LOGIC
        val hasFemaleFaces = if (settings.blurFemaleFaces) {
            result.faceDetectionResult?.detectedFaces?.any { face ->
                val isConfidentFemale = face.genderConfidence > genderThreshold &&
                                       face.estimatedGender.toString().contains("FEMALE", ignoreCase = true)
                
                // INCLUSIVE: Consider moderate confidence males as potential females for safety
                val isModerateConfidenceMale = face.estimatedGender.toString().contains("MALE", ignoreCase = true) &&
                                              face.genderConfidence >= 0.4f &&
                                              face.genderConfidence <= 0.8f
                
                // LOW CONFIDENCE: Any gender with very low confidence might be female
                val isLowConfidenceUnknown = face.genderConfidence < 0.4f
                
                // SAFETY: Include unknown gender faces
                val isUnknownGender = face.estimatedGender.toString().contains("UNKNOWN", ignoreCase = true)
                
                Log.d(TAG, "👩 Female analysis: confidence=${face.genderConfidence}, gender=${face.estimatedGender}, " +
                          "confidentFemale=$isConfidentFemale, moderateMale=$isModerateConfidenceMale, " +
                          "lowConfidence=$isLowConfidenceUnknown, unknown=$isUnknownGender")
                logDebugToDatabase("👩 Female analysis: confidence=${face.genderConfidence}, gender=${face.estimatedGender}, " +
                                 "confidentFemale=$isConfidentFemale, moderateMale=$isModerateConfidenceMale", LogRepository.LogCategory.DETECTION)
                
                // INCLUSIVE: Blur confident females, moderate-confidence males (potential misclassification),
                // low-confidence faces, and unknown gender faces
                isConfidentFemale ||
                (isModerateConfidenceMale && settings.detectionSensitivity > 0.5f) ||
                (isLowConfidenceUnknown && settings.detectionSensitivity > 0.6f) ||
                (isUnknownGender && settings.detectionSensitivity > 0.4f)
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
            logDebugToDatabase("🔞 NSFW analysis: confidence=${nsfwResult.confidence}, isNSFW=${nsfwResult.isNSFW}, threshold=$nsfwThreshold", LogRepository.LogCategory.DETECTION)
            
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
        
        // Apply blur based on consecutive detection count for stability
        val shouldShowBlur = when {
            // Show blur after required consecutive detections
            shouldBlurBasedOnContent && consecutiveNSFWCount >= requiredConsecutiveDetections -> {
                if (!isCurrentlyBlurred) {
                    Log.w(TAG, "🛑 ⚡ BLUR TRIGGERED - $consecutiveNSFWCount consecutive detections!")
                    Log.w(TAG, "   Blur regions: ${result.blurRegions.size}")
                    Log.w(TAG, "   Face detection: ${hasFemaleFaces}")
                    Log.w(TAG, "   NSFW detection: ${hasNSFWContent}")
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
            // Get proper screen metrics with system UI offsets
            val screenMetrics = CoordinateMapper.getScreenMetrics(this)
            
            // Map blur regions from bitmap/detection space to screen space with proper offsets
            val preciseRegions = CoordinateMapper.mapBitmapRegionsToScreen(
                regions = result.blurRegions,
                bitmapWidth = bitmap?.width ?: screenMetrics.realWidth,
                bitmapHeight = bitmap?.height ?: screenMetrics.realHeight,
                screenMetrics = screenMetrics,
                includeStatusBarOffset = false // Accessibility overlay covers full screen
            ).mapNotNull { region ->
                // Only include regions that are valid and not too small
                if (region.width() > 20 && region.height() > 20) {
                    // Debug the mapping for verification
                    if (result.blurRegions.indexOf(region) == 0) {
                        CoordinateMapper.debugRegionMapping(
                            originalRegion = result.blurRegions[0],
                            mappedRegion = region,
                            context = this
                        )
                    }
                    region
                } else null
            }

            if (preciseRegions.isNotEmpty()) {
                Log.w(TAG, "🎯 ===== ACTIVATING BLUR OVERLAY =====")
                Log.w(TAG, "   Screen: ${screenMetrics.realWidth}x${screenMetrics.realHeight}")
                Log.w(TAG, "   Status Bar: ${screenMetrics.statusBarHeight}px")
                Log.w(TAG, "   Navigation Bar: ${screenMetrics.navigationBarHeight}px")
                Log.w(TAG, "   Regions: ${preciseRegions.size}")
                preciseRegions.forEachIndexed { index, rect ->
                    Log.w(TAG, "   Region $index: ($rect.left, $rect.top) -> ($rect.right, $rect.bottom) [${rect.width()}x${rect.height()}]")
                }
                Log.w(TAG, "   Intensity: ${settings.blurIntensity}")
                Log.w(TAG, "   Style: ${settings.blurStyle}")
                Log.w(TAG, "=====================================")
                
                try {
                    blurOverlayManager.showBlurOverlay(
                        blurRegions = preciseRegions,
                        blurIntensity = settings.blurIntensity,
                        blurStyle = settings.blurStyle,
                        contentSensitivity = result.nsfwDetectionResult?.confidence ?: 0.5f,
                        smoothTransition = settings.enableSmoothBlurAnimations
                    )
                    Log.w(TAG, "✅ Blur overlay showBlurOverlay() called successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ CRITICAL: Failed to show blur overlay", e)
                }
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
                logInfoToDatabase("👤 Faces detected: ${faceResult.facesDetected}", LogRepository.LogCategory.DETECTION)
            }
        }
        
        result.nsfwDetectionResult?.let { nsfwResult ->
            if (nsfwResult.isNSFW) {
                Log.d(TAG, "🔞 NSFW content detected with confidence: ${nsfwResult.confidence}")
                logInfoToDatabase("🔞 NSFW content detected with confidence: ${nsfwResult.confidence}", LogRepository.LogCategory.DETECTION)
            }
        }
    }
    
    // Removed old safeExecute function to avoid conflicts with new enhanced version

    /**
     * Check if the given package name is a browser app
     */
    private fun isBrowserApp(packageName: String): Boolean {
        return knownBrowserPackages.contains(packageName.lowercase(Locale.ROOT))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        safeExecute("onAccessibilityEvent") {
            when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val newPackageName = event.packageName?.toString()
                val oldApp = currentAppPackage
                currentAppPackage = newPackageName

                if (oldApp != newPackageName) {
                    Log.d(TAG, "App changed from: $oldApp to: $currentAppPackage")
                    logDebugToDatabase("App changed from: $oldApp to: $currentAppPackage", LogRepository.LogCategory.DETECTION)

                    // Clean up overlays when switching away from monitored apps
                    safeExecute("cleanupOverlaysOnAppChange") {
                        if (oldApp != null && isBrowserApp(oldApp)) {
                            Log.d(TAG, "🧹 Cleaning up overlays after leaving browser: $oldApp")
                            blurOverlayManager.cleanupAllOverlays()
                        }
                        true
                    }
                }

                Log.d(TAG, "Window state changed: ${event.packageName}")
                logDebugToDatabase("Window state changed: ${event.packageName}", LogRepository.LogCategory.DETECTION)

                // Check app filtering and control screen capture
                serviceScope.launch {
                    try {
                        val shouldMonitor = appFilteringManager.shouldMonitorApp(currentAppPackage)
                        Log.d(TAG, "App changed to $currentAppPackage, should monitor: $shouldMonitor")
                        logInfoToDatabase("App changed to $currentAppPackage, should monitor: $shouldMonitor", LogRepository.LogCategory.DETECTION)
                        
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
                            Log.i(TAG, "Starting screen capture for monitored app: $currentAppPackage")
                            logInfoToDatabase("Starting screen capture for monitored app: $currentAppPackage", LogRepository.LogCategory.DETECTION)
                            
                            // Force restart screen capture if not active
                            if (!screenCaptureManager.isCapturingActive()) {
                                Log.d(TAG, "📸 Screen capture not active - starting now")
                                // Reset processing state to allow startContentMonitoring to run
                                if (isProcessingActive) {
                                    isProcessingActive = false
                                }
                                startContentMonitoring()
                            } else {
                                Log.d(TAG, "✅ Screen capture already active")
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
                    safeExecute("checkForUrlChanges") {
                        runBlocking { checkForUrlChanges(event) }
                        true
                    }
                }

                // Trigger immediate content analysis when window changes
                serviceScope.launch {
                    delay(100) // Minimal delay for faster response
                    if (isProcessingActive) {
                        // Force immediate processing for window changes
                        lastProcessingTime = 0L
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Check for URL changes in content updates
                serviceScope.launch {
                    safeExecute("checkForUrlChanges") {
                        runBlocking { checkForUrlChanges(event) }
                        true
                    }
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
            true
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
            
            // Proper cleanup of blur overlay manager with lifecycle teardown
            if (this::blurOverlayManager.isInitialized) {
                blurOverlayManager.cleanup()
                Log.d(TAG, "BlurOverlayManager cleaned up with lifecycle support")
            }

            // Cleanup all enhanced detection services
            serviceLifecycleManager.cleanupServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun performTerminationCleanup(reason: String) {
        Log.w(TAG, "Ensuring overlays and screen capture are stopped ($reason)")

        if (this::blurOverlayManager.isInitialized) {
            runCatching { hideBlockedSiteOverlay() }.onFailure {
                Log.e(TAG, "Failed to hide blocked site overlay during $reason", it)
            }

            runCatching { blurOverlayManager.hideBlurOverlay() }.onFailure {
                Log.e(TAG, "Failed to hide blur overlay during $reason", it)
            }
        } else {
            Log.w(TAG, "BlurOverlayManager not initialized; skipping overlay cleanup")
        }

        if (this::screenCaptureManager.isInitialized) {
            runCatching { screenCaptureManager.stopCapturing() }.onFailure {
                Log.e(TAG, "Failed to stop screen capture during $reason", it)
            }
        } else {
            Log.w(TAG, "ScreenCaptureManager not initialized; skipping capture stop")
        }

        isProcessingActive = false
        isCurrentlyBlurred = false
        isShowingBlockedSiteOverlay = false
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
     * Public method to trigger emergency reset via broadcast
     * Call this if overlays get stuck (e.g., via ADB)
     */
    fun triggerEmergencyReset() {
        Log.w(TAG, "TRIGGER EMERGENCY RESET: Initiating reset via internal method")
        emergencyReset()
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

        // Throttle URL checking to avoid excessive processing (optimized from 500ms to 2000ms)
        if (currentTime - lastUrlCheckTime < 2000) { // Reduced frequency for better performance
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
            if (packageName != null && isBrowserApp(packageName)) {
                val extractedUrl = extractUrlFromAccessibilityEvent(event)
                if (extractedUrl != null && extractedUrl != currentUrl) {
                    currentUrl = extractedUrl

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
    
    // Initialize resilient URL extractor
    private val resilientUrlExtractor = ResilientUrlExtractor()

    /**
     * Extract URL from accessibility event using resilient extraction with error recovery
     */
    private suspend fun extractUrlFromAccessibilityEvent(event: AccessibilityEvent?): String? {
        if (event?.source == null) return null

        val packageName = event.packageName?.toString()

        return errorRecovery.executeUrlExtraction(packageName) {
            // Use resilient URL extractor as primary method
            resilientUrlExtractor.extractUrl(event.source, packageName)
                ?: // Fallback to legacy methods
                extractUrlFromNodeInfo(event.source)
                ?: extractUrlFromText(event.text?.toString())
                ?: extractUrlFromContentDescription(event.contentDescription?.toString())
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
     * Extract URL from text using optimized pattern matching
     * Enhanced for better performance and accuracy
     */
    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null

        // Optimized URL patterns - more specific and efficient
        val urlPatterns = listOf(
            // Full HTTP/HTTPS URLs (most specific first)
            Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE),
            // WWW URLs (add protocol)
            Regex("www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE),
            // Domain-based URLs
            Regex("[\\w\\-._~]+\\.[a-zA-Z]{2,}[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*", RegexOption.IGNORE_CASE)
        )

        // Single pass through patterns for better performance
        for (pattern in urlPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                var url = match.value

                // Add protocol if missing and it's not already there
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    // Only add protocol for www or domain patterns
                    if (url.startsWith("www.") || url.contains(".")) {
                        url = "https://$url"
                    }
                }

                // Basic validation - must contain at least one dot and be reasonable length
                if (url.length in 10..2048 && url.contains(".")) {
                    return url
                }
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

            // Show dialog with background tasks and reflection time
            Log.w(TAG, "🚀 PORN SITE BLOCKING - Dialog with background closure and reflection")
            logPornBlockingEvent(url, blockingResult, "reflection_dialog_shown")
            
            // Show interactive blocking dialog that stays open during background work
            showReflectivePornBlockingDialog(url, blockingResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling porn site blocking", e)
            logPornBlockingEvent(url, blockingResult, "error_occurred", error = e.message)
            // Fallback to standard blocking
            try { showBlockedSiteOverlay(blockingResult) } catch (_: Exception) {}
        }
    }

    /**
     * Show reflective porn blocking dialog with background tasks and user interaction
     */
    private fun showReflectivePornBlockingDialog(url: String, blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        try {
            Log.w(TAG, "🚨 Showing reflective porn blocking dialog with background tasks")
            
            var backgroundTasksComplete = false
            var reflectionTimeComplete = false
            
            // Use the existing porn blocking overlay with enhanced interaction
            blurOverlayManager.showPornBlockingOverlay(
                blockingResult = blockingResult,
                guidance = null,
                onAction = { action ->
                    when (action) {
                        is com.hieltech.haramblur.data.WarningDialogAction.Close -> {
                            // Only allow closing if background tasks and reflection time are complete
                            if (backgroundTasksComplete && reflectionTimeComplete) {
                                Log.d(TAG, "User dismissed dialog after reflection - tasks complete")
                                blurOverlayManager.emergencyHideAllOverlays()
                                logPornBlockingEvent(url, blockingResult, "user_dismissed_after_reflection")
                            } else {
                                Log.d(TAG, "User tried to close dialog too early - tasks still running")
                                // Could show a message that tasks are still running
                            }
                        }
                        else -> {
                            Log.d(TAG, "Dialog action during background tasks: $action")
                        }
                    }
                }
            )
            
            // Start background tasks immediately while dialog is visible
            serviceScope.launch {
                try {
                    pornClosureInFlight = true
                    
                    Log.d(TAG, "🔄 Starting background tab closure while dialog is visible")
                    
                    // Perform background tab closure
                    navigateAwayFromBlockedSite()
                    
                    backgroundTasksComplete = true
                    Log.d(TAG, "✅ Background tasks completed")
                    logPornBlockingEvent(url, blockingResult, "background_tasks_completed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background porn site closure", e)
                    backgroundTasksComplete = true // Mark as complete even on error
                    logPornBlockingEvent(url, blockingResult, "background_tasks_error", error = e.message)
                } finally {
                    pornClosureInFlight = false
                }
            }
            
            // Start reflection timer
            serviceScope.launch {
                val reflectionTimeSeconds = blockingResult.reflectionTimeSeconds.coerceAtLeast(5) // Minimum 5 seconds
                Log.d(TAG, "⏱️ Starting reflection timer: ${reflectionTimeSeconds} seconds")
                
                delay(reflectionTimeSeconds * 1000L)
                
                reflectionTimeComplete = true
                Log.d(TAG, "✅ Reflection time completed")
                logPornBlockingEvent(url, blockingResult, "reflection_time_completed")
                
                // If both tasks and reflection are complete, show completion message
                if (backgroundTasksComplete && reflectionTimeComplete) {
                    Log.d(TAG, "🎯 All tasks complete - user can now dismiss dialog")
                    // Could update dialog UI to show "OK" button or completion message
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing reflective porn blocking dialog", e)
        }
    }

    /**
     * Show quick porn blocking dialog for immediate visual feedback (legacy)
     */
    private fun showQuickPornBlockingDialog(blockingResult: com.hieltech.haramblur.detection.SiteBlockingResult) {
        try {
            Log.w(TAG, "🚨 Showing quick porn blocking dialog")
            
            // Use the existing porn blocking overlay but with immediate display
            blurOverlayManager.showPornBlockingOverlay(
                blockingResult = blockingResult,
                guidance = null,
                onAction = { action ->
                    // For quick dialog, any action just hides it
                    Log.d(TAG, "Quick dialog action: $action")
                    blurOverlayManager.emergencyHideAllOverlays()
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing quick porn blocking dialog", e)
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
    private fun performAggressivePornSiteClosure() {
        serviceScope.launch {
            try {
                Log.w(TAG, "🚫 Performing aggressive porn site closure")

                // Ensure overlay state is managed properly during closure
                var closureSuccessful = false

                // Strategy 1: Close current tab
                try {
                    val closeSuccess = closeCurrentBrowserTab()
                    if (closeSuccess) {
                        Log.d(TAG, "✅ Successfully closed porn tab")
                        closureSuccessful = true
                        delay(1000)
                        openSafePageAfterBlocking()
                        return@launch
                    }
                } catch (tabCloseError: Exception) {
                    Log.w(TAG, "❌ Error closing browser tab", tabCloseError)
                }

                // Strategy 2: Force back navigation multiple times
                if (!closureSuccessful) {
                    Log.d(TAG, "🔄 Porn tab close failed, trying back navigation")
                    try {
                        repeat(3) { attempt ->
                            val backSuccess = performBrowserAwareGlobalAction(
                                GLOBAL_ACTION_BACK,
                                "aggressive porn site closure back attempt ${attempt + 1}"
                            )
                            if (backSuccess) {
                                delay(500)
                                closureSuccessful = true
                            } else {
                                Log.w(
                                    TAG,
                                    "Back action attempt ${attempt + 1} skipped or failed during aggressive closure"
                                )
                                delay(300)
                            }
                        }
                    } catch (backNavError: Exception) {
                        Log.w(TAG, "❌ Error during back navigation", backNavError)
                    }
                }

                // Strategy 3: Go to home screen
                if (!closureSuccessful) {
                    try {
                        delay(1000)
                        val homeSuccess = performBrowserAwareGlobalAction(
                            GLOBAL_ACTION_HOME,
                            "aggressive porn site closure home"
                        )
                        if (homeSuccess) {
                            Log.d(TAG, "✅ Forced home screen from porn site")
                            closureSuccessful = true
                        }
                    } catch (homeError: Exception) {
                        Log.w(TAG, "❌ Error going to home screen", homeError)
                    }
                }

                // Clear URL to prevent re-blocking
                currentUrl = null

                // Hide overlay with safety delay
                delay(1000)
                try {
                    hideBlockedSiteOverlay()
                } catch (overlayError: Exception) {
                    Log.w(TAG, "❌ Error hiding blocked site overlay", overlayError)
                }

                if (closureSuccessful) {
                    Log.d(TAG, "✅ Aggressive porn site closure completed successfully")
                } else {
                    Log.w(TAG, "⚠️ Aggressive porn site closure completed with partial success")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Critical error in aggressive porn site closure", e)
                // Emergency fallback with enhanced error handling
                try {
                    Log.w(TAG, "🚨 Executing emergency fallback procedures")
                    performBrowserAwareGlobalAction(
                        GLOBAL_ACTION_HOME,
                        "aggressive porn site closure emergency home"
                    )
                    delay(500)
                    hideBlockedSiteOverlay()
                    currentUrl = null
                    Log.d(TAG, "✅ Emergency fallback completed")
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "❌ Emergency fallback also failed - system may be in unstable state", fallbackError)
                    // Force reset critical state variables
                    currentUrl = null
                    isShowingBlockedSiteOverlay = false
                }
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
            val backSuccess = performBrowserAwareGlobalAction(
                GLOBAL_ACTION_BACK,
                "navigate away from inappropriate content"
            )
            if (backSuccess) {
                Log.d(TAG, "Navigated back from inappropriate content")
            } else {
                Log.w(TAG, "Back navigation skipped or failed for inappropriate content")
                return
            }

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

                // Minimal delay for faster response
                delay(200)

                // Strategy 1: Close the current tab (more aggressive for porn sites)
                Log.d(TAG, "🔄 Strategy 1: Attempting to close current tab (AGGRESSIVE MODE for porn sites)")
                val isPornSite = currentUrl?.contains(Regex("(porn|xxx|sex|adult|nsfw|xvideos|pornhub|xhamster|redtube|xnxx)", RegexOption.IGNORE_CASE)) == true
                
                if (isPornSite) {
                    Log.w(TAG, "⚠️ PORN SITE DETECTED - Using aggressive close strategy")
                    // Try multiple times to ensure the tab is closed
                    for (attempt in 1..3) {
                        Log.d(TAG, "Close attempt #$attempt for porn site")
                        val closeTabSuccess = closeCurrentBrowserTab()
                        if (closeTabSuccess) {
                            Log.d(TAG, "✅ Successfully closed porn site tab on attempt #$attempt")
                            delay(100)
                            // Immediately hide overlays after closing porn tab
                            blurOverlayManager.emergencyHideAllOverlays()
                            delay(100)
                            // Check if we need to open a safe page
                            openSafePageAfterBlocking()
                            return@launch
                        }
                        delay(100) // Faster retry attempts
                    }
                    
                    // If tab close failed, force navigation to home
                    Log.w(TAG, "❌ Failed to close porn tab after 3 attempts, forcing HOME action")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    delay(200)
                    blurOverlayManager.emergencyHideAllOverlays()
                    return@launch
                } else {
                    // Regular close for non-porn sites
                    val closeTabSuccess = closeCurrentBrowserTab()
                    if (closeTabSuccess) {
                        Log.d(TAG, "✅ Successfully closed browser tab")
                        delay(300) // Faster response
                        // Check if we need to open a safe page
                        openSafePageAfterBlocking()
                        return@launch // Use return@launch for coroutine scope
                    }
                }

                // Strategy 2: Try to go back in browser history
                Log.d(TAG, "🔄 Strategy 2: Attempting to navigate back in history")
                try {
                    val backSuccess = performBrowserAwareGlobalAction(
                        GLOBAL_ACTION_BACK,
                        "navigate away from blocked site back navigation"
                    )
                    if (backSuccess) {
                        Log.d(TAG, "✅ Successfully navigated back in browser history")
                        delay(500) // Faster stability check

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
                delay(300) // Faster overlay hiding
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
                    performBrowserAwareGlobalAction(
                        GLOBAL_ACTION_HOME,
                        "navigate away from blocked site emergency home"
                    )
                } catch (homeError: Exception) {
                    Log.e(TAG, "❌ Error performing home action", homeError)
                }

                isActionInProgress = false // Reset flag on error
            }
        }
    }

    /**
     * Attempt to close the current browser tab with enhanced multi-browser support and crash prevention
     */
    private fun closeCurrentBrowserTab(): Boolean {
        return executeTabOperationSafely("closeCurrentBrowserTab") {
            Log.d(TAG, "🔍 Attempting to close current browser tab")

            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No root node available for tab closing")
                return@executeTabOperationSafely false
            }

            val packageName = rootNode.packageName?.toString()
            Log.d(TAG, "Detected browser package: $packageName")

            var closeSuccess = false
            var rootNodeRecycled = false
            
            try {
                closeSuccess = when {
                    packageName?.contains("firefox", ignoreCase = true) == true -> {
                        executeWithTimeout("closeFirefoxTab", 5000L) {
                            closeFirefoxTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("chrome", ignoreCase = true) == true -> {
                        executeWithTimeout("closeChromeTab", 5000L) {
                            closeChromeTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("edge", ignoreCase = true) == true -> {
                        executeWithTimeout("closeEdgeTab", 5000L) {
                            closeEdgeTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("samsung", ignoreCase = true) == true -> {
                        executeWithTimeout("closeSamsungBrowserTab", 5000L) {
                            closeSamsungBrowserTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("opera", ignoreCase = true) == true -> {
                        executeWithTimeout("closeOperaTab", 5000L) {
                            closeOperaTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("brave", ignoreCase = true) == true -> {
                        executeWithTimeout("closeBraveTab", 5000L) {
                            closeBraveTab(rootNode)
                        } ?: false
                    }
                    packageName?.contains("duckduckgo", ignoreCase = true) == true -> {
                        executeWithTimeout("closeDuckDuckGoTab", 5000L) {
                            closeDuckDuckGoTab(rootNode)
                        } ?: false
                    }
                    else -> {
                        executeWithTimeout("closeGenericBrowserTab", 5000L) {
                            closeGenericBrowserTab(rootNode)
                        } ?: false
                    }
                }

                if (closeSuccess) {
                    Log.d(TAG, "✅ Successfully closed tab in $packageName")
                    return@executeTabOperationSafely true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in browser-specific close logic: ${e.javaClass.simpleName}: ${e.message}")
                // Don't rethrow - we'll try fallback strategies
            } finally {
                // Ensure root node is always recycled
                safelyRecycleNode(rootNode)
                rootNodeRecycled = true
            }

            Log.d(TAG, "⚠️ Browser-specific close failed, using fallback method")

            // Enhanced fallback methods with fresh root node
            val fallbackRootNode = try {
                rootInActiveWindow
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get root node for fallback", e)
                null
            }
            
            return@executeTabOperationSafely if (fallbackRootNode != null) {
                try {
                    closeTabWithMultipleStrategies(fallbackRootNode, packageName)
                } finally {
                    safelyRecycleNode(fallbackRootNode)
                }
            } else {
                // Last resort fallback without accessibility nodes
                executeGlobalActionFallback()
            }
        }
    }

    /**
     * Enhanced tab closing with multiple fallback strategies
     */
    private fun closeTabWithMultipleStrategies(rootNode: AccessibilityNodeInfo, packageName: String?): Boolean {
        Log.d(TAG, "🔄 Trying multiple tab closing strategies")

        // Strategy 1: Find any close button or X button
        if (safeExecute("findCloseButton") { findAndClickAnyCloseButton(rootNode) }) {
            Log.d(TAG, "✅ Closed tab using close button detection")
            return true
        }

        // Strategy 2: Keyboard shortcut (Ctrl+W)
        if (safeExecute("keyboardShortcut") { sendKeyboardShortcut() }) {
            Log.d(TAG, "✅ Closed tab using keyboard shortcut")
            return true
        }

        // Strategy 3: Multiple back actions
        if (safeExecute("multipleBackActions") { performMultipleBackActions() }) {
            Log.d(TAG, "✅ Closed tab using multiple back actions")
            return true
        }

        // Strategy 4: Navigate to safe website instead of closing
        if (safeExecute("navigateToSafeWebsite") { navigateToIslamicWebsite() }) {
            Log.d(TAG, "✅ Navigated to Islamic website instead of closing tab")
            return true
        }

        // Strategy 5: Force home screen as last resort
        if (safeExecute("forceHomeScreen") { performGlobalAction(GLOBAL_ACTION_HOME) }) {
            Log.d(TAG, "✅ Forced home screen as last resort")
            return true
        }

        return false
    }

    /**
     * Find and click any close button (X, close, etc.)
     */
    private fun findAndClickAnyCloseButton(rootNode: AccessibilityNodeInfo): Boolean {
        val closeTexts = listOf("close", "x", "✕", "×", "⨯", "dismiss", "cancel")
        val closeDescriptions = listOf("close", "close tab", "dismiss", "cancel")

        // Search for clickable nodes with close-related text or content description
        return findAndClickNodeByText(rootNode, closeTexts) ||
               findAndClickNodeByDescription(rootNode, closeDescriptions) ||
               findAndClickNodeByClassName(rootNode, "android.widget.ImageButton") ||
               findAndClickNodeByClassName(rootNode, "android.widget.Button")
    }

    /**
     * Send keyboard shortcut Ctrl+W to close tab
     */
    private fun sendKeyboardShortcut(): Boolean {
        // This is a simplified approach - actual implementation would need
        // to inject key events, which requires additional permissions
        Log.d(TAG, "Keyboard shortcut not implemented - would need INJECT_EVENTS permission")
        return false
    }

    /**
     * Perform multiple back actions to navigate away
     */
    private fun performMultipleBackActions(): Boolean {
        var success = false
        repeat(3) { attempt ->
            if (performGlobalAction(GLOBAL_ACTION_BACK)) {
                success = true
                Log.d(TAG, "Back action ${attempt + 1} successful")
                Thread.sleep(500) // Small delay between actions
            } else {
                Log.w(TAG, "Back action ${attempt + 1} failed")
            }
        }
        return success
    }

    /**
     * Navigate to Islamic website instead of closing tab
     */
    private fun navigateToIslamicWebsite(): Boolean {
        val islamicWebsites = listOf(
            "https://quran.com",
            "https://islamqa.info",
            "https://sunnah.com",
            "https://islamhouse.com",
            "https://islamweb.net",
            "https://islamicity.org"
        )

        val selectedWebsite = islamicWebsites.random()
        Log.d(TAG, "🕌 Redirecting to Islamic website: $selectedWebsite")

        return navigateToUrl(selectedWebsite)
    }

    /**
     * Navigate to a specific URL in the current browser
     */
    private fun navigateToUrl(url: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            // Strategy 1: Find address bar and type URL
            val addressBar = findAddressBar(rootNode)
            if (addressBar != null) {
                if (addressBar.isFocusable) {
                    addressBar.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }

                // Clear existing text and type new URL
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url)
                if (addressBar.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                    // Press Enter to navigate
                    Thread.sleep(500)
                    return performGlobalAction(GLOBAL_ACTION_BACK) // This might trigger navigation
                }
            }

            // Strategy 2: Use Intent to open URL in browser
            return openUrlWithIntent(url)

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to URL: $url", e)
            return false
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Find the address bar in the browser
     */
    private fun findAddressBar(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Common address bar identifiers
        val addressBarIds = listOf(
            "url_bar", "address_bar", "omnibox", "location_bar",
            "search_box", "url_field", "address_field"
        )

        val addressBarTexts = listOf(
            "address", "url", "search", "location", "omnibox"
        )

        // Search by resource ID
        for (id in addressBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                return nodes[0]
            }
        }

        // Search by text content
        for (text in addressBarTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isEditable || node.className == "android.widget.EditText") {
                    return node
                }
            }
        }

        return null
    }

    /**
     * Open URL using Intent (fallback method)
     */
    private fun openUrlWithIntent(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "✅ Opened URL with Intent: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL with Intent: $url", e)
            false
        }
    }

    /**
     * Find and click node by text content
     */
    private fun findAndClickNodeByText(rootNode: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.d(TAG, "✅ Clicked node with text: $text")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Find and click node by content description
     */
    private fun findAndClickNodeByDescription(rootNode: AccessibilityNodeInfo, descriptions: List<String>): Boolean {
        fun searchNode(node: AccessibilityNodeInfo): Boolean {
            val description = node.contentDescription?.toString()?.lowercase()
            if (description != null) {
                for (desc in descriptions) {
                    if (description.contains(desc.lowercase()) && node.isClickable) {
                        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.d(TAG, "✅ Clicked node with description: $description")
                            return true
                        }
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && searchNode(child)) {
                    return true
                }
            }
            return false
        }

        return searchNode(rootNode)
    }

    /**
     * Find and click node by class name
     */
    private fun findAndClickNodeByClassName(rootNode: AccessibilityNodeInfo, className: String): Boolean {
        fun searchNode(node: AccessibilityNodeInfo): Boolean {
            if (node.className?.toString() == className && node.isClickable) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.d(TAG, "✅ Clicked node with class: $className")
                    return true
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && searchNode(child)) {
                    return true
                }
            }
            return false
        }

        return searchNode(rootNode)
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
            var foregroundPackage: String? = null
            if (rootNode != null) {
                try {
                    foregroundPackage = rootNode.packageName?.toString()
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
            repeat(2) { attempt ->
                val backSuccess = performBrowserAwareGlobalAction(
                    GLOBAL_ACTION_BACK,
                    "tab closing fallback back attempt ${attempt + 1}",
                    if (attempt == 0) foregroundPackage else null
                )
                if (backSuccess) {
                    Log.d(TAG, "✅ Successfully performed back action (attempt ${attempt + 1})")
                    Thread.sleep(500) // Brief pause between actions
                } else {
                    Log.w(TAG, "Back action skipped or failed (attempt ${attempt + 1})")
                }
            }

            // Method 3: Try to close app entirely (last resort)
            Log.d(TAG, "🔄 Attempting to close entire browser app")
            val recentAppsSuccess = performBrowserAwareGlobalAction(
                GLOBAL_ACTION_RECENTS,
                "tab closing fallback open recents",
                foregroundPackage
            )
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
                val text = node.text?.toString()?.lowercase(Locale.ROOT)
                val description = node.contentDescription?.toString()?.lowercase(Locale.ROOT)

                for (keyword in closeKeywords) {
                    if (text?.contains(keyword) == true || description?.contains(keyword) == true) {
                        if (activateCloseNode(node, keyword, depth)) {
                            Log.d(TAG, "Activated close control via keyword '$keyword' at depth $depth")
                            return true
                        }
                    }
                }

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

    private fun activateCloseNode(node: AccessibilityNodeInfo, keyword: String, depth: Int): Boolean {
        if (depth > 12) {
            return false
        }

        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d(TAG, "Clicked close candidate using ACTION_CLICK (keyword='$keyword', depth=$depth)")
            return true
        }

        val parent = node.parent
        if (parent != null) {
            try {
                if (activateCloseNode(parent, keyword, depth + 1)) {
                    return true
                }
            } finally {
                parent.recycle()
            }
        }

        return performTapGestureOnNode(node, "close keyword '$keyword'")
    }

    private fun performTapGestureOnNode(node: AccessibilityNodeInfo, reason: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture tap not supported on this API level for $reason")
            return false
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) {
            Log.w(TAG, "Cannot perform gesture for $reason: empty bounds")
            return false
        }

        val tapPath = Path().apply {
            moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(tapPath, 0, 150))
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture tap completed for $reason at bounds=$bounds")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Gesture tap cancelled for $reason at bounds=$bounds")
                }
            },
            null
        )

        if (!accepted) {
            Log.w(TAG, "Gesture tap request rejected for $reason at bounds=$bounds")
        }

        return accepted
    }

    /**
     * Open a safe Islamic page after blocking with multiple options
     */
    private suspend fun openSafePageAfterBlocking() {
        try {
            Log.d(TAG, "🕌 Opening safe Islamic page after blocking")

            // Minimal wait for navigation
            delay(500)

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
            val homeSuccess = performBrowserAwareGlobalAction(
                GLOBAL_ACTION_HOME,
                "navigate to safe location - home"
            )
            if (homeSuccess) {
                Log.d(TAG, "Navigated to home screen")
                return
            }

            // Strategy 2: Try to close current app/tab
            delay(500)
            val closeSuccess = performBrowserAwareGlobalAction(
                GLOBAL_ACTION_BACK,
                "navigate to safe location - back"
            )
            if (closeSuccess) {
                Log.d(TAG, "Closed current app/tab")
                delay(500)

                // Try home again
                val secondHomeSuccess = performBrowserAwareGlobalAction(
                    GLOBAL_ACTION_HOME,
                    "navigate to safe location - second home"
                )
                if (secondHomeSuccess) {
                    return
                }
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
            performBrowserAwareGlobalAction(
                GLOBAL_ACTION_HOME,
                "navigate to safe URL fallback home"
            )

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
     * Optimized for better performance with limited recursion
     */
    private fun findUrlInNodeHierarchy(node: AccessibilityNodeInfo?, depth: Int = 0): String? {
        if (node == null || depth > 3) return null // Reduced depth for better performance

        try {
            // Check current node text first (most likely to contain URL)
            val nodeText = node.text?.toString()
            if (!nodeText.isNullOrBlank()) {
                extractUrlFromText(nodeText)?.let { return it }
            }

            // Check content description as secondary option
            val nodeDescription = node.contentDescription?.toString()
            if (!nodeDescription.isNullOrBlank()) {
                extractUrlFromText(nodeDescription)?.let { return it }
            }

            // Check child nodes with optimized limits
            val childCount = node.childCount
            if (childCount > 0) {
                // Only check first few children that are likely to contain URLs
                val maxChildrenToCheck = minOf(childCount, 8) // Reduced from 20 for performance

                for (i in 0 until maxChildrenToCheck) {
                    val child = node.getChild(i)
                    val childUrl = findUrlInNodeHierarchy(child, depth + 1)
                    child?.recycle()
                    if (childUrl != null) return childUrl
                }
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

                val backSuccess = performBrowserAwareGlobalAction(
                    GLOBAL_ACTION_BACK,
                    "manual navigate back action"
                )
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
                val homeSuccess = performBrowserAwareGlobalAction(
                    GLOBAL_ACTION_HOME,
                    "auto close app - home"
                )
                if (homeSuccess) {
                    Log.d(TAG, "✅ Successfully closed app (went to home)")
                } else {
                    // Strategy 2: Try back button multiple times
                    Log.d(TAG, "🔄 Home failed, trying back navigation")
                    repeat(3) { attempt ->
                        val backSuccess = performBrowserAwareGlobalAction(
                            GLOBAL_ACTION_BACK,
                            "auto close app - back attempt ${attempt + 1}"
                        )
                        if (backSuccess) {
                            delay(500)
                        } else {
                            Log.w(
                                TAG,
                                "Back navigation skipped or failed (auto close attempt ${attempt + 1})"
                            )
                        }
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
                    val navigationSuccess = performBrowserAwareGlobalAction(
                        GLOBAL_ACTION_BACK,
                        "gentle redirect back navigation"
                    )
                    if (navigationSuccess) {
                        Log.d(TAG, "✅ Gentle redirect completed")
                    } else {
                        Log.w(TAG, "❌ Gentle redirect back navigation skipped or failed")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error showing gentle redirect warning", e)
                    // Fallback to just navigation
                    performBrowserAwareGlobalAction(
                        GLOBAL_ACTION_BACK,
                        "gentle redirect fallback back"
                    )
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Scroll gesture not supported on this API level")
            performBrowserAwareGlobalAction(
                GLOBAL_ACTION_BACK,
                "scroll gesture fallback (legacy)"
            )
            return
        }

        try {
            Log.d(TAG, "🖱️ Attempting scroll gesture via dispatchGesture")
            val metrics = resources.displayMetrics
            val startX = metrics.widthPixels / 2f
            val startY = metrics.heightPixels * 0.7f
            val endY = metrics.heightPixels * 0.3f

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()

            val accepted = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "Scroll gesture completed successfully")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Scroll gesture cancelled by system")
                    }
                },
                null
            )

            if (!accepted) {
                Log.w(TAG, "Scroll gesture request rejected, using back action fallback")
                performBrowserAwareGlobalAction(
                    GLOBAL_ACTION_BACK,
                    "scroll gesture fallback (rejected)"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error performing scroll gesture", e)
            performBrowserAwareGlobalAction(
                GLOBAL_ACTION_BACK,
                "scroll gesture fallback (error)"
            )
        }
    }
    
    /**
     * Check if the service can perform global actions - prevents crashes
     */
    private fun canPerformGlobalActions(): Boolean {
        return try {
            // Check if service is properly connected and has capabilities
            serviceInfo != null && 
            rootInActiveWindow != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if can perform global actions", e)
            false
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

    // ========================================
    // CRASH PREVENTION SAFETY WRAPPERS
    // ========================================

    /**
     * Execute tab operation with comprehensive crash prevention
     */
    private fun <T> executeTabOperationSafely(operationName: String, operation: () -> T): T {
        // Check if we're exceeding concurrent operation limits
        if (activeTabOperations.get() >= maxConcurrentTabOperations) {
            Log.w(TAG, "Too many concurrent tab operations (${activeTabOperations.get()}), rejecting $operationName")
            throw IllegalStateException("Too many concurrent tab operations")
        }

        val startTime = System.currentTimeMillis()
        activeTabOperations.incrementAndGet()
        
        return try {
            // Check for recent failures of the same operation
            val recentFailures = tabOperationHistory
                .filter { it.second == operationName && (startTime - it.first) < 30000L }
                .size
            
            if (recentFailures >= 3) {
                Log.w(TAG, "Operation $operationName has failed $recentFailures times recently, using fallback")
                return executeGlobalActionFallback() as T
            }

            // Execute the operation with timeout protection
            val result = executeWithTimeout(operationName, 10000L) {
                operation()
            }
            
            result ?: throw TimeoutException("Operation $operationName timed out")
            
        } catch (e: Exception) {
            Log.e(TAG, "Tab operation $operationName failed: ${e.javaClass.simpleName}: ${e.message}")
            
            // Record failure
            tabOperationHistory.add(Pair(startTime, operationName))
            
            // Clean old history entries
            tabOperationHistory.removeAll { (startTime - it.first) > 300000L } // 5 minutes
            
            // Return safe fallback
            executeGlobalActionFallback() as T
            
        } finally {
            activeTabOperations.decrementAndGet()
        }
    }

    /**
     * Execute operation with timeout protection
     */
    private fun <T> executeWithTimeout(operationName: String, timeoutMs: Long, operation: () -> T): T? {
        return try {
            val future = serviceScope.async {
                try {
                    operation()
                } catch (e: Exception) {
                    Log.w(TAG, "Operation $operationName threw exception: ${e.javaClass.simpleName}: ${e.message}")
                    throw e
                }
            }
            
            runBlocking {
                withTimeout(timeoutMs) {
                    future.await()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Operation $operationName timed out after ${timeoutMs}ms")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Operation $operationName failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    /**
     * Safely recycle AccessibilityNodeInfo with error handling
     */
    private fun safelyRecycleNode(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        try {
            // AccessibilityNodeInfo doesn't have isRecycled method in all API levels
            // Just try to recycle and catch any exceptions
            node.recycle()
            Log.v(TAG, "Successfully recycled accessibility node")
        } catch (e: IllegalStateException) {
            // Node was already recycled
            Log.v(TAG, "Node was already recycled")
        } catch (e: Exception) {
            Log.w(TAG, "Error recycling accessibility node: ${e.javaClass.simpleName}: ${e.message}")
            // Don't rethrow - this is cleanup code
        }
    }

    /**
     * Execute global action fallback when all else fails
     */
    private fun executeGlobalActionFallback(): Boolean {
        return try {
            Log.i(TAG, "Executing global action fallback sequence")
            
            // Try multiple fallback strategies in order of preference
            val strategies = listOf(
                { performBrowserAwareGlobalAction(GLOBAL_ACTION_BACK, "fallback_back") },
                { performBrowserAwareGlobalAction(GLOBAL_ACTION_HOME, "fallback_home") },
                { performGlobalAction(GLOBAL_ACTION_BACK) },
                { performGlobalAction(GLOBAL_ACTION_HOME) }
            )
            
            for ((index, strategy) in strategies.withIndex()) {
                try {
                    if (strategy()) {
                        Log.d(TAG, "Global action fallback strategy ${index + 1} succeeded")
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Global action fallback strategy ${index + 1} failed", e)
                }
                
                // Small delay between strategies
                Thread.sleep(200)
            }
            
            Log.w(TAG, "All global action fallback strategies failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in global action fallback", e)
            false
        }
    }

    /**
     * Safe execution wrapper for any operation
     */
    private fun <T> safeExecute(operationName: String, defaultValue: T, operation: () -> T): T {
        return try {
            operation()
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception in $operationName: ${e.message}")
            defaultValue
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Illegal state in $operationName: ${e.message}")
            defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in $operationName: ${e.javaClass.simpleName}: ${e.message}")
            defaultValue
        }
    }

    /**
     * Enhanced safeExecute that returns boolean (for existing compatibility)
     */
    private fun safeExecute(operationName: String, operation: () -> Boolean): Boolean {
        return safeExecute(operationName, false, operation)
    }

    /**
     * Enhanced emergency cleanup when things go wrong
     */
    private fun performEmergencyReset() {
        try {
            Log.w(TAG, "🚨 EMERGENCY RESET INITIATED")
            
            // Stop all active operations
            activeTabOperations.set(0)
            
            // Clear operation history
            tabOperationHistory.clear()
            tabOperationTimeouts.clear()
            
            // Hide all overlays
            blurOverlayManager.emergencyHideAllOverlays()
            
            // Reset blur state
            isCurrentlyBlurred = false
            lastBlurStartTime = 0
            
            // Clear URL state
            currentUrl = null
            
            // Reset action flags
            isActionInProgress = false
            pornClosureInFlight = false
            
            Log.w(TAG, "🚨 EMERGENCY RESET COMPLETED")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL: Emergency reset failed", e)
        }
    }

    /**
     * Enhanced cleanup when service is terminating
     */
    private fun performEnhancedTerminationCleanup(reason: String) {
        try {
            Log.i(TAG, "Performing termination cleanup: $reason")
            
            // Stop content monitoring
            stopContentMonitoring()
            
            // Emergency reset to clean state
            performEmergencyReset()
            
            Log.i(TAG, "Termination cleanup completed: $reason")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during termination cleanup: $reason", e)
        }
    }

    /**
     * Enhanced component cleanup with better error handling
     */
    private fun performEnhancedComponentCleanup() {
        val components = listOf(
            "ScreenCaptureManager" to { screenCaptureManager.stopCapturing() },
            "BlurOverlayManager" to { blurOverlayManager.emergencyHideAllOverlays() },
            "ServiceLifecycleManager" to { 
                try {
                    serviceLifecycleManager.cleanupServices()
                } catch (e: Exception) {
                    Log.w(TAG, "ServiceLifecycleManager cleanup failed: ${e.message}")
                }
            }
        )
        
        for ((name, cleanup) in components) {
            try {
                cleanup()
                Log.d(TAG, "$name cleaned up successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up $name: ${e.message}")
            }
        }
    }
}
