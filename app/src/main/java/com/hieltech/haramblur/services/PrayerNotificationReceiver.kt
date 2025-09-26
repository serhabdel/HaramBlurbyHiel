package com.hieltech.haramblur.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import com.hieltech.haramblur.di.PrayerNotificationEntryPoint

/**
 * Handles prayer notification button actions
 */
class PrayerNotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PrayerNotificationReceiver"
        
        // Import actions from PrayerTimeNotificationManager
        const val ACTION_PRAYER_COMPLETED = PrayerTimeNotificationManager.ACTION_PRAYER_COMPLETED
        const val ACTION_PRAYER_NOT_COMPLETED = PrayerTimeNotificationManager.ACTION_PRAYER_NOT_COMPLETED
        const val ACTION_PRAYER_WILL_DO_NOW = PrayerTimeNotificationManager.ACTION_PRAYER_WILL_DO_NOW
        const val ACTION_PRAYER_ALREADY_DONE = PrayerTimeNotificationManager.ACTION_PRAYER_ALREADY_DONE
        const val ACTION_SHOW_QURANIC_GUIDANCE = PrayerTimeNotificationManager.ACTION_SHOW_QURANIC_GUIDANCE
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received prayer notification action: ${intent.action}")
        
        val prayerName = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_NAME)
        val prayerTime = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_TIME)
        
        if (prayerName == null || prayerTime == null) {
            Log.e(TAG, "Missing prayer information in intent")
            return
        }
        
        try {
            // Get PrayerTimeNotificationManager using Hilt EntryPoint
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                PrayerNotificationEntryPoint::class.java
            )
            val prayerNotificationManager = entryPoint.prayerTimeNotificationManager()
            
            when (intent.action) {
                ACTION_PRAYER_COMPLETED -> {
                    Log.i(TAG, "User confirmed prayer completed: $prayerName")
                    prayerNotificationManager.handlePrayerCompleted(prayerName, prayerTime)
                }
                
                ACTION_PRAYER_NOT_COMPLETED -> {
                    Log.i(TAG, "User indicated prayer not completed: $prayerName")
                    prayerNotificationManager.handlePrayerNotCompleted(prayerName, prayerTime)
                }
                
                ACTION_PRAYER_ALREADY_DONE -> {
                    Log.i(TAG, "User confirmed prayer already done: $prayerName")
                    prayerNotificationManager.handlePrayerAlreadyDone(prayerName, prayerTime)
                }
                
                ACTION_PRAYER_WILL_DO_NOW -> {
                    Log.i(TAG, "User committed to pray now: $prayerName")
                    prayerNotificationManager.handleWillPrayNow(prayerName, prayerTime)
                }
                
                else -> {
                    Log.w(TAG, "Unknown prayer notification action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling prayer notification action", e)
        }
    }
}