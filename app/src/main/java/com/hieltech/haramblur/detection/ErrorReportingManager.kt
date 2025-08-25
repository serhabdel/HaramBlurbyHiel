package com.hieltech.haramblur.detection

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Error reporting and logging system for debugging and improvement
 * Collects error information locally for analysis and debugging
 */
@Singleton
class ErrorReportingManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorReportingManager"
        private const val LOG_FILE_NAME = "haramblur_errors.log"
        private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        private const val MAX_ERROR_REPORTS = 1000
        private const val LOG_CLEANUP_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private val reportingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    // Error tracking
    private val errorReports = ConcurrentLinkedQueue<ErrorReport>()
    private val _reportingMetrics = MutableStateFlow(ReportingMetrics.empty())
    val reportingMetrics: StateFlow<ReportingMetrics> = _reportingMetrics.asStateFlow()
    
    // Log file management
    private val logFile: File by lazy {
        File(context.filesDir, LOG_FILE_NAME)
    }
    
    private var cleanupJob: Job? = null
    
    /**
     * Initialize error reporting system
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing error reporting manager...")
            
            // Ensure log directory exists
            logFile.parentFile?.mkdirs()
            
            // Start periodic cleanup
            startPeriodicCleanup()
            
            Log.d(TAG, "Error reporting manager initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize error reporting manager", e)
            false
        }
    }
    
    /**
     * Report detection error with context
     */
    fun reportDetectionError(
        error: DetectionError,
        context: String,
        operationType: OperationType,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        reportingScope.launch {
            try {
                val errorReport = createErrorReport(error, context, operationType, additionalData)
                
                // Add to in-memory collection
                errorReports.offer(errorReport)
                
                // Maintain size limit
                while (errorReports.size > MAX_ERROR_REPORTS) {
                    errorReports.poll()
                }
                
                // Write to log file
                writeToLogFile(errorReport)
                
                // Update metrics
                updateReportingMetrics()
                
                Log.d(TAG, "Error reported: ${error::class.simpleName} in context: $context")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report error", e)
            }
        }
    }
    
    /**
     * Report performance issue
     */
    fun reportPerformanceIssue(
        operationType: OperationType,
        processingTimeMs: Long,
        targetTimeMs: Long,
        context: String,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        reportingScope.launch {
            try {
                val performanceReport = PerformanceIssueReport(
                    timestamp = System.currentTimeMillis(),
                    operationType = operationType,
                    processingTimeMs = processingTimeMs,
                    targetTimeMs = targetTimeMs,
                    context = context,
                    deviceInfo = getDeviceInfo(),
                    additionalData = additionalData
                )
                
                writePerformanceReportToLog(performanceReport)
                updateReportingMetrics()
                
                Log.d(TAG, "Performance issue reported: $operationType took ${processingTimeMs}ms (target: ${targetTimeMs}ms)")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report performance issue", e)
            }
        }
    }
    
    /**
     * Report memory pressure event
     */
    fun reportMemoryPressure(
        level: MemoryPressureLevel,
        usedMemoryMB: Long,
        maxMemoryMB: Long,
        context: String
    ) {
        reportingScope.launch {
            try {
                val memoryReport = MemoryPressureReport(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    usedMemoryMB = usedMemoryMB,
                    maxMemoryMB = maxMemoryMB,
                    memoryPressure = usedMemoryMB.toFloat() / maxMemoryMB,
                    context = context,
                    deviceInfo = getDeviceInfo()
                )
                
                writeMemoryReportToLog(memoryReport)
                updateReportingMetrics()
                
                Log.d(TAG, "Memory pressure reported: $level (${usedMemoryMB}MB/${maxMemoryMB}MB)")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report memory pressure", e)
            }
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): ErrorStatistics {
        val reports = errorReports.toList()
        val currentTime = System.currentTimeMillis()
        val last24Hours = currentTime - (24 * 60 * 60 * 1000L)
        val lastHour = currentTime - (60 * 60 * 1000L)
        
        val recent24h = reports.filter { it.timestamp >= last24Hours }
        val recentHour = reports.filter { it.timestamp >= lastHour }
        
        val errorsByType = reports.groupBy { it.error::class.simpleName }
            .mapValues { it.value.size }
        
        val errorsByOperation = reports.groupBy { it.operationType }
            .mapValues { it.value.size }
        
        val errorsByContext = reports.groupBy { it.context }
            .mapValues { it.value.size }
        
        return ErrorStatistics(
            totalErrors = reports.size,
            errorsLast24h = recent24h.size,
            errorsLastHour = recentHour.size,
            errorsByType = errorsByType,
            errorsByOperation = errorsByOperation,
            errorsByContext = errorsByContext,
            mostCommonError = errorsByType.maxByOrNull { it.value }?.key,
            mostProblematicOperation = errorsByOperation.maxByOrNull { it.value }?.key,
            logFileSize = if (logFile.exists()) logFile.length() else 0L
        )
    }
    
    /**
     * Export error logs for debugging
     */
    suspend fun exportErrorLogs(): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!logFile.exists()) {
                Log.w(TAG, "No log file exists to export")
                return@withContext null
            }
            
            val exportFile = File(context.getExternalFilesDir(null), "haramblur_error_export_${System.currentTimeMillis()}.log")
            logFile.copyTo(exportFile, overwrite = true)
            
            Log.d(TAG, "Error logs exported to: ${exportFile.absolutePath}")
            exportFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export error logs", e)
            null
        }
    }
    
    /**
     * Clear all error logs
     */
    fun clearErrorLogs() {
        reportingScope.launch {
            try {
                errorReports.clear()
                if (logFile.exists()) {
                    logFile.delete()
                }
                updateReportingMetrics()
                Log.d(TAG, "Error logs cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear error logs", e)
            }
        }
    }
    
    private fun createErrorReport(
        error: DetectionError,
        context: String,
        operationType: OperationType,
        additionalData: Map<String, Any>
    ): ErrorReport {
        return ErrorReport(
            timestamp = System.currentTimeMillis(),
            error = error,
            context = context,
            operationType = operationType,
            deviceInfo = getDeviceInfo(),
            stackTrace = when (error) {
                is DetectionError.UnknownError -> error.throwable.stackTraceToString()
                else -> null
            },
            additionalData = additionalData
        )
    }
    
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE,
            appVersion = "1.0.0", // Would be retrieved from BuildConfig in real app
            totalMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024),
            availableProcessors = Runtime.getRuntime().availableProcessors()
        )
    }
    
    private suspend fun writeToLogFile(errorReport: ErrorReport) = withContext(Dispatchers.IO) {
        try {
            // Check file size and rotate if needed
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile()
            }
            
            val logEntry = formatErrorReportAsJson(errorReport)
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    private suspend fun writePerformanceReportToLog(report: PerformanceIssueReport) = withContext(Dispatchers.IO) {
        try {
            val logEntry = formatPerformanceReportAsJson(report)
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write performance report to log", e)
        }
    }
    
    private suspend fun writeMemoryReportToLog(report: MemoryPressureReport) = withContext(Dispatchers.IO) {
        try {
            val logEntry = formatMemoryReportAsJson(report)
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write memory report to log", e)
        }
    }
    
    private fun formatErrorReportAsJson(report: ErrorReport): String {
        return JSONObject().apply {
            put("type", "error_report")
            put("timestamp", dateFormat.format(Date(report.timestamp)))
            put("error_type", report.error::class.simpleName)
            put("context", report.context)
            put("operation_type", report.operationType.name)
            put("device_info", JSONObject().apply {
                put("manufacturer", report.deviceInfo.manufacturer)
                put("model", report.deviceInfo.model)
                put("android_version", report.deviceInfo.androidVersion)
                put("android_release", report.deviceInfo.androidRelease)
                put("app_version", report.deviceInfo.appVersion)
                put("total_memory_mb", report.deviceInfo.totalMemoryMB)
                put("available_processors", report.deviceInfo.availableProcessors)
            })
            report.stackTrace?.let { put("stack_trace", it) }
            if (report.additionalData.isNotEmpty()) {
                put("additional_data", JSONObject(report.additionalData))
            }
        }.toString()
    }
    
    private fun formatPerformanceReportAsJson(report: PerformanceIssueReport): String {
        return JSONObject().apply {
            put("type", "performance_report")
            put("timestamp", dateFormat.format(Date(report.timestamp)))
            put("operation_type", report.operationType.name)
            put("processing_time_ms", report.processingTimeMs)
            put("target_time_ms", report.targetTimeMs)
            put("context", report.context)
            put("device_info", JSONObject().apply {
                put("manufacturer", report.deviceInfo.manufacturer)
                put("model", report.deviceInfo.model)
                put("android_version", report.deviceInfo.androidVersion)
            })
            if (report.additionalData.isNotEmpty()) {
                put("additional_data", JSONObject(report.additionalData))
            }
        }.toString()
    }
    
    private fun formatMemoryReportAsJson(report: MemoryPressureReport): String {
        return JSONObject().apply {
            put("type", "memory_report")
            put("timestamp", dateFormat.format(Date(report.timestamp)))
            put("level", report.level.name)
            put("used_memory_mb", report.usedMemoryMB)
            put("max_memory_mb", report.maxMemoryMB)
            put("memory_pressure", report.memoryPressure)
            put("context", report.context)
            put("device_info", JSONObject().apply {
                put("manufacturer", report.deviceInfo.manufacturer)
                put("model", report.deviceInfo.model)
                put("android_version", report.deviceInfo.androidVersion)
            })
        }.toString()
    }
    
    private fun rotateLogFile() {
        try {
            val backupFile = File(logFile.parent, "${LOG_FILE_NAME}.backup")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
            Log.d(TAG, "Log file rotated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    private fun startPeriodicCleanup() {
        cleanupJob = reportingScope.launch {
            while (isActive) {
                try {
                    delay(LOG_CLEANUP_INTERVAL_MS)
                    performCleanup()
                } catch (e: Exception) {
                    Log.w(TAG, "Error during periodic cleanup", e)
                }
            }
        }
    }
    
    private fun performCleanup() {
        try {
            // Remove old error reports (older than 7 days)
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val iterator = errorReports.iterator()
            var removedCount = 0
            
            while (iterator.hasNext()) {
                val report = iterator.next()
                if (report.timestamp < cutoffTime) {
                    iterator.remove()
                    removedCount++
                }
            }
            
            if (removedCount > 0) {
                Log.d(TAG, "Cleaned up $removedCount old error reports")
                updateReportingMetrics()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    private fun updateReportingMetrics() {
        val reports = errorReports.toList()
        val currentTime = System.currentTimeMillis()
        val last24Hours = currentTime - (24 * 60 * 60 * 1000L)
        
        val recentReports = reports.filter { it.timestamp >= last24Hours }
        
        _reportingMetrics.value = ReportingMetrics(
            totalReports = reports.size,
            recentReports = recentReports.size,
            logFileSize = if (logFile.exists()) logFile.length() else 0L,
            lastReportTime = reports.maxOfOrNull { it.timestamp } ?: 0L,
            timestamp = currentTime
        )
    }
    
    fun cleanup() {
        cleanupJob?.cancel()
        reportingScope.cancel()
        Log.d(TAG, "Error reporting manager cleaned up")
    }
}

/**
 * Error report data class
 */
private data class ErrorReport(
    val timestamp: Long,
    val error: DetectionError,
    val context: String,
    val operationType: OperationType,
    val deviceInfo: DeviceInfo,
    val stackTrace: String?,
    val additionalData: Map<String, Any>
)

/**
 * Performance issue report
 */
private data class PerformanceIssueReport(
    val timestamp: Long,
    val operationType: OperationType,
    val processingTimeMs: Long,
    val targetTimeMs: Long,
    val context: String,
    val deviceInfo: DeviceInfo,
    val additionalData: Map<String, Any>
)

/**
 * Memory pressure report
 */
private data class MemoryPressureReport(
    val timestamp: Long,
    val level: MemoryPressureLevel,
    val usedMemoryMB: Long,
    val maxMemoryMB: Long,
    val memoryPressure: Float,
    val context: String,
    val deviceInfo: DeviceInfo
)

/**
 * Device information
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val androidRelease: String,
    val appVersion: String,
    val totalMemoryMB: Long,
    val availableProcessors: Int
)

/**
 * Error statistics
 */
data class ErrorStatistics(
    val totalErrors: Int,
    val errorsLast24h: Int,
    val errorsLastHour: Int,
    val errorsByType: Map<String?, Int>,
    val errorsByOperation: Map<OperationType, Int>,
    val errorsByContext: Map<String, Int>,
    val mostCommonError: String?,
    val mostProblematicOperation: OperationType?,
    val logFileSize: Long
)

/**
 * Reporting metrics
 */
data class ReportingMetrics(
    val totalReports: Int,
    val recentReports: Int,
    val logFileSize: Long,
    val lastReportTime: Long,
    val timestamp: Long
) {
    companion object {
        fun empty() = ReportingMetrics(0, 0, 0L, 0L, 0L)
    }
}