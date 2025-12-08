package com.hieltech.haramblur.ui

import android.content.Context
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.prayer.LocationData
import com.hieltech.haramblur.utils.LocationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionWizardLocationTest {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationHelper: LocationHelper
    private lateinit var context: Context
    private lateinit var viewModel: PermissionWizardViewModel
    private lateinit var testDispatcher: TestDispatcher
    
    // Mocks state
    private val permissionFlow = MutableStateFlow<Map<String, PermissionHelper.PermissionResult>>(emptyMap())
    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        permissionHelper = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        locationHelper = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Setup PermissionHelper flow
        every { permissionHelper.permissionStatusFlow } returns permissionFlow
        // Setup initial permissions (all denied by default mock relaxed, but let's be explicit if needed)
        // If we want LOCATION to be GRANTED initially to test init logic:
        // We'll do that in specific test.

        // Setup SettingsRepository
        every { settingsRepository.settings } returns settingsFlow
        coEvery { settingsRepository.getCurrentSettings() } returns settingsFlow.value
        
        // Mock Resource strings (since ViewModel uses context.getString)
        every { context.getString(any()) } returns "Mock String"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should fetch location and set Morocco calculation method when location is Morocco`() = runTest(testDispatcher) {
        // Given: Location permission is GRANTED
        val locationGranted = PermissionHelper.PermissionResult.Granted("LOCATION_PERMISSION")
        permissionFlow.value = mapOf("LOCATION_PERMISSION" to locationGranted)
        
        // Given: LocationHelper returns Morocco
        coEvery { locationHelper.hasLocationPermission() } returns true
        coEvery { locationHelper.getBestLocation() } returns LocationData(
            latitude = 31.7917,
            longitude = -7.0926,
            city = "Rabat",
            country = "Morocco",
            accuracy = 10.0f
        )
        
        // Given: Settings has NO city set yet
        settingsFlow.value = AppSettings(locationCity = null)
        coEvery { settingsRepository.getCurrentSettings() } returns settingsFlow.value

        // When: ViewModel is initialized
        viewModel = PermissionWizardViewModel(
            permissionHelper,
            settingsRepository,
            locationHelper,
            context
        )
        
        // Run coroutines
        advanceUntilIdle()

        // Then: verify settings updated with method 15 (Morocco Ministry)
        val slot = slot<AppSettings>()
        coVerify { settingsRepository.updateSettings(capture(slot)) }
        
        assertEquals("Should set city to Rabat", "Rabat", slot.captured.locationCity)
        assertEquals("Should set country to Morocco", "Morocco", slot.captured.locationCountry)
        assertEquals("Should set calculation method to 15 (Morocco)", 15, slot.captured.prayerCalculationMethod)
    }

    @Test
    fun `should fetch location and set Turkey calculation method when location is Turkey`() = runTest(testDispatcher) {
        // Given: Location permission is GRANTED
        permissionFlow.value = mapOf("LOCATION_PERMISSION" to PermissionHelper.PermissionResult.Granted("LOCATION_PERMISSION"))
        
        // Given: LocationHelper returns Turkey
        coEvery { locationHelper.hasLocationPermission() } returns true
        coEvery { locationHelper.getBestLocation() } returns LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            city = "Istanbul",
            country = "Turkey",
            accuracy = 10.0f
        )
        
        settingsFlow.value = AppSettings(locationCity = null)
        coEvery { settingsRepository.getCurrentSettings() } returns settingsFlow.value

        // When: ViewModel initialized
        viewModel = PermissionWizardViewModel(
            permissionHelper,
            settingsRepository,
            locationHelper,
            context
        )
        advanceUntilIdle()

        // Then: verify settings updated with method 13 (Diyanet)
        val slot = slot<AppSettings>()
        coVerify { settingsRepository.updateSettings(capture(slot)) }
        assertEquals("Should set country to Turkey", "Turkey", slot.captured.locationCountry)
        assertEquals("Should set calculation method to 13 (Diyanet)", 13, slot.captured.prayerCalculationMethod)
    }
    
    @Test
    fun `should NOT update settings if city is already present`() = runTest(testDispatcher) {
        // Given: Location permission is GRANTED but settings already has a city
        permissionFlow.value = mapOf("LOCATION_PERMISSION" to PermissionHelper.PermissionResult.Granted("LOCATION_PERMISSION"))
        
        settingsFlow.value = AppSettings(locationCity = "ExistingCity", locationCountry = "ExistingCountry")
        coEvery { settingsRepository.getCurrentSettings() } returns settingsFlow.value

        coEvery { locationHelper.hasLocationPermission() } returns true
        
        // When: ViewModel initialized
        viewModel = PermissionWizardViewModel(
            permissionHelper,
            settingsRepository,
            locationHelper,
            context
        )
        advanceUntilIdle()

        // Then: verify updateSettings is NOT called (or at least not for location update logic)
        // Since initializeWizardState calls updatePermissionStatuses (which might trigger things), detecting "Not Called" is tricky if other things call it.
        // But our logic specifically guards with 'if (settings.locationCity == null)'.
        // Let's verify locationHelper.getBestLocation() was NOT called.
        coVerify(exactly = 0) { locationHelper.getBestLocation() }
    }
}
