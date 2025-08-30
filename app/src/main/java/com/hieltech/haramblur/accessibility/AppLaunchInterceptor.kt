package com.hieltech.haramblur.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.LogRepository.LogCategory
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.BlockedAppLaunchCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Broadcast receiver that intercepts app launch intents and blocks them proactively
 */
class AppLaunchInterceptor : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppLaunchInterceptor"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            // Skip package installation events
            return
        }

        val packageName = intent.data?.schemeSpecificPart ?: return

        // Get dependencies from application
        val application = context.applicationContext as? com.hieltech.haramblur.HaramBlurApplication
        val appBlockingManager = application?.appBlockingManager
        val logRepository = application?.logRepository

        if (appBlockingManager == null || logRepository == null) {
            Log.e(TAG, "Failed to get dependencies from application")
            return
        }

        scope.launch {
            try {
                // Check if this app is blocked
                val isBlocked = appBlockingManager.isAppBlocked(packageName)

                if (isBlocked) {
                    logRepository.logInfo(
                        tag = TAG,
                        message = "Blocked app launch intercepted: $packageName",
                        category = LogCategory.BLOCKING,
                        userAction = "APP_LAUNCH_INTERCEPTED"
                    )

                    // Block the app launch
                    val blocked = appBlockingManager.enforceBlock(packageName)

                    logRepository.logInfo(
                        tag = TAG,
                        message = "App launch block result for $packageName: $blocked",
                        category = LogCategory.BLOCKING,
                        userAction = if (blocked) "APP_LAUNCH_BLOCKED" else "APP_LAUNCH_BLOCK_FAILED"
                    )
                }
            } catch (e: Exception) {
                logRepository.logInfo(
                    tag = TAG,
                    message = "Error intercepting app launch for $packageName: ${e.message}",
                    category = LogCategory.BLOCKING,
                    userAction = "APP_LAUNCH_INTERCEPT_ERROR"
                )
            }
        }
    }
}