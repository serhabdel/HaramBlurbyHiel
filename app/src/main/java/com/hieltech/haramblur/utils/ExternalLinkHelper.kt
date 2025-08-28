package com.hieltech.haramblur.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.hieltech.haramblur.data.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for handling external links safely and consistently
 */
@Singleton
class ExternalLinkHelper @Inject constructor(
    private val logRepository: LogRepository
) {

    /**
     * Enum for different donation platforms
     */
    enum class DonationPlatform(val baseUrl: String) {
        BUY_ME_COFFEE("https://www.buymeacoffee.com/"),
        PATREON("https://www.patreon.com/"),
        GITHUB_SPONSORS("https://github.com/sponsors/")
    }

    /**
     * Open a URL safely with proper intent handling and fallbacks
     */
    fun openUrl(
        context: Context,
        url: String,
        fallbackAction: (() -> Unit)? = null,
        scope: CoroutineScope? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Add referrer for better tracking
                putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${context.packageName}"))
            }

            // Try opening with intent chooser for better compatibility
            val chooserIntent = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try opening with different methods in order of preference
            var success = false
            
            // Method 1: Try chooser intent first
            if (isIntentResolvable(context, chooserIntent)) {
                try {
                    context.startActivity(chooserIntent)
                    success = true
                } catch (e: Exception) {
                    // Continue to next method
                }
            }
            
            // Method 2: Try direct intent
            if (!success && isIntentResolvable(context, intent)) {
                try {
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    // Continue to next method
                }
            }
            
            // Method 3: Force try direct intent as last resort
            if (!success) {
                try {
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    // All methods failed
                }
            }
            
            if (success) {
                scope?.launch {
                    logRepository.logInfo(
                        "ExternalLinkHelper",
                        "Successfully opened URL: $url",
                        LogRepository.LogCategory.UI,
                        userAction = "open_external_link"
                    )
                }
            } else {
                showToast(context, "No app found to open this link")
                fallbackAction?.invoke()
                scope?.launch {
                    logRepository.logWarning(
                        "ExternalLinkHelper",
                        "No app available to handle URL: $url",
                        LogRepository.LogCategory.UI,
                        userAction = "open_external_link_failed"
                    )
                }
            }
        } catch (e: Exception) {
            showToast(context, "Failed to open link: ${e.message}")
            scope?.launch {
                logRepository.logError(
                    "ExternalLinkHelper",
                    "Failed to open URL: $url",
                    e,
                    LogRepository.LogCategory.UI,
                    userAction = "open_external_link_error"
                )
            }
        }
    }

    /**
     * Open GitHub repository with optional path
     */
    fun openGitHub(
        context: Context,
        path: String = "",
        scope: CoroutineScope? = null
    ) {
        val githubUrl = "https://github.com/hieltech/haramblur$path"
        openUrl(context, githubUrl, scope = scope)
    }

    /**
     * Open donation link for specified platform and identifier
     */
    fun openDonationLink(
        context: Context,
        platform: DonationPlatform,
        identifier: String,
        scope: CoroutineScope? = null
    ) {
        val donationUrl = "${platform.baseUrl}$identifier"
        openUrl(
            context,
            donationUrl,
            fallbackAction = {
                // Fallback to general donation URL
                openUrl(context, "https://github.com/hieltech/haramblur#support", scope = scope)
            },
            scope = scope
        )
    }

    /**
     * Share app information
     */
    fun shareApp(context: Context, scope: CoroutineScope? = null) {
        try {
            val shareText = """
                Check out HaramBlur - Islamic Content Filtering App!

                HaramBlur automatically detects and blurs inappropriate content across all apps on your Android device.

                Features:
                🛡️ Real-time content detection
                🎯 Female-focused filtering
                ⚡ GPU-accelerated processing
                🔒 Local processing (no data sent to servers)

                Download from: https://github.com/hieltech/haramblur
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out HaramBlur!")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share HaramBlur"))
            scope?.launch {
                logRepository.logInfo(
                    "ExternalLinkHelper",
                    "App shared via external sharing",
                    LogRepository.LogCategory.UI,
                    userAction = "share_app"
                )
            }
        } catch (e: Exception) {
            showToast(context, "Failed to share app")
            scope?.launch {
                logRepository.logError(
                    "ExternalLinkHelper",
                    "Failed to share app",
                    e,
                    LogRepository.LogCategory.UI,
                    userAction = "share_app_error"
                )
            }
        }
    }

    /**
     * Check if an intent can be resolved by any app
     */
    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return try {
            val packageManager = context.packageManager
            // Use 0 instead of MATCH_DEFAULT_ONLY for broader compatibility
            val activities = packageManager.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            // If query fails, try a different approach
            try {
                intent.resolveActivity(context.packageManager) != null
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Show a toast message safely
     */
    private fun showToast(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Context might be invalid, silently fail
        }
    }

    /**
     * Open GitHub repository main page
     */
    fun openGitHubRepository(context: Context, scope: CoroutineScope? = null) {
        openGitHub(context, "", scope)
    }

    /**
     * Open GitHub issues page
     */
    fun openGitHubIssues(context: Context, scope: CoroutineScope? = null) {
        openGitHub(context, "/issues", scope)
    }

    /**
     * Open GitHub discussions page
     */
    fun openGitHubDiscussions(context: Context, scope: CoroutineScope? = null) {
        openGitHub(context, "/discussions", scope)
    }

    /**
     * Open documentation (README)
     */
    fun openDocumentation(context: Context, scope: CoroutineScope? = null) {
        openGitHub(context, "#readme", scope)
    }

    /**
     * Open Buy Me a Coffee donation page
     */
    fun openBuyMeCoffee(context: Context, identifier: String = "hieltech", scope: CoroutineScope? = null) {
        openDonationLink(context, DonationPlatform.BUY_ME_COFFEE, identifier, scope)
    }
}
