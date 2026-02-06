package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenCaptureManager @Inject constructor() {
    
    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val DEFAULT_CAPTURE_DELAY = 500L // 500ms between captures for faster response
        private const val CAPTURE_TIMEOUT = 3000L // 3 second timeout
    }
    
    private var captureJob: Job? = null
    private var isCapturing = false
    private var captureDelay = DEFAULT_CAPTURE_DELAY
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    
    fun startCapturing(onScreenCaptured: (Bitmap) -> Unit) {
        if (isCapturing && captureJob?.isActive == true) {
            Log.w(TAG, "Screen capture already running and active")
            return
        }
        
        // Reset state if capture job is not active
        if (isCapturing && captureJob?.isActive != true) {
            Log.w(TAG, "⚠️ isCapturing was true but job not active - resetting state")
            isCapturing = false
        }
        
        isCapturing = true
        Log.d(TAG, "🎬 Starting screen capture loop (background)...")
        // OPTIMIZATION: Run capture loop on Default dispatcher to avoid blocking Main thread
        captureJob = CoroutineScope(Dispatchers.Default).launch {
            var consecutiveFailures = 0
            val maxFailures = 5
            
            while (isCapturing) {
                try {
                    val screenshot = captureScreen()
                    if (screenshot != null && !screenshot.isRecycled) {
                        Log.d(TAG, "📸 Screenshot captured: ${screenshot.width}x${screenshot.height}")
                        
                        // Validate bitmap before passing to callback
                        if (screenshot.width > 0 && screenshot.height > 0 && screenshot.config != null) {
                            onScreenCaptured(screenshot)
                            consecutiveFailures = 0 // Reset on success
                        } else {
                            Log.w(TAG, "Invalid screenshot - dimensions or config null")
                            screenshot.recycle()
                        }
                    } else {
                        Log.w(TAG, "Screenshot is null or recycled")
                    }
                    delay(captureDelay)
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory during screen capture", e)
                    consecutiveFailures++
                    System.gc() // Force garbage collection
                    delay(captureDelay * 3) // Wait longer on OOM
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception during screen capture - stopping", e)
                    break // Don't continue if we lose permissions
                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing screen", e)
                    consecutiveFailures++
                    delay(captureDelay * 2) // Wait longer on error
                }
                
                // Stop if too many consecutive failures
                if (consecutiveFailures >= maxFailures) {
                    Log.e(TAG, "Too many consecutive screen capture failures ($consecutiveFailures), stopping")
                    break
                }
            }
            
            // Clean up when exiting
            isCapturing = false
            Log.w(TAG, "🛑 Screen capture loop ended - isCapturing set to false")
        }
        Log.d(TAG, "✅ Screen capture started with ${captureDelay}ms interval")
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
        captureDelay = delayMs.coerceAtLeast(200L) // Minimum 200ms for high-alert mode
        Log.d(TAG, "Capture delay set to ${captureDelay}ms")
    }
    
    fun getCurrentDelay(): Long = captureDelay
    
    private suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        return@withContext try {
            val service = HaramBlurAccessibilityService.getInstance()
            if (service == null) {
                Log.w(TAG, "Accessibility service not available")
                return@withContext null
            }
            
            // Use AccessibilityService screenshot capability (API 30+)
            bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                withTimeout(CAPTURE_TIMEOUT) {
                    service.takeScreenshot()
                }
            } else {
                // For older versions, create a simulated screenshot
                createSimulatedScreenshot(service)
            }
            
            // Validate and normalize bitmap before returning
            if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                val normalized = normalizeToCurrentDisplay(bitmap!!, service)
                normalized
            } else {
                Log.w(TAG, "Invalid bitmap captured")
                bitmap?.recycle() // Clean up invalid bitmap
                null
            }
            
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Screenshot capture timed out")
            bitmap?.recycle() // Clean up on timeout
            null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during screenshot capture", e)
            bitmap?.recycle() // Clean up on OOM
            throw e // Re-throw to trigger memory cleanup
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen", e)
            bitmap?.recycle() // Clean up on error
            null
        }
    }
    
    @Suppress("DEPRECATION")
    private suspend fun createSimulatedScreenshot(service: AccessibilityService): Bitmap? = withContext(Dispatchers.Main) {
        try {
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
                    withTimeoutOrNull(CAPTURE_TIMEOUT) {
                        suspendCancellableCoroutine { cont ->
                            var finished = false

                            val callback = object : AccessibilityService.TakeScreenshotCallback {
                                override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                                    if (finished) return
                                    finished = true
                                    try {
                                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                                            screenshot.hardwareBuffer,
                                            screenshot.colorSpace
                                        )

                                        // Convert HARDWARE bitmap to software bitmap for pixel access
                                        val software = convertHardwareToSoftwareBitmap(hardwareBitmap)

                                        // Always close/recycle native resources
                                        try {
                                            screenshot.hardwareBuffer.close()
                                        } catch (_: Exception) {
                                        }
                                        try {
                                            hardwareBitmap?.recycle()
                                        } catch (_: Exception) {
                                        }

                                        cont.resume(software)
                                    } catch (t: Throwable) {
                                        try {
                                            screenshot.hardwareBuffer.close()
                                        } catch (_: Exception) {
                                        }
                                        cont.resumeWithException(t)
                                    }
                                }

                                override fun onFailure(errorCode: Int) {
                                    if (finished) return
                                    finished = true
                                    Log.e(TAG, "Screenshot failed with error code: $errorCode")
                                    cont.resume(null)
                                }
                            }

                            try {
                                takeScreenshot(
                                    android.view.Display.DEFAULT_DISPLAY,
                                    { it.run() }, // Execute on current thread
                                    callback
                                )
                            } catch (e: Exception) {
                                if (!finished) {
                                    finished = true
                                    cont.resume(null)
                                }
                            }

                            cont.invokeOnCancellation {
                                // No additional cleanup required here; hardwareBuffer is only obtained on success.
                            }
                        }
                    }
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

    /**
     * Normalizes screenshot orientation so the bitmap dimensions/orientation match the current display.
     *
     * Why: ML Kit face detection and overlay coordinates become unreliable if the screenshot is rotated
     * relative to the current screen orientation.
     */
    private fun normalizeToCurrentDisplay(bitmap: Bitmap, service: AccessibilityService): Bitmap {
        return try {
            val (displayW, displayH) = getCurrentDisplaySize(service)
            if (displayW <= 0 || displayH <= 0) return bitmap

            if (bitmap.width == displayW && bitmap.height == displayH) {
                // Already matches display.
                return bitmap
            }

            // If dimensions are swapped, rotate to match current display orientation.
            if (bitmap.width == displayH && bitmap.height == displayW) {
                val rotation = getCurrentDisplayRotation(service)
                val degrees = when (rotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 90 // ROTATION_0 but swapped -> pick a sensible default
                }

                Log.w(
                    TAG,
                    "🔄 Screenshot orientation mismatch: bmp=${bitmap.width}x${bitmap.height}, display=${displayW}x${displayH}, rotation=$rotation -> rotate ${degrees}°"
                )

                val rotated = rotateBitmapSafely(bitmap, degrees)
                return rotated
            }

            // Other mismatches (e.g., scaling) - keep as-is but log for diagnostics.
            Log.w(TAG, "⚠️ Screenshot size differs from display: bmp=${bitmap.width}x${bitmap.height}, display=${displayW}x${displayH} (no rotation applied)")
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "normalizeToCurrentDisplay failed - returning original bitmap", e)
            bitmap
        }
    }

    private fun getCurrentDisplayRotation(service: AccessibilityService): Int {
        return try {
            val wm = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                service.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.rotation
            }
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
    }

    private fun getCurrentDisplaySize(service: AccessibilityService): Pair<Int, Int> {
        return try {
            val wm = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                Pair(bounds.width(), bounds.height())
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)
                Pair(metrics.widthPixels, metrics.heightPixels)
            }
        } catch (_: Exception) {
            Pair(0, 0)
        }
    }

    private fun rotateBitmapSafely(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source

        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

            // We own the captured bitmap. If we successfully created a rotated copy, recycle the original.
            runCatching { if (rotated != source && !source.isRecycled) source.recycle() }

            rotated
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory rotating screenshot (${source.width}x${source.height}) by ${degrees}° - using original", e)
            source
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate screenshot by ${degrees}° - using original", e)
            source
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