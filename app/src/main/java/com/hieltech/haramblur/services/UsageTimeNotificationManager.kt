package com.hieltech.haramblur.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.AppUsageTracker
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages usage time notifications with Quranic guidance integration
 */
@Singleton
class UsageTimeNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quranicRepository: QuranicRepository,
    private val appUsageTracker: AppUsageTracker,
    private val appCategoryMapper: AppCategoryMapper
) {

    companion object {
        private const val TAG = "UsageTimeNotificationManager"

        // Notification channels
        private const val USAGE_TIME_CHANNEL_ID = "usage_time_channel"
        private const val USAGE_TIME_STATUS_CHANNEL_ID = "usage_time_status_channel"

        // Notification IDs
        const val USAGE_TIME_NOTIFICATION_ID = 2001
        const val USAGE_TIME_STATUS_ID = 2002
        const val USAGE_NOTIFICATION_ID_START = 2200
        const val USAGE_NOTIFICATION_ID_END = 2299

        // Colors (Islamic green theme)
        private const val ISLAMIC_GREEN = 0xFF4CAF50
        private const val ISLAMIC_DARK_GREEN = 0xFF2E7D32

        // Notification limits
        private const val MAX_NOTIFICATIONS_PER_DAY = 5
        private const val NOTIFICATION_FREQUENCY_MINUTES = 30
        private const val REFLECTION_TIME_SECONDS = 15
    }

    // Notification state tracking
    private val notificationStates = mutableMapOf<String, UsageTimeNotificationState>()
    private val notificationManager = NotificationManagerCompat.from(context)

    // Coroutine scope for managing long-lived operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dailyResetJob: Job? = null

    init {
        createNotificationChannels()
        startDailyResetMonitoring()
    }

    /**
     * Create notification channels for different types of usage notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val usageTimeChannel = NotificationChannel(
                USAGE_TIME_CHANNEL_ID,
                "Usage Time Limits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when app usage limits are exceeded"
                enableVibration(true)
                lightColor = ISLAMIC_GREEN.toInt()
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val statusChannel = NotificationChannel(
                USAGE_TIME_STATUS_CHANNEL_ID,
                "Usage Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing usage status and reminders"
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(usageTimeChannel, statusChannel))
        }
    }

    /**
     * Check if notification should be shown for app based on AppUsageTracker's logic
     */
    fun shouldShowNotification(packageName: String): Boolean {
        return appUsageTracker.shouldShowNotification(packageName)
    }

    /**
     * Show notification when time limit is exceeded
     */
    suspend fun showTimeLimitExceededNotification(
        packageName: String,
        appName: String,
        timeUsed: Int,
        timeLimit: Int
    ) {
        try {
            if (!shouldShowNotification(packageName)) {
                Log.d(TAG, "Skipping notification for $packageName - frequency limit")
                return
            }

            // Get app category and Quranic verse
            val appCategory = appUsageTracker.getAppCategoryFor(packageName)
            val blockingCategory = appCategoryMapper.mapToBlockingCategoryOrDefault(appCategory)
            val quranicVerse = getRelevantVerse(packageName, blockingCategory)

            // Build notification
            val notification = buildTimeLimitNotification(
                packageName, appName, timeUsed, timeLimit, appCategory, quranicVerse
            )

            // Show notification
            notificationManager.notify(
                getNotificationId(packageName),
                notification
            )

            // Update notification state
            val verseId = quranicVerse?.id ?: "default"
            updateNotificationState(packageName, verseId)

            // Record in usage tracker
            appUsageTracker.recordNotificationShown(packageName)

            Log.i(TAG, "Shown time limit notification for $appName (used: ${timeUsed}m, limit: ${timeLimit}m)")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing time limit notification for $packageName", e)
        }
    }

    /**
     * Build comprehensive time limit notification with Quranic guidance
     */
    private suspend fun buildTimeLimitNotification(
        packageName: String,
        appName: String,
        timeUsed: Int,
        timeLimit: Int,
        appCategory: AppCategory?,
        quranicVerse: com.hieltech.haramblur.data.QuranicVerse?
    ): Notification {
        val safeAppCategory = appCategory ?: AppCategory.BROWSERS
        val guidanceMessage = appCategoryMapper.getGuidanceMessage(safeAppCategory, timeUsed, timeLimit)
        val recommendedActions = appCategoryMapper.getRecommendedActions(safeAppCategory)

        val builder = NotificationCompat.Builder(context, USAGE_TIME_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setColor(ISLAMIC_GREEN.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Title with Islamic encouragement
        val title = "⏰ Time Limit Exceeded - $appName"
        builder.setContentTitle(title)

        // Main content
        val overageMinutes = timeUsed - timeLimit
        val mainContent = "Used ${timeUsed}m of ${timeLimit}m limit (+${overageMinutes}m)\n$guidanceMessage"
        builder.setContentText(mainContent)

        // Expandable content with Quranic verse
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(buildExpandedContent(appName, timeUsed, timeLimit, guidanceMessage, quranicVerse, recommendedActions))

        builder.setStyle(bigTextStyle)

        // Action buttons
        addNotificationActions(builder, packageName, appName, quranicVerse?.id)

        // Set content intent to open settings
        val settingsIntent = createSettingsIntent(packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(packageName),
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    /**
     * Build expanded notification content with Quranic guidance
     */
    private fun buildExpandedContent(
        appName: String,
        timeUsed: Int,
        timeLimit: Int,
        guidanceMessage: String,
        quranicVerse: com.hieltech.haramblur.data.QuranicVerse?,
        recommendedActions: List<String>
    ): String {
        val sb = StringBuilder()

        // Usage summary
        sb.append("📱 $appName Usage Summary\n")
        sb.append("⏱️ Time Used: ${timeUsed} minutes\n")
        sb.append("🎯 Time Limit: ${timeLimit} minutes\n")
        sb.append("⚠️ Over Limit: ${timeUsed - timeLimit} minutes\n\n")

        // Islamic guidance
        sb.append("🤲 Islamic Guidance:\n")
        sb.append("$guidanceMessage\n\n")

        // Quranic verse if available
        if (quranicVerse != null) {
            sb.append("📖 Quranic Verse:\n")
            sb.append("﴾ ${quranicVerse.arabicText} ﴿\n\n")
            sb.append("Translation:\n")
            val englishTranslation = quranicVerse.translations[Language.ENGLISH] ?: "Translation not available"
            sb.append("$englishTranslation\n\n")

            if (quranicVerse.transliteration.isNotBlank()) {
                sb.append("Transliteration:\n")
                sb.append("${quranicVerse.transliteration}\n\n")
            }

            sb.append("📍 ${quranicVerse.surahName} (${quranicVerse.surahNumber}:${quranicVerse.verseNumber})\n\n")
        }

        // Recommended actions
        if (recommendedActions.isNotEmpty()) {
            sb.append("✅ Recommended Actions:\n")
            recommendedActions.take(3).forEachIndexed { index, action ->
                sb.append("${index + 1}. $action\n")
            }
            sb.append("\n")
        }

        // Closing reminder
        sb.append("🌙 Remember: \"And remind, for indeed, the reminder benefits the believers.\" (Quran 51:55)")

        return sb.toString()
    }

    /**
     * Add action buttons to notification
     */
    private fun addNotificationActions(
        builder: NotificationCompat.Builder,
        packageName: String,
        appName: String,
        verseId: String?
    ) {
        // Reflect & Continue action
        val reflectIntent = createActionIntent(
            UsageTimeNotificationReceiver.ACTION_REFLECT_AND_CONTINUE,
            packageName,
            appName,
            verseId
        )
        val reflectPendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(packageName) + 1,
            reflectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_view,
            "Reflect & Continue",
            reflectPendingIntent
        )

        // Close App action
        val closeIntent = createActionIntent(
            UsageTimeNotificationReceiver.ACTION_CLOSE_APP,
            packageName,
            appName,
            verseId
        )
        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(packageName) + 2,
            closeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Close App",
            closePendingIntent
        )

        // Settings action
        val settingsIntent = createActionIntent(
            UsageTimeNotificationReceiver.ACTION_OPEN_SETTINGS,
            packageName,
            appName,
            verseId
        )
        val settingsPendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(packageName) + 3,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_preferences,
            "Settings",
            settingsPendingIntent
        )
    }

    /**
     * Create intent for notification actions
     */
    private fun createActionIntent(action: String, packageName: String, appName: String, verseId: String?): Intent {
        return Intent(context, UsageTimeNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(UsageTimeNotificationReceiver.EXTRA_PACKAGE_NAME, packageName)
            putExtra(UsageTimeNotificationReceiver.EXTRA_APP_NAME, appName)
            verseId?.let { putExtra(UsageTimeNotificationReceiver.EXTRA_VERSE_ID, it) }
        }
    }

    /**
     * Create settings intent
     */
    private fun createSettingsIntent(packageName: String): Intent {
        return Intent(context, com.hieltech.haramblur.MainActivity::class.java).apply {
            putExtra("open_screen", "usage_settings")
            putExtra("package_name", packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    /**
     * Get relevant Quranic verse for app category
     */
    private suspend fun getRelevantVerse(packageName: String, blockingCategory: BlockingCategory):
            com.hieltech.haramblur.data.QuranicVerse? {
        return try {
            // Try to get verse for specific category
            val categoryVerse = quranicRepository.getVerseForCategory(blockingCategory)
            if (categoryVerse != null) {
                return categoryVerse
            }

            // Fallback to random verse
            quranicRepository.getRandomVerse()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Quranic verse for $packageName", e)
            null
        }
    }

    /**
     * Show reflection dialog with verse
     */
    fun showReflectionDialog(packageName: String, appName: String, verseId: String?) {
        try {
            // This would integrate with a dialog manager or activity
            // For now, we'll show a simple notification-based reflection
            CoroutineScope(Dispatchers.Main).launch {
                val verse = verseId?.let { quranicRepository.getVerseById(it) }
                val reflectionMessage = buildReflectionMessage(appName, verse)

                val builder = NotificationCompat.Builder(context, USAGE_TIME_STATUS_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setColor(ISLAMIC_DARK_GREEN.toInt())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentTitle("🤲 Reflection Time - $appName")
                    .setContentText("Take $REFLECTION_TIME_SECONDS seconds to reflect on this verse")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(reflectionMessage))
                    .setAutoCancel(true)

                notificationManager.notify(
                    getNotificationId(packageName) + 100,
                    builder.build()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing reflection dialog for $packageName", e)
        }
    }

    /**
     * Build reflection message with verse
     */
    private fun buildReflectionMessage(appName: String, verse: com.hieltech.haramblur.data.QuranicVerse?): String {
        val sb = StringBuilder()
        sb.append("Take a moment to reflect on your usage of $appName.\n\n")

        if (verse != null) {
            sb.append("📖 Reflect on this verse:\n")
            sb.append("﴾ ${verse.arabicText} ﴿\n\n")
            val englishTranslation = verse.translations[Language.ENGLISH] ?: "Translation not available"
            sb.append("$englishTranslation\n\n")
            if (verse.reflection.isNotBlank()) {
                sb.append("🤔 ${verse.reflection}\n\n")
            }
            sb.append("• How does this verse relate to your current activities?\n")
            sb.append("• What beneficial activities could replace this time?\n")
            sb.append("• How can you better manage your digital usage?\n\n")
        }

        sb.append("🌙 May Allah guide us to use our time wisely for His pleasure.")
        return sb.toString()
    }

    /**
     * Show app closure guidance
     */
    suspend fun showAppClosureGuidance(packageName: String, appName: String) {
        try {
            val dua = "اللَّهُمَّ طَهِّرْ قَلْبِي وَأَعِنِّي عَلَى غَضِّ بَصَرِي وَحَصِّنْ فَرْجِي وَأَعِذْنِي مِنَ الْفَحْشَاءِ وَالْمُنْكَرِ"
            val guidanceMessage = buildClosureGuidanceMessage(appName, dua)

            val builder = NotificationCompat.Builder(context, USAGE_TIME_STATUS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setColor(ISLAMIC_DARK_GREEN.toInt())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle("✅ App Closed - $appName")
                .setContentText("May this closure bring you closer to Allah")
                .setStyle(NotificationCompat.BigTextStyle().bigText(guidanceMessage))
                .setAutoCancel(true)

            notificationManager.notify(
                getNotificationId(packageName) + 200,
                builder.build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing closure guidance for $packageName", e)
        }
    }

    /**
     * Build closure guidance message
     */
    private fun buildClosureGuidanceMessage(appName: String, dua: String?): String {
        val sb = StringBuilder()
        sb.append("Alhamdulillah! You have chosen to close $appName.\n\n")
        sb.append("🤲 Make this du'a for protection:\n\n")

        if (dua != null) {
            sb.append("$dua\n\n")
        } else {
            sb.append("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ الْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَغَلَبَةِ الدَّيْنِ وَقَهْرِ الرِّجَالِ\n\n")
            sb.append("(O Allah, I seek refuge in You from helplessness, laziness, cowardice, miserliness, and the burden of debt and domination by men.)\n\n")
        }

        sb.append("✅ Consider these beneficial alternatives:\n")
        sb.append("• Recite Quran\n")
        sb.append("• Make dhikr\n")
        sb.append("• Perform salah\n")
        sb.append("• Call a loved one\n")
        sb.append("• Read Islamic books\n")
        sb.append("• Help with household tasks\n\n")

        sb.append("🌟 May Allah reward you for choosing what pleases Him over momentary distractions.")
        return sb.toString()
    }

    /**
     * Show detailed Islamic guidance
     */
    suspend fun showDetailedGuidance(packageName: String, verseId: String?) {
        try {
            val verse = verseId?.let { quranicRepository.getVerseById(it) }
            val guidance = buildDetailedGuidanceMessage(verse)

            val builder = NotificationCompat.Builder(context, USAGE_TIME_STATUS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setColor(ISLAMIC_DARK_GREEN.toInt())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle("📚 Islamic Guidance")
                .setContentText("Deepening your understanding")
                .setStyle(NotificationCompat.BigTextStyle().bigText(guidance))
                .setAutoCancel(true)

            notificationManager.notify(
                getNotificationId(packageName) + 300,
                builder.build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing detailed guidance for $packageName", e)
        }
    }

    /**
     * Build detailed guidance message
     */
    private fun buildDetailedGuidanceMessage(verse: com.hieltech.haramblur.data.QuranicVerse?): String {
        val sb = StringBuilder()

        if (verse != null) {
            sb.append("📖 Understanding the Verse:\n\n")
            sb.append("﴾ ${verse.arabicText} ﴿\n\n")
            val englishTranslation = verse.translations[Language.ENGLISH] ?: "Translation not available"
            sb.append("Translation:\n$englishTranslation\n\n")

            if (verse.context.isNotBlank()) {
                sb.append("📝 Context:\n${verse.context}\n\n")
            }

            if (verse.reflection.isNotBlank()) {
                sb.append("🤔 Reflection:\n${verse.reflection}\n\n")
            }

            sb.append("🤔 Reflection Points:\n")
            sb.append("• What does this verse teach about time management?\n")
            sb.append("• How can you apply this guidance in your digital life?\n")
            sb.append("• What changes can you make to use technology more wisely?\n\n")
        }

        sb.append("💡 Islamic Wisdom on Time:\n")
        sb.append("• \"By time, indeed, mankind is in loss.\" (Quran 103:1-2)\n")
        sb.append("• \"And it is He who has made the night and day in succession for whoever desires to remember or desires gratitude.\" (Quran 25:62)\n")
        sb.append("• \"And when you have completed the prayer, remember Allah standing, sitting, or [lying] on your sides.\" (Quran 4:103)\n\n")

        sb.append("🌙 May Allah help us all to use our time wisely and seek His pleasure in all our actions.")
        return sb.toString()
    }

    /**
     * Cancel usage notification for specific app
     */
    fun cancelUsageNotification(packageName: String) {
        try {
            notificationManager.cancel(getNotificationId(packageName))
            Log.d(TAG, "Cancelled notification for $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification for $packageName", e)
        }
    }

    /**
     * Record notification dismissal
     */
    fun recordNotificationDismissed(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appUsageTracker.recordNotificationDismissed(packageName)
                Log.d(TAG, "Recorded notification dismissal for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification dismissal for $packageName", e)
            }
        }
    }

    /**
     * Get unique notification ID for package
     */
    private fun getNotificationId(packageName: String): Int {
        return USAGE_NOTIFICATION_ID_START + (abs(packageName.hashCode()) % (USAGE_NOTIFICATION_ID_END - USAGE_NOTIFICATION_ID_START + 1))
    }

    /**
     * Get or create notification state for package
     */
    private fun getOrCreateNotificationState(packageName: String): UsageTimeNotificationState {
        return notificationStates.getOrPut(packageName) {
            UsageTimeNotificationState(packageName)
        }
    }

    /**
     * Update notification state after showing notification
     */
    private fun updateNotificationState(packageName: String, verseId: String) {
        val currentState = getOrCreateNotificationState(packageName)
        notificationStates[packageName] = currentState.afterNotificationShown(verseId)
    }

    /**
     * Start monitoring for daily reset
     */
    private fun startDailyResetMonitoring() {
        dailyResetJob = scope.launch {
            try {
                appUsageTracker.usageStatsFlow.collectLatest { stats ->
                    // Check if it's a new day
                    val today = LocalDate.now()
                    val statesToReset = notificationStates.filter { (_, state) ->
                        state.date.isBefore(today)
                    }

                    if (statesToReset.isNotEmpty()) {
                        Log.d(TAG, "Daily reset detected, resetting ${statesToReset.size} notification states")
                        statesToReset.forEach { (packageName, state) ->
                            notificationStates[packageName] = state.resetForNewDay()
                        }

                        // Cancel all active notifications
                        cancelAllNotifications()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in daily reset monitoring", e)
            }
        }
    }

    /**
     * Clean up resources and cancel long-lived operations
     */
    fun cleanup() {
        try {
            dailyResetJob?.cancel()
            dailyResetJob = null
            Log.d(TAG, "UsageTimeNotificationManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Cancel all active usage notifications
     */
    fun cancelAllNotifications() {
        try {
            // Cancel notification range
            for (i in USAGE_NOTIFICATION_ID_START..USAGE_NOTIFICATION_ID_END) {
                notificationManager.cancel(i)
            }
            Log.d(TAG, "Cancelled all usage notifications for daily reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all notifications", e)
        }
    }

    /**
     * Show usage status notification
     */
    fun showUsageStatusNotification(packageName: String, appName: String, remainingTime: Int) {
        try {
        val builder = NotificationCompat.Builder(context, USAGE_TIME_STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setColor(ISLAMIC_DARK_GREEN.toInt())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle("$appName - Time Remaining")
                .setContentText("${remainingTime}m remaining today")

            // Add quick actions for status notification
            addStatusNotificationActions(builder, packageName, appName)

            notificationManager.notify(
                USAGE_TIME_STATUS_ID,
                builder.build()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error showing usage status notification for $packageName", e)
        }
    }

    /**
     * Add actions to status notification
     */
    private fun addStatusNotificationActions(
        builder: NotificationCompat.Builder,
        packageName: String,
        appName: String
    ) {
        // Quick dhikr action
        val dhikrIntent = Intent(context, com.hieltech.haramblur.MainActivity::class.java).apply {
            putExtra("open_screen", "dhikr")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val dhikrPendingIntent = PendingIntent.getActivity(
            context,
            USAGE_TIME_STATUS_ID + 1,
            dhikrIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_compass,
            "Dhikr",
            dhikrPendingIntent
        )

        // Prayer times action
        val prayerIntent = Intent(context, com.hieltech.haramblur.MainActivity::class.java).apply {
            putExtra("open_screen", "prayer")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val prayerPendingIntent = PendingIntent.getActivity(
            context,
            USAGE_TIME_STATUS_ID + 2,
            prayerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_agenda,
            "Prayer",
            prayerPendingIntent
        )
    }

    /**
     * Cancel status notification
     */
    fun cancelStatusNotification() {
        try {
            notificationManager.cancel(USAGE_TIME_STATUS_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling status notification", e)
        }
    }
}
