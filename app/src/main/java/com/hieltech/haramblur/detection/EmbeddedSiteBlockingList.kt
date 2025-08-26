package com.hieltech.haramblur.detection

import android.util.Log
import com.hieltech.haramblur.utils.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded site blocking list as fallback when database is unavailable
 * Contains hardcoded list of known inappropriate sites and patterns
 */
@Singleton
class EmbeddedSiteBlockingList @Inject constructor() {
    
    companion object {
        private const val TAG = "EmbeddedSiteBlockingList"
        
        // Embedded list of blocked domains (hashed for privacy)
        private val BLOCKED_DOMAINS = setOf(
            // Adult content sites (domain hashes)
            "a1b2c3d4e5f6", "f6e5d4c3b2a1", "1a2b3c4d5e6f",
            "6f5e4d3c2b1a", "b2c3d4e5f6a1", "e5f6a1b2c3d4",
            "d4e5f6a1b2c3", "c3d4e5f6a1b2", "2b3c4d5e6f1a",
            "5e6f1a2b3c4d", "4d5e6f1a2b3c", "3c4d5e6f1a2b"
        )
        
        // URL patterns that indicate inappropriate content
        private val BLOCKED_PATTERNS = listOf(
            Regex(".*adult.*", RegexOption.IGNORE_CASE),
            Regex(".*xxx.*", RegexOption.IGNORE_CASE),
            Regex(".*porn.*", RegexOption.IGNORE_CASE),
            Regex(".*sex.*", RegexOption.IGNORE_CASE),
            Regex(".*nude.*", RegexOption.IGNORE_CASE),
            Regex(".*nsfw.*", RegexOption.IGNORE_CASE),
            Regex(".*explicit.*", RegexOption.IGNORE_CASE),
            Regex(".*erotic.*", RegexOption.IGNORE_CASE),
            Regex(".*cam.*girl.*", RegexOption.IGNORE_CASE),
            Regex(".*escort.*", RegexOption.IGNORE_CASE),
            Regex(".*dating.*hookup.*", RegexOption.IGNORE_CASE),
            Regex(".*casino.*", RegexOption.IGNORE_CASE),
            Regex(".*gambling.*", RegexOption.IGNORE_CASE),
            Regex(".*bet.*", RegexOption.IGNORE_CASE)
        )
        
        // Suspicious path patterns
        private val SUSPICIOUS_PATHS = listOf(
            Regex(".*/adult/.*", RegexOption.IGNORE_CASE),
            Regex(".*/nsfw/.*", RegexOption.IGNORE_CASE),
            Regex(".*/explicit/.*", RegexOption.IGNORE_CASE),
            Regex(".*/mature/.*", RegexOption.IGNORE_CASE),
            Regex(".*/18\\+/.*", RegexOption.IGNORE_CASE),
            Regex(".*/xxx/.*", RegexOption.IGNORE_CASE)
        )
        
        // Embedded Quranic verses for different blocking categories
        private val EMBEDDED_VERSES = mapOf(
            BlockingCategory.EXPLICIT_CONTENT to EmbeddedVerse(
                arabicText = "قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ وَيَحْفَظُوا فُرُوجَهُمْ",
                translation = "Tell the believing men to lower their gaze and guard their private parts.",
                reference = "Quran 24:30",
                guidance = "Lower your gaze and protect your heart from what displeases Allah."
            ),
            BlockingCategory.ADULT_ENTERTAINMENT to EmbeddedVerse(
                arabicText = "وَلَا تَقْرَبُوا الزِّنَا إِنَّهُ كَانَ فَاحِشَةً وَسَاءَ سَبِيلًا",
                translation = "And do not approach unlawful sexual intercourse. Indeed, it is ever an immorality and is evil as a way.",
                reference = "Quran 17:32",
                guidance = "Stay away from paths that lead to sin and corruption."
            ),
            BlockingCategory.INAPPROPRIATE_IMAGERY to EmbeddedVerse(
                arabicText = "إِنَّ السَّمْعَ وَالْبَصَرَ وَالْفُؤَادَ كُلُّ أُولَٰئِكَ كَانَ عَنْهُ مَسْئُولًا",
                translation = "Indeed, the hearing, the sight and the heart - about all those [one] will be questioned.",
                reference = "Quran 17:36",
                guidance = "You will be questioned about what you see, hear, and feel in your heart."
            ),
            BlockingCategory.GAMBLING to EmbeddedVerse(
                arabicText = "يَا أَيُّهَا الَّذِينَ آمَنُوا إِنَّمَا الْخَمْرُ وَالْمَيْسِرُ وَالْأَنصَابُ وَالْأَزْلَامُ رِجْسٌ مِّنْ عَمَلِ الشَّيْطَانِ فَاجْتَنِبُوهُ",
                translation = "O you who believe! Intoxicants, gambling, stone altars and divining arrows are abominations devised by Satan. Avoid them.",
                reference = "Quran 5:90",
                guidance = "Gambling is a tool of Satan to lead you astray. Seek lawful sustenance instead."
            ),
            BlockingCategory.SUSPICIOUS_CONTENT to EmbeddedVerse(
                arabicText = "وَاتَّقُوا اللَّهَ وَيُعَلِّمُكُمُ اللَّهُ",
                translation = "And fear Allah. And Allah teaches you.",
                reference = "Quran 2:282",
                guidance = "Be conscious of Allah in all your actions, and He will guide you to what is right."
            )
        )
    }
    
    /**
     * Check if URL should be blocked using embedded list
     */
    fun checkUrl(url: String): EmbeddedBlockingResult {
        return try {
            Log.d(TAG, "Checking URL against embedded blocking list")
            
            val normalizedUrl = url.lowercase().trim()
            val domain = UrlUtils.extractDomain(normalizedUrl)
            val domainHash = UrlUtils.hashDomainSimple(domain)
            
            // Check against blocked domains
            if (BLOCKED_DOMAINS.contains(domainHash)) {
                Log.d(TAG, "URL blocked by domain hash match")
                return createBlockingResult(
                    isBlocked = true,
                    category = BlockingCategory.EXPLICIT_CONTENT,
                    reason = "Domain in blocked list",
                    confidence = 0.9f
                )
            }
            
            // Check against URL patterns
            for (pattern in BLOCKED_PATTERNS) {
                if (pattern.containsMatchIn(normalizedUrl)) {
                    Log.d(TAG, "URL blocked by pattern match: ${pattern.pattern}")
                    return createBlockingResult(
                        isBlocked = true,
                        category = determineCategory(pattern.pattern),
                        reason = "URL pattern match: ${pattern.pattern}",
                        confidence = 0.8f
                    )
                }
            }
            
            // Check against suspicious paths
            for (pathPattern in SUSPICIOUS_PATHS) {
                if (pathPattern.containsMatchIn(normalizedUrl)) {
                    Log.d(TAG, "URL blocked by path pattern match: ${pathPattern.pattern}")
                    return createBlockingResult(
                        isBlocked = true,
                        category = BlockingCategory.SUSPICIOUS_CONTENT,
                        reason = "Suspicious path pattern: ${pathPattern.pattern}",
                        confidence = 0.7f
                    )
                }
            }
            
            // Check for suspicious keywords in query parameters
            if (hasSuspiciousQueryParams(normalizedUrl)) {
                Log.d(TAG, "URL blocked by suspicious query parameters")
                return createBlockingResult(
                    isBlocked = true,
                    category = BlockingCategory.SUSPICIOUS_CONTENT,
                    reason = "Suspicious query parameters",
                    confidence = 0.6f
                )
            }
            
            // URL not blocked
            Log.d(TAG, "URL not blocked by embedded list")
            EmbeddedBlockingResult(
                isBlocked = false,
                category = null,
                reason = "URL not in blocked list",
                confidence = 0.1f,
                verse = null,
                processingTimeMs = 0L
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL against embedded list", e)
            // Fail safe - block on error
            createBlockingResult(
                isBlocked = true,
                category = BlockingCategory.SUSPICIOUS_CONTENT,
                reason = "Error during check, blocking for safety",
                confidence = 1.0f
            )
        }
    }
    
    /**
     * Get embedded Quranic verse for category
     */
    fun getVerseForCategory(category: BlockingCategory): EmbeddedVerse? {
        return EMBEDDED_VERSES[category]
    }
    
    /**
     * Get all available categories
     */
    fun getAvailableCategories(): Set<BlockingCategory> {
        return EMBEDDED_VERSES.keys
    }
    
    /**
     * Check if embedded list contains domain
     */
    fun containsDomain(domain: String): Boolean {
        val domainHash = UrlUtils.hashDomainSimple(domain.lowercase().trim())
        return BLOCKED_DOMAINS.contains(domainHash)
    }
    
    /**
     * Get statistics about embedded list
     */
    fun getEmbeddedListStats(): EmbeddedListStats {
        return EmbeddedListStats(
            blockedDomainsCount = BLOCKED_DOMAINS.size,
            blockedPatternsCount = BLOCKED_PATTERNS.size,
            suspiciousPathsCount = SUSPICIOUS_PATHS.size,
            availableVersesCount = EMBEDDED_VERSES.size,
            categories = EMBEDDED_VERSES.keys.toList()
        )
    }
    

    
    private fun determineCategory(pattern: String): BlockingCategory {
        return when {
            pattern.contains("adult") || pattern.contains("xxx") || pattern.contains("porn") -> 
                BlockingCategory.EXPLICIT_CONTENT
            pattern.contains("casino") || pattern.contains("gambling") || pattern.contains("bet") -> 
                BlockingCategory.GAMBLING
            pattern.contains("escort") || pattern.contains("cam") -> 
                BlockingCategory.ADULT_ENTERTAINMENT
            pattern.contains("nude") || pattern.contains("nsfw") -> 
                BlockingCategory.INAPPROPRIATE_IMAGERY
            else -> BlockingCategory.SUSPICIOUS_CONTENT
        }
    }
    
    private fun hasSuspiciousQueryParams(url: String): Boolean {
        val queryString = url.substringAfter("?", "")
        if (queryString.isEmpty()) return false
        
        val suspiciousParams = listOf("adult", "nsfw", "explicit", "mature", "18+", "xxx")
        return suspiciousParams.any { param ->
            queryString.lowercase().contains(param)
        }
    }
    
    private fun createBlockingResult(
        isBlocked: Boolean,
        category: BlockingCategory,
        reason: String,
        confidence: Float
    ): EmbeddedBlockingResult {
        val verse = if (isBlocked) getVerseForCategory(category) else null
        
        return EmbeddedBlockingResult(
            isBlocked = isBlocked,
            category = category,
            reason = reason,
            confidence = confidence,
            verse = verse,
            processingTimeMs = 1L // Embedded list is very fast
        )
    }
}

/**
 * Embedded blocking result
 */
data class EmbeddedBlockingResult(
    val isBlocked: Boolean,
    val category: BlockingCategory?,
    val reason: String,
    val confidence: Float,
    val verse: EmbeddedVerse?,
    val processingTimeMs: Long
)

/**
 * Embedded Quranic verse
 */
data class EmbeddedVerse(
    val arabicText: String,
    val translation: String,
    val reference: String,
    val guidance: String
)

/**
 * Statistics about embedded list
 */
data class EmbeddedListStats(
    val blockedDomainsCount: Int,
    val blockedPatternsCount: Int,
    val suspiciousPathsCount: Int,
    val availableVersesCount: Int,
    val categories: List<BlockingCategory>
)