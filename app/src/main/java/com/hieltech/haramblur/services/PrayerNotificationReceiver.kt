package com.hieltech.haramblur.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import com.hieltech.haramblur.di.PrayerNotificationEntryPoint

/**
 * Handles prayer notification button actions
 * 
 * This receiver handles all prayer notification button actions:
 * - PRAYER_COMPLETED: User confirms they have prayed
 * - PRAYER_NOT_COMPLETED: User indicates they haven't prayed yet
 * - PRAYER_ALREADY_DONE: User confirms prayer was already completed (follow-up)
 * - PRAYER_WILL_DO_NOW: User commits to praying immediately (follow-up)
 * 
 * Uses Hilt EntryPoint to access PrayerTimeNotificationManager since
 * BroadcastReceivers cannot use constructor injection.
 * 
 * This receiver must be registered in AndroidManifest.xml with intent filters for:
 * - com.hieltech.haramblur.PRAYER_COMPLETED
 * - com.hieltech.haramblur.PRAYER_NOT_COMPLETED
 * - com.hieltech.haramblur.PRAYER_WILL_DO_NOW
 * - com.hieltech.haramblur.PRAYER_ALREADY_DONE
 * The receiver should be exported=false for security.
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
    
    /**
     * Receives broadcast intents from notification action buttons
     * Extracts prayer name and time from intent extras
     * Delegates to the appropriate handler method in PrayerTimeNotificationManager
     * Handles errors gracefully with try-catch
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received prayer notification action: ${intent.action}")
        
        val prayerName = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_NAME)
        val prayerTime = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_TIME)
        
        if (prayerName == null || prayerTime == null) {
            Log.e(TAG, "Missing prayer information in intent")
            return
        }
        
        Log.d(TAG, "Processing action for prayer: $prayerName at $prayerTime, package: ${intent.`package`}")
        
        // Validate intent action
        if (intent.action == null) {
            Log.e(TAG, "Intent action is null")
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
                    Log.d(TAG, "Successfully handled PRAYER_COMPLETED for $prayerName")
                }
                
                ACTION_PRAYER_NOT_COMPLETED -> {
                    Log.i(TAG, "User indicated prayer not completed: $prayerName")
                    prayerNotificationManager.handlePrayerNotCompleted(prayerName, prayerTime)
                    Log.d(TAG, "Successfully handled PRAYER_NOT_COMPLETED for $prayerName")
                }
                
                ACTION_PRAYER_ALREADY_DONE -> {
                    Log.i(TAG, "User confirmed prayer already done: $prayerName")
                    prayerNotificationManager.handlePrayerAlreadyDone(prayerName, prayerTime)
                    Log.d(TAG, "Successfully handled PRAYER_ALREADY_DONE for $prayerName")
                }
                
                ACTION_PRAYER_WILL_DO_NOW -> {
                    Log.i(TAG, "User committed to pray now: $prayerName")
                    prayerNotificationManager.handleWillPrayNow(prayerName, prayerTime)
                    Log.d(TAG, "Successfully handled PRAYER_WILL_DO_NOW for $prayerName")
                }
                
                else -> {
                    Log.w(TAG, "Unknown prayer notification action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling prayer notification action: ${intent.action} for $prayerName at $prayerTime", e)
        }
    }
}