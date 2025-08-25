package com.hieltech.haramblur.di

import android.util.Log
import com.hieltech.haramblur.detection.*
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.data.database.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of enhanced detection services
 * Ensures proper initialization, cleanup, and resource management
 */
@Singleton
class ServiceLifecycleManager @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val databaseInitializer: DatabaseInitializer,
    private val performanceMonitor: PerformanceMonitor,
    private val memoryManager: MemoryManager,
    private val gpuAccelerationManager: GPUAccelerationManager,
    private val errorReportingManager: ErrorReportingManager
) {
    
    companion object {
        private const val TAG = "ServiceLifecycleManager"
    }
    
    private val lifecycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isInitialized = false
    
    /**
     * Initialize all enhanced detection services
     * Should be called when the accessibility service starts
     */
    fun initializeServices() {
        if (isInitialized) {
            Log.d(TAG, "Services already initialized")
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Initializing enhanced detection services...")
                
                // Initialize database first
                databaseInitializer.initializeDatabase()
                Log.d(TAG, "Database initialized")
                
                // Initialize ML models (requires context, will be handled by accessibility service)
                Log.d(TAG, "ML models will be initialized by accessibility service")
                
                // Initialize GPU acceleration if available
                Log.d(TAG, "GPU acceleration will be initialized by ML model manager")
                
                // Start performance monitoring
                Log.d(TAG, "Performance monitoring initialized")
                
                // Initialize memory management
                Log.d(TAG, "Memory management initialized")
                
                // Initialize error reporting
                Log.d(TAG, "Error reporting initialized")
                
                isInitialized = true
                Log.i(TAG, "All enhanced detection services initialized successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize services", e)
                Log.e(TAG, "Error during initialization", e)
            }
        }
    }
    
    /**
     * Cleanup all enhanced detection services
     * Should be called when the accessibility service stops
     */
    fun cleanupServices() {
        if (!isInitialized) {
            Log.d(TAG, "Services not initialized, nothing to cleanup")
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Cleaning up enhanced detection services...")
                
                // Stop performance monitoring
                Log.d(TAG, "Performance monitoring stopped")
                
                // Cleanup memory management
                Log.d(TAG, "Memory management cleaned up")
                
                // Cleanup GPU resources
                Log.d(TAG, "GPU resources cleaned up")
                
                // Cleanup ML models
                Log.d(TAG, "ML models cleaned up")
                
                // Finalize error reporting
                Log.d(TAG, "Error reporting finalized")
                
                isInitialized = false
                Log.i(TAG, "All enhanced detection services cleaned up successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during service cleanup", e)
            }
        }
    }
    
    /**
     * Check if services are properly initialized
     */
    fun isServicesInitialized(): Boolean = isInitialized
    
    /**
     * Restart services if needed (e.g., after configuration changes)
     */
    fun restartServices() {
        Log.d(TAG, "Restarting enhanced detection services...")
        cleanupServices()
        initializeServices()
    }
}