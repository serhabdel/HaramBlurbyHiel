package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenCaptureManager @Inject constructor() {
    
    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val DEFAULT_CAPTURE_DELAY = 1000L // 1 second between captures
        private const val CAPTURE_TIMEOUT = 3000L // 3 second timeout
    }
    
    private var captureJob: Job? = null
    private var isCapturing = false
    private var captureDelay = DEFAULT_CAPTURE_DELAY
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    
    fun startCapturing(onScreenCaptured: (Bitmap) -> Unit) {
        if (isCapturing) {
            Log.w(TAG, "Screen capture already running")
            return
        }
        
        isCapturing = true
        captureJob = CoroutineScope(Dispatchers.Main).launch {
            while (isCapturing) {
                try {
                    val screenshot = captureScreen()
                    screenshot?.let { bitmap ->
                        Log.d(TAG, "Screenshot captured: ${bitmap.width}x${bitmap.height}")
                        onScreenCaptured(bitmap)
                    }
                    delay(captureDelay)
                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing screen", e)
                    delay(captureDelay * 2) // Wait longer on error
                }
            }
        }
        Log.d(TAG, "Screen capture started with ${captureDelay}ms interval")
    }
    
    fun stopCapturing() {
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        
        // Clean up capture resources
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        
        Log.d(TAG, "Screen capture stopped and resources cleaned")
    }
    
    fun setCaptureDelay(delayMs: Long) {
        captureDelay = delayMs.coerceAtLeast(500L) // Minimum 500ms to prevent excessive CPU usage
        Log.d(TAG, "Capture delay set to ${captureDelay}ms")
    }
    
    private suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val service = HaramBlurAccessibilityService.getInstance()
            if (service == null) {
                Log.w(TAG, "Accessibility service not available")
                return@withContext null
            }
            
            // Use AccessibilityService screenshot capability (API 30+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                withTimeout(CAPTURE_TIMEOUT) {
                    service.takeScreenshot()
                }
            } else {
                // For older versions, create a simulated screenshot
                createSimulatedScreenshot(service)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Screenshot capture timed out")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen", e)
            null
        }
    }
    
    @Suppress("DEPRECATION")
    private fun createSimulatedScreenshot(service: AccessibilityService): Bitmap? {
        return try {
            val windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)
            
            // Create a bitmap representing the screen
            // This is a placeholder - actual screenshot would require more complex implementation
            val bitmap = Bitmap.createBitmap(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            
            // For demo purposes, fill with a pattern
            bitmap.eraseColor(android.graphics.Color.LTGRAY)
            
            Log.d(TAG, "Created simulated screenshot: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create simulated screenshot", e)
            null
        }
    }
    
    private suspend fun AccessibilityService.takeScreenshot(): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    // Use the new takeScreenshot API
                    var bitmap: Bitmap? = null
                    val callback = object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                            val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            
                            // Convert HARDWARE bitmap to software bitmap for pixel access
                            bitmap = convertHardwareToSoftwareBitmap(hardwareBitmap)
                            screenshot.hardwareBuffer.close()
                            hardwareBitmap?.recycle() // Clean up hardware bitmap
                        }
                        
                        override fun onFailure(errorCode: Int) {
                            Log.e(TAG, "Screenshot failed with error code: $errorCode")
                            bitmap = null
                        }
                    }
                    
                    takeScreenshot(
                        android.view.Display.DEFAULT_DISPLAY,
                        { it.run() }, // Execute on current thread
                        callback
                    )
                    
                    // Wait for callback (simplified - in production use proper synchronization)
                    delay(1000)
                    bitmap
                } else {
                    createSimulatedScreenshot(this@takeScreenshot)
                }
            } catch (e: Exception) {
                Log.e(TAG, "takeScreenshot failed", e)
                null
            }
        }
    }
    
    /**
     * Converts a HARDWARE bitmap to a software bitmap that supports pixel access
     */
    private fun convertHardwareToSoftwareBitmap(hardwareBitmap: Bitmap?): Bitmap? {
        return try {
            if (hardwareBitmap == null || hardwareBitmap.isRecycled) {
                Log.w(TAG, "Hardware bitmap is null or recycled")
                return null
            }
            
            // Check if it's already a software bitmap
            if (hardwareBitmap.config != Bitmap.Config.HARDWARE) {
                Log.d(TAG, "Bitmap is already software format: ${hardwareBitmap.config}")
                return hardwareBitmap
            }
            
            Log.d(TAG, "Converting HARDWARE bitmap to software format...")
            
            // Create a software bitmap copy
            val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
            
            if (softwareBitmap != null) {
                Log.d(TAG, "Successfully converted HARDWARE bitmap to ARGB_8888: ${softwareBitmap.width}x${softwareBitmap.height}")
                return softwareBitmap
            } else {
                Log.e(TAG, "Failed to convert HARDWARE bitmap - copy() returned null")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting HARDWARE bitmap to software format", e)
            return null
        }
    }
    
    fun isCapturingActive(): Boolean = isCapturing
    
    fun getCaptureDelay(): Long = captureDelay
    
    fun getCaptureStats(): CaptureStats {
        return CaptureStats(
            isActive = isCapturing,
            captureInterval = captureDelay,
            hasActiveCapture = captureJob?.isActive == true
        )
    }
    
    data class CaptureStats(
        val isActive: Boolean,
        val captureInterval: Long,
        val hasActiveCapture: Boolean
    )
}