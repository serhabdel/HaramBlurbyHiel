package com.hieltech.haramblur.accessibility

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BrowserDetector
 */
class BrowserDetectorTest {

    @Test
    fun `isBrowserPackage returns true for known browsers`() {
        val browsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser"
        )
        
        browsers.forEach { packageName ->
            assertTrue("Should detect $packageName as browser", 
                BrowserDetector.isBrowserPackage(packageName))
        }
    }

    @Test
    fun `isBrowserPackage returns true for browsers with keywords`() {
        val browsers = listOf(
            "com.example.browser",
            "com.myapp.chrome",
            "org.test.firefox",
            "com.company.edge"
        )
        
        browsers.forEach { packageName ->
            assertTrue("Should detect $packageName as browser by keyword",
                BrowserDetector.isBrowserPackage(packageName))
        }
    }

    @Test
    fun `isBrowserPackage returns false for non-browsers`() {
        val nonBrowsers = listOf(
            "com.whatsapp",
            "com.facebook.katana",
            "com.instagram.android",
            "com.example.game",
            null,
            ""
        )
        
        nonBrowsers.forEach { packageName ->
            assertFalse("Should NOT detect $packageName as browser",
                BrowserDetector.isBrowserPackage(packageName))
        }
    }

    @Test
    fun `isBrowserPackage handles case insensitivity`() {
        assertTrue(BrowserDetector.isBrowserPackage("COM.ANDROID.CHROME"))
        assertTrue(BrowserDetector.isBrowserPackage("Com.Android.Chrome"))
        assertTrue(BrowserDetector.isBrowserPackage("Org.Mozilla.Firefox"))
    }

    @Test
    fun `getKnownBrowserPackages returns non-empty set`() {
        val packages = BrowserDetector.getKnownBrowserPackages()
        assertTrue("Should have known browser packages", packages.isNotEmpty())
        assertTrue("Should have at least 10 browsers", packages.size >= 10)
    }
}
