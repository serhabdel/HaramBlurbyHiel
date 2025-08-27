package com.hieltech.haramblur.detection

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Device Admin Receiver for enhanced app blocking capabilities.
 *
 * This receiver enables force-closing of blocked apps for stronger enforcement
 * when Device Admin permission is granted by the user. It's part of the
 * progressive enhancement approach - the app remains fully functional without it.
 */
@AndroidEntryPoint
class HaramBlurDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "HaramBlurDeviceAdmin"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)

        scope.launch {
            logRepository.logInfo(
                tag = TAG,
                message = "Device Admin permission granted - enhanced blocking capabilities activated",
                category = LogRepository.LogCategory.BLOCKING,
                userAction = "DEVICE_ADMIN_ENABLED"
            )
        }

        // Update settings to reflect device admin activation
        settingsRepository.updatePermissionStatus("DEVICE_ADMIN", true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        scope.launch {
            logRepository.logInfo(
                tag = TAG,
                message = "Device Admin permission revoked - falling back to accessibility-based blocking",
                category = LogRepository.LogCategory.BLOCKING,
                userAction = "DEVICE_ADMIN_DISABLED"
            )
        }

        // Update settings to reflect device admin deactivation
        settingsRepository.updatePermissionStatus("DEVICE_ADMIN", false)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        // Provide user-friendly explanation when they try to disable admin access
        return "Disabling Device Admin will reduce app blocking effectiveness. " +
               "Blocked apps may still be blocked through accessibility service, " +
               "but force-closing capabilities will be unavailable."
    }
}
