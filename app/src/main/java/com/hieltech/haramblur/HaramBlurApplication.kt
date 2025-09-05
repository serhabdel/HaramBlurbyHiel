package com.hieltech.haramblur

import android.app.Application
import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.services.DhikrManager
import com.hieltech.haramblur.services.DhikrNotificationManager
import com.hieltech.haramblur.utils.LocaleUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HaramBlurApplication : Application() {

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

        // Initialize app-level components here
        initializeComponents()
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
}