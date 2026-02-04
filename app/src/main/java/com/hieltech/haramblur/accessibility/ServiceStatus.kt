package com.hieltech.haramblur.accessibility

/**
 * Data class representing the current status of the accessibility service
 */
data class ServiceStatus(
    val isServiceRunning: Boolean = false,
    val isProcessingActive: Boolean = false,
    val isCapturingActive: Boolean = false,
    val isOverlayActive: Boolean = false,
    val lastProcessingTime: Long = 0L,
    val currentAppPackage: String? = null,
    val lastError: String = "",
    val totalFramesProcessed: Long = 0L,
    val totalFramesSkipped: Long = 0L,
    val averageProcessingTime: Float = 0f
) {
    companion object {
        val DEFAULT = ServiceStatus()
    }
}
