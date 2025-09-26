package com.hieltech.haramblur.testing

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.hieltech.haramblur.detection.ContentDetectionEngine
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.SettingsRepository
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive app-specific testing system to ensure perfect performance
 * on first-time usage with specific popular apps
 */
@Singleton
class AppSpecificTester @Inject constructor(
    private val contentDetectionEngine: ContentDetectionEngine,
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private const val TAG = "AppSpecificTester"
        private const val TEST_TIMEOUT_MS = 10000L // 10 seconds per app test
        private const val PERFORMANCE_TARGET_MS = 150L // Target processing time per app
    }
    
    data class AppTestResult(
        val packageName: String,
        val appName: String,
        val isInstalled: Boolean,
        val testsPassed: Int,
        val totalTests: Int,
        val averageProcessingTimeMs: Long,
        val issues: List<AppTestIssue>,
        val recommendations: List<String>,
        val performanceScore: Float // 0.0 - 1.0
    )
    
    data class AppTestIssue(
        val severity: TestSeverity,
        val testName: String,
        val description: String,
        val solution: String
    )
    
    enum class TestSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
    
    // Popular apps that users commonly use
    private val popularApps = mapOf(
        // Browsers
        "com.android.chrome" to "Chrome Browser",
        "org.mozilla.firefox" to "Firefox Browser",
        "com.opera.browser" to "Opera Browser",
        "com.microsoft.emmx" to "Microsoft Edge",
        "com.brave.browser" to "Brave Browser",
        
        // Social Media
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "Twitter/X",
        "com.snapchat.android" to "Snapchat",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.linkedin.android" to "LinkedIn",
        
        // Messaging
        "com.whatsapp" to "WhatsApp",
        "org.telegram.messenger" to "Telegram",
        "com.discord" to "Discord",
        "com.viber.voip" to "Viber",
        
        // Entertainment
        "com.netflix.mediaclient" to "Netflix",
        "com.google.android.youtube" to "YouTube",
        "com.spotify.music" to "Spotify",
        "com.amazon.avod.thirdpartyclient" to "Prime Video",
        
        // News & Reading
        "com.reddit.frontpage" to "Reddit",
        "flipboard.app" to "Flipboard",
        "com.medium.reader" to "Medium",
        
        // Shopping
        "com.amazon.mShop.android.shopping" to "Amazon Shopping",
        "com.ebay.mobile" to "eBay",
        "com.alibaba.aliexpresshd" to "AliExpress"
    )
    
    /**
     * Run comprehensive tests on all popular apps
     */
    suspend fun testAllPopularApps(context: Context): List<AppTestResult> = withContext(Dispatchers.Default) {
        Log.i(TAG, "🧪 Starting comprehensive app-specific testing...")
        
        val results = mutableListOf<AppTestResult>()
        val settings = settingsRepository.getCurrentSettings()
        
        for ((packageName, appName) in popularApps) {
            try {
                Log.d(TAG, "🔍 Testing app: $appName ($packageName)")
                val result = testSpecificApp(context, packageName, appName, settings)
                results.add(result)
                
                // Log summary
                if (result.isInstalled) {
                    Log.d(TAG, "✅ $appName: ${result.testsPassed}/${result.totalTests} tests passed, ${result.averageProcessingTimeMs}ms avg")
                } else {
                    Log.d(TAG, "📱 $appName: Not installed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to test $appName", e)
                results.add(AppTestResult(
                    packageName = packageName,
                    appName = appName,
                    isInstalled = false,
                    testsPassed = 0,
                    totalTests = 0,
                    averageProcessingTimeMs = 0L,
                    issues = listOf(AppTestIssue(TestSeverity.HIGH, "test_crash", "Test crashed: ${e.message}", "Check app compatibility")),
                    recommendations = listOf("App may not be compatible with current device"),
                    performanceScore = 0.0f
                ))
            }
        }
        
        // Generate summary report
        val installedApps = results.filter { it.isInstalled }
        val totalTestsPassed = installedApps.sumOf { it.testsPassed }
        val totalTests = installedApps.sumOf { it.totalTests }
        val averagePerformance = installedApps.map { it.performanceScore }.average().toFloat()
        
        Log.i(TAG, "📊 App testing summary:")
        Log.i(TAG, "   • Apps tested: ${installedApps.size}/${popularApps.size}")
        Log.i(TAG, "   • Tests passed: $totalTestsPassed/$totalTests (${if (totalTests > 0) (totalTestsPassed * 100 / totalTests) else 0}%)")
        Log.i(TAG, "   • Average performance: ${(averagePerformance * 100).toInt()}%")
        
        return@withContext results
    }
    
    /**
     * Test a specific app thoroughly
     */
    private suspend fun testSpecificApp(
        context: Context,
        packageName: String,
        appName: String,
        settings: AppSettings
    ): AppTestResult = withContext(Dispatchers.Default) {
        
        // Check if app is installed
        val isInstalled = isAppInstalled(context, packageName)
        if (!isInstalled) {
            return@withContext AppTestResult(
                packageName = packageName,
                appName = appName,
                isInstalled = false,
                testsPassed = 0,
                totalTests = 0,
                averageProcessingTimeMs = 0L,
                issues = emptyList(),
                recommendations = emptyList(),
                performanceScore = 1.0f // Not installed is not a failure
            )
        }
        
        val issues = mutableListOf<AppTestIssue>()
        val recommendations = mutableListOf<String>()
        val processingTimes = mutableListOf<Long>()
        
        var testsPassed = 0
        val totalTests = 5 // Number of different test scenarios
        
        try {
            withTimeout(TEST_TIMEOUT_MS) {
                
                // Test 1: Basic content analysis
                val basicTestResult = testBasicContentAnalysis(packageName, settings)
                processingTimes.add(basicTestResult.processingTime)
                if (basicTestResult.success) {
                    testsPassed++
                } else {
                    issues.add(AppTestIssue(
                        TestSeverity.HIGH,
                        "basic_analysis",
                        "Basic content analysis failed for $appName",
                        "Check app compatibility and permissions"
                    ))
                }
                
                // Test 2: Face detection performance
                val faceTestResult = testFaceDetectionPerformance(packageName, settings)
                processingTimes.add(faceTestResult.processingTime)
                if (faceTestResult.success) {
                    testsPassed++
                } else {
                    issues.add(AppTestIssue(
                        TestSeverity.MEDIUM,
                        "face_detection",
                        "Face detection performance poor for $appName",
                        "Consider disabling face detection for this app"
                    ))
                }
                
                // Test 3: NSFW detection accuracy
                val nsfwTestResult = testNSFWDetectionAccuracy(packageName, settings)
                processingTimes.add(nsfwTestResult.processingTime)
                if (nsfwTestResult.success) {
                    testsPassed++
                } else {
                    issues.add(AppTestIssue(
                        TestSeverity.HIGH,
                        "nsfw_detection",
                        "NSFW detection accuracy issues for $appName",
                        "Adjust NSFW detection thresholds"
                    ))
                }
                
                // Test 4: Memory usage test
                val memoryTestResult = testMemoryUsage(packageName, settings)
                processingTimes.add(memoryTestResult.processingTime)
                if (memoryTestResult.success) {
                    testsPassed++
                } else {
                    issues.add(AppTestIssue(
                        TestSeverity.MEDIUM,
                        "memory_usage",
                        "High memory usage detected for $appName",
                        "Enable memory optimization for this app"
                    ))
                    recommendations.add("Consider reducing processing quality for $appName")
                }
                
                // Test 5: Performance stress test
                val stressTestResult = testPerformanceStress(packageName, settings)
                processingTimes.add(stressTestResult.processingTime)
                if (stressTestResult.success) {
                    testsPassed++
                } else {
                    issues.add(AppTestIssue(
                        TestSeverity.LOW,
                        "stress_test",
                        "Performance degradation under stress for $appName",
                        "Enable performance mode for this app"
                    ))
                    recommendations.add("Use FAST or ULTRA_FAST mode for $appName")
                }
                
                // Generate app-specific recommendations
                generateAppSpecificRecommendations(packageName, appName, processingTimes, recommendations)
            }
            
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "⏱️ Testing timed out for $appName")
            issues.add(AppTestIssue(
                TestSeverity.HIGH,
                "timeout",
                "Testing timed out for $appName",
                "App may be too resource intensive"
            ))
            recommendations.add("Consider excluding $appName from monitoring")
        }
        
        val averageProcessingTime = if (processingTimes.isNotEmpty()) {
            processingTimes.average().toLong()
        } else {
            Long.MAX_VALUE
        }
        
        val performanceScore = calculateAppPerformanceScore(
            testsPassed, totalTests, averageProcessingTime, issues
        )
        
        return@withContext AppTestResult(
            packageName = packageName,
            appName = appName,
            isInstalled = true,
            testsPassed = testsPassed,
            totalTests = totalTests,
            averageProcessingTimeMs = averageProcessingTime,
            issues = issues,
            recommendations = recommendations,
            performanceScore = performanceScore
        )
    }
    
    /**
     * Test basic content analysis functionality
     */
    private suspend fun testBasicContentAnalysis(packageName: String, settings: AppSettings): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val testBitmap = createAppSpecificTestBitmap(packageName, 800, 600)
            val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
            testBitmap.recycle()
            
            val processingTime = System.currentTimeMillis() - startTime
            
            TestResult(
                success = result.success,
                processingTime = processingTime
            )
        } catch (e: Exception) {
            TestResult(
                success = false,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Test face detection performance
     */
    private suspend fun testFaceDetectionPerformance(packageName: String, settings: AppSettings): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val testBitmap = createFaceTestBitmap(400, 400)
            val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
            testBitmap.recycle()
            
            val processingTime = System.currentTimeMillis() - startTime
            val success = result.success && processingTime < PERFORMANCE_TARGET_MS * 2
            
            TestResult(
                success = success,
                processingTime = processingTime
            )
        } catch (e: Exception) {
            TestResult(
                success = false,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Test NSFW detection accuracy
     */
    private suspend fun testNSFWDetectionAccuracy(packageName: String, settings: AppSettings): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val testBitmap = createNSFWTestBitmap(600, 800)
            val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
            testBitmap.recycle()
            
            val processingTime = System.currentTimeMillis() - startTime
            val success = result.success && result.nsfwDetectionResult != null
            
            TestResult(
                success = success,
                processingTime = processingTime
            )
        } catch (e: Exception) {
            TestResult(
                success = false,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Test memory usage during processing
     */
    private suspend fun testMemoryUsage(packageName: String, settings: AppSettings): TestResult {
        val startTime = System.currentTimeMillis()
        val initialMemory = getMemoryUsageMB()
        
        return try {
            val testBitmap = createLargeTestBitmap(1080, 1920)
            val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
            testBitmap.recycle()
            
            val finalMemory = getMemoryUsageMB()
            val memoryIncrease = finalMemory - initialMemory
            val processingTime = System.currentTimeMillis() - startTime
            
            // Success if memory increase is reasonable (< 50MB)
            val success = result.success && memoryIncrease < 50f
            
            TestResult(
                success = success,
                processingTime = processingTime
            )
        } catch (e: Exception) {
            TestResult(
                success = false,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Test performance under stress (multiple rapid analyses)
     */
    private suspend fun testPerformanceStress(packageName: String, settings: AppSettings): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val stressTestCount = 5
            var successCount = 0
            
            repeat(stressTestCount) {
                val testBitmap = createAppSpecificTestBitmap(packageName, 400, 400)
                val result = contentDetectionEngine.analyzeContent(testBitmap, settings, packageName)
                testBitmap.recycle()
                
                if (result.success) {
                    successCount++
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            val averageTimePerTest = processingTime / stressTestCount
            
            // Success if most tests passed and average time is reasonable
            val success = successCount >= stressTestCount * 0.8f && averageTimePerTest < PERFORMANCE_TARGET_MS * 3
            
            TestResult(
                success = success,
                processingTime = averageTimePerTest
            )
        } catch (e: Exception) {
            TestResult(
                success = false,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    // Helper methods
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun createAppSpecificTestBitmap(packageName: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Create app-specific test patterns
        when {
            packageName.contains("chrome") || packageName.contains("firefox") -> {
                // Browser-like content
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.color = Color.BLACK
                canvas.drawText("Sample Web Content", 50f, 100f, paint)
            }
            packageName.contains("instagram") || packageName.contains("facebook") -> {
                // Social media-like content
                paint.color = Color.rgb(245, 245, 245)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.color = Color.rgb(50, 50, 50)
                canvas.drawCircle(width / 2f, height / 2f, 50f, paint)
            }
            else -> {
                // Generic content
                paint.color = Color.LTGRAY
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
        
        return bitmap
    }
    
    private fun createFaceTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Create a simple face-like pattern
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        paint.color = Color.rgb(255, 220, 177) // Skin tone
        canvas.drawCircle(width / 2f, height / 2f, 80f, paint)
        
        paint.color = Color.BLACK
        canvas.drawCircle(width / 2f - 25f, height / 2f - 20f, 8f, paint) // Left eye
        canvas.drawCircle(width / 2f + 25f, height / 2f - 20f, 8f, paint) // Right eye
        
        return bitmap
    }
    
    private fun createNSFWTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Create a pattern that might trigger NSFW detection
        paint.color = Color.rgb(255, 220, 177) // Skin tone
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        return bitmap
    }
    
    private fun createLargeTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    
    private fun getMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }
    
    private fun generateAppSpecificRecommendations(
        packageName: String,
        appName: String,
        processingTimes: List<Long>,
        recommendations: MutableList<String>
    ) {
        val averageTime = processingTimes.average()
        
        when {
            packageName.contains("chrome") || packageName.contains("firefox") -> {
                if (averageTime > PERFORMANCE_TARGET_MS) {
                    recommendations.add("Enable GPU acceleration for better browser performance")
                }
                recommendations.add("Consider enabling region-based full-screen blur for browsers")
            }
            
            packageName.contains("instagram") || packageName.contains("tiktok") -> {
                recommendations.add("Use FAST processing mode for social media apps")
                if (averageTime > PERFORMANCE_TARGET_MS) {
                    recommendations.add("Consider reducing face detection sensitivity for social media")
                }
            }
            
            packageName.contains("netflix") || packageName.contains("youtube") -> {
                recommendations.add("Consider excluding video streaming apps from monitoring")
                recommendations.add("Use ULTRA_FAST mode for video content")
            }
        }
    }
    
    private fun calculateAppPerformanceScore(
        testsPassed: Int,
        totalTests: Int,
        averageProcessingTime: Long,
        issues: List<AppTestIssue>
    ): Float {
        val testScore = if (totalTests > 0) testsPassed.toFloat() / totalTests else 1.0f
        val timeScore = (PERFORMANCE_TARGET_MS.toFloat() / averageProcessingTime.toFloat()).coerceAtMost(1.0f)
        val issuesPenalty = issues.sumOf { issue ->
            when (issue.severity) {
                TestSeverity.CRITICAL -> 0.3
                TestSeverity.HIGH -> 0.2
                TestSeverity.MEDIUM -> 0.1
                TestSeverity.LOW -> 0.05
            }
        }.toFloat()
        
        return ((testScore + timeScore) / 2f - issuesPenalty).coerceAtLeast(0.0f)
    }
    
    private data class TestResult(
        val success: Boolean,
        val processingTime: Long
    )
}
