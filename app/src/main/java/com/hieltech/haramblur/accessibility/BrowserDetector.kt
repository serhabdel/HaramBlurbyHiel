package com.hieltech.haramblur.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Utility class for detecting browser applications
 * Extracted from HaramBlurAccessibilityService for better maintainability
 */
object BrowserDetector {

    /**
     * Known browser package names
     */
    private val knownBrowserPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.canary",
        "com.chrome.dev",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.yandex.browser",
        "org.torproject.torbrowser",
        "com.kiwibrowser.browser",
        "com.ecosia.android",
        "com.qwant.liberty",
        "com.UCMobile.intl",
        "com.puffin.browser",
        "com.adguard.browser"
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    /**
     * Keywords commonly found in browser package names
     */
    private val browserPackageKeywords = listOf(
        "browser",
        "chrome",
        "firefox",
        "edge",
        "opera",
        "brave",
        "duckduckgo",
        "vivaldi",
        "yandex",
        "puffin",
        "samsung",
        "kiwi",
        "tor"
    )

    /**
     * Check if a package name belongs to a known browser
     */
    fun isBrowserPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val normalized = packageName.lowercase(Locale.ROOT)
        
        if (knownBrowserPackages.contains(normalized)) {
            return true
        }
        
        return browserPackageKeywords.any { normalized.contains(it) }
    }

    /**
     * Safely get foreground package name from root node
     */
    fun getForegroundPackageNameSafely(rootNode: AccessibilityNodeInfo?): String? {
        return try {
            rootNode?.packageName?.toString()
        } catch (e: Exception) {
            null
        } finally {
            try {
                rootNode?.recycle()
            } catch (_: Exception) {
                // Ignore recycle errors
            }
        }
    }

    /**
     * Get all known browser packages for reference
     */
    fun getKnownBrowserPackages(): Set<String> = knownBrowserPackages
}
