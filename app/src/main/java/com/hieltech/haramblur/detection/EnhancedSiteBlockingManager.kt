package com.hieltech.haramblur.detection

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.hieltech.haramblur.data.database.*
import com.hieltech.haramblur.utils.UrlUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val dbMutex = Mutex() // Prevent concurrent database access
    private val urlCache = mutableMapOf<String, Pair<Long, Boolean>>()
    private val cacheExpiryMs = 300000L // 5 minutes
    private val maxCacheSize = 500

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
     * Add a custom website to the blocked list with proper error handling
     */
    suspend fun addCustomBlockedWebsite(
        url: String,
        category: BlockingCategory = BlockingCategory.SUSPICIOUS_CONTENT,
        customCategory: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        dbMutex.withLock {
            try {
                Log.d(TAG, "Adding custom blocked website: $url")
                
                if (url.isBlank()) {
                    Log.w(TAG, "Cannot add blank URL to blocked list")
                    return@withContext false
                }

                val cleanUrl = cleanUrl(url)
                val domain = UrlUtils.extractDomain(cleanUrl)
                
                if (domain.isBlank()) {
                    Log.w(TAG, "Invalid domain extracted from URL: $url")
                    return@withContext false
                }

                val domainHash = UrlUtils.hashDomainSha256(domain)
                val currentTime = System.currentTimeMillis()

                // Check if already exists
                val existingSite = blockedSiteDao.getSiteByDomainHash(domainHash)
                if (existingSite != null) {
                    Log.i(TAG, "Site already blocked: $domain")
                    return@withContext false
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

                // Clear cache for this domain
                urlCache.remove(domain)
                Log.i(TAG, "Successfully added custom blocked site: $domain")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to add custom blocked website: $url", e)
                return@withContext false
            }
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
     * Check if a website is blocked by custom rules with caching
     */
    suspend fun isCustomBlocked(url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        
        try {
            val cleanedUrl = cleanUrl(url)
            val domain = UrlUtils.extractDomain(cleanedUrl)
            
            if (domain.isBlank()) {
                Log.w(TAG, "Invalid domain for URL: $url")
                return@withContext false
            }

            // Check cache first
            val currentTime = System.currentTimeMillis()
            val cachedResult = urlCache[domain]
            if (cachedResult != null && (currentTime - cachedResult.first) < cacheExpiryMs) {
                Log.v(TAG, "Using cached result for domain: $domain = ${cachedResult.second}")
                return@withContext cachedResult.second
            }

            // Query database
            val domainHash = UrlUtils.hashDomainSha256(domain)
            val site = blockedSiteDao.getSiteByDomainHash(domainHash)
            val isBlocked = site?.addedByUser == true && site.isActive
            
            // Cache result
            if (urlCache.size >= maxCacheSize) {
                // Remove oldest entries
                val oldestEntries = urlCache.entries.sortedBy { it.value.first }.take(maxCacheSize / 4)
                oldestEntries.forEach { urlCache.remove(it.key) }
            }
            urlCache[domain] = Pair(currentTime, isBlocked)
            
            Log.v(TAG, "Domain $domain is ${if (isBlocked) "blocked" else "allowed"}")
            return@withContext isBlocked
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if URL is custom blocked: $url", e)
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
     * Get custom blocked websites count with error handling
     */
    fun getCustomBlockedWebsitesCount(): Flow<Int> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val count = blockedSiteDao.getUserAddedSiteCount()
                emit(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting custom blocked websites count", e)
                emit(0) // Emit 0 on error instead of crashing
            }
            try {
                delay(2000) // Update every 2 seconds
            } catch (e: CancellationException) {
                Log.d(TAG, "Flow collection cancelled, stopping count updates")
                break // Exit gracefully when flow is cancelled
            }
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
     * Get browser window information with error handling
     */
    suspend fun getBrowserWindowInfo(browserPackage: String): BrowserWindowInfo? = withContext(Dispatchers.IO) {
        try {
            if (browserPackage.isBlank()) {
                Log.w(TAG, "Cannot get browser info for blank package name")
                return@withContext null
            }
            
            // This would use accessibility service to get browser window info
            // For now, return mock data for supported browsers
            if (BROWSER_PACKAGES.contains(browserPackage)) {
                BrowserWindowInfo(
                    packageName = browserPackage,
                    isActive = true,
                    currentUrl = null,
                    tabCount = 1
                )
            } else {
                Log.d(TAG, "Unsupported browser package: $browserPackage")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting browser window info for package: $browserPackage", e)
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
        return try {
            var cleanUrl = url.trim()

            if (cleanUrl.isBlank()) {
                return ""
            }

            // Add protocol if missing
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }

            // Remove fragments and some query parameters
            cleanUrl = cleanUrl.split("#")[0]
            
            // Remove tracking parameters
            cleanUrl = cleanUrl.split("?").let { parts ->
                if (parts.size > 1) {
                    val baseUrl = parts[0]
                    val queryParams = parts[1].split("&")
                        .filter { !it.startsWith("utm_") && !it.startsWith("fbclid") }
                    if (queryParams.isNotEmpty()) {
                        "$baseUrl?${queryParams.joinToString("&")}"
                    } else {
                        baseUrl
                    }
                } else {
                    cleanUrl
                }
            }

            cleanUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning URL: $url", e)
            url.trim() // Return original trimmed URL on error
        }
    }
    
    /**
     * Clear the URL cache - useful for testing or memory management
     */
    fun clearCache() {
        urlCache.clear()
        Log.d(TAG, "URL cache cleared")
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cacheSize" to urlCache.size,
            "maxCacheSize" to maxCacheSize,
            "cacheExpiryMs" to cacheExpiryMs
        )
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
