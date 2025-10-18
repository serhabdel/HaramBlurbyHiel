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
import com.hieltech.haramblur.detection.Language
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
        private const val ADVANCE_NOTIFICATION_BASE_ID = PRAYER_TIME_NOTIFICATION_ID
        private const val QURANIC_GUIDANCE_NOTIFICATION_ID = PRAYER_REMINDER_NOTIFICATION_ID + 200
        
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
        
        // Notification type constants
        private const val NOTIF_TYPE_10MIN = "10min"
        private const val NOTIF_TYPE_5MIN = "5min"
        private const val NOTIF_TYPE_AT_TIME = "attime"
        private const val NOTIF_TYPE_LATE = "late"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs = context.getSharedPreferences("prayer_completion_prefs", Context.MODE_PRIVATE)
    private val followUpPrefs = context.getSharedPreferences("prayer_followup_prefs", Context.MODE_PRIVATE)
    private val notificationTrackingPrefs = context.getSharedPreferences("prayer_notification_tracking", Context.MODE_PRIVATE)
    
    // Track prayer completion status
    private val prayerCompletionStatus = mutableMapOf<String, Boolean>()
    private val pendingReminders = mutableMapOf<String, Long>() // prayer -> timestamp
    
    init {
        createNotificationChannels()
        cleanupOldPrayerCompletionData()
        
        // Initialize notification tracking
        if (!notificationTrackingPrefs.contains("last_reset_date")) {
            notificationTrackingPrefs.edit().putString("last_reset_date", getCurrentDateKey()).apply()
        }
        checkAndResetIfDateChanged()
        scheduleDailyReset()
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
                
                // Check and reset if date changed
                checkAndResetIfDateChanged()
                
                // Check if notification already sent
                if (hasNotificationBeenSent(prayerName.name, NOTIF_TYPE_AT_TIME)) {
                    Log.d(TAG, "Prayer time notification for $prayerName already sent today, skipping")
                    return@launch
                }
                
                Log.i(TAG, "Sending prayer time notification for $prayerName at $prayerTime")
                
                // Create the main notification
                val notification = createPrayerTimeNotification(prayerName, prayerTime, settings.preferredLanguage.name)
                notificationManager.notify(PRAYER_TIME_NOTIFICATION_ID, notification)
                
                // Mark notification as sent
                markNotificationAsSent(prayerName.name, NOTIF_TYPE_AT_TIME)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending prayer time notification", e)
                // Don't mark as sent if there was an error
            }
        }
    }
    
    /**
     * Send advance notification for upcoming prayer
     * @param prayerName The prayer name
     * @param minutesUntil How many minutes until prayer time (10 or 5)
     * @param language The user's preferred language
     */
    fun sendAdvanceNotification(prayerName: PrayerName, minutesUntil: Int, language: String) {
        try {
            // Check if prayer notifications are enabled
            val settings = settingsRepository.getCurrentSettings()
            if (!settings.enablePrayerNotifications) {
                Log.d(TAG, "Prayer notifications disabled, skipping advance notification")
                return
            }
            
            // Check and reset if date changed
            checkAndResetIfDateChanged()
            
            // Determine notification type and check if already sent
            val notificationType = if (minutesUntil == 10) NOTIF_TYPE_10MIN else NOTIF_TYPE_5MIN
            if (hasNotificationBeenSent(prayerName.name, notificationType)) {
                Log.d(TAG, "Advance notification for $prayerName ($minutesUntil min) already sent today, skipping")
                return
            }
            
            Log.i(TAG, "Sending advance notification for $prayerName in $minutesUntil minutes (language: $language)")
            
            // Get localized strings based on minutes
            val titleResId = if (minutesUntil == 10) R.string.prayer_advance_10min_text else R.string.prayer_advance_5min_text
            val messageResId = if (minutesUntil == 10) R.string.prayer_advance_10min_message else R.string.prayer_advance_5min_message
            
            val title = context.getString(titleResId, getPrayerNameForCurrentLocale(prayerName))
            val message = context.getString(messageResId)
            
            // Create PendingIntent to MainActivity
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                prayerName.ordinal * 100 + minutesUntil,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(context, PRAYER_TIME_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_islamic)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()
            
            // Use unique notification ID based on prayer name and minutes
            val uniqueNotificationId = ADVANCE_NOTIFICATION_BASE_ID + (prayerName.ordinal * 100) + minutesUntil
            
            // Send the notification
            notificationManager.notify(uniqueNotificationId, notification)
            Log.d(TAG, "Advance notification sent successfully with ID: $uniqueNotificationId")
            
            // Mark notification as sent
            markNotificationAsSent(prayerName.name, notificationType)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending advance notification for $prayerName in $minutesUntil minutes", e)
            // Don't mark as sent if there was an error
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
     * Send prayer reminder notification (private implementation)
     */
    private fun sendPrayerReminderNotification(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        // Get localized strings
        val title = context.getString(R.string.prayer_reminder_title_bilingual)
        val message = context.getString(
            R.string.prayer_reminder_question_bilingual,
            getPrayerNameEnglish(prayerName),
            getPrayerNameArabic(prayerName)
        )
        
        // Get localized action button labels
        val yesText = context.getString(R.string.prayer_action_yes_bilingual)
        val noText = context.getString(R.string.prayer_action_no_bilingual)
        
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
        
        // Schedule follow-up reminder in 5 minutes
        scheduleFollowUpCheck(prayerName, 5)
        
        // Show Quranic guidance notification
        showPrayerGuidanceNotification(prayerName, prayerTime)
    }
    
    /**
     * Show prayer guidance notification with Quranic verse about timely prayer.
     * This notification is persistent (non-dismissible) and requires user action.
     * 
     * The notification includes:
     * - Bilingual encouraging message (Arabic + English)
     * - Random Quranic verse about prayer timeliness
     * - Full verse text in Arabic with English translation
     * - Reflection on the verse's meaning
     * - "I Will Pray Now" action button to dismiss
     * 
     * @param prayerName The name of the prayer that was missed
     * @param prayerTime The time of the prayer
     */
    private fun showPrayerGuidanceNotification(prayerName: String, prayerTime: String) {
        serviceScope.launch {
            try {
                // Fetch a random prayer verse from QuranicRepository
                val verse = quranicRepository.getPrayerTimelinessVerse()
                val settings = settingsRepository.getCurrentSettings()
                val language = settings.preferredLanguage
                
                // Get localized strings
                val title = context.getString(R.string.prayer_guidance_title_bilingual)
                
                // Create notification message with localized encouragement and verse reference
                val encouragingMessage = context.getString(R.string.prayer_guidance_encouragement_bilingual) + "\n\n"
                val verseReference = context.getString(R.string.prayer_guidance_verse_reference, verse.surahNumber, verse.verseNumber) + "\n\n"
                val arabicText = "${verse.arabicText}\n\n"
                val englishTranslation = "${verse.translations[Language.ENGLISH]}\n\n"
                val reflection = verse.reflection
                
                val fullMessage = encouragingMessage + verseReference + arabicText + englishTranslation + reflection
                
                // Create content intent to open MainActivity
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    QURANIC_GUIDANCE_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Create "I Will Pray Now" action button
                val willPrayIntent = Intent(ACTION_PRAYER_WILL_DO_NOW).apply {
                    putExtra(EXTRA_PRAYER_NAME, prayerName)
                    putExtra(EXTRA_PRAYER_TIME, prayerTime)
                    setPackage(context.packageName)
                }
                val willPrayPendingIntent = PendingIntent.getBroadcast(
                    context,
                    QURANIC_GUIDANCE_NOTIFICATION_ID + 1,
                    willPrayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Build the notification
                val notification = NotificationCompat.Builder(context, PRAYER_REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_shield_islamic)
                    .setContentTitle(title)
                    .setContentText("Quranic guidance about timely prayer")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
                    .addAction(
                        R.drawable.ic_launcher_background,
                        "سأصلي الآن | I Will Pray Now",
                        willPrayPendingIntent
                    )
                    .build()
                
                // Send the notification
                notificationManager.notify(QURANIC_GUIDANCE_NOTIFICATION_ID, notification)
                Log.d(TAG, "Prayer guidance notification sent with verse ${verse.surahNumber}:${verse.verseNumber}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing prayer guidance notification", e)
            }
        }
    }
    
    /**
     * Schedule follow-up reminder if user ignores the reminder
     */
    private fun scheduleFollowUpReminder(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        // Use persistent tracking instead of coroutine delays
        scheduleFollowUpCheck(prayerName.name, 5)
        Log.d(TAG, "Scheduled follow-up check for $prayerName in 5 minutes")
    }
    
    /**
     * Send follow-up reminder with more options
     */
    fun sendFollowUpReminderNotification(prayerName: PrayerName, prayerTime: String, prayerKey: String) {
        // Check if prayer is already completed
        if (isPrayerCompleted(prayerKey)) {
            Log.d(TAG, "Prayer $prayerName already completed, skipping follow-up")
            return
        }
        val settings = settingsRepository.getCurrentSettings()
        val language = settings.preferredLanguage.name
        
        // Get localized strings
        val title = context.getString(R.string.prayer_followup_title_bilingual)
        val message = context.getString(
            R.string.prayer_followup_message_bilingual,
            getPrayerNameEnglish(prayerName),
            getPrayerNameArabic(prayerName)
        )
        
        // Get localized action button labels
        val alreadyDoneText = context.getString(R.string.prayer_action_already_prayed_bilingual)
        val willDoNowText = context.getString(R.string.prayer_action_will_pray_now_bilingual)
        
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
        notificationManager.cancel(QURANIC_GUIDANCE_NOTIFICATION_ID)
        
        // Show encouraging message and schedule final check
        showWillPrayNowAcknowledgment(prayerName)
        
        // Schedule final check in 15 minutes
        scheduleFinalPrayerCheck(prayerName, prayerTime)
    }
    
    /**
     * Show positive acknowledgment for prayer completion
     */
    private fun showPrayerCompletionAcknowledgment(prayerName: String) {
        // Get localized message
        val message = context.getString(R.string.prayer_completion_acknowledgment_bilingual)
        
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
        // Get localized message
        val message = context.getString(R.string.prayer_will_pray_acknowledgment_bilingual)
        
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
     * Schedule final prayer check after user commits to praying
     */
    private fun scheduleFinalPrayerCheck(prayerName: String, prayerTime: String) {
        scheduleFollowUpCheck(prayerName, 15)
        Log.d(TAG, "Scheduled final prayer check for $prayerName in 15 minutes")
    }
    
    /**
     * Cleanup old prayer completion data (older than 7 days)
     */
    private fun cleanupOldPrayerCompletionData() {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(getCurrentDateKey())
            val sevenDaysAgo = currentDate!!.time - (7 * 24 * 60 * 60 * 1000L)
            
            val allKeys = prefs.all.keys
            var cleanedCount = 0
            
            allKeys.forEach { key ->
                // Extract date from key format: {prayerName}_{yyyy-MM-dd}
                val parts = key.split("_")
                if (parts.size >= 2) {
                    val dateStr = parts.takeLast(3).joinToString("-")
                    try {
                        val keyDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                        if (keyDate != null && keyDate.time < sevenDaysAgo) {
                            prefs.edit().remove(key).apply()
                            cleanedCount++
                        }
                    } catch (e: Exception) {
                        // Skip invalid date formats
                    }
                }
            }
            
            if (cleanedCount > 0) {
                Log.d(TAG, "Cleaned up $cleanedCount old prayer completion entries")
            }
            
            // Cleanup old follow-up entries (older than 1 day)
            val oneDayAgo = currentDate!!.time - (24 * 60 * 60 * 1000L)
            val followUpKeys = followUpPrefs.all.keys
            var followUpCleanedCount = 0
            
            followUpKeys.forEach { key ->
                if (key.startsWith("followup_")) {
                    // Extract date from key format: followup_{prayerName}_{yyyy-MM-dd}
                    val parts = key.split("_")
                    if (parts.size >= 4) {
                        val dateStr = parts.takeLast(3).joinToString("-")
                        try {
                            val keyDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                            if (keyDate != null && keyDate.time < oneDayAgo) {
                                followUpPrefs.edit().remove(key).apply()
                                followUpCleanedCount++
                            }
                        } catch (e: Exception) {
                            // Skip invalid date formats
                        }
                    }
                }
            }
            
            if (followUpCleanedCount > 0) {
                Log.d(TAG, "Cleaned up $followUpCleanedCount old follow-up entries")
            }
            
            // Cleanup old notification tracking entries (older than 1 day)
            val notifTrackingKeys = notificationTrackingPrefs.all.keys
            var notifTrackingCleanedCount = 0
            
            notifTrackingKeys.forEach { key ->
                if (key.startsWith("notif_") && key != "last_reset_date") {
                    // Extract date from key format: notif_{prayerName}_{notificationType}_{yyyy-MM-dd}
                    val parts = key.split("_")
                    if (parts.size >= 4) {
                        val dateStr = parts.takeLast(3).joinToString("-")
                        try {
                            val keyDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                            if (keyDate != null && keyDate.time < oneDayAgo) {
                                notificationTrackingPrefs.edit().remove(key).apply()
                                notifTrackingCleanedCount++
                            }
                        } catch (e: Exception) {
                            // Skip invalid date formats
                        }
                    }
                }
            }
            
            if (notifTrackingCleanedCount > 0) {
                Log.d(TAG, "Cleaned up $notifTrackingCleanedCount old notification tracking entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old prayer completion data", e)
        }
    }
    
    /**
     * Schedule a follow-up check for a prayer
     * @param prayerName The prayer name
     * @param followUpDelayMinutes Delay in minutes (5 for regular follow-up, 15 for final check)
     */
    fun scheduleFollowUpCheck(prayerName: String, followUpDelayMinutes: Int) {
        val followUpTimestamp = System.currentTimeMillis() + (followUpDelayMinutes * 60 * 1000L)
        val dateKey = getCurrentDateKey()
        val key = "followup_${prayerName}_$dateKey"
        
        followUpPrefs.edit().putLong(key, followUpTimestamp).apply()
        Log.d(TAG, "Scheduled follow-up for $prayerName in $followUpDelayMinutes minutes (timestamp: $followUpTimestamp)")
    }
    
    /**
     * Get all pending follow-ups
     * @return Map of prayer keys to timestamps
     */
    fun getPendingFollowUps(): Map<String, Long> {
        val pendingFollowUps = mutableMapOf<String, Long>()
        
        followUpPrefs.all.forEach { (key, value) ->
            if (key.startsWith("followup_") && value is Long) {
                pendingFollowUps[key] = value
            }
        }
        
        return pendingFollowUps
    }
    
    /**
     * Clear a follow-up for a specific prayer
     * @param prayerName The prayer name
     */
    fun clearFollowUp(prayerName: String) {
        val dateKey = getCurrentDateKey()
        val key = "followup_${prayerName}_$dateKey"
        
        followUpPrefs.edit().remove(key).apply()
        Log.d(TAG, "Cleared follow-up for $prayerName")
    }
    
    /**
     * Check and send pending follow-up reminders
     * Called by PrayerNotificationWorker during periodic checks
     */
    fun checkAndSendPendingFollowUps() {
        try {
            val currentTime = System.currentTimeMillis()
            val pendingFollowUps = getPendingFollowUps()
            
            pendingFollowUps.forEach { (key, timestamp) ->
                if (currentTime >= timestamp) {
                    // Extract prayer name from key format: followup_{prayerName}_{date}
                    val parts = key.split("_")
                    if (parts.size >= 2) {
                        val prayerName = parts[1]
                        val dateKey = parts.drop(2).joinToString("_")
                        
                        try {
                            val prayerEnum = PrayerName.valueOf(prayerName.uppercase())
                            val prayerKey = "${prayerName}_$dateKey"
                            
                            Log.i(TAG, "Follow-up time reached for $prayerName, sending reminder")
                            sendFollowUpReminderNotification(prayerEnum, "", prayerKey)
                            
                            // Clear the follow-up entry
                            followUpPrefs.edit().remove(key).apply()
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid prayer name in follow-up key: $prayerName", e)
                            // Remove invalid entry
                            followUpPrefs.edit().remove(key).apply()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending follow-ups", e)
        }
    }
    
    /**
     * Cancel all prayer notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(PRAYER_TIME_NOTIFICATION_ID)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID + 100)
        notificationManager.cancel(PRAYER_REMINDER_NOTIFICATION_ID + 101)
        notificationManager.cancel(QURANIC_GUIDANCE_NOTIFICATION_ID)
        
        // Cancel all advance notifications for all prayers
        PrayerName.values().forEach { prayerName ->
            // Cancel 10-minute advance notification
            val id10min = ADVANCE_NOTIFICATION_BASE_ID + (prayerName.ordinal * 100) + 10
            notificationManager.cancel(id10min)
            
            // Cancel 5-minute advance notification
            val id5min = ADVANCE_NOTIFICATION_BASE_ID + (prayerName.ordinal * 100) + 5
            notificationManager.cancel(id5min)
        }
        
        // Clear all pending follow-ups
        followUpPrefs.edit().clear().apply()
        
        // Clear all notification tracking
        notificationTrackingPrefs.edit().clear().apply()
        notificationTrackingPrefs.edit().putString("last_reset_date", getCurrentDateKey()).apply()
        Log.d(TAG, "Notification tracking cleared")
    }
    
    
    /**
     * Send prayer reminder notification (public version)
     * Called by PrayerNotificationWorker when prayer is 10 minutes late
     * @param prayerName The prayer name
     */
    fun sendPrayerReminderNotification(prayerName: PrayerName) {
        serviceScope.launch {
            try {
                // Check and reset if date changed
                checkAndResetIfDateChanged()
                
                // Check if notification already sent
                if (hasNotificationBeenSent(prayerName.name, NOTIF_TYPE_LATE)) {
                    Log.d(TAG, "Late reminder for $prayerName already sent today, skipping")
                    return@launch
                }
                
                val dateKey = getCurrentDateKey()
                val prayerKey = "${prayerName.name}_$dateKey"
                
                // Check if prayer is already completed
                if (isPrayerCompleted(prayerKey)) {
                    Log.d(TAG, "Prayer $prayerName already completed, skipping reminder")
                    return@launch
                }
                
                Log.i(TAG, "Sending 10-minute late reminder for $prayerName")
                
                // Get prayer time
                val prayerTimesResult = prayerTimesRepository.getPrayerTimes()
                val prayerTime = prayerTimesResult.getOrNull()?.let { prayerData ->
                    when (prayerName) {
                        PrayerName.FAJR -> prayerData.timings.Fajr
                        PrayerName.DHUHR -> prayerData.timings.Dhuhr
                        PrayerName.ASR -> prayerData.timings.Asr
                        PrayerName.MAGHRIB -> prayerData.timings.Maghrib
                        PrayerName.ISHA -> prayerData.timings.Isha
                        else -> ""
                    }
                } ?: ""
                
                // Call the private implementation
                sendPrayerReminderNotification(prayerName, prayerTime, prayerKey)
                
                // Mark notification as sent
                markNotificationAsSent(prayerName.name, NOTIF_TYPE_LATE)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending prayer reminder notification for $prayerName", e)
                // Don't mark as sent if there was an error
            }
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
     * Check if prayer is completed (checks both in-memory and persisted status)
     */
    private fun isPrayerCompleted(prayerKey: String): Boolean {
        return prayerCompletionStatus[prayerKey] == true || prefs.getBoolean(prayerKey, false)
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
     * Get prayer name for current device locale
     */
    private fun getPrayerNameForCurrentLocale(prayerName: PrayerName): String {
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (locale.language) {
            "ar" -> getPrayerNameArabic(prayerName)
            "fr" -> getPrayerNameFrench(prayerName)
            else -> getPrayerNameEnglish(prayerName)
        }
    }
    
    /**
     * Check if a notification has been sent today
     */
    fun hasNotificationBeenSent(prayerName: String, notificationType: String): Boolean {
        val dateKey = getCurrentDateKey()
        val key = "notif_${prayerName}_${notificationType}_$dateKey"
        val result = notificationTrackingPrefs.getBoolean(key, false)
        Log.d(TAG, "Checking notification status: $key = $result")
        return result
    }
    
    /**
     * Mark a notification as sent
     */
    fun markNotificationAsSent(prayerName: String, notificationType: String) {
        val dateKey = getCurrentDateKey()
        val key = "notif_${prayerName}_${notificationType}_$dateKey"
        notificationTrackingPrefs.edit().putBoolean(key, true).apply()
        Log.d(TAG, "Marked notification as sent: $prayerName - $notificationType")
    }
    
    /**
     * Reset daily notification tracking (clears all notification tracking)
     */
    fun resetDailyNotificationTracking() {
        notificationTrackingPrefs.edit().clear().apply()
        // Reinitialize last reset date
        notificationTrackingPrefs.edit().putString("last_reset_date", getCurrentDateKey()).apply()
        Log.d(TAG, "Daily notification tracking reset completed")
    }
    
    /**
     * Schedule daily reset at midnight
     */
    private fun scheduleDailyReset() {
        serviceScope.launch {
            try {
                // Calculate milliseconds until next midnight
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val delayMillis = calendar.timeInMillis - System.currentTimeMillis()
                Log.d(TAG, "Scheduled daily reset in ${delayMillis / 1000 / 60} minutes (at midnight)")
                
                // Wait until midnight
                kotlinx.coroutines.delay(delayMillis)
                
                // Reset notification tracking
                resetDailyNotificationTracking()
                
                // Schedule next day's reset
                scheduleDailyReset()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling daily reset", e)
            }
        }
    }
    
    /**
     * Check if date has changed and reset if needed
     */
    private fun checkAndResetIfDateChanged() {
        val lastResetDate = notificationTrackingPrefs.getString("last_reset_date", "")
        val currentDateKey = getCurrentDateKey()
        
        if (lastResetDate != currentDateKey) {
            Log.d(TAG, "Date changed from $lastResetDate to $currentDateKey, resetting notification tracking")
            resetDailyNotificationTracking()
        }
    }
    
    /**
     * Get notification tracking status (for debugging/testing)
     */
    fun getNotificationTrackingStatus(): Map<String, Boolean> {
        return notificationTrackingPrefs.all
            .filterKeys { it.startsWith("notif_") }
            .mapValues { it.value as? Boolean ?: false }
    }
    
    /**
     * Manually reset notification tracking (for debugging/testing)
     */
    fun manuallyResetNotificationTracking() {
        resetDailyNotificationTracking()
        Log.d(TAG, "Manual notification tracking reset triggered")
    }
}