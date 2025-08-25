package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.database.BlockedSiteEntity
import com.hieltech.haramblur.data.database.FalsePositiveEntity
import com.hieltech.haramblur.data.database.FalsePositiveStatus
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for site blocking functionality
 */
interface SiteBlockingManager {
    suspend fun checkUrl(url: String): SiteBlockingResult
    suspend fun getQuranicVerse(category: BlockingCategory): QuranicVerse?
    suspend fun updateBlockingDatabase(): Boolean
    suspend fun reportFalsePositive(url: String, reason: String): Boolean
    suspend fun isUrlBlocked(url: String): Boolean
    suspend fun getBlockingCategory(url: String): BlockingCategory?
    suspend fun addCustomBlockedSite(url: String, category: BlockingCategory): Boolean
    suspend fun removeBlockedSite(url: String): Boolean
}

/**
 * Result of site blocking check
 */
data class SiteBlockingResult(
    val isBlocked: Boolean,
    val category: BlockingCategory?,
    val confidence: Float,
    val quranicVerse: QuranicVerse?,
    val reflectionTimeSeconds: Int,
    val matchedPattern: String? = null,
    val blockingReason: String? = null
)

/**
 * Implementation of site blocking manager with URL pattern matching
 */
@Singleton
class SiteBlockingManagerImpl @Inject constructor(
    private val database: SiteBlockingDatabase
) : SiteBlockingManager {
    
    // Cache for frequently checked domains
    private val domainCache = mutableMapOf<String, SiteBlockingResult>()
    private val maxCacheSize = 1000
    
    // Compiled regex patterns cache
    private val regexCache = mutableMapOf<String, Pattern>()
    
    override suspend fun checkUrl(url: String): SiteBlockingResult = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = cleanUrl(url)
            val domain = extractDomain(cleanUrl)
            
            // Check cache first
            domainCache[domain]?.let { cachedResult ->
                return@withContext cachedResult
            }
            
            // Check against blocked sites
            val blockingResult = checkAgainstBlockedSites(domain, cleanUrl)
            
            // Cache the result
            cacheResult(domain, blockingResult)
            
            blockingResult
        } catch (e: Exception) {
            // Return safe default on error
            SiteBlockingResult(
                isBlocked = false,
                category = null,
                confidence = 0.0f,
                quranicVerse = null,
                reflectionTimeSeconds = 0,
                blockingReason = "Error processing URL: ${e.message}"
            )
        }
    }
    
    override suspend fun getQuranicVerse(category: BlockingCategory): QuranicVerse? = withContext(Dispatchers.IO) {
        try {
            val verseEntity = database.quranicVerseDao().getRandomVerseByCategory(category)
            verseEntity?.let { entity ->
                QuranicVerse(
                    id = entity.id,
                    surahName = entity.surahName,
                    surahNumber = entity.surahNumber,
                    verseNumber = entity.verseNumber,
                    arabicText = entity.arabicText,
                    transliteration = entity.transliteration,
                    translations = buildTranslationMap(entity),
                    category = entity.category,
                    context = entity.context,
                    reflection = entity.reflection,
                    audioUrl = entity.audioUrl
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun updateBlockingDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear cache when database is updated
            domainCache.clear()
            regexCache.clear()
            
            // In a real implementation, this would fetch updates from a remote source
            // For now, we'll just return true to indicate the local database is current
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun reportFalsePositive(url: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val urlHash = hashDomain(url)
            val report = FalsePositiveEntity(
                urlHash = urlHash,
                originalUrl = url,
                reportedAt = System.currentTimeMillis(),
                reason = reason,
                status = FalsePositiveStatus.PENDING
            )
            
            database.falsePositiveDao().insertReport(report)
            
            // Remove from cache to prevent continued blocking
            val domain = extractDomain(url)
            domainCache.remove(domain)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun isUrlBlocked(url: String): Boolean {
        return checkUrl(url).isBlocked
    }
    
    override suspend fun getBlockingCategory(url: String): BlockingCategory? {
        return checkUrl(url).category
    }
    
    override suspend fun addCustomBlockedSite(url: String, category: BlockingCategory): Boolean = withContext(Dispatchers.IO) {
        try {
            val domain = extractDomain(url)
            val blockedSite = BlockedSiteEntity(
                domainHash = hashDomain(domain),
                pattern = domain,
                category = category,
                confidence = 1.0f,
                lastUpdated = System.currentTimeMillis(),
                isRegex = false,
                source = "user_added",
                description = "User-added blocked site"
            )
            
            database.blockedSiteDao().insertSite(blockedSite)
            
            // Clear cache for this domain
            domainCache.remove(domain)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun removeBlockedSite(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val domain = extractDomain(url)
            val domainHash = hashDomain(domain)
            
            database.blockedSiteDao().deactivateSite(domainHash)
            
            // Clear cache for this domain
            domainCache.remove(domain)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check URL against blocked sites database
     */
    private suspend fun checkAgainstBlockedSites(domain: String, fullUrl: String): SiteBlockingResult {
        val blockedSiteDao = database.blockedSiteDao()
        
        // First check exact domain match
        val domainHash = hashDomain(domain)
        val exactMatch = blockedSiteDao.getSiteByDomainHash(domainHash)
        
        if (exactMatch != null) {
            return createBlockingResult(exactMatch, domain)
        }
        
        // Check pattern matches (including subdomains)
        val patternMatches = blockedSiteDao.getSitesByPattern("%$domain%")
        for (site in patternMatches) {
            if (matchesPattern(domain, site.pattern, site.isRegex)) {
                return createBlockingResult(site, site.pattern)
            }
        }
        
        // Check regex patterns
        val regexSites = blockedSiteDao.getRegexSites()
        for (site in regexSites) {
            if (matchesRegexPattern(fullUrl, site.pattern)) {
                return createBlockingResult(site, site.pattern)
            }
        }
        
        // Check for suspicious patterns in URL
        val suspiciousResult = checkSuspiciousPatterns(fullUrl)
        if (suspiciousResult.isBlocked) {
            return suspiciousResult
        }
        
        // Not blocked
        return SiteBlockingResult(
            isBlocked = false,
            category = null,
            confidence = 0.0f,
            quranicVerse = null,
            reflectionTimeSeconds = 0
        )
    }
    
    /**
     * Create blocking result from blocked site entity
     */
    private suspend fun createBlockingResult(site: BlockedSiteEntity, matchedPattern: String): SiteBlockingResult {
        val verse = getQuranicVerse(site.category)
        
        return SiteBlockingResult(
            isBlocked = true,
            category = site.category,
            confidence = site.confidence,
            quranicVerse = verse,
            reflectionTimeSeconds = site.category.defaultReflectionTime,
            matchedPattern = matchedPattern,
            blockingReason = "Matched blocked site pattern"
        )
    }
    
    /**
     * Check for suspicious patterns in URL that might indicate inappropriate content
     */
    private fun checkSuspiciousPatterns(url: String): SiteBlockingResult {
        val suspiciousKeywords = listOf(
            "porn", "sex", "xxx", "adult", "nude", "naked", "cam", "live",
            "casino", "bet", "gambling", "poker", "slots", "jackpot",
            "escort", "hookup", "dating", "singles", "meet"
        )
        
        val lowercaseUrl = url.lowercase()
        
        for (keyword in suspiciousKeywords) {
            if (lowercaseUrl.contains(keyword)) {
                val category = when (keyword) {
                    "porn", "sex", "xxx", "nude", "naked" -> BlockingCategory.EXPLICIT_CONTENT
                    "adult", "cam", "live", "escort" -> BlockingCategory.ADULT_ENTERTAINMENT
                    "casino", "bet", "gambling", "poker", "slots", "jackpot" -> BlockingCategory.GAMBLING
                    "hookup", "dating", "singles", "meet" -> BlockingCategory.DATING_SITES
                    else -> BlockingCategory.SUSPICIOUS_CONTENT
                }
                
                return SiteBlockingResult(
                    isBlocked = true,
                    category = category,
                    confidence = 0.6f, // Lower confidence for heuristic matching
                    quranicVerse = null, // Will be fetched if needed
                    reflectionTimeSeconds = category.defaultReflectionTime,
                    matchedPattern = keyword,
                    blockingReason = "Suspicious keyword detected: $keyword"
                )
            }
        }
        
        return SiteBlockingResult(
            isBlocked = false,
            category = null,
            confidence = 0.0f,
            quranicVerse = null,
            reflectionTimeSeconds = 0
        )
    }
    
    /**
     * Check if domain matches a pattern
     */
    private fun matchesPattern(domain: String, pattern: String, isRegex: Boolean): Boolean {
        return if (isRegex) {
            matchesRegexPattern(domain, pattern)
        } else {
            // Simple wildcard matching
            domain.contains(pattern, ignoreCase = true) ||
            pattern.contains(domain, ignoreCase = true) ||
            domain.endsWith(".$pattern", ignoreCase = true)
        }
    }
    
    /**
     * Check if URL matches a regex pattern
     */
    private fun matchesRegexPattern(url: String, pattern: String): Boolean {
        return try {
            val compiledPattern = regexCache.getOrPut(pattern) {
                Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            }
            compiledPattern.matcher(url).find()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clean and normalize URL
     */
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
    
    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val urlObj = URL(url)
            var host = urlObj.host.lowercase()
            
            // Remove www prefix
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            
            host
        } catch (e: Exception) {
            // Fallback: try to extract domain manually
            url.replace(Regex("^https?://"), "")
                .replace(Regex("^www\\."), "")
                .split("/")[0]
                .split("?")[0]
                .lowercase()
        }
    }
    
    /**
     * Hash domain for privacy
     */
    private fun hashDomain(domain: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(domain.lowercase().toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Build translation map from entity
     */
    private fun buildTranslationMap(entity: com.hieltech.haramblur.data.database.QuranicVerseEntity): Map<Language, String> {
        val translations = mutableMapOf<Language, String>()
        
        translations[Language.ENGLISH] = entity.englishTranslation
        entity.arabicTranslation?.let { translations[Language.ARABIC] = it }
        entity.urduTranslation?.let { translations[Language.URDU] = it }
        entity.frenchTranslation?.let { translations[Language.FRENCH] = it }
        entity.indonesianTranslation?.let { translations[Language.INDONESIAN] = it }
        
        return translations
    }
    
    /**
     * Cache blocking result
     */
    private fun cacheResult(domain: String, result: SiteBlockingResult) {
        if (domainCache.size >= maxCacheSize) {
            // Remove oldest entries (simple FIFO)
            val keysToRemove = domainCache.keys.take(maxCacheSize / 4)
            keysToRemove.forEach { domainCache.remove(it) }
        }
        
        domainCache[domain] = result
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        domainCache.clear()
        regexCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "domainCacheSize" to domainCache.size,
            "regexCacheSize" to regexCache.size,
            "maxCacheSize" to maxCacheSize
        )
    }
}