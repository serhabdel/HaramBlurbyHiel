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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
        private const val DHIKR_NOTIFICATION_ID = 1001
        private const val STATUS_NOTIFICATION_ID = 1002

        // Action constants
        private const val ACTION_DISMISS = "com.hieltech.haramblur.DHIKR_DISMISS"
        private const val ACTION_NEXT = "com.hieltech.haramblur.DHIKR_NEXT"
        private const val ACTION_SHOW_NOW = "com.hieltech.haramblur.DHIKR_SHOW_NOW"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DHIKR_CHANNEL_ID,
                "Dhikr Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Islamic remembrance notifications"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Show dhikr as notification with expandable content
     */
    fun showDhikrNotification(dhikr: Dhikr, settings: DhikrSettings) {
        val notification = createDhikrNotification(dhikr, settings)
        notificationManager.notify(DHIKR_NOTIFICATION_ID, notification)
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
            .setContentTitle("${dhikr.time.displayName} Dhikr")
            .setContentText(dhikr.arabicText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
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
     * Show persistent status notification
     */
    fun showStatusNotification(
        nextDhikrTime: String,
        dailyCount: Int,
        isEnabled: Boolean
    ) {
        val notification = createStatusNotification(nextDhikrTime, dailyCount, isEnabled)
        notificationManager.notify(STATUS_NOTIFICATION_ID, notification)
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

        val builder = NotificationCompat.Builder(context, DHIKR_CHANNEL_ID)
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