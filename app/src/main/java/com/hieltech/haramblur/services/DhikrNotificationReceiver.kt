package com.hieltech.haramblur.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrSettings

class DhikrNotificationReceiver : BroadcastReceiver() {

    // We'll get instances from the application context
    private lateinit var dhikrManager: DhikrManager
    private lateinit var notificationManager: DhikrNotificationManager

    companion object {
        private const val TAG = "DhikrNotificationReceiver"
        const val ACTION_DISMISS = "com.hieltech.haramblur.DHIKR_DISMISS"
        const val ACTION_NEXT = "com.hieltech.haramblur.DHIKR_NEXT"
        const val ACTION_SHOW_NOW = "com.hieltech.haramblur.DHIKR_SHOW_NOW"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received notification action: ${intent.action}")

        // Get dependencies from application context
        val application = context.applicationContext as com.hieltech.haramblur.HaramBlurApplication
        dhikrManager = application.dhikrManager
        notificationManager = application.notificationManager

        when (intent.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Dismissing dhikr notification")
                notificationManager.cancelDhikrNotification()
                dhikrManager.forceHide() // Also hide any overlay
            }

            ACTION_NEXT -> {
                Log.d(TAG, "Showing next dhikr")
                // Get next dhikr and show it
                val nextDhikr = dhikrManager.getNextDhikrInSequence()
                if (nextDhikr != null) {
                    val settings = dhikrManager.getCurrentSettings()
                    dhikrManager.showDhikrWithFallback(nextDhikr, settings)
                } else {
                    Log.d(TAG, "No next dhikr available")
                }
            }

            ACTION_SHOW_NOW -> {
                Log.d(TAG, "Showing dhikr now")
                dhikrManager.showDhikrNow()
            }
        }
    }
}