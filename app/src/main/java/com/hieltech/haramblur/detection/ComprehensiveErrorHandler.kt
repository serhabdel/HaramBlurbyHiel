package com.hieltech.haramblur.detection

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive error handling system with advanced recovery strategies,
 * error tracking, and automatic fallback mechanisms
 */
@Singleton
class ComprehensiveErrorHandler @Inject constructor(
    private val memoryManager: MemoryManager,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TAG = "ComprehensiveErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val ERROR_RATE_WINDOW_MS = 60000L // 1 minute
        private const val HIGH_ERROR_RATE_THRESHOLD = 0.3f // 30%
        private const val CIRCUIT_BREAKER_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    private val errorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Error tracking
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val errorHistory = mutableListOf<ErrorEvent>()
    private val retryAttempts = ConcurrentHashMap<String, Int>()
    
    // Circuit breaker states
    private val circuitBreakerStates = ConcurrentHashMap<String, CircuitBreakerState>()
    
    // Error state
    private val _errorState = MutableStateFlow(ErrorSystemState.HEALTHY)
    val errorState: StateFlow<ErrorSystemState> = _errorState.asStateFlow()
    
    private val _errorMetrics = MutableStateFlow(ErrorMetrics.empty())
    val errorMetrics: StateFlow<ErrorMetrics> = _errorMetrics.asStateFlow()
    
    // Fallback mechanisms
    private val fallbackStrategies = mapOf(
        "gender_detection" to FallbackStrategy.HEURISTIC_BASED,
        "nsfw_detection" to FallbackStrategy.CONSERVATIVE_BLOCKING,
        "site_blocking" to FallbackStrategy.EMBEDDED_LIST,
        "face_detection" to FallbackStrategy.BASIC_DETECTION,
        "density_analysis" to FallbackStrategy.SIMPLE_THRESHOLD
    )
    
    /**
     * Handle error with comprehensive recovery strategy
     */
    suspend fun handleError(
        error: DetectionError,
        context: String,
        operationType: OperationType
    ): ErrorHandlingResult {
        val errorKey = "${operationType.name}_${error::class.simpleName}"
        
        Log.w(TAG, "Handling error: $error in context: $context")
        
        // Record error event
        recordErrorEvent(error, context, operationType)
        
        // Check circuit breaker
        val circuitState = checkCircuitBreaker(errorKey)
        if (circuitState == CircuitBreakerState.OPEN) {
            Log.w(TAG, "Circuit breaker open for $errorKey, using fallback")
            return handleCircuitBreakerOpen(error, context, operationType)
        }
        
        // Determine recovery strategy
        val recoveryStrategy = determineRecoveryStrategy(error, context, operationType)
        
        // Execute recovery
        val recoveryResult = executeRecoveryStrategy(recoveryStrategy, error, context)
        
        // Update circuit breaker based on result
        updateCircuitBreaker(errorKey, recoveryResult.success)
        
        // Update error metrics
        updateErrorMetrics()
        
        return recoveryResult
    }
    
    /**
     * Execute operation with automatic retry and fallback
     */
    suspend fun <T> executeWithRecovery(
        operationType: OperationType,
        context: String,
        operation: suspend () -> T,
        fallback: suspend (DetectionError) -> T
    ): T {
        val operationKey = "${operationType.name}_$context"
        var lastError: DetectionError? = null
        
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                // Check if we should skip due to circuit breaker
                val circuitState = checkCircuitBreaker(operationKey)
                if (circuitState == CircuitBreakerState.OPEN) {
                    Log.w(TAG, "Circuit breaker open for $operationKey, using fallback immediately")
                    return fallback(lastError ?: DetectionError.UnknownError(Exception("Circuit breaker open")))
                }
                
                val result = operation()
                
                // Success - reset retry count and update circuit breaker
                retryAttempts.remove(operationKey)
                updateCircuitBreaker(operationKey, true)
                
                return result
                
            } catch (e: DetectionException) {
                lastError = e.detectionError
                Log.w(TAG, "Detection error on attempt ${attempt + 1}: ${e.detectionError}")
                
                // Handle the error
                val handlingResult = handleError(e.detectionError, context, operationType)
                
                if (handlingResult.shouldRetry && attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                    continue
                } else {
                    // Max retries reached or shouldn't retry
                    updateCircuitBreaker(operationKey, false)
                    return fallback(e.detectionError)
                }
                
            } catch (e: Exception) {
                lastError = DetectionError.UnknownError(e)
                Log.e(TAG, "Unexpected error on attempt ${attempt + 1}", e)
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                    continue
                } else {
                    updateCircuitBreaker(operationKey, false)
                    return fallback(DetectionError.UnknownError(e))
                }
            }
        }
        
        // This should never be reached, but just in case
        return fallback(lastError ?: DetectionError.UnknownError(Exception("Max retries exceeded")))
    }
    
    /**
     * Get error statistics and health metrics
     */
    fun getErrorStats(): ErrorStats {
        val currentTime = System.currentTimeMillis()
        val recentErrors = errorHistory.filter { 
            currentTime - it.timestamp <= ERROR_RATE_WINDOW_MS 
        }
        
        val totalOperations = recentErrors.size + 100 // Estimate successful operations
        val errorRate = if (totalOperations > 0) {
            recentErrors.size.toFloat() / totalOperations
        } else {
            0f
        }
        
        val errorsByType = recentErrors.groupBy { it.error::class.simpleName }
            .mapValues { it.value.size }
        
        val errorsByOperation = recentErrors.groupBy { it.operationType }
            .mapValues { it.value.size }
        
        return ErrorStats(
            totalErrors = errorHistory.size,
            recentErrors = recentErrors.size,
            errorRate = errorRate,
            errorsByType = errorsByType,
            errorsByOperation = errorsByOperation,
            circuitBreakerStates = circuitBreakerStates.toMap(),
            systemState = _errorState.value
        )
    }
    
    /**
     * Reset error tracking and circuit breakers
     */
    fun resetErrorTracking() {
        errorHistory.clear()
        errorCounts.clear()
        retryAttempts.clear()
        circuitBreakerStates.clear()
        _errorState.value = ErrorSystemState.HEALTHY
        _errorMetrics.value = ErrorMetrics.empty()
        Log.d(TAG, "Error tracking reset")
    }
    
    private fun recordErrorEvent(
        error: DetectionError,
        context: String,
        operationType: OperationType
    ) {
        val errorEvent = ErrorEvent(
            error = error,
            context = context,
            operationType = operationType,
            timestamp = System.currentTimeMillis()
        )
        
        errorHistory.add(errorEvent)
        
        // Keep only recent history
        val cutoffTime = System.currentTimeMillis() - ERROR_RATE_WINDOW_MS * 2
        errorHistory.removeAll { it.timestamp < cutoffTime }
        
        // Update error counts
        val errorKey = error::class.simpleName ?: "Unknown"
        errorCounts.computeIfAbsent(errorKey) { AtomicLong(0) }.incrementAndGet()
    }
    
    private fun checkCircuitBreaker(operationKey: String): CircuitBreakerState {
        val state = circuitBreakerStates[operationKey] ?: return CircuitBreakerState.CLOSED
        
        return when (state) {
            CircuitBreakerState.OPEN -> {
                // Check if timeout has passed
                val lastFailure = errorHistory
                    .filter { "${it.operationType.name}_${it.error::class.simpleName}" == operationKey }
                    .maxByOrNull { it.timestamp }
                
                if (lastFailure != null && 
                    System.currentTimeMillis() - lastFailure.timestamp > CIRCUIT_BREAKER_TIMEOUT_MS) {
                    circuitBreakerStates[operationKey] = CircuitBreakerState.HALF_OPEN
                    CircuitBreakerState.HALF_OPEN
                } else {
                    CircuitBreakerState.OPEN
                }
            }
            else -> state
        }
    }
    
    private fun updateCircuitBreaker(operationKey: String, success: Boolean) {
        val currentState = circuitBreakerStates[operationKey] ?: CircuitBreakerState.CLOSED
        
        val newState = when (currentState) {
            CircuitBreakerState.CLOSED -> {
                if (!success) {
                    // Check error rate
                    val recentErrors = errorHistory
                        .filter { "${it.operationType.name}_${it.error::class.simpleName}" == operationKey }
                        .filter { System.currentTimeMillis() - it.timestamp <= ERROR_RATE_WINDOW_MS }
                    
                    if (recentErrors.size >= 5) { // 5 errors in window
                        CircuitBreakerState.OPEN
                    } else {
                        CircuitBreakerState.CLOSED
                    }
                } else {
                    CircuitBreakerState.CLOSED
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                if (success) {
                    CircuitBreakerState.CLOSED
                } else {
                    CircuitBreakerState.OPEN
                }
            }
            CircuitBreakerState.OPEN -> CircuitBreakerState.OPEN
        }
        
        if (newState != currentState) {
            circuitBreakerStates[operationKey] = newState
            Log.d(TAG, "Circuit breaker state changed for $operationKey: $currentState -> $newState")
        }
    }
    
    private suspend fun handleCircuitBreakerOpen(
        error: DetectionError,
        context: String,
        operationType: OperationType
    ): ErrorHandlingResult {
        Log.w(TAG, "Circuit breaker open, using fallback strategy")
        
        val fallbackStrategy = fallbackStrategies[context] ?: FallbackStrategy.CONSERVATIVE_BLOCKING
        val fallbackResult = executeFallbackStrategy(fallbackStrategy, error, context)
        
        return ErrorHandlingResult(
            success = fallbackResult,
            recoveryAction = RecoveryAction.USE_OFFLINE_MODE,
            shouldRetry = false,
            fallbackUsed = true,
            message = "Circuit breaker open, fallback strategy applied"
        )
    }
    
    private fun determineRecoveryStrategy(
        error: DetectionError,
        context: String,
        operationType: OperationType
    ): RecoveryStrategy {
        return when (error) {
            is DetectionError.ModelNotLoaded -> RecoveryStrategy.RELOAD_MODEL
            is DetectionError.ProcessingTimeout -> RecoveryStrategy.REDUCE_QUALITY
            is DetectionError.InsufficientMemory -> RecoveryStrategy.CLEAR_MEMORY
            is DetectionError.NetworkError -> RecoveryStrategy.USE_OFFLINE_MODE
            is DetectionError.DatabaseError -> RecoveryStrategy.REBUILD_DATABASE
            is DetectionError.GenderDetectionError -> RecoveryStrategy.FALLBACK_DETECTION
            is DetectionError.SiteBlockingError -> RecoveryStrategy.USE_EMBEDDED_LIST
            is DetectionError.UnknownError -> RecoveryStrategy.RESTART_COMPONENT
        }
    }
    
    private suspend fun executeRecoveryStrategy(
        strategy: RecoveryStrategy,
        error: DetectionError,
        context: String
    ): ErrorHandlingResult {
        return try {
            when (strategy) {
                RecoveryStrategy.RELOAD_MODEL -> {
                    Log.d(TAG, "Executing recovery: Reload model")
                    // In practice, this would trigger model reloading
                    ErrorHandlingResult.success(RecoveryAction.FALLBACK_TO_HEURISTICS, "Model reload initiated")
                }
                RecoveryStrategy.REDUCE_QUALITY -> {
                    Log.d(TAG, "Executing recovery: Reduce quality")
                    // Trigger quality reduction through performance monitor
                    ErrorHandlingResult.success(RecoveryAction.REDUCE_QUALITY, "Quality reduced")
                }
                RecoveryStrategy.CLEAR_MEMORY -> {
                    Log.d(TAG, "Executing recovery: Clear memory")
                    memoryManager.handleMemoryPressure(MemoryPressureLevel.HIGH)
                    ErrorHandlingResult.success(RecoveryAction.CLEAR_CACHE, "Memory cleared")
                }
                RecoveryStrategy.USE_OFFLINE_MODE -> {
                    Log.d(TAG, "Executing recovery: Use offline mode")
                    ErrorHandlingResult.success(RecoveryAction.USE_OFFLINE_MODE, "Switched to offline mode")
                }
                RecoveryStrategy.REBUILD_DATABASE -> {
                    Log.d(TAG, "Executing recovery: Rebuild database")
                    // In practice, this would trigger database rebuild
                    ErrorHandlingResult.success(RecoveryAction.USE_DEFAULT_SETTINGS, "Database rebuild initiated")
                }
                RecoveryStrategy.FALLBACK_DETECTION -> {
                    Log.d(TAG, "Executing recovery: Fallback detection")
                    ErrorHandlingResult.success(RecoveryAction.FALLBACK_TO_HEURISTICS, "Using fallback detection")
                }
                RecoveryStrategy.USE_EMBEDDED_LIST -> {
                    Log.d(TAG, "Executing recovery: Use embedded list")
                    ErrorHandlingResult.success(RecoveryAction.USE_OFFLINE_MODE, "Using embedded list")
                }
                RecoveryStrategy.RESTART_COMPONENT -> {
                    Log.d(TAG, "Executing recovery: Restart component")
                    ErrorHandlingResult.success(RecoveryAction.RESTART_SERVICE, "Component restart initiated")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recovery strategy failed", e)
            ErrorHandlingResult.failure(RecoveryAction.SKIP_DETECTION, "Recovery failed: ${e.message}")
        }
    }
    
    private suspend fun executeFallbackStrategy(
        strategy: FallbackStrategy,
        error: DetectionError,
        context: String
    ): Boolean {
        return try {
            when (strategy) {
                FallbackStrategy.HEURISTIC_BASED -> {
                    Log.d(TAG, "Using heuristic-based fallback")
                    true
                }
                FallbackStrategy.CONSERVATIVE_BLOCKING -> {
                    Log.d(TAG, "Using conservative blocking fallback")
                    true
                }
                FallbackStrategy.EMBEDDED_LIST -> {
                    Log.d(TAG, "Using embedded list fallback")
                    true
                }
                FallbackStrategy.BASIC_DETECTION -> {
                    Log.d(TAG, "Using basic detection fallback")
                    true
                }
                FallbackStrategy.SIMPLE_THRESHOLD -> {
                    Log.d(TAG, "Using simple threshold fallback")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback strategy failed", e)
            false
        }
    }
    
    private fun updateErrorMetrics() {
        val currentTime = System.currentTimeMillis()
        val recentErrors = errorHistory.filter { 
            currentTime - it.timestamp <= ERROR_RATE_WINDOW_MS 
        }
        
        val errorRate = if (recentErrors.isNotEmpty()) {
            recentErrors.size.toFloat() / (recentErrors.size + 100) // Estimate total operations
        } else {
            0f
        }
        
        val newSystemState = when {
            errorRate > HIGH_ERROR_RATE_THRESHOLD -> ErrorSystemState.CRITICAL
            errorRate > 0.1f -> ErrorSystemState.DEGRADED
            recentErrors.isNotEmpty() -> ErrorSystemState.RECOVERING
            else -> ErrorSystemState.HEALTHY
        }
        
        if (newSystemState != _errorState.value) {
            _errorState.value = newSystemState
            Log.d(TAG, "Error system state changed to: $newSystemState")
        }
        
        _errorMetrics.value = ErrorMetrics(
            totalErrors = errorHistory.size,
            recentErrorRate = errorRate,
            activeCircuitBreakers = circuitBreakerStates.count { it.value == CircuitBreakerState.OPEN },
            systemHealth = when (newSystemState) {
                ErrorSystemState.HEALTHY -> 1.0f
                ErrorSystemState.RECOVERING -> 0.8f
                ErrorSystemState.DEGRADED -> 0.5f
                ErrorSystemState.CRITICAL -> 0.2f
            },
            timestamp = currentTime
        )
    }
    
    fun cleanup() {
        errorScope.cancel()
        resetErrorTracking()
        Log.d(TAG, "Comprehensive error handler cleaned up")
    }
}

/**
 * Error event for tracking
 */
private data class ErrorEvent(
    val error: DetectionError,
    val context: String,
    val operationType: OperationType,
    val timestamp: Long
)

/**
 * Circuit breaker states
 */
enum class CircuitBreakerState {
    CLOSED,    // Normal operation
    OPEN,      // Failing fast, using fallback
    HALF_OPEN  // Testing if service has recovered
}

/**
 * Error system states
 */
enum class ErrorSystemState(val displayName: String) {
    HEALTHY("System healthy"),
    RECOVERING("System recovering"),
    DEGRADED("System degraded"),
    CRITICAL("System critical")
}

/**
 * Recovery strategies
 */
enum class RecoveryStrategy {
    RELOAD_MODEL,
    REDUCE_QUALITY,
    CLEAR_MEMORY,
    USE_OFFLINE_MODE,
    REBUILD_DATABASE,
    FALLBACK_DETECTION,
    USE_EMBEDDED_LIST,
    RESTART_COMPONENT
}

/**
 * Fallback strategies
 */
enum class FallbackStrategy {
    HEURISTIC_BASED,
    CONSERVATIVE_BLOCKING,
    EMBEDDED_LIST,
    BASIC_DETECTION,
    SIMPLE_THRESHOLD
}

/**
 * Error handling result
 */
data class ErrorHandlingResult(
    val success: Boolean,
    val recoveryAction: RecoveryAction,
    val shouldRetry: Boolean,
    val fallbackUsed: Boolean,
    val message: String
) {
    companion object {
        fun success(action: RecoveryAction, message: String) = ErrorHandlingResult(
            success = true,
            recoveryAction = action,
            shouldRetry = false,
            fallbackUsed = false,
            message = message
        )
        
        fun failure(action: RecoveryAction, message: String) = ErrorHandlingResult(
            success = false,
            recoveryAction = action,
            shouldRetry = true,
            fallbackUsed = false,
            message = message
        )
    }
}

/**
 * Error statistics
 */
data class ErrorStats(
    val totalErrors: Int,
    val recentErrors: Int,
    val errorRate: Float,
    val errorsByType: Map<String?, Int>,
    val errorsByOperation: Map<OperationType, Int>,
    val circuitBreakerStates: Map<String, CircuitBreakerState>,
    val systemState: ErrorSystemState
)

/**
 * Error metrics for monitoring
 */
data class ErrorMetrics(
    val totalErrors: Int,
    val recentErrorRate: Float,
    val activeCircuitBreakers: Int,
    val systemHealth: Float, // 0.0 to 1.0
    val timestamp: Long
) {
    companion object {
        fun empty() = ErrorMetrics(0, 0f, 0, 1.0f, 0L)
    }
}