package com.hieltech.haramblur.detection

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.database.BlockedSiteDao
import com.hieltech.haramblur.data.database.BlockedSiteEntity
import com.hieltech.haramblur.data.database.FalsePositiveDao
import com.hieltech.haramblur.data.database.FalsePositiveEntity
import com.hieltech.haramblur.data.database.FalsePositiveStatus
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import com.hieltech.haramblur.utils.UrlUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

/**
 * Unified site blocking manager that consolidates all blocking functionality
 * Combines database-based blocking, embedded lists, and custom user rules
 */
@Singleton
class UnifiedSiteBlockingManager @Inject constructor(
    private val database: SiteBlockingDatabase,
    private val quranicRepository: QuranicRepository,
    private val errorRecovery: ComprehensiveErrorRecovery,
    @ApplicationContext private val context: Context
) : SiteBlockingManager {

    companion object {
        private const val TAG = "UnifiedSiteBlockingManager"
        private const val CACHE_EXPIRY_MS = 300_000L // 5 minutes
        private const val MAX_CACHE_SIZE = 1000
        private const val MAX_RECURSION_DEPTH = 3
    }

    // Thread-safe caches
    private val domainCache = ConcurrentHashMap<String, CachedResult>()
    private val regexCache = ConcurrentHashMap<String, Pattern>()
    private val whitelistCache = ConcurrentHashMap<String, Boolean>()
    
    // Bloom filter for fast negative lookups (will be implemented)
    private var bloomFilter: BloomFilter? = null
    
    // Database access objects
    private val blockedSiteDao: BlockedSiteDao by lazy { database.blockedSiteDao() }
    private val falsePositiveDao: FalsePositiveDao by lazy { database.falsePositiveDao() }
    
    // Mutex for database operations
    private val dbMutex = Mutex()

    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Use the comprehensive adult content database
    private val prebuiltAdultDomains = AdultContentDatabase.PREBUILT_ADULT_DOMAINS
    
    // Use comprehensive adult content patterns from database
    private val adultContentPatterns = listOf(
        Pattern.compile(".*\\b(${AdultContentDatabase.ADULT_KEYWORDS.joinToString("|")})\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.(${AdultContentDatabase.ADULT_TLDS.joinToString("|") { it.substring(1) }})\\b.*", Pattern.CASE_INSENSITIVE)
    )

    // Use comprehensive adult keywords from database
    private val suspiciousKeywords = AdultContentDatabase.ADULT_KEYWORDS
    
    // Whitelisted domains that should never be blocked
    private val whitelistedDomains = setOf(
        "google.com", "youtube.com", "wikipedia.org", "github.com", "stackoverflow.com",
        "microsoft.com", "apple.com", "amazon.com", "facebook.com", "twitter.com",
        "linkedin.com", "reddit.com", "quora.com", "medium.com", "netflix.com",
        "spotify.com", "dropbox.com", "zoom.us", "slack.com", "discord.com"
    )

    data class CachedResult(
        val result: SiteBlockingResult,
        val timestamp: Long
    )

    init {
        // Initialize bloom filter for performance
        initializeBloomFilter()
        
        // Precompile regex patterns
        precompileRegexPatterns()
        
        Log.d(TAG, "UnifiedSiteBlockingManager initialized with ${prebuiltAdultDomains.size} prebuilt adult domains")
    }

    override suspend fun checkUrl(url: String): SiteBlockingResult {
        return errorRecovery.executeBlockingOperation(url) {
            withContext(Dispatchers.IO) {
                if (url.isBlank()) {
                    return@withContext createSafeResult("Blank URL")
                }

                val cleanUrl = UrlUtils.cleanUrl(url)
                val domain = UrlUtils.extractDomain(cleanUrl)

                if (domain.isBlank()) {
                    return@withContext createSafeResult("Invalid domain")
                }

                // Check cache first
                val cachedResult = getCachedResult(domain)
                if (cachedResult != null) {
                    Log.v(TAG, "Cache hit for domain: $domain")
                    return@withContext cachedResult
                }

                // Early whitelist check
                if (isWhitelistedDomain(domain)) {
                    val result = createSafeResult("Whitelisted domain")
                    cacheResult(domain, result)
                    return@withContext result
                }

                // Multi-strategy blocking check
                val blockingResult = performBlockingCheck(cleanUrl, domain)

                // Cache the result
                cacheResult(domain, blockingResult)

                blockingResult
            }
        }
    }

    /**
     * Perform comprehensive blocking check using multiple strategies
     */
    private suspend fun performBlockingCheck(url: String, domain: String): SiteBlockingResult {
        // Strategy 1: Check prebuilt adult content (highest priority)
        val prebuiltResult = checkPrebuiltAdultContent(url, domain)
        if (prebuiltResult.isBlocked) {
            Log.d(TAG, "Blocked by prebuilt adult content: $domain")
            return prebuiltResult
        }

        // Strategy 2: Check database patterns
        val databaseResult = checkDatabasePatterns(url, domain)
        if (databaseResult.isBlocked) {
            Log.d(TAG, "Blocked by database pattern: $domain")
            return databaseResult
        }

        // Strategy 3: Check suspicious patterns
        val suspiciousResult = checkSuspiciousPatterns(url)
        if (suspiciousResult.isBlocked) {
            Log.d(TAG, "Blocked by suspicious pattern: $domain")
            return suspiciousResult
        }

        // Not blocked
        return createSafeResult("Not blocked")
    }

    /**
     * Check against prebuilt adult content database with bloom filter optimization
     */
    private suspend fun checkPrebuiltAdultContent(url: String, domain: String): SiteBlockingResult {
        val domainHash = UrlUtils.hashDomainSha256(domain)

        // Fast negative lookup using bloom filter
        if (bloomFilter?.mightContain(domainHash) == false) {
            // Definitely not in the set, skip expensive checks
            return createSafeResult("Not in bloom filter")
        }

        // Check exact domain hash match (bloom filter said it might be present)
        if (prebuiltAdultDomains.contains(domainHash)) {
            return createBlockingResult(
                category = BlockingCategory.EXPLICIT_CONTENT,
                confidence = 1.0f,
                reason = "Prebuilt adult content site",
                matchedPattern = domain
            )
        }

        // Check URL patterns for adult content
        val lowercaseUrl = url.lowercase()
        for (pattern in adultContentPatterns) {
            if (pattern.matcher(lowercaseUrl).matches()) {
                return createBlockingResult(
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    confidence = 0.95f,
                    reason = "Adult content pattern match",
                    matchedPattern = pattern.pattern()
                )
            }
        }

        // Quick keyword check
        for (keyword in suspiciousKeywords) {
            if (lowercaseUrl.contains(keyword)) {
                return createBlockingResult(
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    confidence = 0.8f,
                    reason = "Adult content keyword: $keyword",
                    matchedPattern = keyword
                )
            }
        }

        return createSafeResult("Not adult content")
    }

    /**
     * Check against database patterns
     */
    private suspend fun checkDatabasePatterns(url: String, domain: String): SiteBlockingResult {
        return try {
            // Check exact domain match
            val domainHash = UrlUtils.hashDomainSha256(domain)
            val exactMatch = blockedSiteDao.getSiteByDomainHash(domainHash)
            
            if (exactMatch != null && exactMatch.isActive) {
                return createBlockingResult(
                    category = exactMatch.category,
                    confidence = exactMatch.confidence,
                    reason = "Database exact match",
                    matchedPattern = exactMatch.pattern
                )
            }

            // Check pattern matches
            val patternMatches = blockedSiteDao.getSitesByPattern("%$domain%")
            for (site in patternMatches) {
                if (site.isActive && matchesPattern(domain, site.pattern, site.isRegex)) {
                    return createBlockingResult(
                        category = site.category,
                        confidence = site.confidence,
                        reason = "Database pattern match",
                        matchedPattern = site.pattern
                    )
                }
            }

            // Check regex patterns
            val regexSites = blockedSiteDao.getRegexSites()
            for (site in regexSites) {
                if (site.isActive && matchesRegexPattern(url, site.pattern)) {
                    return createBlockingResult(
                        category = site.category,
                        confidence = site.confidence,
                        reason = "Database regex match",
                        matchedPattern = site.pattern
                    )
                }
            }

            createSafeResult("Not in database")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking database patterns", e)
            createSafeResult("Database error: ${e.message}")
        }
    }

    /**
     * Check for suspicious patterns in URL
     * FIXED: Focus on domain-only analysis to prevent false positives from query parameters
     */
    private fun checkSuspiciousPatterns(url: String): SiteBlockingResult {
        val cleanUrl = UrlUtils.cleanUrl(url)
        val domain = UrlUtils.extractDomain(cleanUrl)
        val lowercaseUrl = url.lowercase()
        val lowercaseDomain = domain.lowercase()

        // Early exit for authentication and legitimate service URLs
        if (isAuthenticationUrl(cleanUrl) || isLegitimateServiceUrl(domain)) {
            return createSafeResult("Legitimate authentication/service URL")
        }

        // Check for dating/hookup keywords (domain only)
        val datingKeywords = listOf(
            "dating", "hookup", "tinder", "bumble", "match", "singles",
            "flirt", "romance", "meet", "chat", "adult-dating"
        )

        for (keyword in datingKeywords) {
            if (lowercaseDomain.contains(keyword)) {
                return SiteBlockingResult(
                    isBlocked = true,
                    category = BlockingCategory.DATING_SITES,
                    confidence = 0.7f,
                    quranicVerse = null,
                    reflectionTimeSeconds = BlockingCategory.DATING_SITES.defaultReflectionTime,
                    matchedPattern = keyword,
                    blockingReason = "Dating/hookup site detected: $keyword"
                )
            }
        }

        // Check for porn TLD patterns (domain only)
        val pornTlds = listOf(".porn", ".sex", ".xxx", ".adult", ".cam", ".tube", ".video")
        for (tld in pornTlds) {
            if (lowercaseDomain.contains(tld)) {
                return SiteBlockingResult(
                    isBlocked = true,
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    confidence = 0.95f,
                    quranicVerse = null,
                    reflectionTimeSeconds = BlockingCategory.EXPLICIT_CONTENT.defaultReflectionTime,
                    matchedPattern = tld,
                    blockingReason = "Pornographic TLD detected: $tld"
                )
            }
        }

        return createSafeResult("No suspicious patterns")
    }

    /**
     * Check if URL is an authentication/OAuth URL that should never be blocked
     */
    private fun isAuthenticationUrl(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        
        // Google authentication URLs
        if (lowercaseUrl.contains("accounts.google.com") || 
            lowercaseUrl.contains("oauth2.googleapis.com") ||
            lowercaseUrl.contains("google.com/oauth")) {
            return true
        }
        
        // Common authentication patterns
        val authPatterns = listOf(
            "oauth", "authenticate", "signin", "login", "sso", "auth",
            "openid", "saml", "cas", "oidc"
        )
        
        // Check if URL contains authentication patterns in the path (not domain)
        for (pattern in authPatterns) {
            if (lowercaseUrl.contains("/$pattern") || 
                lowercaseUrl.contains("?$pattern") ||
                lowercaseUrl.contains("&$pattern")) {
                return true
            }
        }
        
        // Check for common OAuth parameters
        val oauthParams = listOf(
            "client_id=", "redirect_uri=", "response_type=", "scope=",
            "access_token=", "refresh_token=", "code=", "state="
        )
        
        for (param in oauthParams) {
            if (lowercaseUrl.contains(param)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Check if domain is a legitimate service that should never be blocked
     */
    private fun isLegitimateServiceUrl(domain: String): Boolean {
        val lowercaseDomain = domain.lowercase()
        
        // Major tech companies and their services
        val legitimateDomains = setOf(
            "google.com", "accounts.google.com", "oauth2.googleapis.com",
            "microsoft.com", "login.microsoftonline.com", "apple.com",
            "facebook.com", "amazon.com", "netflix.com", "spotify.com",
            "github.com", "stackoverflow.com", "linkedin.com", "twitter.com",
            "instagram.com", "youtube.com", "discord.com", "slack.com"
        )
        
        // Check exact match or subdomain
        return legitimateDomains.any { legitDomain ->
            lowercaseDomain == legitDomain || lowercaseDomain.endsWith(".$legitDomain")
        }
    }

    /**
     * Check if domain is whitelisted
     */
    private fun isWhitelistedDomain(domain: String): Boolean {
        // Check cache first
        whitelistCache[domain]?.let { return it }

        val lowercaseDomain = domain.lowercase()
        val isWhitelisted = whitelistedDomains.any { whitelistDomain ->
            lowercaseDomain == whitelistDomain ||
            lowercaseDomain.endsWith(".$whitelistDomain")
        }

        // Cache result
        if (whitelistCache.size >= MAX_CACHE_SIZE / 4) {
            whitelistCache.clear() // Simple cache eviction
        }
        whitelistCache[domain] = isWhitelisted

        return isWhitelisted
    }

    /**
     * Pattern matching with improved precision
     */
    private fun matchesPattern(domain: String, pattern: String, isRegex: Boolean): Boolean {
        return if (isRegex) {
            matchesRegexPattern(domain, pattern)
        } else {
            // Improved wildcard matching to reduce false positives
            when {
                pattern.startsWith("*.") -> {
                    val patternDomain = pattern.substring(2)
                    domain == patternDomain || domain.endsWith(".$patternDomain")
                }
                pattern.contains("*") -> {
                    val regexPattern = pattern.replace("*", ".*")
                    domain.matches(regexPattern.toRegex(RegexOption.IGNORE_CASE))
                }
                else -> {
                    domain == pattern || domain.endsWith(".$pattern")
                }
            }
        }
    }

    /**
     * Regex pattern matching with caching
     */
    private fun matchesRegexPattern(text: String, pattern: String): Boolean {
        return try {
            val compiledPattern = regexCache.getOrPut(pattern) {
                Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            }
            compiledPattern.matcher(text).find()
        } catch (e: Exception) {
            Log.w(TAG, "Invalid regex pattern: $pattern", e)
            false
        }
    }

    /**
     * Cache management
     */
    private fun getCachedResult(domain: String): SiteBlockingResult? {
        val cached = domainCache[domain] ?: return null
        val currentTime = System.currentTimeMillis()

        return if (currentTime - cached.timestamp < CACHE_EXPIRY_MS) {
            cached.result
        } else {
            domainCache.remove(domain)
            null
        }
    }

    private fun cacheResult(domain: String, result: SiteBlockingResult) {
        if (domainCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries (simple LRU)
            val oldestEntries = domainCache.entries
                .sortedBy { it.value.timestamp }
                .take(MAX_CACHE_SIZE / 4)
            oldestEntries.forEach { domainCache.remove(it.key) }
        }

        domainCache[domain] = CachedResult(result, System.currentTimeMillis())
    }

    /**
     * Initialize bloom filter for performance optimization
     */
    private fun initializeBloomFilter() {
        try {
            bloomFilter = BloomFilter(
                expectedElements = prebuiltAdultDomains.size + 5000, // Account for database entries
                falsePositiveRate = 0.01 // 1% false positive rate
            )

            // Add all prebuilt adult domains to bloom filter
            for (domainHash in prebuiltAdultDomains) {
                bloomFilter?.add(domainHash)
            }

            Log.d(TAG, "Bloom filter initialized with ${prebuiltAdultDomains.size} prebuilt domains")

            // Asynchronously load database domains into bloom filter
            serviceScope.launch {
                loadDatabaseDomainsIntoBloomFilter()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize bloom filter", e)
            bloomFilter = null
        }
    }

    /**
     * Load database domains into bloom filter for performance
     */
    private suspend fun loadDatabaseDomainsIntoBloomFilter() {
        try {
            val activeSites = blockedSiteDao.getAllActiveSites()
            var addedCount = 0

            for (site in activeSites) {
                bloomFilter?.add(site.domainHash)
                addedCount++
            }

            Log.d(TAG, "Added $addedCount database domains to bloom filter")

            val stats = bloomFilter?.getStats()
            Log.d(TAG, "Bloom filter stats: ${stats}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load database domains into bloom filter", e)
        }
    }

    /**
     * Precompile regex patterns for better performance
     */
    private fun precompileRegexPatterns() {
        // Precompile common adult content patterns
        adultContentPatterns.forEach { pattern ->
            regexCache[pattern.pattern()] = pattern
        }
        Log.d(TAG, "Precompiled ${adultContentPatterns.size} regex patterns")
    }

    private fun createSafeResult(reason: String): SiteBlockingResult {
        return SiteBlockingResult(
            isBlocked = false,
            category = null,
            confidence = 0.0f,
            quranicVerse = null,
            reflectionTimeSeconds = 0,
            blockingReason = reason
        )
    }

    private suspend fun createBlockingResult(
        category: BlockingCategory,
        confidence: Float,
        reason: String,
        matchedPattern: String
    ): SiteBlockingResult {
        val verse = quranicRepository.getVerseForCategory(category)
        return SiteBlockingResult(
            isBlocked = true,
            category = category,
            confidence = confidence,
            quranicVerse = verse,
            reflectionTimeSeconds = category.defaultReflectionTime,
            blockingReason = reason,
            matchedPattern = matchedPattern
        )
    }

    // Implement remaining SiteBlockingManager interface methods

    override suspend fun getQuranicVerse(category: BlockingCategory): QuranicVerse? {
        return quranicRepository.getVerseForCategory(category)
    }

    override suspend fun updateBlockingDatabase(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Clear caches to force refresh
            domainCache.clear()
            whitelistCache.clear()

            // TODO: Implement database update logic if needed
            Log.d(TAG, "Blocking database updated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update blocking database", e)
            false
        }
    }

    override suspend fun reportFalsePositive(url: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        dbMutex.withLock {
            try {
                val urlHash = UrlUtils.hashDomainSha256(url)
                val report = FalsePositiveEntity(
                    urlHash = urlHash,
                    originalUrl = url,
                    reportedAt = System.currentTimeMillis(),
                    reason = reason,
                    status = FalsePositiveStatus.PENDING
                )

                falsePositiveDao.insertReport(report)

                // Remove from cache to prevent continued blocking
                val domain = UrlUtils.extractDomain(url)
                domainCache.remove(domain)

                Log.d(TAG, "False positive reported for: $url")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report false positive", e)
                false
            }
        }
    }

    override suspend fun isUrlBlocked(url: String): Boolean {
        return checkUrl(url).isBlocked
    }

    override suspend fun getBlockingCategory(url: String): BlockingCategory? {
        return checkUrl(url).category
    }

    override suspend fun addCustomBlockedSite(url: String, category: BlockingCategory): Boolean = withContext(Dispatchers.IO) {
        dbMutex.withLock {
            try {
                if (url.isBlank()) {
                    Log.w(TAG, "Cannot add blank URL to blocked list")
                    return@withContext false
                }

                val cleanUrl = UrlUtils.cleanUrl(url)
                val domain = UrlUtils.extractDomain(cleanUrl)

                if (domain.isBlank()) {
                    Log.w(TAG, "Invalid domain extracted from URL: $url")
                    return@withContext false
                }

                // Check if domain is whitelisted
                if (isWhitelistedDomain(domain)) {
                    Log.w(TAG, "Cannot block whitelisted domain: $domain")
                    return@withContext false
                }

                val domainHash = UrlUtils.hashDomainSha256(domain)
                val currentTime = System.currentTimeMillis()

                // Check if already exists
                val existingSite = blockedSiteDao.getSiteByDomainHash(domainHash)
                if (existingSite != null && existingSite.isActive) {
                    Log.i(TAG, "Site already blocked: $domain")
                    return@withContext false
                }

                // Insert custom site
                val blockedSite = BlockedSiteEntity(
                    domainHash = domainHash,
                    pattern = domain,
                    category = category,
                    confidence = 1.0f,
                    lastUpdated = currentTime,
                    isRegex = false,
                    source = "user_added",
                    description = "User-added blocked site",
                    addedByUser = true,
                    dateAdded = currentTime
                )

                blockedSiteDao.insertSite(blockedSite)

                // Clear cache for this domain
                domainCache.remove(domain)

                Log.i(TAG, "Successfully added custom blocked site: $domain")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add custom blocked site", e)
                false
            }
        }
    }

    override suspend fun removeBlockedSite(url: String): Boolean = withContext(Dispatchers.IO) {
        dbMutex.withLock {
            try {
                val domain = UrlUtils.extractDomain(url)
                val domainHash = UrlUtils.hashDomainSha256(domain)

                // Only allow removal of user-added sites
                val site = blockedSiteDao.getSiteByDomainHash(domainHash)
                if (site?.addedByUser == true) {
                    blockedSiteDao.deactivateSite(domainHash)
                    domainCache.remove(domain)
                    Log.i(TAG, "Removed blocked site: $domain")
                    true
                } else {
                    Log.w(TAG, "Cannot remove non-user-added site: $domain")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove blocked site", e)
                false
            }
        }
    }

    /**
     * Additional utility methods for enhanced functionality
     */

    suspend fun getCustomBlockedWebsites(): List<BlockedSiteEntity> = withContext(Dispatchers.IO) {
        try {
            blockedSiteDao.getUserAddedSites()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get custom blocked websites", e)
            emptyList()
        }
    }

    suspend fun isCustomBlocked(url: String): Boolean {
        return try {
            val domain = UrlUtils.extractDomain(url)
            val domainHash = UrlUtils.hashDomainSha256(domain)
            val site = blockedSiteDao.getSiteByDomainHash(domainHash)
            site?.addedByUser == true && site.isActive
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if URL is custom blocked", e)
            false
        }
    }

    /**
     * Get comprehensive blocking statistics
     */
    suspend fun getBlockingStats(): BlockingStats = withContext(Dispatchers.IO) {
        try {
            val totalBlocked = blockedSiteDao.getActiveSiteCount()
            val userAdded = blockedSiteDao.getUserAddedSites().size
            val prebuilt = prebuiltAdultDomains.size
            val cacheHitRate = calculateCacheHitRate()

            BlockingStats(
                totalBlockedSites = totalBlocked,
                userAddedSites = userAdded,
                prebuiltAdultSites = prebuilt,
                cacheHitRate = cacheHitRate,
                cacheSize = domainCache.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get blocking stats", e)
            BlockingStats(0, 0, prebuiltAdultDomains.size, 0.0f, domainCache.size)
        }
    }

    private fun calculateCacheHitRate(): Float {
        // Simple cache hit rate calculation
        return if (domainCache.size > 0) {
            domainCache.size.toFloat() / MAX_CACHE_SIZE.toFloat()
        } else {
            0.0f
        }
    }

    /**
     * Clear all caches - useful for testing or memory management
     */
    fun clearCaches() {
        domainCache.clear()
        whitelistCache.clear()
        regexCache.clear()
        Log.d(TAG, "All caches cleared")
    }
}

/**
 * Data class for blocking statistics
 */
data class BlockingStats(
    val totalBlockedSites: Int,
    val userAddedSites: Int,
    val prebuiltAdultSites: Int,
    val cacheHitRate: Float,
    val cacheSize: Int
)

/**
 * Simple Bloom Filter implementation for fast negative lookups
 * Optimized for domain blocking with low false positive rate
 */
class BloomFilter(
    private val expectedElements: Int = 10000,
    private val falsePositiveRate: Double = 0.01
) {
    private val bitArray: BooleanArray
    private val hashFunctions: Int
    private val bitArraySize: Int

    init {
        // Calculate optimal bit array size and number of hash functions
        bitArraySize = (-expectedElements * ln(falsePositiveRate) / (ln(2.0) * ln(2.0))).toInt()
        hashFunctions = (bitArraySize * ln(2.0) / expectedElements).toInt().coerceAtLeast(1)
        bitArray = BooleanArray(bitArraySize)

        Log.d("BloomFilter", "Initialized with size=$bitArraySize, hashFunctions=$hashFunctions")
    }

    /**
     * Add element to bloom filter
     */
    fun add(element: String) {
        val hashes = getHashes(element)
        for (hash in hashes) {
            bitArray[hash] = true
        }
    }

    /**
     * Check if element might be in the set
     * Returns true if element might be present (could be false positive)
     * Returns false if element is definitely not present
     */
    fun mightContain(element: String): Boolean {
        val hashes = getHashes(element)
        return hashes.all { bitArray[it] }
    }

    /**
     * Generate hash values for an element
     */
    private fun getHashes(element: String): IntArray {
        val hash1 = element.hashCode()
        val hash2 = element.reversed().hashCode()

        val hashes = IntArray(hashFunctions)
        for (i in 0 until hashFunctions) {
            hashes[i] = Math.abs((hash1 + i * hash2) % bitArraySize)
        }
        return hashes
    }

    /**
     * Get current false positive probability estimate
     */
    fun getCurrentFalsePositiveRate(): Double {
        val setBits = bitArray.count { it }
        val ratio = setBits.toDouble() / bitArraySize
        return Math.pow(ratio, hashFunctions.toDouble())
    }

    /**
     * Clear the bloom filter
     */
    fun clear() {
        bitArray.fill(false)
    }

    /**
     * Get statistics about the bloom filter
     */
    fun getStats(): BloomFilterStats {
        val setBits = bitArray.count { it }
        return BloomFilterStats(
            bitArraySize = bitArraySize,
            setBits = setBits,
            hashFunctions = hashFunctions,
            estimatedFalsePositiveRate = getCurrentFalsePositiveRate()
        )
    }
}

/**
 * Statistics for bloom filter performance monitoring
 */
data class BloomFilterStats(
    val bitArraySize: Int,
    val setBits: Int,
    val hashFunctions: Int,
    val estimatedFalsePositiveRate: Double
)
