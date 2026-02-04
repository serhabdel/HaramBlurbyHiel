package com.hieltech.haramblur.accessibility

import android.util.Log
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.utils.AppConstants.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility class for logging within the accessibility service
 * Handles both Android Log and database logging
 */
class ServiceLogger(
    private val serviceScope: CoroutineScope,
    private val logRepository: LogRepository
) {

    /**
     * Log a message to both Android Log and database
     */
    fun log(
        message: String,
        level: LogRepository.LogLevel = LogRepository.LogLevel.DEBUG,
        category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY
    ) {
        // Always log to Android Log
        when (level) {
            LogRepository.LogLevel.DEBUG -> Log.d(TAG, message)
            LogRepository.LogLevel.INFO -> Log.i(TAG, message)
            LogRepository.LogLevel.WARN -> Log.w(TAG, message)
            LogRepository.LogLevel.ERROR -> Log.e(TAG, message)
        }

        // Log to database asynchronously
        serviceScope.launch {
            try {
                logRepository.log(TAG, message, level, category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log to database: $message", e)
            }
        }
    }

    /**
     * Log debug message
     */
    fun debug(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        log(message, LogRepository.LogLevel.DEBUG, category)
    }

    /**
     * Log info message
     */
    fun info(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        log(message, LogRepository.LogLevel.INFO, category)
    }

    /**
     * Log warning message
     */
    fun warn(message: String, category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY) {
        log(message, LogRepository.LogLevel.WARN, category)
    }

    /**
     * Log error message
     */
    fun error(
        message: String,
        exception: Exception? = null,
        category: LogRepository.LogCategory = LogRepository.LogCategory.ACCESSIBILITY
    ) {
        Log.e(TAG, message, exception)
        
        serviceScope.launch {
            try {
                logRepository.logError(TAG, message, exception, category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log error to database: $message", e)
            }
        }
    }

    companion object {
        private const val TAG = Tags.ACCESSIBILITY_SERVICE
    }
}
