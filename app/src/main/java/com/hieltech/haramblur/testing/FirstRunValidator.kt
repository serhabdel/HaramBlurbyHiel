package com.hieltech.haramblur.testing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.SettingsRepository
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive first-run validation system to ensure perfect performance
 * on fresh installs and specific apps on first-time usage
 */
@Singleton
class FirstRunValidator @Inject constructor(
    private val contentDetectionEngine: ContentDetectionEngine,
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private const val TAG = "FirstRunValidator"
        private const val VALIDATION_TIMEOUT_MS = 30000L // 30 seconds max
        private const val PERFORMANCE_TARGET_MS = 200L // Target processing time
        private const val MIN_ACCURACY_THRESHOLD = 0.85f // 85% minimum accuracy
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val performanceScore: Float, // 0.0 - 1.0
        val accuracyScore: Float,    // 0.0 - 1.0
        val issues: List<ValidationIssue>,
        val recommendations: List<String>,
        val processingTimeMs: Long,
        val memoryUsageMB: Float
    )
    
    data class ValidationIssue(
        val severity: IssueSeverity,
        val component: String,
        val description: String,
        val solution: String
    )
    
    enum class IssueSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
    
    /**
     * Run comprehensive first-run validation
     */
    suspend fun validateFirstRun(context: Context): ValidationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.i(TAG, "🚀 Starting comprehensive first-run validation...")
        
        try {
            withTimeout(VALIDATION_TIMEOUT_MS) {
                
                // 1. Validate core component initialization
                val initResults = validateComponentInitialization(context)
                issues.addAll(initResults.issues)
                recommendations.addAll(initResults.recommendations)
                
                // 2. Validate AI model performance
                val aiResults = validateAIModelPerformance(context)
                issues.addAll(aiResults.issues)
                recommendations.addAll(aiResults.recommendations)
                
                // 3. Validate memory and performance
                val perfResults = validatePerformanceMetrics(context)
                issues.addAll(perfResults.issues)
                recommendations.addAll(perfResults.recommendations)
                
                // 4. Validate specific app scenarios
                val appResults = validateSpecificAppScenarios(context)
                issues.addAll(appResults.issues)
                recommendations.addAll(appResults.recommendations)
                
                // 5. Validate edge cases
                val edgeResults = validateEdgeCases(context)
                issues.addAll(edgeResults.issues)
                recommendations.addAll(edgeResults.recommendations)
                
                val totalTime = System.currentTimeMillis() - startTime
                val memoryUsage = getMemoryUsageMB()
                
                // Calculate scores
                val performanceScore = calculatePerformanceScore(totalTime, memoryUsage)
                val accuracyScore = calculateAccuracyScore(issues)
                val isValid = issues.none { it.severity == IssueSeverity.CRITICAL }
                
                Log.i(TAG, "✅ First-run validation completed in ${totalTime}ms")
                Log.i(TAG, "📊 Performance Score: $performanceScore, Accuracy Score: $accuracyScore")
                Log.i(TAG, "🔍 Found ${issues.size} issues: ${issues.count { it.severity == IssueSeverity.CRITICAL }} critical")
                
                return@withTimeout ValidationResult(
                    isValid = isValid,
                    performanceScore = performanceScore,
                    accuracyScore = accuracyScore,
                    issues = issues,
                    recommendations = recommendations,
                    processingTimeMs = totalTime,
                    memoryUsageMB = memoryUsage
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "❌ First-run validation timed out after ${VALIDATION_TIMEOUT_MS}ms")
            issues.add(ValidationIssue(
                IssueSeverity.CRITICAL,
                "Validation",
                "First-run validation timed out",
                "Check system resources and model loading"
            ))
            
            return@withContext ValidationResult(
                isValid = false,
                performanceScore = 0.0f,
                accuracyScore = 0.0f,
                issues = issues,
                recommendations = recommendations,
                processingTimeMs = VALIDATION_TIMEOUT_MS,
                memoryUsageMB = getMemoryUsageMB()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ First-run validation failed", e)
            issues.add(ValidationIssue(
                IssueSeverity.CRITICAL,
                "Validation",
                "Validation crashed: ${e.message}",
                "Check logs and report issue"
            ))
            
            return@withContext ValidationResult(
                isValid = false,
                performanceScore = 0.0f,
                accuracyScore = 0.0f,
                issues = issues,
                recommendations = recommendations,
                processingTimeMs = System.currentTimeMillis() - startTime,
                memoryUsageMB = getMemoryUsageMB()
            )
        }
    }
    
    /**
     * Validate core component initialization
     */
    private suspend fun validateComponentInitialization(context: Context): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.d(TAG, "🔧 Validating component initialization...")
        
        // Test ContentDetectionEngine initialization
        val detectionInitialized = contentDetectionEngine.initialize(context)
        if (!detectionInitialized) {
            issues.add(ValidationIssue(
                IssueSeverity.CRITICAL,
                "ContentDetectionEngine",
                "Failed to initialize content detection engine",
                "Check AI model files and permissions"
            ))
        }
        
        // Test engine readiness
        val engineReady = contentDetectionEngine.isEngineReady()
        if (!engineReady) {
            issues.add(ValidationIssue(
                IssueSeverity.HIGH,
                "ContentDetectionEngine",
                "Detection engine not ready after initialization",
                "Check ML model loading and GPU acceleration"
            ))
            recommendations.add("Consider disabling GPU acceleration if initialization fails")
        }
        
        // Test settings repository
        try {
            val settings = settingsRepository.getCurrentSettings()
            Log.d(TAG, "✅ Settings loaded: GPU=${settings.enableGPUAcceleration}, NSFW=${settings.enableNSFWDetection}")
        } catch (e: Exception) {
            issues.add(ValidationIssue(
                IssueSeverity.HIGH,
                "SettingsRepository",
                "Failed to load settings: ${e.message}",
                "Reset to default settings"
            ))
        }
        
        return ValidationResult(
            isValid = issues.none { it.severity == IssueSeverity.CRITICAL },
            performanceScore = if (issues.isEmpty()) 1.0f else 0.5f,
            accuracyScore = 1.0f,
            issues = issues,
            recommendations = recommendations,
            processingTimeMs = 0,
            memoryUsageMB = 0f
        )
    }
    
    /**
     * Validate AI model performance with test images
     */
    private suspend fun validateAIModelPerformance(context: Context): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.d(TAG, "🧠 Validating AI model performance...")
        
        try {
            // Create test bitmaps for validation
            val testBitmaps = createTestBitmaps(context)
            val settings = settingsRepository.getCurrentSettings()
            
            var totalProcessingTime = 0L
            var successfulTests = 0
            
            for ((testName, bitmap) in testBitmaps) {
                try {
                    val startTime = System.currentTimeMillis()
                    val result = contentDetectionEngine.analyzeContent(bitmap, settings, "com.test.validation")
                    val processingTime = System.currentTimeMillis() - startTime
                    
                    totalProcessingTime += processingTime
                    
                    if (result.success) {
                        successfulTests++
                        Log.d(TAG, "✅ Test '$testName' passed in ${processingTime}ms")
                        
                        // Check performance targets
                        if (processingTime > PERFORMANCE_TARGET_MS) {
                            issues.add(ValidationIssue(
                                IssueSeverity.MEDIUM,
                                "Performance",
                                "Test '$testName' took ${processingTime}ms (target: ${PERFORMANCE_TARGET_MS}ms)",
                                "Consider enabling GPU acceleration or reducing image quality"
                            ))
                        }
                    } else {
                        issues.add(ValidationIssue(
                            IssueSeverity.HIGH,
                            "AI Model",
                            "Test '$testName' failed: ${result.error}",
                            "Check model files and input format"
                        ))
                    }
                    
                    bitmap.recycle()
                } catch (e: Exception) {
                    issues.add(ValidationIssue(
                        IssueSeverity.HIGH,
                        "AI Model",
                        "Test '$testName' crashed: ${e.message}",
                        "Check model compatibility and memory"
                    ))
                }
            }
            
            val averageProcessingTime = if (successfulTests > 0) totalProcessingTime / successfulTests else Long.MAX_VALUE
            val accuracyScore = successfulTests.toFloat() / testBitmaps.size
            
            if (accuracyScore < MIN_ACCURACY_THRESHOLD) {
                issues.add(ValidationIssue(
                    IssueSeverity.CRITICAL,
                    "AI Model",
                    "AI model accuracy too low: ${accuracyScore * 100}% (minimum: ${MIN_ACCURACY_THRESHOLD * 100}%)",
                    "Reinstall app or check device compatibility"
                ))
            }
            
            if (averageProcessingTime > PERFORMANCE_TARGET_MS * 2) {
                recommendations.add("Enable GPU acceleration for better performance")
                recommendations.add("Consider reducing processing quality in settings")
            }
            
        } catch (e: Exception) {
            issues.add(ValidationIssue(
                IssueSeverity.CRITICAL,
                "AI Model",
                "AI model validation failed: ${e.message}",
                "Check model files and device compatibility"
            ))
        }
        
        return ValidationResult(
            isValid = issues.none { it.severity == IssueSeverity.CRITICAL },
            performanceScore = 1.0f,
            accuracyScore = 1.0f,
            issues = issues,
            recommendations = recommendations,
            processingTimeMs = 0,
            memoryUsageMB = 0f
        )
    }
    
    /**
     * Validate performance metrics and resource usage
     */
    private suspend fun validatePerformanceMetrics(context: Context): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.d(TAG, "⚡ Validating performance metrics...")
        
        // Check available memory
        val memoryUsage = getMemoryUsageMB()
        val availableMemory = getAvailableMemoryMB()
        
        if (memoryUsage > availableMemory * 0.3f) { // Using more than 30% of available memory
            issues.add(ValidationIssue(
                IssueSeverity.MEDIUM,
                "Memory",
                "High memory usage: ${memoryUsage}MB (${(memoryUsage/availableMemory*100).toInt()}% of available)",
                "Close other apps or reduce processing quality"
            ))
        }
        
        // Check CPU performance with stress test
        val cpuScore = performCPUStressTest()
        if (cpuScore < 0.5f) {
            issues.add(ValidationIssue(
                IssueSeverity.MEDIUM,
                "CPU",
                "Low CPU performance score: $cpuScore",
                "Enable power saving mode or reduce processing frequency"
            ))
            recommendations.add("Consider using FAST or ULTRA_FAST processing mode")
        }
        
        return ValidationResult(
            isValid = true,
            performanceScore = cpuScore,
            accuracyScore = 1.0f,
            issues = issues,
            recommendations = recommendations,
            processingTimeMs = 0,
            memoryUsageMB = memoryUsage
        )
    }
    
    /**
     * Validate specific app scenarios (browsers, social media, etc.)
     */
    private suspend fun validateSpecificAppScenarios(context: Context): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.d(TAG, "📱 Validating specific app scenarios...")
        
        val testApps = listOf(
            "com.android.chrome" to "Chrome Browser",
            "org.mozilla.firefox" to "Firefox Browser", 
            "com.instagram.android" to "Instagram",
            "com.facebook.katana" to "Facebook",
            "com.twitter.android" to "Twitter"
        )
        
        val settings = settingsRepository.getCurrentSettings()
        
        for ((packageName, appName) in testApps) {
            try {
                // Test app-specific filtering
                val shouldMonitor = shouldMonitorApp(packageName)
                Log.d(TAG, "App monitoring test: $appName -> $shouldMonitor")
                
                // Test with app-specific bitmap
                val testBitmap = createAppSpecificTestBitmap(context, packageName)
                if (testBitmap != null) {
                    val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
                    if (!result.success) {
                        issues.add(ValidationIssue(
                            IssueSeverity.MEDIUM,
                            "App Compatibility",
                            "Failed to analyze content for $appName: ${result.error}",
                            "Check app-specific settings and compatibility"
                        ))
                    }
                    testBitmap.recycle()
                }
                
            } catch (e: Exception) {
                issues.add(ValidationIssue(
                    IssueSeverity.LOW,
                    "App Testing",
                    "Failed to test $appName: ${e.message}",
                    "App may not be installed or accessible"
                ))
            }
        }
        
        return ValidationResult(
            isValid = true,
            performanceScore = 1.0f,
            accuracyScore = 1.0f,
            issues = issues,
            recommendations = recommendations,
            processingTimeMs = 0,
            memoryUsageMB = 0f
        )
    }
    
    /**
     * Validate edge cases and error handling
     */
    private suspend fun validateEdgeCases(context: Context): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val recommendations = mutableListOf<String>()
        
        Log.d(TAG, "🔍 Validating edge cases...")
        
        val settings = settingsRepository.getCurrentSettings()
        
        // Test with null bitmap
        try {
            // This should be handled gracefully
            val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val result = contentDetectionEngine.analyzeContent(emptyBitmap, settings, "test")
            emptyBitmap.recycle()
            
            if (result.success) {
                Log.d(TAG, "✅ Empty bitmap handled correctly")
            }
        } catch (e: Exception) {
            issues.add(ValidationIssue(
                IssueSeverity.MEDIUM,
                "Error Handling",
                "Failed to handle empty bitmap gracefully: ${e.message}",
                "Improve input validation"
            ))
        }
        
        // Test with very large bitmap
        try {
            val largeBitmap = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888)
            val startTime = System.currentTimeMillis()
            val result = contentDetectionEngine.analyzeContent(largeBitmap, settings, "test")
            val processingTime = System.currentTimeMillis() - startTime
            largeBitmap.recycle()
            
            if (processingTime > PERFORMANCE_TARGET_MS * 5) {
                issues.add(ValidationIssue(
                    IssueSeverity.MEDIUM,
                    "Performance",
                    "Large bitmap processing too slow: ${processingTime}ms",
                    "Implement bitmap scaling for large images"
                ))
            }
        } catch (e: Exception) {
            issues.add(ValidationIssue(
                IssueSeverity.LOW,
                "Edge Case",
                "Large bitmap test failed: ${e.message}",
                "May be memory limitation"
            ))
        }
        
        return ValidationResult(
            isValid = true,
            performanceScore = 1.0f,
            accuracyScore = 1.0f,
            issues = issues,
            recommendations = recommendations,
            processingTimeMs = 0,
            memoryUsageMB = 0f
        )
    }
    
    // Helper methods
    private fun createTestBitmaps(context: Context): Map<String, Bitmap> {
        val testBitmaps = mutableMapOf<String, Bitmap>()
        
        // Create simple test bitmaps
        testBitmaps["small_image"] = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        testBitmaps["medium_image"] = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        testBitmaps["large_image"] = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        
        return testBitmaps
    }
    
    private fun createAppSpecificTestBitmap(context: Context, packageName: String): Bitmap? {
        // Create app-specific test bitmap (could load from assets)
        return Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    }
    
    private fun shouldMonitorApp(packageName: String): Boolean {
        // Simplified app monitoring check
        val browserApps = listOf("com.android.chrome", "org.mozilla.firefox", "com.opera.browser")
        val socialApps = listOf("com.instagram.android", "com.facebook.katana", "com.twitter.android")
        
        return packageName in browserApps || packageName in socialApps
    }
    
    private fun getMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }
    
    private fun getAvailableMemoryMB(): Float {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / (1024f * 1024f)
    }
    
    private suspend fun performCPUStressTest(): Float = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val iterations = 100000
        
        // Simple CPU stress test
        var result = 0.0
        for (i in 0 until iterations) {
            result += Math.sin(i.toDouble()) * Math.cos(i.toDouble())
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        val targetTime = 100L // 100ms target for stress test
        
        return@withContext (targetTime.toFloat() / processingTime.toFloat()).coerceAtMost(1.0f)
    }
    
    private fun calculatePerformanceScore(processingTime: Long, memoryUsage: Float): Float {
        val timeScore = (PERFORMANCE_TARGET_MS.toFloat() / processingTime.toFloat()).coerceAtMost(1.0f)
        val memoryScore = (100f / memoryUsage).coerceAtMost(1.0f) // Assume 100MB is good baseline
        
        return (timeScore + memoryScore) / 2f
    }
    
    private fun calculateAccuracyScore(issues: List<ValidationIssue>): Float {
        val criticalCount = issues.count { it.severity == IssueSeverity.CRITICAL }
        val highCount = issues.count { it.severity == IssueSeverity.HIGH }
        val mediumCount = issues.count { it.severity == IssueSeverity.MEDIUM }
        
        val totalPenalty = criticalCount * 0.5f + highCount * 0.3f + mediumCount * 0.1f
        
        return (1.0f - totalPenalty).coerceAtLeast(0.0f)
    }
}
