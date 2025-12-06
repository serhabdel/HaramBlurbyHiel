package com.hieltech.haramblur

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.services.DhikrManager
import com.hieltech.haramblur.services.DhikrNotificationManager
import com.hieltech.haramblur.services.PrayerNotificationWorker
import com.hieltech.haramblur.utils.LocaleUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HaramBlurApplication : Application(), Configuration.Provider {

    private val TAG = "HaramBlurApplication"

    @Inject
    lateinit var appBlockingManager: AppBlockingManager

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var dhikrManager: DhikrManager

    @Inject
    lateinit var notificationManager: DhikrNotificationManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HaramBlur Application created")

        // Initialize WorkManager first (required for Glance widgets)
        initializeWorkManager()
        
        // Schedule periodic widget updates for prayer countdown
        scheduleWidgetUpdates()
        
        // Verify ML capabilities before initializing components
        verifyMLCapabilities()
        
        // Initialize app-level components here
        initializeComponents()
    }
    
    /**
     * Initialize WorkManager manually since we disabled auto-initialization
     */
    private fun initializeWorkManager() {
        try {
            // Check if already initialized
            try {
                WorkManager.getInstance(this)
                Log.d(TAG, "✅ WorkManager already initialized")
            } catch (e: IllegalStateException) {
                // Not initialized, do it now
                WorkManager.initialize(this, workManagerConfiguration)
                Log.d(TAG, "✅ WorkManager initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize WorkManager", e)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    
    /**
     * Schedule periodic widget updates for prayer countdown
     */
    private fun scheduleWidgetUpdates() {
        try {
            com.hieltech.haramblur.widget.WidgetUpdateWorker.schedule(this)
            Log.d(TAG, "✅ Widget update worker scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule widget updates", e)
        }
    }

    override fun attachBaseContext(base: Context) {
        // Apply language before creating the base context using LocaleUtils
        val language = getSavedLanguage(base)
        Log.d(TAG, "Wrapping base context with saved language via LocaleUtils: ${language.displayName} (${language.name})")
        val wrapped = LocaleUtils.wrap(base, language)
        super.attachBaseContext(wrapped)
    }

    private fun getSavedLanguage(context: Context): com.hieltech.haramblur.detection.Language {
        return try {
            // Read directly from SharedPreferences since DI isn't available here
            val prefs = context.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
            val stored = prefs.getString(
                "preferred_language",
                com.hieltech.haramblur.detection.Language.ENGLISH.name
            )
            val lang = runCatching {
                com.hieltech.haramblur.detection.Language.valueOf(
                    stored ?: com.hieltech.haramblur.detection.Language.ENGLISH.name
                )
            }.getOrElse { com.hieltech.haramblur.detection.Language.ENGLISH }
            Log.d(TAG, "Loaded saved language from prefs: ${lang.displayName} (${lang.name})")
            lang
        } catch (e: Exception) {
            Log.e(TAG, "Error getting saved language, using English", e)
            com.hieltech.haramblur.detection.Language.ENGLISH
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "HaramBlur Application terminating")

        // Perform cleanup
        performCleanup()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")

        // Trigger memory cleanup
        triggerMemoryCleanup()
    }

    private fun initializeComponents() {
        // Initialize any app-level components
        Log.d(TAG, "Initializing app components")
        
        // Schedule prayer notifications worker
        try {
            Log.d(TAG, "🕌 Scheduling prayer notification worker")
            PrayerNotificationWorker.schedulePrayerNotifications(this)
            Log.i(TAG, "✅ Prayer notification worker scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule prayer notification worker", e)
            // Try to schedule again after a delay
            CoroutineScope(Dispatchers.IO).launch {
                delay(5000) // Wait 5 seconds
                try {
                    Log.d(TAG, "🔄 Retrying prayer notification worker scheduling")
                    PrayerNotificationWorker.schedulePrayerNotifications(this@HaramBlurApplication)
                    Log.i(TAG, "✅ Prayer notification worker retry successful")
                } catch (retryException: Exception) {
                    Log.e(TAG, "❌ Prayer notification worker retry failed", retryException)
                }
            }
        }
    }

    private fun performCleanup() {
        try {
            // Perform cleanup of any resources
            Log.d(TAG, "Performing application cleanup")

            // Note: Most cleanup should be handled by individual components
            // This is mainly for app-level resources

        } catch (e: Exception) {
            Log.e(TAG, "Error during application cleanup", e)
        }
    }

    private fun triggerMemoryCleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Trigger garbage collection
                System.gc()

                Log.d(TAG, "Memory cleanup triggered")
            } catch (e: Exception) {
                Log.e(TAG, "Error during memory cleanup", e)
            }
        }
    }
    
    /**
     * Verify ML libraries are available before any detection services start
     */
    private fun verifyMLCapabilities() {
        try {
            // Check TensorFlow Lite availability
            Class.forName("org.tensorflow.lite.Interpreter")
            Log.d(TAG, "✅ TensorFlow Lite classes available")
            
            // Check ML Kit availability
            Class.forName("com.google.mlkit.vision.face.FaceDetection")
            Log.d(TAG, "✅ ML Kit classes available")
            
            // Attempt to load native libraries
            verifyNativeLibraries()
            
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "❌ ML libraries not available", e)
            // Set global flag for fallback mode - this will be used by detection services
            setFallbackMode(true)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Native ML libraries failed to load", e)
            setFallbackMode(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error verifying ML capabilities", e)
            setFallbackMode(true)
        }
    }
    
    /**
     * Attempt to load native ML libraries
     */
    private fun verifyNativeLibraries() {
        val libraries = listOf(
            "tensorflowlite_jni",
            "tensorflowlite_gpu_jni",
            "face_detector_v2_jni"
        )
        
        libraries.forEach { libName ->
            try {
                System.loadLibrary(libName)
                Log.d(TAG, "✅ Successfully loaded native library: $libName")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "⚠️ Failed to load native library: $libName", e)
                // Don't fail completely - some libraries might be optional
            }
        }
    }
    
    /**
     * Set fallback mode for when ML libraries are not available
     */
    private fun setFallbackMode(enabled: Boolean) {
        // Store in application-level storage for other components to check
        getSharedPreferences("haramblur_ml_status", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("ml_fallback_mode", enabled)
            .apply()
        
        if (enabled) {
            Log.w(TAG, "⚠️ ML fallback mode enabled - using heuristic-only detection")
        }
    }
    
    /**
     * Check if ML fallback mode is enabled
     */
    fun isMLFallbackMode(): Boolean {
        return getSharedPreferences("haramblur_ml_status", Context.MODE_PRIVATE)
            .getBoolean("ml_fallback_mode", false)
    }
    
    /**
     * Get ML capability status for diagnostics
     */
    fun getMLCapabilityStatus(): MLCapabilityStatus {
        return MLCapabilityStatus(
            tensorFlowLiteAvailable = isClassAvailable("org.tensorflow.lite.Interpreter"),
            mlKitAvailable = isClassAvailable("com.google.mlkit.vision.face.FaceDetection"),
            fallbackMode = isMLFallbackMode(),
            deviceInfo = getDeviceInfo()
        )
    }
    
    private fun isClassAvailable(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    private fun getDeviceInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info["androidVersion"] = android.os.Build.VERSION.RELEASE
        info["sdkVersion"] = android.os.Build.VERSION.SDK_INT.toString()
        info["deviceModel"] = android.os.Build.MODEL
        info["deviceManufacturer"] = android.os.Build.MANUFACTURER
        info["abi"] = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        return info
    }
    
    data class MLCapabilityStatus(
        val tensorFlowLiteAvailable: Boolean,
        val mlKitAvailable: Boolean,
        val fallbackMode: Boolean,
        val deviceInfo: Map<String, String>
    )
}