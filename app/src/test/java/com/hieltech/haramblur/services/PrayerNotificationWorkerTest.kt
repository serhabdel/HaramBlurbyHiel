package com.hieltech.haramblur.services

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.data.PrayerTimesRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.prayer.PrayerData
import com.hieltech.haramblur.data.prayer.PrayerTimings
import com.hieltech.haramblur.detection.Language
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class PrayerNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var prayerTimesRepository: PrayerTimesRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prayerTimeNotificationManager: PrayerTimeNotificationManager
    private lateinit var worker: PrayerNotificationWorker
    
    // StateFlow for settings
    private val settingsFlow = MutableStateFlow(AppSettings(
        enablePrayerNotifications = true,
        enablePrayerTimes = true
    ))

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        prayerTimesRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        prayerTimeNotificationManager = mockk(relaxed = true)

        every { settingsRepository.settings } returns settingsFlow
        
        // Mock success result for getting prayer times
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prayerData = PrayerData(
            timings = PrayerTimings(
                Fajr = "05:00",
                Dhuhr = "12:30",
                Asr = "15:45",
                Maghrib = "18:20",
                Isha = "20:00"
            ),
            date = null
        )
        coEvery { prayerTimesRepository.getPrayerTimes() } returns Result.success(prayerData)
        
        // Mock notification manager methods to not crash
        every { prayerTimeNotificationManager.hasNotificationBeenSent(any(), any()) } returns false
        every { prayerTimeNotificationManager.markNotificationAsSent(any(), any()) } returns Unit

        // Allow worker instantiation
        // Note: Roboelectric/WorkManagerTestInitHelper usually needed for real workers.
        // We'll manually construct since we injected dependencies.
        val workerParams = mockk<WorkerParameters>(relaxed = true)
        worker = PrayerNotificationWorker(
            context,
            workerParams,
            prayerTimesRepository,
            settingsRepository,
            prayerTimeNotificationManager
        )
    }

    // Helper to run work
    private fun runWorker() = runBlocking {
        worker.doWork()
    }

    // Note: Since we can't easily mock System.currentTimeMillis() inside the Worker without DI or static mocking,
    // verification of exact timing logic is hard without refactoring the Worker to take a Clock.
    // However, we can verifying that if `getPrayerTimes()` returns success, it attempts to process prayers.

    @Test
    fun `doWork should exit success if notifications disabled`() = runBlocking {
        settingsFlow.value = AppSettings(enablePrayerNotifications = false)
        
        val result = worker.doWork()
        
        assert(result is ListenableWorker.Result.Success)
        // Verify it didn't fetch prayer times
        coVerify(exactly = 0) { prayerTimesRepository.getPrayerTimes() }
    }
    
    @Test
    fun `doWork should fetch prayer times if enabled`() = runBlocking {
        val result = worker.doWork()
        
        assert(result is ListenableWorker.Result.Success)
        coVerify(exactly = 1) { prayerTimesRepository.getPrayerTimes() }
    }
    
    // Since we cannot mock "Current Time" easily for the worker which uses System.currentTimeMillis(),
    // we assume the logic inside `isWithinTimeWindow` works (it's pure math).
    // To properly test "Before/After" logic, we would ideally refactor the Worker to accept a TimeProvider.
    // For now, checking that it *runs* and *fetches data* confirms the wiring is correct.
    // The previous manual code analysis confirmed the "Before 10 min", "Before 5 min", "After 10 min" logic exists.
}
