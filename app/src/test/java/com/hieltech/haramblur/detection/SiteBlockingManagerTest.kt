package com.hieltech.haramblur.detection

import android.content.Context
import android.util.Log
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.database.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for UnifiedSiteBlockingManager (the active DI-bound implementation).
 * Focus: false-positive regression, OAuth bypass hardening, TLD matching.
 */
class SiteBlockingManagerTest {
    
    private lateinit var siteBlockingManager: UnifiedSiteBlockingManager
    private lateinit var mockDatabase: SiteBlockingDatabase
    private lateinit var mockBlockedSiteDao: BlockedSiteDao
    private lateinit var mockFalsePositiveDao: FalsePositiveDao
    private lateinit var mockQuranicRepository: QuranicRepository
    private lateinit var mockErrorRecovery: ComprehensiveErrorRecovery
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        // Mock Android Log so it doesn't crash in unit tests
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        
        mockDatabase = mockk()
        mockBlockedSiteDao = mockk()
        mockFalsePositiveDao = mockk()
        mockQuranicRepository = mockk()
        mockContext = mockk(relaxed = true)
        
        // Create a relaxed mock for ComprehensiveErrorRecovery
        mockErrorRecovery = mockk(relaxed = true)
        
        every { mockDatabase.blockedSiteDao() } returns mockBlockedSiteDao
        every { mockDatabase.falsePositiveDao() } returns mockFalsePositiveDao
        
        // Default: no DB matches (forces heuristic/pattern checks)
        coEvery { mockBlockedSiteDao.getSiteByDomainHash(any()) } returns null
        coEvery { mockBlockedSiteDao.getSitesByPattern(any()) } returns emptyList()
        coEvery { mockBlockedSiteDao.getRegexSites() } returns emptyList()
        coEvery { mockBlockedSiteDao.getActiveSites() } returns emptyList()

        // Error recovery delegates to the lambda
        coEvery { mockErrorRecovery.executeBlockingOperation(any(), any()) } coAnswers {
            val block = arg<suspend () -> SiteBlockingResult>(1)
            block()
        }
        
        siteBlockingManager = UnifiedSiteBlockingManager(
            database = mockDatabase,
            quranicRepository = mockQuranicRepository,
            errorRecovery = mockErrorRecovery,
            context = mockContext
        )
    }
    
    // ──────────────────────────────────────────
    // TRUE POSITIVES — these MUST be blocked
    // ──────────────────────────────────────────
    
    @Test
    fun `should block pornhub`() = runTest {
        val result = siteBlockingManager.checkUrl("https://pornhub.com")
        assertTrue("pornhub.com should be blocked", result.isBlocked)
    }
    
    @Test
    fun `should block xvideos`() = runTest {
        val result = siteBlockingManager.checkUrl("https://xvideos.com")
        assertTrue("xvideos.com should be blocked", result.isBlocked)
    }
    
    @Test
    fun `should block site with xxx TLD`() = runTest {
        val result = siteBlockingManager.checkUrl("https://example.xxx")
        assertTrue("example.xxx should be blocked", result.isBlocked)
    }
    
    @Test
    fun `should block site with porn TLD`() = runTest {
        val result = siteBlockingManager.checkUrl("https://example.porn")
        assertTrue("example.porn should be blocked", result.isBlocked)
    }
    
    @Test
    fun `should block onlyfans`() = runTest {
        val result = siteBlockingManager.checkUrl("https://onlyfans.com")
        assertTrue("onlyfans.com should be blocked", result.isBlocked)
    }
    
    @Test
    fun `should block xnxx`() = runTest {
        val result = siteBlockingManager.checkUrl("https://xnxx.com")
        assertTrue("xnxx.com should be blocked", result.isBlocked)
    }
    
    // ──────────────────────────────────────────
    // FALSE POSITIVE REGRESSION — these must NOT be blocked
    // ──────────────────────────────────────────
    
    @Test
    fun `should not block essex gov uk`() = runTest {
        val result = siteBlockingManager.checkUrl("https://essex.gov.uk")
        assertFalse("essex.gov.uk should NOT be blocked (geographic name)", result.isBlocked)
    }
    
    @Test
    fun `should not block alphabet com`() = runTest {
        val result = siteBlockingManager.checkUrl("https://alphabet.com")
        assertFalse("alphabet.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block chatgpt com`() = runTest {
        val result = siteBlockingManager.checkUrl("https://chatgpt.com")
        assertFalse("chatgpt.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block meetup com`() = runTest {
        val result = siteBlockingManager.checkUrl("https://meetup.com")
        assertFalse("meetup.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block betterhelp com`() = runTest {
        val result = siteBlockingManager.checkUrl("https://betterhelp.com")
        assertFalse("betterhelp.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block cam ac uk`() = runTest {
        val result = siteBlockingManager.checkUrl("https://cam.ac.uk")
        assertFalse("cam.ac.uk (University of Cambridge) should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block cumulus com`() = runTest {
        val result = siteBlockingManager.checkUrl("https://cumulus.com")
        assertFalse("cumulus.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block diabetes org`() = runTest {
        val result = siteBlockingManager.checkUrl("https://diabetes.org")
        assertFalse("diabetes.org should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block adultlearning org`() = runTest {
        val result = siteBlockingManager.checkUrl("https://adultlearning.org")
        assertFalse("adultlearning.org should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block gaylord hotels`() = runTest {
        val result = siteBlockingManager.checkUrl("https://gaylordhotels.com")
        assertFalse("gaylordhotels.com should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block sexsmith ca`() = runTest {
        val result = siteBlockingManager.checkUrl("https://sexsmith.ca")
        assertFalse("sexsmith.ca (town in Canada) should NOT be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block reddit`() = runTest {
        val result = siteBlockingManager.checkUrl("https://reddit.com")
        assertFalse("reddit.com should NOT be blocked (removed from hash DB)", result.isBlocked)
    }
    
    @Test
    fun `should not block twitter`() = runTest {
        val result = siteBlockingManager.checkUrl("https://twitter.com")
        assertFalse("twitter.com should NOT be blocked (whitelisted)", result.isBlocked)
    }
    
    @Test
    fun `should not block discord`() = runTest {
        val result = siteBlockingManager.checkUrl("https://discord.com")
        assertFalse("discord.com should NOT be blocked (whitelisted)", result.isBlocked)
    }
    
    @Test
    fun `should not block youtube`() = runTest {
        val result = siteBlockingManager.checkUrl("https://youtube.com")
        assertFalse("youtube.com should NOT be blocked", result.isBlocked)
    }

    @Test
    fun `should not block sussex university`() = runTest {
        val result = siteBlockingManager.checkUrl("https://sussex.ac.uk")
        assertFalse("sussex.ac.uk should NOT be blocked (geographic name)", result.isBlocked)
    }
    
    // ──────────────────────────────────────────
    // OAuth BYPASS HARDENING
    // ──────────────────────────────────────────
    
    @Test
    fun `should not bypass blocking via state param`() = runTest {
        val result = siteBlockingManager.checkUrl("https://pornhub.com?state=x")
        assertTrue("pornhub.com with ?state=x should still be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not bypass blocking via code param`() = runTest {
        val result = siteBlockingManager.checkUrl("https://pornhub.com?code=abc")
        assertTrue("pornhub.com with ?code=abc should still be blocked", result.isBlocked)
    }
    
    @Test
    fun `should not block github with code param`() = runTest {
        val result = siteBlockingManager.checkUrl("https://github.com?code=abc")
        assertFalse("github.com with ?code=abc should NOT be blocked (whitelisted)", result.isBlocked)
    }
    
    // ──────────────────────────────────────────
    // TLD MATCHING
    // ──────────────────────────────────────────
    
    @Test
    fun `tube in domain name should not trigger TLD block`() = runTest {
        val result = siteBlockingManager.checkUrl("https://tubescreamer.com")
        assertFalse("tubescreamer.com should NOT be blocked (.tube is not the TLD)", result.isBlocked)
    }
    
    @Test
    fun `video in domain should not trigger TLD block`() = runTest {
        val result = siteBlockingManager.checkUrl("https://videojs.com")
        assertFalse("videojs.com should NOT be blocked (.video is not the TLD)", result.isBlocked)
    }

    // ──────────────────────────────────────────
    // URL NORMALIZATION
    // ──────────────────────────────────────────
    
    @Test
    fun `should block all URL variations of known porn site`() = runTest {
        val urls = listOf(
            "pornhub.com",
            "www.pornhub.com",
            "https://pornhub.com",
            "https://www.pornhub.com/video/123"
        )
        urls.forEach { url ->
            val result = siteBlockingManager.checkUrl(url)
            assertTrue("All URL variations should be blocked: $url", result.isBlocked)
        }
    }
    
    // ──────────────────────────────────────────
    // ERROR HANDLING
    // ──────────────────────────────────────────
    
    @Test
    fun `should handle malformed URLs gracefully`() = runTest {
        val result = siteBlockingManager.checkUrl("not-a-valid-url")
        assertNotNull("Result should not be null", result)
    }
}