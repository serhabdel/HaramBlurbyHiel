package com.hieltech.haramblur.accessibility

import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class for blur regions with metadata for debounced updates
 */
data class BlurRegionWithMeta(
    val regions: List<Rect>,
    val intensity: com.hieltech.haramblur.data.BlurIntensity,
    val style: com.hieltech.haramblur.data.BlurStyle,
    val confidence: Float,
    val timestamp: Long
)

/**
 * Debouncer for blur updates to prevent excessive updates
 * Uses structured concurrency with proper scope management
 */
class BlurUpdateDebouncer(private val debounceMs: Long) {
    private var pendingUpdate: BlurRegionWithMeta? = null
    private var debounceJob: Job? = null
    private val debounceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    fun scheduleUpdate(
        blurRegionWithMeta: BlurRegionWithMeta,
        onUpdate: (BlurRegionWithMeta) -> Unit
    ) {
        // Cancel any existing debounce job
        debounceJob?.cancel()

        // Store the latest update
        pendingUpdate = blurRegionWithMeta

        // Schedule new debounced update using structured concurrency
        debounceJob = debounceScope.launch {
            try {
                delay(debounceMs)

                // Apply the pending update
                pendingUpdate?.let { update ->
                    onUpdate(update)
                    pendingUpdate = null
                }
            } catch (e: CancellationException) {
                // Job was cancelled, which is expected behavior
                Log.d(TAG, "Debounced update cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error in debounced update", e)
            }
        }
    }

    fun cancelPendingUpdates() {
        debounceJob?.cancel()
        pendingUpdate = null
    }

    /**
     * Clean up resources when the debouncer is no longer needed
     */
    fun cleanup() {
        debounceScope.cancel()
        pendingUpdate = null
    }

    companion object {
        private const val TAG = "BlurUpdateDebouncer"
    }
}
