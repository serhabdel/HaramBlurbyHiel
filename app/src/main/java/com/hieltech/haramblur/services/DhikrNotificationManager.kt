package com.hieltech.haramblur.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrSettings
import com.hieltech.haramblur.data.DhikrTime
import com.hieltech.haramblur.data.getDisplayName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.hieltech.haramblur.services.DhikrNotificationReceiver.Companion.ACTION_DISMISS
import com.hieltech.haramblur.services.DhikrNotificationReceiver.Companion.ACTION_NEXT
import com.hieltech.haramblur.services.DhikrNotificationReceiver.Companion.ACTION_SHOW_NOW

/**
 * Manages dhikr notifications as fallback when overlay is not available
 */
@Singleton
class DhikrNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "DhikrNotificationManager"
        private const val DHIKR_CHANNEL_ID = "dhikr_channel"
        private const val STATUS_CHANNEL_ID = "dhikr_status_channel"
        private const val DHIKR_NOTIFICATION_ID = 1001
        private const val STATUS_NOTIFICATION_ID = 1002
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android 8.0+ with separate channels for dhikr and status
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // High-priority channel for actual dhikr notifications
            val dhikrChannel = NotificationChannel(
                DHIKR_CHANNEL_ID,
                "Dhikr Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Islamic remembrance notifications"
                enableVibration(false)
                // Vibration disabled as per user request
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt() // Islamic green color
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC // Show on lockscreen
                setBypassDnd(true) // Bypass Do Not Disturb for important dhikrs
            }

            // Low-priority silent channel for status notifications
            val statusChannel = NotificationChannel(
                STATUS_CHANNEL_ID,
                "Dhikr Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent status notifications showing dhikr countdown"
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null) // No sound
            }

            manager.createNotificationChannel(dhikrChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    /**
     * Show dhikr as notification with expandable content and smart heads-up
     */
    fun showDhikrNotification(dhikr: Dhikr, settings: DhikrSettings) {
        val notification = createDhikrNotification(dhikr, settings)

        // Use heads-up for important dhikrs at appropriate times
        if (shouldUseHeadsUp(dhikr, settings)) {
            // Force heads-up by using a higher priority notification
            val headsUpNotification = NotificationCompat.Builder(context, DHIKR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_islamic)
                .setContentTitle("🔔 ${dhikr.time.getDisplayName(context)} Dhikr")
                .setContentText(dhikr.arabicText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setTimeoutAfter(settings.displayDurationSeconds * 1000L)
                .setContentIntent(notification.contentIntent)
                .setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🔔 ${dhikr.time.getDisplayName(context)} Dhikr")
                    .bigText("${dhikr.arabicText}\n\n${if (settings.showTranslation) dhikr.englishTranslation else ""}"))
                .build()

            notificationManager.notify(DHIKR_NOTIFICATION_ID, headsUpNotification)
        } else {
            notificationManager.notify(DHIKR_NOTIFICATION_ID, notification)
        }
    }

    /**
     * Create rich dhikr notification
     */
    private fun createDhikrNotification(dhikr: Dhikr, settings: DhikrSettings): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(ACTION_DISMISS).apply {
            setPackage(context.packageName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(ACTION_NEXT).apply {
            setPackage(context.packageName)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, DHIKR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle("${dhikr.time.getDisplayName(context)} Dhikr")
            .setContentText(dhikr.arabicText)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Reduced from MAX to prevent sticking
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(settings.displayDurationSeconds * 1000L) // Use settings duration
            .setContentIntent(pendingIntent)
            .setOngoing(false) // Ensure notification is not persistent
            .addAction(R.drawable.ic_launcher_background, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_launcher_background, "Next", nextPendingIntent)

        // Add expandable content
        val bigTextStyle = NotificationCompat.BigTextStyle()

        val contentBuilder = StringBuilder()
        contentBuilder.append(dhikr.arabicText)
        contentBuilder.append("\n\n")

        if (settings.showTransliteration) {
            contentBuilder.append(dhikr.transliteration)
            contentBuilder.append("\n\n")
        }

        if (settings.showTranslation) {
            contentBuilder.append(dhikr.englishTranslation)
            contentBuilder.append("\n\n")
        }

        contentBuilder.append("Category: ${dhikr.category}")

        bigTextStyle.bigText(contentBuilder.toString())
        builder.setStyle(bigTextStyle)

        return builder.build()
    }

    /**
     * Determine if notification should use heads-up style
     */
    private fun shouldUseHeadsUp(dhikr: Dhikr, settings: DhikrSettings): Boolean {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when {
            // Always heads-up for morning dhikrs during morning hours (5-10 AM)
            dhikr.time == DhikrTime.MORNING && currentHour in 5..10 -> true

            // Always heads-up for evening dhikrs during evening hours (5-10 PM)
            dhikr.time == DhikrTime.EVENING && currentHour in 17..22 -> true

            // Heads-up for anytime dhikrs if user has been inactive
            dhikr.time == DhikrTime.ANYTIME && currentHour in 9..21 -> true

            // Default: use standard notification
            else -> false
        }
    }

    /**
     * Show persistent status notification (DISABLED - was causing stuck notifications)
     */
    fun showStatusNotification(
        nextDhikrTime: String,
        dailyCount: Int,
        isEnabled: Boolean
    ) {
        // Status notifications disabled to prevent stuck notifications
        android.util.Log.d(TAG, "Status notification disabled to prevent stuck notifications")
        // Don't show persistent status notifications - they cause issues
    }

    /**
     * Create status notification showing next dhikr time
     */
    private fun createStatusNotification(
        nextDhikrTime: String,
        dailyCount: Int,
        isEnabled: Boolean
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showNowIntent = Intent(ACTION_SHOW_NOW).apply {
            setPackage(context.packageName)
        }
        val showNowPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            showNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isEnabled) {
            "Next dhikr: $nextDhikrTime"
        } else {
            "Dhikr disabled"
        }

        val builder = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_islamic)
            .setContentTitle("Dhikr Reminder")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (isEnabled) {
            builder.addAction(R.drawable.ic_launcher_background, "Show Now", showNowPendingIntent)
        }

        // Add subtext with daily count
        builder.setSubText("Today's dhikrs: $dailyCount")

        return builder.build()
    }

    /**
     * Cancel dhikr notification
     */
    fun cancelDhikrNotification() {
        notificationManager.cancel(DHIKR_NOTIFICATION_ID)
    }

    /**
     * Cancel status notification
     */
    fun cancelStatusNotification() {
        notificationManager.cancel(STATUS_NOTIFICATION_ID)
    }

    /**
     * Cancel all dhikr notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(DHIKR_NOTIFICATION_ID)
        notificationManager.cancel(STATUS_NOTIFICATION_ID)
    }
}
