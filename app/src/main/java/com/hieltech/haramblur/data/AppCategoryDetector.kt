package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.models.AppCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for detecting app categories and identifying specific app types
 */
@Singleton
class AppCategoryDetector @Inject constructor() {

    /**
     * Determine the category of an app based on its package name
     * Uses AppRegistry fallback for broader detection
     */
    fun determineAppCategory(packageName: String): AppCategory? {
        // First check direct category matches
        val directMatch = AppCategory.values().find { category ->
            category.defaultApps.contains(packageName)
        }
        if (directMatch != null) {
            return directMatch
        }

        // Fallback: Use AppRegistry for pattern-based detection
        if (AppRegistry.isSocialMediaRelated(packageName)) {
            return AppCategory.SOCIAL_MEDIA
        }

        // Fallback: Check for browser patterns
        if (isBrowserApp(packageName)) {
            return AppCategory.BROWSERS
        }

        return null
    }

    /**
     * Check if an app is a browser based on package name and common patterns
     */
    fun isBrowserApp(packageName: String): Boolean {
        // Check if it's in the browsers category
        if (AppCategory.BROWSERS.defaultApps.contains(packageName)) {
            return true
        }

        // Check for common browser package patterns
        val browserPatterns = listOf(
            "browser", "chrome", "firefox", "opera", "edge", "safari",
            "webview", "webkit", "mozilla", "brave", "duckduckgo",
            "samsung.android.app.sbrowser", "ucmobile", "kiwibrowser"
        )

        return browserPatterns.any { pattern ->
            packageName.lowercase().contains(pattern)
        }
    }

    /**
     * Check if an app is social media based on category and patterns
     */
    fun isSocialMediaApp(packageName: String): Boolean {
        // Check if it's in the social media category
        if (AppCategory.SOCIAL_MEDIA.defaultApps.contains(packageName)) {
            return true
        }

        // Use AppRegistry for broader social media detection
        return AppRegistry.isSocialMediaRelated(packageName)
    }

    /**
     * Check if an app is a dating app
     */
    fun isDatingApp(packageName: String): Boolean {
        return AppCategory.DATING.defaultApps.contains(packageName)
    }

    /**
     * Check if an app is a messaging app
     */
    fun isMessagingApp(packageName: String): Boolean {
        return AppCategory.MESSAGING.defaultApps.contains(packageName)
    }

    /**
     * Check if an app is an entertainment app
     */
    fun isEntertainmentApp(packageName: String): Boolean {
        return AppCategory.ENTERTAINMENT.defaultApps.contains(packageName)
    }

    /**
     * Get all apps in a specific category
     */
    fun getAppsInCategory(category: AppCategory): Set<String> {
        return category.defaultApps
    }

    /**
     * Get confidence score for app category detection
     */
    fun getCategoryConfidence(packageName: String, category: AppCategory): Double {
        return when {
            category.defaultApps.contains(packageName) -> 1.0
            category == AppCategory.BROWSERS && isBrowserApp(packageName) -> 0.8
            category == AppCategory.SOCIAL_MEDIA && AppRegistry.isSocialMediaRelated(packageName) -> 0.7
            else -> 0.0
        }
    }

    /**
     * Check if an app should be considered high-risk for inappropriate content
     */
    fun isHighRiskApp(packageName: String): Boolean {
        return isBrowserApp(packageName) ||
               isSocialMediaApp(packageName) ||
               isDatingApp(packageName)
    }

    /**
     * Get human-readable category name for an app
     */
    fun getCategoryDisplayName(packageName: String): String? {
        return determineAppCategory(packageName)?.displayName
    }

    /**
     * Check if an app matches any of the provided categories
     */
    fun matchesAnyCategory(packageName: String, categories: Set<AppCategory>): Boolean {
        val appCategory = determineAppCategory(packageName)
        return appCategory != null && categories.contains(appCategory)
    }

    /**
     * Get all package names for multiple categories
     */
    fun getAllPackageNamesForCategories(categories: Set<AppCategory>): Set<String> {
        return categories.flatMap { it.defaultApps }.toSet()
    }
}
