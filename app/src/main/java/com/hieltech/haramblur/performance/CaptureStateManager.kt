package com.hieltech.haramblur.performance

import androidx.annotation.VisibleForTesting
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages adaptive capture intervals based on screen activity.
 * Reduces battery usage by slowing down capture when screen is static.
 */
@Singleton
class CaptureStateManager @Inject constructor() {
    
    enum class CaptureState {
        IDLE,       // 5 second intervals - static screen
        NORMAL,     // 2 second intervals - normal activity
        ACTIVE,     // 500ms intervals - frequent changes
        HIGH_ALERT  // 200ms intervals - detected inappropriate content
    }
    
    data class CaptureMetrics(
        val currentState: CaptureState,
        val currentIntervalMs: Long,
        val consecutiveDetections: Int,
        val consecutiveStaticFrames: Int,
        val estimatedBatterySaving: Float
    )
    
    private var currentState: CaptureState = CaptureState.NORMAL
    private var consecutiveDetections = 0
    private var consecutiveStaticFrames = 0
    private var lastStateChangeTime = System.currentTimeMillis()
    
    // Thresholds for state transitions
    private var staticFrameThreshold = 5  // Frames before going to IDLE
    private var detectionThreshold = 2     // Detections before going to HIGH_ALERT
    private var activeChangeThreshold = 3  // Changes before going to ACTIVE
    
    private val captureIntervals = mapOf(
        CaptureState.IDLE to 5000L,
        CaptureState.NORMAL to 2000L,
        CaptureState.ACTIVE to 500L,
        CaptureState.HIGH_ALERT to 200L
    )
    
    /**
     * Get current capture interval in milliseconds
     */
    fun getCurrentInterval(): Long = captureIntervals[currentState] ?: 2000L
    
    /**
     * Get current capture state
     */
    fun getCurrentState(): CaptureState = currentState
    
    /**
     * Analyze frame result and adjust capture state
     */
    fun analyzeFrame(screenChanged: Boolean, detectedInappropriate: Boolean) {
        when {
            detectedInappropriate -> {
                consecutiveDetections++
                consecutiveStaticFrames = 0
                
                if (consecutiveDetections >= detectionThreshold && 
                    currentState != CaptureState.HIGH_ALERT) {
                    transitionTo(CaptureState.HIGH_ALERT)
                }
            }
            screenChanged -> {
                consecutiveStaticFrames = 0
                consecutiveDetections = 0
                
                if (currentState == CaptureState.IDLE || 
                    currentState == CaptureState.HIGH_ALERT) {
                    transitionTo(CaptureState.ACTIVE)
                }
            }
            else -> {
                consecutiveStaticFrames++
                consecutiveDetections = 0
                
                if (consecutiveStaticFrames >= staticFrameThreshold) {
                    when (currentState) {
                        CaptureState.HIGH_ALERT -> transitionTo(CaptureState.ACTIVE)
                        CaptureState.ACTIVE -> transitionTo(CaptureState.NORMAL)
                        CaptureState.NORMAL -> transitionTo(CaptureState.IDLE)
                        CaptureState.IDLE -> { /* Already idle */ }
                    }
                }
            }
        }
    }
    
    /**
     * Force a specific state (for testing or manual override)
     */
    @VisibleForTesting
    fun forceState(state: CaptureState) {
        currentState = state
        lastStateChangeTime = System.currentTimeMillis()
    }
    
    /**
     * Reset state to NORMAL (e.g., when app changes)
     */
    fun reset() {
        currentState = CaptureState.NORMAL
        consecutiveDetections = 0
        consecutiveStaticFrames = 0
        lastStateChangeTime = System.currentTimeMillis()
    }
    
    /**
     * Get current metrics
     */
    fun getMetrics(): CaptureMetrics {
        return CaptureMetrics(
            currentState = currentState,
            currentIntervalMs = getCurrentInterval(),
            consecutiveDetections = consecutiveDetections,
            consecutiveStaticFrames = consecutiveStaticFrames,
            estimatedBatterySaving = calculateBatterySaving()
        )
    }
    
    private fun transitionTo(newState: CaptureState) {
        if (newState != currentState) {
            currentState = newState
            lastStateChangeTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Calculate estimated battery saving vs fixed 1-second interval
     */
    private fun calculateBatterySaving(): Float {
        val currentInterval = getCurrentInterval()
        val baselineInterval = 1000L // Compare to 1 second baseline
        
        return if (currentInterval > baselineInterval) {
            ((currentInterval - baselineInterval).toFloat() / baselineInterval) * 100f
        } else {
            -((baselineInterval - currentInterval).toFloat() / baselineInterval) * 100f
        }
    }
    
    companion object {
        // Configurable thresholds
        const val DEFAULT_STATIC_THRESHOLD = 5
        const val DEFAULT_DETECTION_THRESHOLD = 2
        const val DEFAULT_CHANGE_THRESHOLD = 3
    }
}
