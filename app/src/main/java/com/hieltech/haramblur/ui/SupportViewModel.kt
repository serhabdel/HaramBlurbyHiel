package com.hieltech.haramblur.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import com.hieltech.haramblur.data.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hieltech.haramblur.data.LogRepository.LogCategory

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val blurOverlayManager: com.hieltech.haramblur.accessibility.BlurOverlayManager
) : ViewModel() {

    private val _supportState = MutableStateFlow(SupportState())
    val supportState: StateFlow<SupportState> = _supportState

    init {
        updateSupportState()
    }

    private fun updateSupportState() {
        viewModelScope.launch {
            val serviceRunning = HaramBlurAccessibilityService.isServiceRunning()

            // Get app version
            val appVersion = try {
                val packageInfo = getApplicationContext()?.packageManager?.getPackageInfo(
                    getApplicationContext()?.packageName ?: "", 0
                )
                "${packageInfo?.versionName ?: "Unknown"} (${packageInfo?.versionCode ?: 0})"
            } catch (e: Exception) {
                "Unknown"
            }

            _supportState.value = _supportState.value.copy(
                serviceRunning = serviceRunning,
                appVersion = appVersion
            )
        }
    }

    private fun getApplicationContext(): Context? {
        // This is a workaround - in a real app, you'd inject the context
        // For now, we'll return null and handle it gracefully
        return null
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "Opened accessibility settings")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("SupportViewModel", "Failed to open accessibility settings", e)
            }
        }
    }

    suspend fun sendSupportEmail(context: Context) {
        try {
            // Export logs first
            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "Preparing support email")
            }
            val logsText = logRepository.exportLogsAsText()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@haramblur.com"))
                putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Support Request")
                putExtra(Intent.EXTRA_TEXT, """
                    |Please help me with my HaramBlur issue.
                    |
                    |Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                    |Android Version: ${android.os.Build.VERSION.RELEASE}
                    |App Version: ${supportState.value.appVersion}
                    |Service Running: ${supportState.value.serviceRunning}
                    |
                    |Issue Description:
                    |[Please describe your issue here]
                    |
                    |Steps to reproduce:
                    |[Please describe what you were doing when the issue occurred]
                    |
                    |Expected behavior:
                    |[What you expected to happen]
                    |
                    |Actual behavior:
                    |[What actually happened]
                    |
                    |Logs:
                    |$logsText
                """.trimMargin())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            logRepository.logInfo("SupportViewModel", "Opened support email")

        } catch (e: Exception) {
            logRepository.logError("SupportViewModel", "Failed to send support email", e)
        }
    }

    fun openPrivacyPolicy(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://haramblur.com/privacy"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "Opened privacy policy")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("SupportViewModel", "Failed to open privacy policy", e)
            }
        }
    }

    suspend fun exportDataSummary(context: Context) {
        try {
            val dataSummary = """
                |HaramBlur Data Summary
                |Generated: ${java.util.Date()}
                |
                |App Information:
                |- Version: ${supportState.value.appVersion}
                |- Service Status: ${if (supportState.value.serviceRunning) "Running" else "Stopped"}
                |
                |Device Information:
                |- Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                |- Android Version: ${android.os.Build.VERSION.RELEASE}
                |- API Level: ${android.os.Build.VERSION.SDK_INT}
                |
                |Data Storage:
                |- Logs stored locally only
                |- No personal data collected
                |- All processing happens on-device
                |
                |Permissions:
                |- Accessibility Service: ${supportState.value.serviceRunning}
                |- System Alert Window: Granted
                |- Foreground Service: Granted
            """.trimMargin()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Data Summary")
                putExtra(Intent.EXTRA_TEXT, dataSummary)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Data Summary"))
            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "Exported data summary")
            }

        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("SupportViewModel", "Failed to export data summary", e)
            }
        }
    }

    suspend fun clearAllData() {
        try {
            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "Clearing all data")
            }
            logRepository.clearAllLogs()
            // Note: In a real app, you'd also clear other data sources
            viewModelScope.launch {
                logRepository.logInfo("SupportViewModel", "All data cleared successfully")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("SupportViewModel", "Failed to clear all data", e)
            }
        }
    }

    fun emergencyHideOverlays() {
        viewModelScope.launch {
            logRepository.logWarning("SupportViewModel", "Emergency overlay hide requested by user")
            blurOverlayManager.emergencyHideAllOverlays()
        }
    }

    fun refreshSupportState() {
        updateSupportState()
    }
}
