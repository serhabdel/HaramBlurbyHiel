package com.hieltech.haramblur

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class HaramBlurApplication : Application() {

    private val TAG = "HaramBlurApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HaramBlur Application created")

        // Initialize app-level components here
        initializeComponents()
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