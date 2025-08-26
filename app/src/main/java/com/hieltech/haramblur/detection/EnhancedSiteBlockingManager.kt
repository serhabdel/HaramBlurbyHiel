package com.hieltech.haramblur.detection

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hieltech.haramblur.data.database.*
import com.hieltech.haramblur.utils.UrlUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced site blocking manager with custom URLs and browser management
 */
@Singleton
class EnhancedSiteBlockingManager @Inject constructor(
    private val database: SiteBlockingDatabase,
    @ApplicationContext private val context: Context,
    private val originalManager: SiteBlockingManager
) : SiteBlockingManager by originalManager {

    private val blockedSiteDao = database.blockedSiteDao()

    companion object {
        private const val TAG = "EnhancedSiteBlockingManager"
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.samsung.android.app.sbrowser",
            "com.UCMobile.intl",
            "com.kiwibrowser.browser",
            "org.mozilla.focus",
            "com.android.browser",
            "com.sec.android.app.sbrowser"
        )
    }

    /**
     * Add a custom website to the blocked list
     */
    suspend fun addCustomBlockedWebsite(
        url: String,
        category: BlockingCategory = BlockingCategory.SUSPICIOUS_CONTENT,
        customCategory: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = cleanUrl(url)
            val domain = UrlUtils.extractDomain(cleanUrl)
            val domainHash = UrlUtils.hashDomainSha256(domain)
            val currentTime = System.currentTimeMillis()

            // Check if already exists
            val existingSite = blockedSiteDao.getSiteByDomainHash(domainHash)
            if (existingSite != null) {
                return@withContext false // Already blocked
            }

            // Insert custom site
            blockedSiteDao.insertCustomSite(
                domainHash = domainHash,
                pattern = domain,
                category = category,
                confidence = 1.0f,
                lastUpdated = currentTime,
                isRegex = false,
                source = "user_added",
                description = "Custom blocked site added by user",
                customCategory = customCategory,
                dateAdded = currentTime
            )

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Remove a custom blocked website
     */
    suspend fun removeCustomBlockedWebsite(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val domain = UrlUtils.extractDomain(cleanUrl(url))
            val domainHash = UrlUtils.hashDomainSha256(domain)

            val site = blockedSiteDao.getSiteByDomainHash(domainHash)
            if (site != null && site.addedByUser) {
                blockedSiteDao.deactivateSite(domainHash)
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Get all custom blocked websites
     */
    suspend fun getCustomBlockedWebsites(): List<BlockedSiteEntity> = withContext(Dispatchers.IO) {
        blockedSiteDao.getUserAddedSites()
    }

    /**
     * Check if a website is blocked by custom rules
     */
    suspend fun isCustomBlocked(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val domain = UrlUtils.extractDomain(cleanUrl(url))
            val domainHash = UrlUtils.hashDomainSha256(domain)

            val site = blockedSiteDao.getSiteByDomainHash(domainHash)
            return@withContext site?.addedByUser == true && site.isActive
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Get websites blocked by category
     */
    suspend fun getWebsitesByCategory(category: BlockingCategory): List<BlockedSiteEntity> = withContext(Dispatchers.IO) {
        blockedSiteDao.getSitesByCategory(category)
    }

    /**
     * Search blocked websites
     */
    suspend fun searchBlockedWebsites(query: String): List<BlockedSiteEntity> = withContext(Dispatchers.IO) {
        blockedSiteDao.searchSites(query)
    }

    /**
     * Get custom blocked websites count
     */
    fun getCustomBlockedWebsitesCount(): Flow<Int> = flow {
        while (currentCoroutineContext().isActive) {
            val count = blockedSiteDao.getUserAddedSiteCount()
            emit(count)
            delay(2000) // Update every 2 seconds
        }
    }

    /**
     * Close blocked browser tabs
     */
    suspend fun closeBlockedBrowserTabs(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // This would use accessibility service to close tabs
            // For now, we'll just return true to indicate the intention
            // The actual implementation would be in the accessibility service
            android.util.Log.d(TAG, "Requesting to close blocked browser tabs for: $url")
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Navigate browser to safe page
     */
    suspend fun navigateToSafePage(browserPackage: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!BROWSER_PACKAGES.contains(browserPackage)) {
                return@withContext false
            }

            // Create intent to navigate to a safe page
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com")
                `package` = browserPackage
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Get browser window information
     */
    suspend fun getBrowserWindowInfo(browserPackage: String): BrowserWindowInfo? = withContext(Dispatchers.IO) {
        // This would use accessibility service to get browser window info
        // For now, return mock data
        if (BROWSER_PACKAGES.contains(browserPackage)) {
            BrowserWindowInfo(
                packageName = browserPackage,
                isActive = true,
                currentUrl = null,
                tabCount = 1
            )
        } else {
            null
        }
    }

    /**
     * Check if URL is in a supported browser
     */
    fun isBrowserSupported(packageName: String): Boolean {
        return BROWSER_PACKAGES.contains(packageName)
    }

    /**
     * Get supported browsers
     */
    fun getSupportedBrowsers(): Set<String> {
        return BROWSER_PACKAGES
    }

    // Private helper methods

    private fun cleanUrl(url: String): String {
        var cleanUrl = url.trim()

        // Add protocol if missing
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        // Remove fragments and some query parameters
        cleanUrl = cleanUrl.split("#")[0]

        return cleanUrl
    }


}

/**
 * Data class for browser window information
 */
data class BrowserWindowInfo(
    val packageName: String,
    val isActive: Boolean,
    val currentUrl: String? = null,
    val tabCount: Int = 1,
    val canCloseTabs: Boolean = false
)
