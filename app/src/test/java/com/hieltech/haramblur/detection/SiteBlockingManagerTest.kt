package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.database.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SiteBlockingManagerTest {
    
    private lateinit var siteBlockingManager: SiteBlockingManagerImpl
    private lateinit var mockDatabase: SiteBlockingDatabase
    private lateinit var mockBlockedSiteDao: BlockedSiteDao
    private lateinit var mockQuranicVerseDao: QuranicVerseDao
    private lateinit var mockFalsePositiveDao: FalsePositiveDao
    
    @Before
    fun setup() {
        mockDatabase = mockk()
        mockBlockedSiteDao = mockk()
        mockQuranicVerseDao = mockk()
        mockFalsePositiveDao = mockk()
        
        every { mockDatabase.blockedSiteDao() } returns mockBlockedSiteDao
        every { mockDatabase.quranicVerseDao() } returns mockQuranicVerseDao
        every { mockDatabase.falsePositiveDao() } returns mockFalsePositiveDao
        
        siteBlockingManager = SiteBlockingManagerImpl(mockDatabase)
    }
    
    @Test
    fun `should block known pornographic sites`() = runTest {
        // Given
        val testUrl = "https://pornhub.com/video/123"
        val expectedHash = "a1b2c3d4e5f6" // Mock hash
        val blockedSite = BlockedSiteEntity(
            id = 1,
            domainHash = expectedHash,
            pattern = "pornhub.com",
            category = BlockingCategory.EXPLICIT_CONTENT,
            confidence = 1.0f,
            lastUpdated = System.currentTimeMillis()
        )
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns blockedSite
        coEvery { mockQuranicVerseDao.getRandomVerseByCategory(BlockingCategory.EXPLICIT_CONTENT) } returns null
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertTrue("URL should be blocked", result.isBlocked)
        assertEquals("Category should match", BlockingCategory.EXPLICIT_CONTENT, result.category)
        assertEquals("Confidence should be high", 1.0f, result.confidence, 0.01f)
        assertTrue("Reflection time should be positive", result.reflectionTimeSeconds > 0)
    }
    
    @Test
    fun `should not block legitimate sites`() = runTest {
        // Given
        val testUrl = "https://google.com"
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns null
        coEvery { mockBlockedSiteDao.getSitesByPattern(any()) } returns emptyList()
        coEvery { mockBlockedSiteDao.getRegexSites() } returns emptyList()
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertFalse("Legitimate URL should not be blocked", result.isBlocked)
        assertNull("Category should be null", result.category)
        assertEquals("Confidence should be zero", 0.0f, result.confidence, 0.01f)
    }
    
    @Test
    fun `should detect suspicious keywords in URL`() = runTest {
        // Given
        val testUrl = "https://example.com/porn-videos"
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns null
        coEvery { mockBlockedSiteDao.getSitesByPattern(any()) } returns emptyList()
        coEvery { mockBlockedSiteDao.getRegexSites() } returns emptyList()
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertTrue("URL with suspicious keyword should be blocked", result.isBlocked)
        assertEquals("Category should be explicit content", BlockingCategory.EXPLICIT_CONTENT, result.category)
        assertTrue("Confidence should be moderate", result.confidence > 0.5f)
        assertEquals("Should match porn keyword", "porn", result.matchedPattern)
    }
    
    @Test
    fun `should handle regex pattern matching`() = runTest {
        // Given
        val testUrl = "https://example.xxx"
        val regexSite = BlockedSiteEntity(
            id = 1,
            domainHash = "hash123",
            pattern = ".*\\.xxx$",
            category = BlockingCategory.EXPLICIT_CONTENT,
            confidence = 0.95f,
            lastUpdated = System.currentTimeMillis(),
            isRegex = true
        )
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns null
        coEvery { mockBlockedSiteDao.getSitesByPattern(any()) } returns emptyList()
        coEvery { mockBlockedSiteDao.getRegexSites() } returns listOf(regexSite)
        coEvery { mockQuranicVerseDao.getRandomVerseByCategory(any()) } returns null
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertTrue("URL matching regex should be blocked", result.isBlocked)
        assertEquals("Category should match", BlockingCategory.EXPLICIT_CONTENT, result.category)
        assertEquals("Pattern should match", ".*\\.xxx$", result.matchedPattern)
    }
    
    @Test
    fun `should report false positives correctly`() = runTest {
        // Given
        val testUrl = "https://legitimate-site.com"
        val reason = "This is a legitimate educational website"
        
        coEvery { mockFalsePositiveDao.insertReport(any()) } returns 1L
        
        // When
        val result = siteBlockingManager.reportFalsePositive(testUrl, reason)
        
        // Then
        assertTrue("False positive should be reported successfully", result)
        coVerify { mockFalsePositiveDao.insertReport(any()) }
    }
    
    @Test
    fun `should handle gambling sites correctly`() = runTest {
        // Given
        val testUrl = "https://bet365.com"
        val gamblingKeyword = "bet"
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns null
        coEvery { mockBlockedSiteDao.getSitesByPattern(any()) } returns emptyList()
        coEvery { mockBlockedSiteDao.getRegexSites() } returns emptyList()
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertTrue("Gambling URL should be blocked", result.isBlocked)
        assertEquals("Category should be gambling", BlockingCategory.GAMBLING, result.category)
        assertEquals("Should match bet keyword", gamblingKeyword, result.matchedPattern)
    }
    
    @Test
    fun `should add custom blocked site`() = runTest {
        // Given
        val testUrl = "https://custom-blocked-site.com"
        val category = BlockingCategory.INAPPROPRIATE_IMAGERY
        
        coEvery { mockBlockedSiteDao.insertSite(any()) } returns 1L
        
        // When
        val result = siteBlockingManager.addCustomBlockedSite(testUrl, category)
        
        // Then
        assertTrue("Custom site should be added successfully", result)
        coVerify { mockBlockedSiteDao.insertSite(any()) }
    }
    
    @Test
    fun `should remove blocked site`() = runTest {
        // Given
        val testUrl = "https://site-to-remove.com"
        
        coEvery { mockBlockedSiteDao.deactivateSite(any()) } just Runs
        
        // When
        val result = siteBlockingManager.removeBlockedSite(testUrl)
        
        // Then
        assertTrue("Site should be removed successfully", result)
        coVerify { mockBlockedSiteDao.deactivateSite(any()) }
    }
    
    @Test
    fun `should handle malformed URLs gracefully`() = runTest {
        // Given
        val malformedUrl = "not-a-valid-url"
        
        // When
        val result = siteBlockingManager.checkUrl(malformedUrl)
        
        // Then
        assertFalse("Malformed URL should not cause blocking", result.isBlocked)
        assertNotNull("Should have blocking reason", result.blockingReason)
    }
    
    @Test
    fun `should normalize URLs correctly`() = runTest {
        // Given
        val urlVariations = listOf(
            "pornhub.com",
            "www.pornhub.com",
            "https://pornhub.com",
            "https://www.pornhub.com/video/123"
        )
        
        val blockedSite = BlockedSiteEntity(
            id = 1,
            domainHash = "hash123",
            pattern = "pornhub.com",
            category = BlockingCategory.EXPLICIT_CONTENT,
            confidence = 1.0f,
            lastUpdated = System.currentTimeMillis()
        )
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns blockedSite
        coEvery { mockQuranicVerseDao.getRandomVerseByCategory(any()) } returns null
        
        // When & Then
        urlVariations.forEach { url ->
            val result = siteBlockingManager.checkUrl(url)
            assertTrue("All URL variations should be blocked: $url", result.isBlocked)
        }
    }
    
    @Test
    fun `should get appropriate Quranic verse for category`() = runTest {
        // Given
        val category = BlockingCategory.EXPLICIT_CONTENT
        val mockVerse = QuranicVerseEntity(
            id = "24_30",
            surahName = "An-Nur",
            surahNumber = 24,
            verseNumber = 30,
            arabicText = "قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ",
            transliteration = "Qul lil-mu'mineena yaghuddu min absarihim",
            englishTranslation = "Tell the believing men to lower their gaze",
            category = category,
            context = "About lowering gaze",
            reflection = "Reflect on purity"
        )
        
        coEvery { mockQuranicVerseDao.getRandomVerseByCategory(category) } returns mockVerse
        
        // When
        val result = siteBlockingManager.getQuranicVerse(category)
        
        // Then
        assertNotNull("Should return a verse", result)
        assertEquals("Verse ID should match", "24_30", result?.id)
        assertEquals("Category should match", category, result?.category)
        assertTrue("Should have English translation", result?.translations?.containsKey(Language.ENGLISH) == true)
    }
    
    @Test
    fun `should handle database errors gracefully`() = runTest {
        // Given
        val testUrl = "https://test.com"
        
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } throws Exception("Database error")
        
        // When
        val result = siteBlockingManager.checkUrl(testUrl)
        
        // Then
        assertFalse("Should not block on database error", result.isBlocked)
        assertNotNull("Should have error message", result.blockingReason)
        assertTrue("Should mention error", result.blockingReason?.contains("Error") == true)
    }
}