package com.hieltech.haramblur.detection

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive error recovery system for the blocking functionality
 * Handles failures gracefully and implements retry mechanisms
 */
@Singleton
class ComprehensiveErrorRecovery @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorRecovery"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val CIRCUIT_BREAKER_THRESHOLD = 5
        private const val CIRCUIT_BREAKER_TIMEOUT_MS = 30000L
        private const val ERROR_RATE_WINDOW_MS = 60000L
    }
    
    // Error tracking
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val lastErrorTimes = ConcurrentHashMap<String, Long>()
    private val circuitBreakerStates = ConcurrentHashMap<String, CircuitBreakerState>()
    
    // Recovery state
    private val _recoveryState = MutableStateFlow(RecoveryState.HEALTHY)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState
    
    // Error statistics
    private val errorStats = ConcurrentHashMap<String, ErrorStatistics>()
    
    enum class RecoveryState {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        RECOVERING
    }
    
    enum class CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast
        HALF_OPEN  // Testing recovery
    }
    
    data class ErrorStatistics(
        val totalErrors: Int,
        val errorRate: Float,
        val lastErrorTime: Long,
        val recoveryAttempts: Int
    )
    
    /**
     * Execute operation with comprehensive error recovery
     */
    suspend fun <T> executeWithRecovery(
        operationName: String,
        operation: suspend () -> T,
        fallback: (suspend () -> T)? = null,
        retryPolicy: RetryPolicy = RetryPolicy.DEFAULT
    ): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                // Check circuit breaker
                if (isCircuitBreakerOpen(operationName)) {
                    Log.w(TAG, "Circuit breaker open for $operationName, using fallback")
                    return@withContext if (fallback != null) {
                        try {
                            Result.success(fallback())
                        } catch (e: Exception) {
                            Result.failure(CircuitBreakerException("Circuit breaker open and fallback failed", e))
                        }
                    } else {
                        Result.failure(CircuitBreakerException("Circuit breaker open for $operationName"))
                    }
                }
                
                // Execute with retry logic
                var lastException: Exception? = null
                repeat(retryPolicy.maxAttempts) { attempt ->
                    try {
                        val result = operation()
                        
                        // Success - reset error tracking
                        resetErrorTracking(operationName)
                        updateRecoveryState()
                        
                        return@withContext Result.success(result)
                        
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Attempt ${attempt + 1} failed for $operationName", e)
                        
                        // Track error
                        trackError(operationName, e)
                        
                        // Check if we should retry
                        if (attempt < retryPolicy.maxAttempts - 1 && shouldRetry(e, retryPolicy)) {
                            delay(calculateRetryDelay(attempt, retryPolicy))
                        }
                    }
                }
                
                // All retries failed
                updateCircuitBreaker(operationName)
                updateRecoveryState()
                
                // Try fallback if available
                if (fallback != null) {
                    try {
                        Log.i(TAG, "Using fallback for $operationName after all retries failed")
                        val fallbackResult = fallback()
                        return@withContext Result.success(fallbackResult)
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback also failed for $operationName", e)
                    }
                }
                
                Result.failure(lastException ?: Exception("Unknown error in $operationName"))
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in error recovery for $operationName", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Execute blocking operation with specific error handling
     */
    suspend fun executeBlockingOperation(
        url: String,
        operation: suspend () -> SiteBlockingResult
    ): SiteBlockingResult {
        val result = executeWithRecovery(
            operationName = "blocking_check",
            operation = operation,
            fallback = {
                // Safe fallback - allow access but log the failure
                Log.w(TAG, "Blocking check failed for $url, allowing access as fallback")
                SiteBlockingResult(
                    isBlocked = false,
                    category = null,
                    confidence = 0.0f,
                    quranicVerse = null,
                    reflectionTimeSeconds = 0,
                    blockingReason = "Error recovery fallback - check failed"
                )
            }
        )
        
        return result.getOrElse { exception ->
            Log.e(TAG, "Critical blocking failure for $url", exception)
            // In case of critical failure, err on the side of caution
            SiteBlockingResult(
                isBlocked = true,
                category = BlockingCategory.SUSPICIOUS_CONTENT,
                confidence = 0.5f,
                quranicVerse = null,
                reflectionTimeSeconds = 10,
                blockingReason = "Blocked due to system error (safety measure)"
            )
        }
    }
    
    /**
     * Execute URL extraction with error recovery
     */
    suspend fun executeUrlExtraction(
        packageName: String?,
        operation: suspend () -> String?
    ): String? {
        val result = executeWithRecovery(
            operationName = "url_extraction",
            operation = operation,
            fallback = {
                Log.w(TAG, "URL extraction failed for $packageName, returning null")
                null
            }
        )
        
        return result.getOrNull()
    }
    
    /**
     * Track error occurrence
     */
    private fun trackError(operationName: String, exception: Exception) {
        val currentTime = System.currentTimeMillis()
        
        // Update error count
        val errorCount = errorCounts.computeIfAbsent(operationName) { AtomicInteger(0) }
        errorCount.incrementAndGet()
        
        // Update last error time
        lastErrorTimes[operationName] = currentTime
        
        // Update error statistics
        updateErrorStatistics(operationName, exception)
        
        Log.d(TAG, "Error tracked for $operationName: ${exception.javaClass.simpleName}")
    }
    
    /**
     * Reset error tracking for successful operations
     */
    private fun resetErrorTracking(operationName: String) {
        errorCounts[operationName]?.set(0)
        circuitBreakerStates[operationName] = CircuitBreakerState.CLOSED
        Log.v(TAG, "Error tracking reset for $operationName")
    }
    
    /**
     * Check if circuit breaker is open
     */
    private fun isCircuitBreakerOpen(operationName: String): Boolean {
        val state = circuitBreakerStates[operationName] ?: CircuitBreakerState.CLOSED
        val lastErrorTime = lastErrorTimes[operationName] ?: 0L
        val currentTime = System.currentTimeMillis()
        
        return when (state) {
            CircuitBreakerState.CLOSED -> false
            CircuitBreakerState.OPEN -> {
                // Check if timeout has passed
                if (currentTime - lastErrorTime > CIRCUIT_BREAKER_TIMEOUT_MS) {
                    circuitBreakerStates[operationName] = CircuitBreakerState.HALF_OPEN
                    Log.i(TAG, "Circuit breaker transitioning to HALF_OPEN for $operationName")
                    false
                } else {
                    true
                }
            }
            CircuitBreakerState.HALF_OPEN -> false
        }
    }
    
    /**
     * Update circuit breaker state based on error count
     */
    private fun updateCircuitBreaker(operationName: String) {
        val errorCount = errorCounts[operationName]?.get() ?: 0
        
        if (errorCount >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerStates[operationName] = CircuitBreakerState.OPEN
            Log.w(TAG, "Circuit breaker opened for $operationName (errors: $errorCount)")
        }
    }
    
    /**
     * Update overall recovery state
     */
    private fun updateRecoveryState() {
        val totalErrors = errorCounts.values.sumOf { it.get() }
        val openCircuitBreakers = circuitBreakerStates.values.count { it == CircuitBreakerState.OPEN }
        
        val newState = when {
            totalErrors == 0 && openCircuitBreakers == 0 -> RecoveryState.HEALTHY
            totalErrors < 10 && openCircuitBreakers < 2 -> RecoveryState.DEGRADED
            totalErrors < 20 && openCircuitBreakers < 5 -> RecoveryState.CRITICAL
            else -> RecoveryState.RECOVERING
        }
        
        if (_recoveryState.value != newState) {
            _recoveryState.value = newState
            Log.i(TAG, "Recovery state changed to $newState (errors: $totalErrors, open circuits: $openCircuitBreakers)")
        }
    }
    
    /**
     * Update error statistics
     */
    private fun updateErrorStatistics(operationName: String, exception: Exception) {
        val currentTime = System.currentTimeMillis()
        val currentStats = errorStats[operationName] ?: ErrorStatistics(0, 0.0f, 0L, 0)
        
        val newStats = currentStats.copy(
            totalErrors = currentStats.totalErrors + 1,
            errorRate = calculateErrorRate(operationName),
            lastErrorTime = currentTime,
            recoveryAttempts = if (exception is RecoveryException) currentStats.recoveryAttempts + 1 else currentStats.recoveryAttempts
        )
        
        errorStats[operationName] = newStats
    }
    
    /**
     * Calculate error rate for an operation
     */
    private fun calculateErrorRate(operationName: String): Float {
        val errorCount = errorCounts[operationName]?.get() ?: 0
        val lastErrorTime = lastErrorTimes[operationName] ?: 0L
        val currentTime = System.currentTimeMillis()
        
        return if (currentTime - lastErrorTime < ERROR_RATE_WINDOW_MS) {
            errorCount.toFloat() / (ERROR_RATE_WINDOW_MS / 1000f) // errors per second
        } else {
            0.0f
        }
    }
    
    /**
     * Determine if operation should be retried
     */
    private fun shouldRetry(exception: Exception, retryPolicy: RetryPolicy): Boolean {
        return when (exception) {
            is CircuitBreakerException -> false
            is SecurityException -> false
            is IllegalArgumentException -> false
            else -> retryPolicy.retryableExceptions.any { it.isInstance(exception) }
        }
    }
    
    /**
     * Calculate retry delay with exponential backoff
     */
    private fun calculateRetryDelay(attempt: Int, retryPolicy: RetryPolicy): Long {
        return when (retryPolicy.backoffStrategy) {
            BackoffStrategy.FIXED -> retryPolicy.baseDelayMs
            BackoffStrategy.LINEAR -> retryPolicy.baseDelayMs * (attempt + 1)
            BackoffStrategy.EXPONENTIAL -> retryPolicy.baseDelayMs * (1L shl attempt)
        }
    }
    
    /**
     * Get current error statistics
     */
    fun getErrorStatistics(): Map<String, ErrorStatistics> {
        return errorStats.toMap()
    }
    
    /**
     * Get current recovery state
     */
    fun getCurrentRecoveryState(): RecoveryState {
        return _recoveryState.value
    }
    
    /**
     * Clear all error tracking (for testing or manual recovery)
     */
    fun clearErrorTracking() {
        errorCounts.clear()
        lastErrorTimes.clear()
        circuitBreakerStates.clear()
        errorStats.clear()
        _recoveryState.value = RecoveryState.HEALTHY
        Log.i(TAG, "All error tracking cleared")
    }
}

/**
 * Retry policy configuration
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 1000L,
    val backoffStrategy: BackoffStrategy = BackoffStrategy.EXPONENTIAL,
    val retryableExceptions: List<Class<out Exception>> = listOf(
        RuntimeException::class.java,
        java.io.IOException::class.java,
        java.sql.SQLException::class.java
    )
) {
    companion object {
        val DEFAULT = RetryPolicy()
        val AGGRESSIVE = RetryPolicy(maxAttempts = 5, baseDelayMs = 500L)
        val CONSERVATIVE = RetryPolicy(maxAttempts = 2, baseDelayMs = 2000L)
    }
}

/**
 * Backoff strategies for retry delays
 */
enum class BackoffStrategy {
    FIXED,
    LINEAR,
    EXPONENTIAL
}

/**
 * Custom exceptions for error recovery
 */
class CircuitBreakerException(message: String, cause: Throwable? = null) : Exception(message, cause)
class RecoveryException(message: String, cause: Throwable? = null) : Exception(message, cause)
