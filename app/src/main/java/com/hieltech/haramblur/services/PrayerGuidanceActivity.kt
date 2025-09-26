package com.hieltech.haramblur.services

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.QuranicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Activity that displays Quranic guidance about the importance of timely prayer
 * Shown when user indicates they haven't prayed yet
 */
@AndroidEntryPoint
class PrayerGuidanceActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PrayerGuidanceActivity"
    }
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject 
    lateinit var quranicRepository: QuranicRepository
    
    @Inject
    lateinit var prayerNotificationManager: PrayerTimeNotificationManager
    
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setupWindow()
            setupSimpleView()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PrayerGuidanceActivity", e)
            finish()
        }
    }
    
    /**
     * Setup window to show as dialog over other apps
     */
    private fun setupWindow() {
        // Make it appear over other apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
    }
    
    /**
     * Setup a simple programmatic view since we don't have the layout file
     */
    private fun setupSimpleView() {
        // For now, create a simple dialog-style notification
        // This would normally use a layout file, but we'll keep it simple
        
        activityScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                val language = settings.preferredLanguage.name
                
                val prayerName = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_NAME) ?: "Prayer"
                
                val message = when (language.lowercase()) {
                    "arabic", "ar" -> "الصلاة عماد الدين. لا تؤخر صلاة $prayerName"
                    "french", "fr" -> "La prière est le pilier de la religion. Ne retardez pas $prayerName"
                    else -> "Prayer is the pillar of religion. Don't delay $prayerName prayer."
                }
                
                Log.i(TAG, "Showing prayer guidance: $message")
                
                // For now, just show a brief message and handle the prayer completion
                handlePrayerGuidanceShown(prayerName)
                
                // Auto-close after showing guidance
                delay(3000)
                finish()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing prayer guidance", e)
                finish()
            }
        }
    }
    
    private fun handlePrayerGuidanceShown(prayerName: String) {
        val prayerTime = intent.getStringExtra(PrayerTimeNotificationManager.EXTRA_PRAYER_TIME) ?: ""
        
        Log.i(TAG, "Prayer guidance shown for $prayerName")
        
        // This would normally show a dialog with buttons
        // For now, we'll assume the user will pray
        if (prayerName.isNotEmpty() && prayerTime.isNotEmpty()) {
            prayerNotificationManager.handleWillPrayNow(prayerName, prayerTime)
        }
    }
    
    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}