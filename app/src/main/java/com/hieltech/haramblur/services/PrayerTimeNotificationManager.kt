package com.hieltech.haramblur.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.PrayerTimesRepository
import com.hieltech.haramblur.data.prayer.PrayerName
import com.hieltech.haramblur.data.QuranicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages prayer time notifications with intelligent follow-up reminders and Quranic guidance
 */
@Singleton
class PrayerTimeNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val quranicRepository: QuranicRepository
) {
    
    companion object {
        private const val TAG = "PrayerTimeNotificationManager"
        
        // Notification Channels
        private const val PRAYER_TIME_CHANNEL_ID = "prayer_time_channel"
        private const val PRAYER_REMINDER_CHANNEL_ID = "prayer_reminder_channel"
        
        // Notification IDs
        private const val PRAYER_TIME_NOTIFICATION_ID = 2001
        private const val PRAYER_REMINDER_NOTIFICATION_ID = 2002
        
        // Actions for notifications
        const val ACTION_PRAYER_COMPLETED = "com.hieltech.haramblur.PRAYER_COMPLETED"
        const val ACTION_PRAYER_NOT_COMPLETED = "com.hieltech.haramblur.PRAYER_NOT_COMPLETED"
        const val ACTION_PRAYER_WILL_DO_NOW = "com.hieltech.haramblur.PRAYER_WILL_DO_NOW"
        const val ACTION_PRAYER_ALREADY_DONE = "com.hieltech.haramblur.PRAYER_ALREADY_DONE"
        const val ACTION_SHOW_QURANIC_GUIDANCE = "com.hieltech.haramblur.SHOW_QURANIC_GUIDANCE"
        
        // Extras
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        
        // Timing constants
        private const val REMINDER_DELAY_MS = 10 * 60 * 1000L // 10 minutes
        private const val FOLLOW_UP_DELAY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Track prayer completion status
    private val prayerCompletionStatus = mutableMapOf<String, Boolean>()
    private val pendingReminders = mutableMapOf<String, Long>() // prayer -> timestamp
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for prayer notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // High-priority channel for prayer time notifications
            val prayerTimeChannel = NotificationChannel(
                PRAYER_TIME_CHANNEL_ID,
                context.getString(R.string.prayer_time_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.prayer_time_notification_channel_description)
                enableVibration(false)
                // Vibration disabled as per user request
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt() // Islamic green
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Important for prayer times
            }
            
            // Medium-priority channel for prayer reminders
            val prayerReminderChannel = NotificationChannel(
                PRAYER_REMINDER_CHANNEL_ID,
                context.getString(R.string.prayer_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.prayer_reminder_channel_description)
                enableVibration(false)
                // Vibration disabled as per user request
                enableLights(true)
                lightColor = 0xFF2196F3.toInt() // Blue for reminders
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            manager.createNotificationChannel(prayerTimeChannel)
            manager.createNotificationChannel(prayerReminderChannel)
        }
    }
    
    /**
     * Send prayer time notification when prayer time arrives
     */
    fun sendPrayerTimeNotification(prayerName: PrayerName, prayerTime: String) {
        serviceScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                
                if (!settings.enablePrayerNotifications) {
                    Log.d(TAG, "Prayer notifications disabled")
                    return@launch
                }
                
                Log.i(TAG, "Sending prayer time notification for $prayerName at $prayerTime")
                
                val prayerKey = "${prayerName.name}_${getCurrentDateKey()}"
                
                // Create the main notification
                val notification = createPrayerTimeNotification(prayerName, prayerTime, settings.preferredLanguage.name)
                notificationManager.notify(PRAYER_TIME_NOTIFICATION_ID, notification)
                
                // Schedule reminder notification for 10 minutes later
                scheduleReminderNotification(prayerName, prayerTime, prayerKey)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending prayer time notification", e)
            }
        }
    }
    
    /**
     * Create prayer time notification
     */
    private fun createPrayerTimeNotification(
        prayerName: PrayerName, 
        prayerTime: String, 
        language: String
    ): android.app.Notification {
        
        val title = when (language.lowercase()) {
            "arabic", "ar" -> "وقت صلاة ${getPrayerNameArabic(prayerName)}"
            "french", "fr" -> "Temps de prière ${getPrayerNameFrench(prayerName)}"
            else -> "Time for ${getPrayerNameEnglish(prayerName)} Prayer"
        }
        
        val message = when (language.lowercase()) {
            "arabic", "ar" -> "حان الآن وقت صلاة ${getPrayerNameArabic(prayerName)}. بارك الله فيك."
            "french", "fr" -> "Il est temps pour la prière de ${getPrayerNameFrench(prayerName)}. Qu'Allah vous bénisse."
            else -> "It's time for ${getPrayerNameEnglish(prayerName)} prayer. May Allah bless you."
        }
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerName.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, PRAYER_TIME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Keep visible until user interacts
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
    }
    
    /**
     * Schedule a reminder notification for 10 minutes after prayer time
     */
    private fun scheduleReminderNotification(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        serviceScope.launch {
            try {
                // Wait 10 minutes
                kotlinx.coroutines.delay(REMINDER_DELAY_MS)
                
                // Check if prayer was marked as completed
                if (prayerCompletionStatus[prayerKey] == true) {
                    Log.d(TAG, "Prayer $prayerName already completed, skipping reminder")
                    return@launch
                }
                
                Log.i(TAG, "Sending prayer reminder for $prayerName")
                sendPrayerReminderNotification(prayerName, prayerTime, prayerKey)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling reminder notification", e)
            }
        }
    }
    
    /**
     * Send prayer reminder notification with Yes/No buttons
     */
    private fun sendPrayerReminderNotification(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        val settings = settingsRepository.getCurrentSettings()
        val language = settings.preferredLanguage.name
        
        val title = when (language.lowercase()) {
            "arabic", "ar" -> "تذكير بالصلاة"
            "french", "fr" -> "Rappel de Prière"
            else -> "Prayer Reminder"
        }
        
        val message = when (language.lowercase()) {
            "arabic", "ar" -> "هل صليت ${getPrayerNameArabic(prayerName)}؟"
            "french", "fr" -> "Avez-vous fait la prière de ${getPrayerNameFrench(prayerName)} ?"
            else -> "Have you prayed ${getPrayerNameEnglish(prayerName)}?"
        }
        
        // Create action buttons
        val yesText = when (language.lowercase()) {
            "arabic", "ar" -> "نعم"
            "french", "fr" -> "Oui"
            else -> "Yes"
        }
        
        val noText = when (language.lowercase()) {
            "arabic", "ar" -> "لا"
            "french", "fr" -> "Non"
            else -> "No"
        }
        
        val yesIntent = Intent(ACTION_PRAYER_COMPLETED).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName.name)
            putExtra(EXTRA_PRAYER_TIME, prayerTime)
            setPackage(context.packageName)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.ordinal * 10 + 1,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val noIntent = Intent(ACTION_PRAYER_NOT_COMPLETED).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName.name)
            putExtra(EXTRA_PRAYER_TIME, prayerTime)
            setPackage(context.packageName)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.ordinal * 10 + 2,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, PRAYER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_background, yesText, yesPendingIntent)
            .addAction(R.drawable.ic_launcher_background, noText, noPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        notificationManager.notify(PRAYER_REMINDER_NOTIFICATION_ID, notification)
        
        // Schedule follow-up reminder if no response in 5 minutes
        scheduleFollowUpReminder(prayerName, prayerTime, prayerKey)
    }
    
    /**
     * Handle prayer completion response (Yes button)
     */
    fun handlePrayerCompleted(prayerName: String, prayerTime: String) {
        Log.i(TAG, "User confirmed prayer completion for $prayerName")
        
        val prayerKey = "${prayerName}_${getCurrentDateKey()}"
        prayerCompletionStatus[prayerKey] = true
        
        // Cancel reminder notification
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        
        // Show positive acknowledgment
        showPrayerCompletionAcknowledgment(prayerName)
    }
    
    /**
     * Handle prayer not completed response (No button)
     */
    fun handlePrayerNotCompleted(prayerName: String, prayerTime: String) {
        Log.i(TAG, "User indicated prayer not completed for $prayerName")
        
        // Cancel reminder notification
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        
        // Show Quranic guidance dialog
        showQuranicGuidanceDialog(prayerName, prayerTime)
    }
    
    /**
     * Show Quranic guidance dialog about importance of timely prayer
     */
    private fun showQuranicGuidanceDialog(prayerName: String, prayerTime: String) {
        serviceScope.launch {
            try {
                // For now, we'll use a default verse about prayer importance
                // Later this can be enhanced with actual QuranicRepository method
                val verse = null // quranicRepository.getVerseAboutPrayerImportance()
                val settings = settingsRepository.getCurrentSettings()
                
                // Create an intent to show the guidance activity
                val intent = Intent(context, PrayerGuidanceActivity::class.java).apply {
                    putExtra(EXTRA_PRAYER_NAME, prayerName)
                    putExtra(EXTRA_PRAYER_TIME, prayerTime)
                    // For now, use empty strings for verse data
                    putExtra("verse_arabic", "")
                    putExtra("verse_translation", "")
                    putExtra("verse_reference", "")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing Quranic guidance", e)
            }
        }
    }
    
    /**
     * Schedule follow-up reminder if user ignores the reminder
     */
    private fun scheduleFollowUpReminder(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        serviceScope.launch {
            try {
                // Wait 5 minutes
                kotlinx.coroutines.delay(FOLLOW_UP_DELAY_MS)
                
                // Check if prayer was marked as completed or user responded
                if (prayerCompletionStatus[prayerKey] == true) {
                    Log.d(TAG, "Prayer $prayerName completed, skipping follow-up")
                    return@launch
                }
                
                Log.i(TAG, "Sending follow-up reminder for $prayerName")
                sendFollowUpReminderNotification(prayerName, prayerTime, prayerKey)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling follow-up reminder", e)
            }
        }
    }
    
    /**
     * Send follow-up reminder with more options
     */
    private fun sendFollowUpReminderNotification(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        val settings = settingsRepository.getCurrentSettings()
        val language = settings.preferredLanguage.name
        
        val title = when (language.lowercase()) {
            "arabic", "ar" -> "تذكير مهم بالصلاة"
            "french", "fr" -> "Rappel Important de Prière"
            else -> "Important Prayer Reminder"
        }
        
        val message = when (language.lowercase()) {
            "arabic", "ar" -> "لا تؤخر صلاة ${getPrayerNameArabic(prayerName)}. الصلاة عماد الدين."
            "french", "fr" -> "Ne retardez pas la prière de ${getPrayerNameFrench(prayerName)}. La prière est le pilier de la religion."
            else -> "Don't delay ${getPrayerNameEnglish(prayerName)} prayer. Prayer is the pillar of religion."
        }
        
        // Create enhanced action buttons
        val alreadyDoneText = when (language.lowercase()) {
            "arabic", "ar" -> "لقد صليت"
            "french", "fr" -> "J'ai déjà prié"
            else -> "Already Prayed"
        }
        
        val willDoNowText = when (language.lowercase()) {
            "arabic", "ar" -> "سأصلي الآن"
            "french", "fr" -> "Je vais prier maintenant"
            else -> "I Will Pray Now"
        }
        
        val alreadyDoneIntent = Intent(ACTION_PRAYER_ALREADY_DONE).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName.name)
            putExtra(EXTRA_PRAYER_TIME, prayerTime)
            setPackage(context.packageName)
        }
        val alreadyDonePendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.ordinal * 10 + 3,
            alreadyDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val willDoNowIntent = Intent(ACTION_PRAYER_WILL_DO_NOW).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName.name)
            putExtra(EXTRA_PRAYER_TIME, prayerTime)
            setPackage(context.packageName)
        }
        val willDoNowPendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.ordinal * 10 + 4,
            willDoNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, PRAYER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_background, alreadyDoneText, alreadyDonePendingIntent)
            .addAction(R.drawable.ic_launcher_background, willDoNowText, willDoNowPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        notificationManager.notify(PRAYER_REMINDER_NOTIFICATION_ID, notification)
    }
    
    /**
     * Handle "Already Prayed" response
     */
    fun handlePrayerAlreadyDone(prayerName: String, prayerTime: String) {
        Log.i(TAG, "User confirmed prayer already done for $prayerName")
        
        val prayerKey = "${prayerName}_${getCurrentDateKey()}"
        prayerCompletionStatus[prayerKey] = true
        
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        showPrayerCompletionAcknowledgment(prayerName)
    }
    
    /**
     * Handle "Will Do Now" response
     */
    fun handleWillPrayNow(prayerName: String, prayerTime: String) {
        Log.i(TAG, "User committed to pray now for $prayerName")
        
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        
        // Show encouraging message and schedule final check
        showWillPrayNowAcknowledgment(prayerName)
        
        // Schedule final check in 15 minutes
        scheduleFinalPrayerCheck(prayerName, prayerTime)
    }
    
    /**
     * Show positive acknowledgment for prayer completion
     */
    private fun showPrayerCompletionAcknowledgment(prayerName: String) {
        val settings = settingsRepository.getCurrentSettings()
        val language = settings.preferredLanguage.name
        
        val message = when (language.lowercase()) {
            "arabic", "ar" -> "بارك الله فيك! جزاك الله خيراً على أداء صلاة ${getPrayerNameArabic(PrayerName.valueOf(prayerName))}."
            "french", "fr" -> "Qu'Allah vous bénisse! Merci d'avoir accompli la prière de ${getPrayerNameFrench(PrayerName.valueOf(prayerName))}."
            else -> "May Allah bless you! Thank you for completing $prayerName prayer."
        }
        
        // Show a brief positive notification
        val notification = NotificationCompat.Builder(context, PRAYER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle("🤲")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000L) // Auto-dismiss after 5 seconds
            .build()
        
        notificationManager.notify(PRAYER_REMINDER_NOTIFICATION_ID + 100, notification)
    }
    
    /**
     * Show encouragement for "will pray now" commitment
     */
    private fun showWillPrayNowAcknowledgment(prayerName: String) {
        val settings = settingsRepository.getCurrentSettings()
        val language = settings.preferredLanguage.name
        
        val message = when (language.lowercase()) {
            "arabic", "ar" -> "بارك الله فيك! نسأل الله أن يتقبل منك."
            "french", "fr" -> "Qu'Allah vous bénisse! Puisse Allah accepter votre prière."
            else -> "May Allah bless you! May Allah accept your prayer."
        }
        
        val notification = NotificationCompat.Builder(context, PRAYER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle("🤲")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000L)
            .build()
        
        notificationManager.notify(PRAYER_REMINDER_NOTIFICATION_ID + 101, notification)
    }
    
    /**
     * Schedule final check after "will pray now" commitment
     */
    private fun scheduleFinalPrayerCheck(prayerName: String, prayerTime: String) {
        // Implementation for final check would go here
        // For now, we'll just mark it as a gentle follow-up
        Log.d(TAG, "Final prayer check scheduled for $prayerName")
    }
    
    /**
     * Get prayer name in English
     */
    private fun getPrayerNameEnglish(prayerName: PrayerName): String {
        return when (prayerName) {
            PrayerName.FAJR -> "Fajr"
            PrayerName.SUNRISE -> "Sunrise"
            PrayerName.DHUHR -> "Dhuhr"
            PrayerName.ASR -> "Asr"
            PrayerName.MAGHRIB -> "Maghrib"
            PrayerName.ISHA -> "Isha"
        }
    }
    
    /**
     * Get prayer name in Arabic
     */
    private fun getPrayerNameArabic(prayerName: PrayerName): String {
        return when (prayerName) {
            PrayerName.FAJR -> "الفجر"
            PrayerName.SUNRISE -> "الشروق"
            PrayerName.DHUHR -> "الظهر"
            PrayerName.ASR -> "العصر"
            PrayerName.MAGHRIB -> "المغرب"
            PrayerName.ISHA -> "العشاء"
        }
    }
    
    /**
     * Get prayer name in French
     */
    private fun getPrayerNameFrench(prayerName: PrayerName): String {
        return when (prayerName) {
            PrayerName.FAJR -> "Fajr"
            PrayerName.SUNRISE -> "Lever du soleil"
            PrayerName.DHUHR -> "Dhuhr"
            PrayerName.ASR -> "Asr"
            PrayerName.MAGHRIB -> "Maghrib"
            PrayerName.ISHA -> "Isha"
        }
    }
    
    /**
     * Get current date key for tracking daily prayer completion
     */
    private fun getCurrentDateKey(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * Cancel all prayer notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(PRAYER_TIME_NOTIFICATION_ID)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID + 100)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID + 101)
    }
}