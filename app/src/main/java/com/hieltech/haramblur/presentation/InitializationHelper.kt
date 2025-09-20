package com.hieltech.haramblur.presentation

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.database.BlockedSiteEntity
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.utils.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for initializing test data and fixing database issues
 */
@Singleton
class InitializationHelper @Inject constructor(
    private val database: SiteBlockingDatabase,
    private val enhancedSiteBlockingManager: EnhancedSiteBlockingManager
) {
    companion object {
        private const val TAG = "InitializationHelper"
    }

    /**
     * Initialize test NSFW URLs as requested
     */
    suspend fun initializeTestUrls() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing test NSFW URLs...")
            
            // Add the requested test URL: nsfw.ma
            val testUrls = listOf(
                "nsfw.ma",
                "https://nsfw.ma",
                "www.nsfw.ma"
            )
            
            for (url in testUrls) {
                val success = enhancedSiteBlockingManager.addCustomBlockedWebsite(
                    url = url,
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    customCategory = "test_nsfw"
                )
                if (success) {
                    Log.i(TAG, "✅ Added test NSFW URL: $url")
                } else {
                    Log.w(TAG, "⚠️ Failed to add or already exists: $url")
                }
            }
            
            // Add some common porn sites for testing (without full domains to be safe)
            val commonPatterns = listOf(
                "porn",
                "xxx", 
                "sex",
                "adult",
                "nsfw",
                "18+"
            )
            
            for (pattern in commonPatterns) {
                try {
                    val blockedSiteDao = database.blockedSiteDao()
                    val domainHash = UrlUtils.hashDomainSha256(pattern)
                    
                    // Check if already exists
                    val existing = blockedSiteDao.getSiteByDomainHash(domainHash)
                    if (existing == null) {
                        blockedSiteDao.insertCustomSite(
                            domainHash = domainHash,
                            pattern = pattern,
                            category = BlockingCategory.EXPLICIT_CONTENT,
                            confidence = 0.95f,
                            lastUpdated = System.currentTimeMillis(),
                            isRegex = true, // Use as regex pattern
                            source = "test_pattern",
                            description = "Test pattern for NSFW detection",
                            customCategory = "test_pattern",
                            dateAdded = System.currentTimeMillis()
                        )
                        Log.i(TAG, "✅ Added test pattern: $pattern")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding pattern: $pattern", e)
                }
            }
            
            Log.i(TAG, "✅ Test URLs initialization complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing test URLs", e)
        }
    }
    
    /**
     * Clear all test URLs for cleanup
     */
    suspend fun clearTestUrls() = withContext(Dispatchers.IO) {
        try {
            val blockedSiteDao = database.blockedSiteDao()
            
            // Get all test sites
            val testSites = blockedSiteDao.getSitesBySource("test_url")
            val testPatterns = blockedSiteDao.getSitesBySource("test_pattern")
            
            val allTestSites = testSites + testPatterns
            
            for (site in allTestSites) {
                blockedSiteDao.deleteSite(site)
            }
            
            Log.i(TAG, "✅ Cleared ${allTestSites.size} test URLs")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing test URLs", e)
        }
    }
    
    /**
     * Fix database issues that might cause crashes
     */
    suspend fun fixDatabaseIssues() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for database issues...")
            
            val blockedSiteDao = database.blockedSiteDao()
            
            // Clean up any invalid entries
            val allSites = blockedSiteDao.getAllActiveSites()
            var fixedCount = 0
            
            for (site in allSites) {
                // Check for empty patterns that might cause issues
                if (site.pattern.isBlank()) {
                    Log.w(TAG, "Found site with blank pattern, removing...")
                    blockedSiteDao.deleteSite(site)
                    fixedCount++
                }
                
                // Check for invalid regex patterns
                if (site.isRegex) {
                    try {
                        Regex(site.pattern)
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid regex pattern: ${site.pattern}, fixing...")
                        // Convert to non-regex pattern
                        val fixedSite = site.copy(isRegex = false)
                        blockedSiteDao.updateSite(fixedSite)
                        fixedCount++
                    }
                }
            }
            
            if (fixedCount > 0) {
                Log.i(TAG, "✅ Fixed $fixedCount database issues")
            } else {
                Log.d(TAG, "✅ No database issues found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fixing database issues", e)
        }
    }
    
    /**
     * Initialize default safe sites (optional whitelist)
     */
    suspend fun initializeSafeSites() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing safe sites...")
            
            // These would be sites that should never be blocked
            val safeSites = listOf(
                "google.com",
                "youtube.com", // with restrictions
                "wikipedia.org",
                "github.com",
                "stackoverflow.com"
            )
            
            // Store as non-blocked entries or in a separate whitelist table
            // For now, just log them
            Log.d(TAG, "Safe sites noted: ${safeSites.joinToString()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing safe sites", e)
        }
    }
}