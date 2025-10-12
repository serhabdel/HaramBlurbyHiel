package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.data.models.DetectionMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for AppFilteringManager to verify first-install behavior
 * and proper interaction with settings
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppFilteringManagerTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appCategoryDetector: AppCategoryDetector
    private lateinit var testScope: TestScope
    private lateinit var appFilteringManager: AppFilteringManager
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>

    @Before
    fun setup() {
        // Create test scope with unconfined dispatcher for immediate execution
        val testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)

        // Create mock settings repository
        settingsRepository = mockk(relaxed = true)
        
        // Create default settings (first install state)
        val defaultSettings = AppSettings(
            enableAppSpecificDetection = true,
            monitoredAppCategories = setOf(
                AppCategory.SOCIAL_MEDIA,
                AppCategory.BROWSERS,
                AppCategory.DATING
            ),
            customMonitoredApps = emptySet(),
            excludedApps = emptySet()
        )
        
        settingsFlow = MutableStateFlow(defaultSettings)
        every { settingsRepository.settings } returns settingsFlow

        // Create mock app category detector
        appCategoryDetector = mockk(relaxed = true)
        every { appCategoryDetector.determineAppCategory("com.instagram.android") } returns AppCategory.SOCIAL_MEDIA
        every { appCategoryDetector.determineAppCategory("com.android.chrome") } returns AppCategory.BROWSERS
        every { appCategoryDetector.determineAppCategory("com.tinder") } returns AppCategory.DATING
        every { appCategoryDetector.determineAppCategory("com.google.android.youtube") } returns AppCategory.ENTERTAINMENT
        every { appCategoryDetector.determineAppCategory("com.example.notes") } returns null

        // Create AppFilteringManager
        appFilteringManager = AppFilteringManager(
            settingsRepository = settingsRepository,
            appCategoryDetector = appCategoryDetector,
            applicationScope = testScope
        )
    }

    @Test
    fun `initial value should use DetectionScope defaults on first install`() = runTest {
        // Given: AppFilteringManager just created (simulating first install)
        // The detectionScopeFlow should have correct initial value

        // When: Get the initial detection scope value
        val initialScope = appFilteringManager.detectionScopeFlow.value

        // Then: Should use DetectionScope() defaults
        assertEquals(
            "Initial mode should be SPECIFIC_CATEGORIES",
            DetectionMode.SPECIFIC_CATEGORIES,
            initialScope.mode
        )
        assertTrue(
            "Initial categories should include SOCIAL_MEDIA",
            initialScope.monitoredCategories.contains(AppCategory.SOCIAL_MEDIA)
        )
        assertTrue(
            "Initial categories should include BROWSERS",
            initialScope.monitoredCategories.contains(AppCategory.BROWSERS)
        )
        assertTrue(
            "Initial categories should include DATING",
            initialScope.monitoredCategories.contains(AppCategory.DATING)
        )
        assertEquals(
            "Initial categories should have exactly 3 categories",
            3,
            initialScope.monitoredCategories.size
        )
    }

    @Test
    fun `should monitor Instagram on first install`() = runTest {
        // Given: First install with default settings
        
        // When: Check if Instagram should be monitored
        val shouldMonitor = appFilteringManager.shouldMonitorApp("com.instagram.android")

        // Then: Instagram should be monitored (in SOCIAL_MEDIA category)
        assertTrue("Instagram should be monitored on first install", shouldMonitor)
    }

    @Test
    fun `should monitor Chrome on first install`() = runTest {
        // Given: First install with default settings
        
        // When: Check if Chrome should be monitored
        val shouldMonitor = appFilteringManager.shouldMonitorApp("com.android.chrome")

        // Then: Chrome should be monitored (in BROWSERS category)
        assertTrue("Chrome should be monitored on first install", shouldMonitor)
    }

    @Test
    fun `should monitor Tinder on first install`() = runTest {
        // Given: First install with default settings
        
        // When: Check if Tinder should be monitored
        val shouldMonitor = appFilteringManager.shouldMonitorApp("com.tinder")

        // Then: Tinder should be monitored (in DATING category)
        assertTrue("Tinder should be monitored on first install", shouldMonitor)
    }

    @Test
    fun `should NOT monitor non-category app on first install`() = runTest {
        // Given: First install with default settings
        
        // When: Check if a non-category app should be monitored
        val shouldMonitor = appFilteringManager.shouldMonitorApp("com.example.notes")

        // Then: Non-category app should NOT be monitored
        assertFalse("Non-category app should not be monitored on first install", shouldMonitor)
    }

    @Test
    fun `should NOT monitor YouTube on first install`() = runTest {
        // Given: First install with default settings (ENTERTAINMENT not included)
        
        // When: Check if YouTube should be monitored
        val shouldMonitor = appFilteringManager.shouldMonitorApp("com.google.android.youtube")

        // Then: YouTube should NOT be monitored (ENTERTAINMENT not in default categories)
        assertFalse("YouTube should not be monitored on first install", shouldMonitor)
    }

    @Test
    fun `should respect exclusions in ALL_APPS mode`() = runTest {
        // Given: Settings with ALL_APPS mode and Instagram excluded
        settingsFlow.value = AppSettings(
            enableAppSpecificDetection = false, // This triggers ALL_APPS mode
            monitoredAppCategories = emptySet(),
            customMonitoredApps = emptySet(),
            excludedApps = setOf("com.instagram.android")
        )

        // When: Check if Instagram and Chrome should be monitored
        val instagramMonitored = appFilteringManager.shouldMonitorApp("com.instagram.android")
        val chromeMonitored = appFilteringManager.shouldMonitorApp("com.android.chrome")

        // Then: Instagram should NOT be monitored (excluded), Chrome should be
        assertFalse("Instagram should not be monitored when excluded", instagramMonitored)
        assertTrue("Chrome should be monitored in ALL_APPS mode", chromeMonitored)
    }

    @Test
    fun `should fallback to ALL_APPS when categories and custom apps are empty`() = runTest {
        // Given: Settings with app-specific detection enabled but no categories or custom apps
        settingsFlow.value = AppSettings(
            enableAppSpecificDetection = true,
            monitoredAppCategories = emptySet(),
            customMonitoredApps = emptySet(),
            excludedApps = emptySet()
        )

        // When: Check the detection scope mode
        val scope = appFilteringManager.detectionScopeFlow.value

        // Then: Should fallback to ALL_APPS mode
        assertEquals(
            "Should fallback to ALL_APPS when no categories or custom apps",
            DetectionMode.ALL_APPS,
            scope.mode
        )
    }

    @Test
    fun `should monitor all apps when fallback to ALL_APPS occurs`() = runTest {
        // Given: Settings with app-specific detection enabled but no categories or custom apps
        settingsFlow.value = AppSettings(
            enableAppSpecificDetection = true,
            monitoredAppCategories = emptySet(),
            customMonitoredApps = emptySet(),
            excludedApps = emptySet()
        )

        // When: Check if various apps should be monitored
        val instagramMonitored = appFilteringManager.shouldMonitorApp("com.instagram.android")
        val notesMonitored = appFilteringManager.shouldMonitorApp("com.example.notes")
        val youtubeMonitored = appFilteringManager.shouldMonitorApp("com.google.android.youtube")

        // Then: All apps should be monitored (fallback to ALL_APPS)
        assertTrue("Instagram should be monitored in ALL_APPS fallback", instagramMonitored)
        assertTrue("Notes app should be monitored in ALL_APPS fallback", notesMonitored)
        assertTrue("YouTube should be monitored in ALL_APPS fallback", youtubeMonitored)
    }

    @Test
    fun `should respect custom monitored apps`() = runTest {
        // Given: Settings with only SOCIAL_MEDIA category and a custom app
        settingsFlow.value = AppSettings(
            enableAppSpecificDetection = true,
            monitoredAppCategories = setOf(AppCategory.SOCIAL_MEDIA),
            customMonitoredApps = setOf("com.example.customapp"),
            excludedApps = emptySet()
        )

        // When: Check if custom app and category apps should be monitored
        val customAppMonitored = appFilteringManager.shouldMonitorApp("com.example.customapp")
        val instagramMonitored = appFilteringManager.shouldMonitorApp("com.instagram.android")
        val chromeMonitored = appFilteringManager.shouldMonitorApp("com.android.chrome")

        // Then: Custom app and Instagram should be monitored, Chrome should not
        assertTrue("Custom app should be monitored", customAppMonitored)
        assertTrue("Instagram should be monitored (in SOCIAL_MEDIA)", instagramMonitored)
        assertFalse("Chrome should not be monitored (BROWSERS not included)", chromeMonitored)
    }

    @Test
    fun `shouldMonitorAppSync should work correctly`() = runTest {
        // Given: Default first install settings
        
        // When: Check apps using synchronous method
        val instagramMonitored = appFilteringManager.shouldMonitorAppSync("com.instagram.android")
        val notesMonitored = appFilteringManager.shouldMonitorAppSync("com.example.notes")

        // Then: Should return same results as async version
        assertTrue("Instagram should be monitored (sync)", instagramMonitored)
        assertFalse("Notes app should not be monitored (sync)", notesMonitored)
    }

    @Test
    fun `getMonitoringReason should return correct reasons`() = runTest {
        // Given: Default first install settings
        
        // When: Get monitoring reasons for different apps
        val instagramReason = appFilteringManager.getMonitoringReason("com.instagram.android")
        val notesReason = appFilteringManager.getMonitoringReason("com.example.notes")

        // Then: Should return appropriate reasons
        assertTrue(
            "Instagram reason should indicate monitored category",
            instagramReason.contains("in_monitored_category")
        )
        assertEquals(
            "Notes app reason should indicate not monitored",
            "not_monitored",
            notesReason
        )
    }

    @Test
    fun `getMonitoringStats should return correct statistics`() = runTest {
        // Given: Default first install settings
        
        // When: Get monitoring statistics
        val stats = appFilteringManager.getMonitoringStats()

        // Then: Should reflect default configuration
        assertEquals("Mode should be SPECIFIC_CATEGORIES", DetectionMode.SPECIFIC_CATEGORIES, stats.mode)
        assertEquals("Should have 3 monitored categories", 3, stats.monitoredCategories)
        assertEquals("Should have 0 custom apps", 0, stats.customApps)
        assertEquals("Should have 0 excluded apps", 0, stats.excludedApps)
    }

    @Test
    fun `isFilteringConfigured should return true for default settings`() = runTest {
        // Given: Default first install settings
        
        // When: Check if filtering is configured
        val isConfigured = appFilteringManager.isFilteringConfigured()

        // Then: Should be configured (has monitored categories)
        assertTrue("Filtering should be configured on first install", isConfigured)
    }

    @Test
    fun `null or blank package name should return correct values`() = runTest {
        // Given: Default first install settings (SPECIFIC_CATEGORIES mode)
        
        // When: Check null and blank package names
        val nullMonitored = appFilteringManager.shouldMonitorApp(null)
        val blankMonitored = appFilteringManager.shouldMonitorApp("")

        // Then: Should return false (not in ALL_APPS mode)
        assertFalse("Null package should not be monitored in SPECIFIC_CATEGORIES", nullMonitored)
        assertFalse("Blank package should not be monitored in SPECIFIC_CATEGORIES", blankMonitored)
    }

    @Test
    fun `null or blank package name should return true in ALL_APPS mode`() = runTest {
        // Given: Settings with ALL_APPS mode
        settingsFlow.value = AppSettings(
            enableAppSpecificDetection = false,
            monitoredAppCategories = emptySet(),
            customMonitoredApps = emptySet(),
            excludedApps = emptySet()
        )

        // When: Check null and blank package names
        val nullMonitored = appFilteringManager.shouldMonitorApp(null)
        val blankMonitored = appFilteringManager.shouldMonitorApp("")

        // Then: Should return true (in ALL_APPS mode)
        assertTrue("Null package should be monitored in ALL_APPS mode", nullMonitored)
        assertTrue("Blank package should be monitored in ALL_APPS mode", blankMonitored)
    }
}
