package com.hieltech.haramblur.detection

/**
 * Error types for enhanced detection system with recovery strategies
 */
sealed class DetectionError {
    object ModelNotLoaded : DetectionError()
    object ProcessingTimeout : DetectionError()
    object InsufficientMemory : DetectionError()
    data class NetworkError(val message: String) : DetectionError()
    data class DatabaseError(val message: String) : DetectionError()
    data class GenderDetectionError(val message: String) : DetectionError()
    data class SiteBlockingError(val message: String) : DetectionError()
    data class UnknownError(val throwable: Throwable) : DetectionError()
}

/**
 * Recovery actions for different error types
 */
enum class RecoveryAction {
    FALLBACK_TO_HEURISTICS,
    REDUCE_QUALITY,
    CLEAR_CACHE,
    USE_OFFLINE_MODE,
    RESTART_SERVICE,
    SKIP_DETECTION,
    USE_DEFAULT_SETTINGS
}

/**
 * Error recovery manager for handling detection failures
 */
interface ErrorRecoveryManager {
    
    /**
     * Handles detection errors and returns appropriate recovery action
     */
    suspend fun handleDetectionError(error: DetectionError): RecoveryAction
    
    /**
     * Implements graceful degradation strategy
     */
    suspend fun implementGracefulDegradation(error: DetectionError): Boolean
    
    /**
     * Reports error for debugging and improvement
     */
    fun reportError(error: DetectionError, context: String)
}

/**
 * Detection exception for enhanced error handling
 */
class DetectionException(
    val detectionError: DetectionError,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)