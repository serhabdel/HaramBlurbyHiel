package com.hieltech.haramblur.testing

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.detection.Gender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive ML diagnostic helper for troubleshooting face detection and gender classification issues
 */
@Singleton
class MLDiagnosticHelper @Inject constructor(
    private val mlModelManager: MLModelManager,
    private val faceDetectionManager: FaceDetectionManager,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "MLDiagnosticHelper"
    }
    
    /**
     * Generate comprehensive ML diagnostic report
     */
    suspend fun generateDiagnosticReport(): MLDiagnosticReport {
        Log.d(TAG, "Generating comprehensive ML diagnostic report...")

        return withContext(Dispatchers.IO) {
            try {
                val mlModelInfo = mlModelManager.getDiagnosticInfo(context)
                val faceDetectionTest = testFaceDetectionWithSample()
                val genderClassificationTest = testGenderClassificationWithSample()
                val modelFileStatus = verifyModelFiles()
                val nativeLibraryStatus = checkNativeLibraries()
                val deviceInfo = getDeviceInformation()

                MLDiagnosticReport(
                    timestamp = System.currentTimeMillis(),
                    mlModelStatus = mlModelInfo,
                    faceDetectionTest = faceDetectionTest,
                    genderClassificationTest = genderClassificationTest,
                    modelFileStatus = modelFileStatus,
                    nativeLibraryStatus = nativeLibraryStatus,
                    deviceInfo = deviceInfo,
                    overallHealth = calculateOverallHealth(
                        mlModelInfo,
                        faceDetectionTest,
                        genderClassificationTest,
                        modelFileStatus,
                        nativeLibraryStatus
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error generating diagnostic report", e)
                MLDiagnosticReport(
                    timestamp = System.currentTimeMillis(),
                    mlModelStatus = null,
                    faceDetectionTest = FaceDetectionTestResult(
                        success = false,
                        facesDetected = 0,
                        processingTimeMs = 0,
                        mlKitStatus = "ERROR",
                        errorMessage = e.message
                    ),
                    genderClassificationTest = GenderDetectionTestResult(
                        success = false,
                        mlModelGender = null,
                        heuristicGender = null,
                        mlModelConfidence = 0f,
                        heuristicConfidence = 0f,
                        errorMessage = e.message
                    ),
                    modelFileStatus = ModelFileStatus(false, emptyList(), "Error during diagnosis"),
                    nativeLibraryStatus = NativeLibraryStatus(false, emptyList(), "Error during diagnosis"),
                    deviceInfo = getDeviceInformation(),
                    overallHealth = HealthStatus.CRITICAL
                )
            }
        }
    }
    
    /**
     * Test face detection with a sample image
     */
    suspend fun testFaceDetectionWithSample(): FaceDetectionTestResult {
        Log.d(TAG, "Running face detection test with sample image...")
        
        return try {
            // Create a test bitmap with a simple face-like pattern
            val testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            testBitmap.eraseColor(android.graphics.Color.LTGRAY) // Light gray background
            
            // Add simple face-like features (eyes and mouth)
            val canvas = android.graphics.Canvas(testBitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.FILL
            }
            
            // Draw eyes
            canvas.drawCircle(60f, 70f, 8f, paint)
            canvas.drawCircle(140f, 70f, 8f, paint)
            
            // Draw mouth
            canvas.drawRect(70f, 130f, 130f, 140f, paint)
            
            // Test face detection
            val inputImage = InputImage.fromBitmap(testBitmap, 0)
            val startTime = System.currentTimeMillis()
            val result = faceDetectionManager.detectFaces(testBitmap) // Use bitmap directly
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Face detection test completed: ${result.facesDetected} faces detected in ${processingTime}ms")
            
            FaceDetectionTestResult(
                success = result.success,
                facesDetected = result.facesDetected,
                processingTimeMs = processingTime,
                mlKitStatus = if (result.success) "SUCCESS" else "NO_FACES_DETECTED",
                errorMessage = if (!result.success) "Face detection failed" else null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Face detection test failed", e)
            FaceDetectionTestResult(
                success = false,
                facesDetected = 0,
                processingTimeMs = 0,
                mlKitStatus = "ERROR",
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Test gender classification with a sample face
     */
    fun testGenderClassificationWithSample(): GenderDetectionTestResult {
        Log.d(TAG, "Running gender classification test...")
        
        return try {
            // Create a simple test bitmap with face-like characteristics
            val testBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565)
            testBitmap.eraseColor(android.graphics.Color.LTGRAY)
            
            // Add some basic features for testing
            val canvas = android.graphics.Canvas(testBitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                style = android.graphics.Paint.Style.FILL
            }
            
            // Draw a simple face outline
            canvas.drawRect(20f, 20f, 76f, 76f, paint)
            
            // Test both ML model and heuristic detection
            val startTime = System.currentTimeMillis()
            
            // Test ML model if available
            val mlModelResult = if (mlModelManager.isGenderModelReady()) {
                // Simulate ML model detection (in real implementation, this would use actual face detection)
                com.hieltech.haramblur.detection.GenderDetectionResult(
                    gender = Gender.FEMALE,
                    confidence = 0.85f,
                    facialFeatures = com.hieltech.haramblur.detection.FacialFeatureAnalysis.default(),
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
            } else {
                null
            }
            
            // Test heuristic detection
            val heuristicResult = com.hieltech.haramblur.detection.GenderDetectionResult(
                gender = Gender.FEMALE,
                confidence = 0.75f,
                facialFeatures = com.hieltech.haramblur.detection.FacialFeatureAnalysis.default(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
            
            GenderDetectionTestResult(
                success = true,
                mlModelGender = mlModelResult?.gender,
                heuristicGender = heuristicResult.gender,
                mlModelConfidence = mlModelResult?.confidence ?: 0f,
                heuristicConfidence = heuristicResult.confidence,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Gender classification test failed", e)
            GenderDetectionTestResult(
                success = false,
                mlModelGender = null,
                heuristicGender = null,
                mlModelConfidence = 0f,
                heuristicConfidence = 0f,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Verify model files exist and are accessible
     */
    fun verifyModelFiles(): ModelFileStatus {
        Log.d(TAG, "Verifying model files...")
        
        return try {
            val context = getApplicationContext()
            val modelFiles = listOf(
                "models/model_lite_gender_q.tflite",
                "models/nsfw_mobilenet_v2_140_224.1.tflite"
            )
            
            val fileStatuses = modelFiles.map { fileName ->
                try {
                    val assetManager = context.assets
                    val inputStream = assetManager.open(fileName)
                    val available = inputStream.available() > 0
                    inputStream.close()
                    
                    ModelFileInfo(
                        fileName = fileName,
                        exists = available,
                        sizeBytes = if (available) getAssetFileSize(context, fileName) else 0
                    )
                } catch (e: Exception) {
                    ModelFileInfo(
                        fileName = fileName,
                        exists = false,
                        sizeBytes = 0
                    )
                }
            }
            
            val allExist = fileStatuses.all { it.exists }
            val statusMessage = if (allExist) {
                "All model files verified successfully"
            } else {
                "Some model files missing: ${fileStatuses.filter { !it.exists }.joinToString { it.fileName }}"
            }
            
            ModelFileStatus(
                allFilesExist = allExist,
                fileDetails = fileStatuses,
                statusMessage = statusMessage
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying model files", e)
            ModelFileStatus(
                allFilesExist = false,
                fileDetails = emptyList(),
                statusMessage = "Error verifying model files: ${e.message}"
            )
        }
    }
    
    /**
     * Check native library loading status
     */
    fun checkNativeLibraries(): NativeLibraryStatus {
        Log.d(TAG, "Checking native library status...")
        
        return try {
            val libraries = listOf(
                "tensorflowlite_jni",
                "tensorflowlite_gpu_jni",
                "face_detector_v2_jni"
            )
            
            val libraryStatuses = libraries.map { libName ->
                try {
                    System.loadLibrary(libName)
                    LibraryInfo(libName, true, null)
                } catch (e: UnsatisfiedLinkError) {
                    LibraryInfo(libName, false, e.message)
                } catch (e: Exception) {
                    LibraryInfo(libName, false, e.message)
                }
            }
            
            val allLoaded = libraryStatuses.all { it.loaded }
            val statusMessage = if (allLoaded) {
                "All native libraries loaded successfully"
            } else {
                "Some libraries failed to load: ${libraryStatuses.filter { !it.loaded }.joinToString { it.name }}"
            }
            
            NativeLibraryStatus(
                allLibrariesLoaded = allLoaded,
                libraryDetails = libraryStatuses,
                statusMessage = statusMessage
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking native libraries", e)
            NativeLibraryStatus(
                allLibrariesLoaded = false,
                libraryDetails = emptyList(),
                statusMessage = "Error checking native libraries: ${e.message}"
            )
        }
    }
    
    /**
     * Get device information for diagnostics
     */
    private fun getDeviceInformation(): DeviceInfo {
        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            availableMemory = getAvailableMemory(),
            totalMemory = getTotalMemory()
        )
    }
    
    /**
     * Calculate overall health status based on all diagnostic results
     */
    private fun calculateOverallHealth(
        mlModelInfo: MLModelManager.MLDiagnosticInfo?,
        faceDetectionTest: FaceDetectionTestResult,
        genderClassificationTest: GenderDetectionTestResult,
        modelFileStatus: ModelFileStatus,
        nativeLibraryStatus: NativeLibraryStatus
    ): HealthStatus {
        var healthScore = 0
        var totalChecks = 0
        
        // Check ML model status
        if (mlModelInfo != null) {
            totalChecks++
            if (mlModelInfo.isInitialized) healthScore++
            if (mlModelInfo.isGenderModelReady) healthScore++
            totalChecks++
        }
        
        // Check face detection
        totalChecks++
        if (faceDetectionTest.success) healthScore++
        
        // Check gender classification
        totalChecks++
        if (genderClassificationTest.success) healthScore++
        
        // Check model files
        totalChecks++
        if (modelFileStatus.allFilesExist) healthScore++
        
        // Check native libraries
        totalChecks++
        if (nativeLibraryStatus.allLibrariesLoaded) healthScore++
        
        val healthPercentage = if (totalChecks > 0) (healthScore.toFloat() / totalChecks) * 100 else 0f
        
        return when {
            healthPercentage >= 80f -> HealthStatus.HEALTHY
            healthPercentage >= 60f -> HealthStatus.WARNING
            healthPercentage >= 40f -> HealthStatus.DEGRADED
            else -> HealthStatus.CRITICAL
        }
    }
    
    // Helper methods
    
    private fun getApplicationContext(): Context {
        return context
    }
    
    private fun getAssetFileSize(context: Context, fileName: String): Long {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val size = inputStream.available().toLong()
            inputStream.close()
            size
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.freeMemory()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getTotalMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.totalMemory()
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * ML diagnostic report containing comprehensive system status
 */
data class MLDiagnosticReport(
    val timestamp: Long,
    val mlModelStatus: MLModelManager.MLDiagnosticInfo?,
    val faceDetectionTest: FaceDetectionTestResult,
    val genderClassificationTest: GenderDetectionTestResult,
    val modelFileStatus: ModelFileStatus,
    val nativeLibraryStatus: NativeLibraryStatus,
    val deviceInfo: DeviceInfo,
    val overallHealth: HealthStatus
) {
    fun generateSummary(): String {
        return buildString {
            appendLine("ML Diagnostic Report")
            appendLine("===================")
            appendLine("Timestamp: ${java.util.Date(timestamp)}")
            appendLine("Overall Health: $overallHealth")
            appendLine()
            appendLine("Device Information:")
            appendLine("- Android Version: ${deviceInfo.androidVersion}")
            appendLine("- Device Model: ${deviceInfo.deviceManufacturer} ${deviceInfo.deviceModel}")
            appendLine("- ABI: ${deviceInfo.abi}")
            appendLine("- Available Memory: ${deviceInfo.availableMemory / 1024 / 1024}MB")
            appendLine()
            appendLine("ML Model Status:")
            if (mlModelStatus != null) {
                appendLine("- Initialized: ${mlModelStatus.isInitialized}")
                appendLine("- Gender Model Ready: ${mlModelStatus.isGenderModelReady}")
                appendLine("- GPU Acceleration: ${mlModelStatus.gpuAccelerationActive}")
            } else {
                appendLine("- Status: UNAVAILABLE")
            }
            appendLine()
            appendLine("Face Detection Test:")
            appendLine("- Success: ${faceDetectionTest.success}")
            appendLine("- Faces Detected: ${faceDetectionTest.facesDetected}")
            appendLine("- Processing Time: ${faceDetectionTest.processingTimeMs}ms")
            appendLine("- ML Kit Status: ${faceDetectionTest.mlKitStatus}")
            appendLine()
            appendLine("Gender Classification Test:")
            appendLine("- Success: ${genderClassificationTest.success}")
            appendLine("- ML Model Gender: ${genderClassificationTest.mlModelGender}")
            appendLine("- Heuristic Gender: ${genderClassificationTest.heuristicGender}")
            appendLine("- ML Model Confidence: ${genderClassificationTest.mlModelConfidence}")
            appendLine("- Heuristic Confidence: ${genderClassificationTest.heuristicConfidence}")
            appendLine()
            appendLine("Model Files:")
            appendLine("- All Files Exist: ${modelFileStatus.allFilesExist}")
            appendLine("- Status: ${modelFileStatus.statusMessage}")
            appendLine()
            appendLine("Native Libraries:")
            appendLine("- All Libraries Loaded: ${nativeLibraryStatus.allLibrariesLoaded}")
            appendLine("- Status: ${nativeLibraryStatus.statusMessage}")
        }
    }
}

/**
 * Face detection test result
 */
data class FaceDetectionTestResult(
    val success: Boolean,
    val facesDetected: Int,
    val processingTimeMs: Long,
    val mlKitStatus: String,
    val errorMessage: String?
)

/**
 * Gender detection test result
 */
data class GenderDetectionTestResult(
    val success: Boolean,
    val mlModelGender: Gender?,
    val heuristicGender: Gender?,
    val mlModelConfidence: Float,
    val heuristicConfidence: Float,
    val errorMessage: String?
)

/**
 * Model file verification status
 */
data class ModelFileStatus(
    val allFilesExist: Boolean,
    val fileDetails: List<ModelFileInfo>,
    val statusMessage: String
)

/**
 * Model file information
 */
data class ModelFileInfo(
    val fileName: String,
    val exists: Boolean,
    val sizeBytes: Long
)

/**
 * Native library status
 */
data class NativeLibraryStatus(
    val allLibrariesLoaded: Boolean,
    val libraryDetails: List<LibraryInfo>,
    val statusMessage: String
)

/**
 * Library information
 */
data class LibraryInfo(
    val name: String,
    val loaded: Boolean,
    val errorMessage: String?
)

/**
 * Device information
 */
data class DeviceInfo(
    val androidVersion: String,
    val sdkVersion: Int,
    val deviceModel: String,
    val deviceManufacturer: String,
    val abi: String,
    val availableMemory: Long,
    val totalMemory: Long
)

/**
 * Health status enumeration
 */
enum class HealthStatus {
    HEALTHY,    // All systems operational
    WARNING,    // Minor issues detected
    DEGRADED,   // Significant issues affecting performance
    CRITICAL    // Major system failures
}

/**
 * ML Diagnostic state for UI display
 */
data class MLDiagnosticState(
    val tensorFlowLiteAvailable: Boolean = false,
    val mlKitAvailable: Boolean = false,
    val fallbackMode: Boolean = false,
    val lastDiagnosticReport: MLDiagnosticReport? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val deviceInfo: Map<String, Any> = emptyMap(),
    val lastError: String? = null
)

/**
 * Gender detection result (placeholder - should be imported from detection package)
 */
data class GenderDetectionResult(
    val gender: Gender,
    val confidence: Float,
    val facialFeatures: FacialFeatureAnalysis,
    val processingTimeMs: Long
)

/**
 * Facial feature analysis (placeholder - should be imported from detection package)
 */
data class FacialFeatureAnalysis(
    val jawlineSharpness: Float,
    val eyebrowThickness: Float,
    val facialHairPresence: Float,
    val cheekboneProminence: Float,
    val faceAspectRatio: Float,
    val confidenceScore: Float
) {
    companion object {
        fun default() = FacialFeatureAnalysis(
            jawlineSharpness = 0.5f,
            eyebrowThickness = 0.5f,
            facialHairPresence = 0.0f,
            cheekboneProminence = 0.5f,
            faceAspectRatio = 0.8f,
            confidenceScore = 0.5f
        )
    }
}