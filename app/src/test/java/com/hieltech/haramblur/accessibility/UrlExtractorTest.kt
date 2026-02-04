package com.hieltech.haramblur.accessibility

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UrlExtractor
 */
class UrlExtractorTest {

    @Test
    fun `extractUrlFromText finds http URLs`() {
        val text = "Check out https://example.com for more info"
        val url = UrlExtractor.extractUrlFromText(text)
        assertEquals("https://example.com", url)
    }

    @Test
    fun `extractUrlFromText finds www URLs`() {
        val text = "Visit www.example.com today"
        val url = UrlExtractor.extractUrlFromText(text)
        assertNotNull(url)
        assertTrue(url!!.contains("example.com"))
    }

    @Test
    fun `extractUrlFromText returns null for no URL`() {
        val text = "This is just regular text without any URLs"
        val url = UrlExtractor.extractUrlFromText(text)
        assertNull(url)
    }

    @Test
    fun `extractUrlFromText handles null input`() {
        assertNull(UrlExtractor.extractUrlFromText(null))
        assertNull(UrlExtractor.extractUrlFromText(""))
        assertNull(UrlExtractor.extractUrlFromText("   "))
    }

    @Test
    fun `isValidUrl validates correct URLs`() {
        val validUrls = listOf(
            "https://example.com",
            "http://test.org",
            "www.example.com",
            "example.co.uk"
        )
        
        validUrls.forEach { url ->
            assertTrue("$url should be valid", UrlExtractor.isValidUrl(url))
        }
    }

    @Test
    fun `isValidUrl rejects invalid URLs`() {
        val invalidUrls = listOf(
            "not a url",
            "",
            "ftp://invalid",
            "localhost"
        )
        
        invalidUrls.forEach { url ->
            assertFalse("$url should be invalid", UrlExtractor.isValidUrl(url))
        }
    }
}
