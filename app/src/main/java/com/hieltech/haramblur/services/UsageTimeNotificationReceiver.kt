package com.hieltech.haramblur.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hieltech.haramblur.HaramBlurApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles actions from usage time notification buttons
 */
class UsageTimeNotificationReceiver : BroadcastReceiver() {

    // Note: Dependency injection will be added later when the notification manager is fully implemented
    private lateinit var usageTimeNotificationManager: UsageTimeNotificationManager

    companion object {
        private const val TAG = "UsageTimeNotificationReceiver"

        // Action constants
        const val ACTION_REFLECT_AND_CONTINUE = "com.hieltech.haramblur.REFLECT_AND_CONTINUE"
        const val ACTION_CLOSE_APP = "com.hieltech.haramblur.CLOSE_APP"
        const val ACTION_OPEN_SETTINGS = "com.hieltech.haramblur.OPEN_SETTINGS"
        const val ACTION_DISMISS_NOTIFICATION = "com.hieltech.haramblur.DISMISS_USAGE_NOTIFICATION"
        const val ACTION_SHOW_GUIDANCE = "com.hieltech.haramblur.SHOW_GUIDANCE"

        // Extra keys
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_VERSE_ID = "verse_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        try {
            // Get dependencies from application
            val app = context.applicationContext as HaramBlurApplication

            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
            val verseId = intent.getStringExtra(EXTRA_VERSE_ID)

            Log.d(TAG, "Received action: ${intent.action} for app: $packageName")

            when (intent.action) {
                ACTION_REFLECT_AND_CONTINUE -> {
                    handleReflectAndContinue(context, packageName, appName, verseId)
                }
                ACTION_CLOSE_APP -> {
                    handleCloseApp(context, packageName, appName)
                }
                ACTION_OPEN_SETTINGS -> {
                    handleOpenSettings(context, packageName)
                }
                ACTION_DISMISS_NOTIFICATION -> {
                    handleDismissNotification(context, packageName)
                }
                ACTION_SHOW_GUIDANCE -> {
                    handleShowGuidance(context, packageName, verseId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification action", e)
        }
    }

    private fun handleReflectAndContinue(context: Context, packageName: String, appName: String, verseId: String?) {
        try {
            val intent = Intent(context, com.hieltech.haramblur.ui.ReflectionActivity::class.java).apply {
                putExtra("package_name", packageName)
                putExtra("app_name", appName)
                verseId?.let { putExtra("verse_id", it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching ReflectionActivity", e)
        }
    }

    private fun handleCloseApp(context: Context, packageName: String, appName: String) {
        try {
            // Delegate to an activity to leverage Hilt and required privileges
            val intent = Intent(context, com.hieltech.haramblur.ui.ReflectionActivity::class.java).apply {
                putExtra("package_name", packageName)
                putExtra("app_name", appName)
                // Launch with zero reflection time and immediate close action
                putExtra("force_close", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Close App action", e)
        }
    }

    private fun handleOpenSettings(context: Context, packageName: String) {
        try {
            // Open app settings screen
            val settingsIntent = Intent(context, com.hieltech.haramblur.MainActivity::class.java).apply {
                putExtra("open_screen", "usage_settings")
                putExtra("package_name", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opening settings for $packageName (notification manager not yet implemented)")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
        }
    }

    private fun handleDismissNotification(context: Context, packageName: String) {
        // Note: Implementation will be added when notification manager is fully implemented
        Log.i(TAG, "Notification dismissed for $packageName (notification manager not yet implemented)")
    }

    private fun handleShowGuidance(context: Context, packageName: String, verseId: String?) {
        // Note: Implementation will be added when notification manager is fully implemented
        Log.i(TAG, "Show Guidance clicked for $packageName (notification manager not yet implemented)")
    }
}
