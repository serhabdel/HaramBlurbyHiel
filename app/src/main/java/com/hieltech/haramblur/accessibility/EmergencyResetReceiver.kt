package com.hieltech.haramblur.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hieltech.haramblur.utils.AppConstants.Tags

/**
 * Broadcast receiver for emergency reset functionality
 * Extracted from HaramBlurAccessibilityService for better separation of concerns
 */
class EmergencyResetReceiver(
    private val onEmergencyReset: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_EMERGENCY_RESET) {
            Log.w(TAG, "Received emergency reset broadcast")
            onEmergencyReset()
        }
    }

    /**
     * Register this receiver with the appropriate permissions
     */
    fun register(context: Context): Boolean {
        return try {
            val filter = IntentFilter(ACTION_EMERGENCY_RESET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(this, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(this, filter)
            }
            Log.d(TAG, "✅ Emergency reset broadcast receiver registered")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register emergency reset receiver", e)
            false
        }
    }

    /**
     * Unregister this receiver
     */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Log.d(TAG, "Emergency reset receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
            Log.w(TAG, "Emergency reset receiver was not registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering emergency reset receiver", e)
        }
    }

    companion object {
        private const val TAG = Tags.ACCESSIBILITY_SERVICE
        const val ACTION_EMERGENCY_RESET = "com.hieltech.haramblur.EMERGENCY_RESET"

        /**
         * Send emergency reset broadcast programmatically
         * Usage from ADB: adb shell am broadcast -a com.hieltech.haramblur.EMERGENCY_RESET
         */
        fun sendEmergencyResetBroadcast(context: Context) {
            val intent = Intent(ACTION_EMERGENCY_RESET)
            context.sendBroadcast(intent)
            Log.w(TAG, "Emergency reset broadcast sent")
        }
    }
}
