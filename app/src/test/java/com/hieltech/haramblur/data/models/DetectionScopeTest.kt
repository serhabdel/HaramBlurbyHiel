package com.hieltech.haramblur.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DetectionScope to verify first-install behavior and app monitoring logic
 */
class DetectionScopeTest {

    @Test
    fun `default DetectionScope should monitor Instagram`() {
        // Given: Default DetectionScope (SPECIFIC_CATEGORIES with SOCIAL_MEDIA, BROWSERS, DATING)
        val scope = DetectionScope()

        // When: Check if Instagram should be monitored
        val shouldMonitor = scope.shouldMonitorApp("com.instagram.android")

        // Then: Instagram should be monitored (it's in SOCIAL_MEDIA category)
        assertTrue("Instagram should be monitored by default", shouldMonitor)
    }

    @Test
    fun `default DetectionScope should monitor Chrome`() {
        // Given: Default DetectionScope
        val scope = DetectionScope()

        // When: Check if Chrome should be monitored
        val shouldMonitor = scope.shouldMonitorApp("com.android.chrome")

        // Then: Chrome should be monitored (it's in BROWSERS category)
        assertTrue("Chrome should be monitored by default", shouldMonitor)
    }

    @Test
    fun `default DetectionScope should monitor Tinder`() {
        // Given: Default DetectionScope
        val scope = DetectionScope()

        // When: Check if Tinder should be monitored
        val shouldMonitor = scope.shouldMonitorApp("com.tinder")

        // Then: Tinder should be monitored (it's in DATING category)
        assertTrue("Tinder should be monitored by default", shouldMonitor)
    }

    @Test
    fun `default DetectionScope should NOT monitor non-category app`() {
        // Given: Default DetectionScope
        val scope = DetectionScope()

        // When: Check if a non-category app (e.g., Notes app) should be monitored
        val shouldMonitor = scope.shouldMonitorApp("com.example.notes")

        // Then: Non-category app should NOT be monitored
        assertFalse("Non-category app should not be monitored by default", shouldMonitor)
    }

    @Test
    fun `default DetectionScope should NOT monitor YouTube`() {
        // Given: Default DetectionScope (doesn't include ENTERTAINMENT category)
        val scope = DetectionScope()

        // When: Check if YouTube should be monitored
        val shouldMonitor = scope.shouldMonitorApp("com.google.android.youtube")

        // Then: YouTube should NOT be monitored (ENTERTAINMENT not in default categories)
        assertFalse("YouTube should not be monitored by default", shouldMonitor)
    }

    @Test
    fun `ALL_APPS mode should monitor all apps`() {
        // Given: DetectionScope with ALL_APPS mode
        val scope = DetectionScope(mode = DetectionMode.ALL_APPS)

        // When: Check various apps
        val instagramMonitored = scope.shouldMonitorApp("com.instagram.android")
        val notesMonitored = scope.shouldMonitorApp("com.example.notes")
        val youtubeMonitored = scope.shouldMonitorApp("com.google.android.youtube")

        // Then: All apps should be monitored
        assertTrue("Instagram should be monitored in ALL_APPS mode", instagramMonitored)
        assertTrue("Notes app should be monitored in ALL_APPS mode", notesMonitored)
        assertTrue("YouTube should be monitored in ALL_APPS mode", youtubeMonitored)
    }

    @Test
    fun `ALL_APPS mode should respect exclusions`() {
        // Given: DetectionScope with ALL_APPS mode but Instagram excluded
        val scope = DetectionScope(
            mode = DetectionMode.ALL_APPS,
            excludedApps = setOf("com.instagram.android")
        )

        // When: Check if Instagram should be monitored
        val instagramMonitored = scope.shouldMonitorApp("com.instagram.android")
        val chromeMonitored = scope.shouldMonitorApp("com.android.chrome")

        // Then: Instagram should NOT be monitored (excluded), but Chrome should be
        assertFalse("Instagram should not be monitored when excluded", instagramMonitored)
        assertTrue("Chrome should be monitored in ALL_APPS mode", chromeMonitored)
    }

    @Test
    fun `SPECIFIC_CATEGORIES mode should respect exclusions`() {
        // Given: DetectionScope with SPECIFIC_CATEGORIES but Chrome excluded
        val scope = DetectionScope(
            mode = DetectionMode.SPECIFIC_CATEGORIES,
            monitoredCategories = setOf(AppCategory.BROWSERS, AppCategory.SOCIAL_MEDIA),
            excludedApps = setOf("com.android.chrome")
        )

        // When: Check if Chrome and Firefox should be monitored
        val chromeMonitored = scope.shouldMonitorApp("com.android.chrome")
        val firefoxMonitored = scope.shouldMonitorApp("org.mozilla.firefox")

        // Then: Chrome should NOT be monitored (excluded), but Firefox should be
        assertFalse("Chrome should not be monitored when excluded", chromeMonitored)
        assertTrue("Firefox should be monitored (in BROWSERS category)", firefoxMonitored)
    }

    @Test
    fun `DISABLED mode should not monitor any apps`() {
        // Given: DetectionScope with DISABLED mode
        val scope = DetectionScope(mode = DetectionMode.DISABLED)

        // When: Check various apps
        val instagramMonitored = scope.shouldMonitorApp("com.instagram.android")
        val chromeMonitored = scope.shouldMonitorApp("com.android.chrome")

        // Then: No apps should be monitored
        assertFalse("Instagram should not be monitored in DISABLED mode", instagramMonitored)
        assertFalse("Chrome should not be monitored in DISABLED mode", chromeMonitored)
    }

    @Test
    fun `custom included apps should be monitored`() {
        // Given: DetectionScope with custom app included
        val scope = DetectionScope(
            mode = DetectionMode.SPECIFIC_CATEGORIES,
            monitoredCategories = setOf(AppCategory.SOCIAL_MEDIA),
            customIncludedApps = setOf("com.example.customapp")
        )

        // When: Check if custom app should be monitored
        val customAppMonitored = scope.shouldMonitorApp("com.example.customapp")
        val instagramMonitored = scope.shouldMonitorApp("com.instagram.android")
        val chromeMonitored = scope.shouldMonitorApp("com.android.chrome")

        // Then: Custom app and Instagram should be monitored, Chrome should not
        assertTrue("Custom app should be monitored", customAppMonitored)
        assertTrue("Instagram should be monitored (in SOCIAL_MEDIA)", instagramMonitored)
        assertFalse("Chrome should not be monitored (BROWSERS not included)", chromeMonitored)
    }

    @Test
    fun `getMonitoredPackageNames should return correct apps for default scope`() {
        // Given: Default DetectionScope
        val scope = DetectionScope()

        // When: Get monitored package names
        val monitoredApps = scope.getMonitoredPackageNames()

        // Then: Should include apps from SOCIAL_MEDIA, BROWSERS, and DATING categories
        assertTrue("Should include Instagram", monitoredApps.contains("com.instagram.android"))
        assertTrue("Should include Chrome", monitoredApps.contains("com.android.chrome"))
        assertTrue("Should include Tinder", monitoredApps.contains("com.tinder"))
        assertFalse("Should not include YouTube", monitoredApps.contains("com.google.android.youtube"))
    }

    @Test
    fun `getMonitoredPackageNames should return empty set for ALL_APPS mode`() {
        // Given: DetectionScope with ALL_APPS mode
        val scope = DetectionScope(mode = DetectionMode.ALL_APPS)

        // When: Get monitored package names
        val monitoredApps = scope.getMonitoredPackageNames()

        // Then: Should return empty set (special case - monitor everything)
        assertTrue("Should return empty set for ALL_APPS mode", monitoredApps.isEmpty())
    }

    @Test
    fun `isMonitoringAllApps should return correct values`() {
        // Given: Different detection scopes
        val allAppsScope = DetectionScope(mode = DetectionMode.ALL_APPS)
        val specificScope = DetectionScope(mode = DetectionMode.SPECIFIC_CATEGORIES)
        val disabledScope = DetectionScope(mode = DetectionMode.DISABLED)

        // Then: Check monitoring status
        assertTrue("ALL_APPS scope should be monitoring all apps", allAppsScope.isMonitoringAllApps())
        assertFalse("SPECIFIC_CATEGORIES scope should not be monitoring all apps", specificScope.isMonitoringAllApps())
        assertFalse("DISABLED scope should not be monitoring all apps", disabledScope.isMonitoringAllApps())
    }

    @Test
    fun `isDetectionDisabled should return correct values`() {
        // Given: Different detection scopes
        val allAppsScope = DetectionScope(mode = DetectionMode.ALL_APPS)
        val specificScope = DetectionScope(mode = DetectionMode.SPECIFIC_CATEGORIES)
        val disabledScope = DetectionScope(mode = DetectionMode.DISABLED)

        // Then: Check disabled status
        assertFalse("ALL_APPS scope should not be disabled", allAppsScope.isDetectionDisabled())
        assertFalse("SPECIFIC_CATEGORIES scope should not be disabled", specificScope.isDetectionDisabled())
        assertTrue("DISABLED scope should be disabled", disabledScope.isDetectionDisabled())
    }
}
