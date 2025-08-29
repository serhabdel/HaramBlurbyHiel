package com.hieltech.haramblur.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class DhikrRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private const val TAG = "DhikrRepository"
        private const val DHIKR_PREFS = "dhikr_preferences"
        private const val LAST_DHIKR_SHOWN = "last_dhikr_shown"
        private const val DHIKR_COUNT_TODAY = "dhikr_count_today"
        private const val LAST_DHIKR_DATE = "last_dhikr_date"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(DHIKR_PREFS, Context.MODE_PRIVATE)
    
    // Get dhikr settings from app settings  
    private val _dhikrSettings = MutableStateFlow(getDhikrSettingsFromApp())
    val dhikrSettings: StateFlow<DhikrSettings> = _dhikrSettings.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        // Update dhikr settings whenever app settings change
        scope.launch {
            settingsRepository.settings.collect { appSettings ->
                _dhikrSettings.value = DhikrSettings(
                    enabled = appSettings.dhikrEnabled,
                    morningEnabled = appSettings.dhikrMorningEnabled,
                    eveningEnabled = appSettings.dhikrEveningEnabled,
                    anytimeEnabled = appSettings.dhikrAnytimeEnabled,
                    morningStartTime = appSettings.dhikrMorningStart,
                    morningEndTime = appSettings.dhikrMorningEnd,
                    eveningStartTime = appSettings.dhikrEveningStart,
                    eveningEndTime = appSettings.dhikrEveningEnd,
                    intervalMinutes = appSettings.dhikrIntervalMinutes,
                    displayDurationSeconds = appSettings.dhikrDisplayDuration,
                    displayPosition = DhikrPosition.valueOf(appSettings.dhikrPosition),
                    showTransliteration = appSettings.dhikrShowTransliteration,
                    showTranslation = appSettings.dhikrShowTranslation,
                    animationEnabled = appSettings.dhikrAnimationEnabled,
                    soundEnabled = appSettings.dhikrSoundEnabled
                )
            }
        }
    }
    
    private fun getDhikrSettingsFromApp(): DhikrSettings {
        val appSettings = settingsRepository.settings.value
        return DhikrSettings(
            enabled = appSettings.dhikrEnabled,
            morningEnabled = appSettings.dhikrMorningEnabled,
            eveningEnabled = appSettings.dhikrEveningEnabled,
            anytimeEnabled = appSettings.dhikrAnytimeEnabled,
            morningStartTime = appSettings.dhikrMorningStart,
            morningEndTime = appSettings.dhikrMorningEnd,
            eveningStartTime = appSettings.dhikrEveningStart,
            eveningEndTime = appSettings.dhikrEveningEnd,
            intervalMinutes = appSettings.dhikrIntervalMinutes,
            displayDurationSeconds = appSettings.dhikrDisplayDuration,
            displayPosition = DhikrPosition.valueOf(appSettings.dhikrPosition),
            showTransliteration = appSettings.dhikrShowTransliteration,
            showTranslation = appSettings.dhikrShowTranslation,
            animationEnabled = appSettings.dhikrAnimationEnabled,
            soundEnabled = appSettings.dhikrSoundEnabled
        )
    }
    
    private val _currentDhikr = MutableStateFlow<Dhikr?>(null)
    val currentDhikr: StateFlow<Dhikr?> = _currentDhikr.asStateFlow()
    
    private val _isDisplaying = MutableStateFlow(false)
    val isDisplaying: StateFlow<Boolean> = _isDisplaying.asStateFlow()
    
    fun getCurrentTimeType(): DhikrTime {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val settings = dhikrSettings.value
        
        return when {
            hour >= settings.morningStartTime && hour < settings.morningEndTime -> DhikrTime.MORNING
            hour >= settings.eveningStartTime && hour < settings.eveningEndTime -> DhikrTime.EVENING
            else -> DhikrTime.ANYTIME
        }
    }
    
    fun shouldShowDhikr(): Boolean {
        val settings = dhikrSettings.value
        if (!settings.enabled) return false
        
        val currentTime = getCurrentTimeType()
        val timeEnabled = when (currentTime) {
            DhikrTime.MORNING -> settings.morningEnabled
            DhikrTime.EVENING -> settings.eveningEnabled
            DhikrTime.ANYTIME -> settings.anytimeEnabled
        }
        
        if (!timeEnabled) return false
        
        // Check if enough time has passed since last dhikr
        val lastShown = prefs.getLong(LAST_DHIKR_SHOWN, 0)
        val intervalMs = settings.intervalMinutes * 60 * 1000L
        val currentTimeMs = System.currentTimeMillis()
        
        return (currentTimeMs - lastShown) >= intervalMs
    }
    
    fun getNextDhikr(): Dhikr? {
        if (!shouldShowDhikr()) return null
        
        val currentTime = getCurrentTimeType()
        val dhikrList = when (currentTime) {
            DhikrTime.MORNING -> DhikrDataSource.morningDhikr
            DhikrTime.EVENING -> DhikrDataSource.eveningDhikr
            DhikrTime.ANYTIME -> DhikrDataSource.anytimeDhikr
        }
        
        return if (dhikrList.isNotEmpty()) dhikrList.random() else null
    }
    
    fun showDhikr(dhikr: Dhikr) {
        _currentDhikr.value = dhikr
        _isDisplaying.value = true
        
        // Update last shown time
        prefs.edit().putLong(LAST_DHIKR_SHOWN, System.currentTimeMillis()).apply()
        
        // Update daily count
        updateDailyDhikrCount()
        
        Log.d(TAG, "Showing dhikr: ${dhikr.id} - ${dhikr.time}")
    }
    
    fun hideDhikr() {
        _currentDhikr.value = null
        _isDisplaying.value = false
        Log.d(TAG, "Dhikr hidden")
    }
    
    private fun updateDailyDhikrCount() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastDate = prefs.getInt(LAST_DHIKR_DATE, -1)
        
        if (today != lastDate) {
            // New day, reset count
            prefs.edit()
                .putInt(DHIKR_COUNT_TODAY, 1)
                .putInt(LAST_DHIKR_DATE, today)
                .apply()
        } else {
            // Same day, increment count
            val currentCount = prefs.getInt(DHIKR_COUNT_TODAY, 0)
            prefs.edit().putInt(DHIKR_COUNT_TODAY, currentCount + 1).apply()
        }
    }
    
    fun getDailyDhikrCount(): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastDate = prefs.getInt(LAST_DHIKR_DATE, -1)
        
        return if (today == lastDate) {
            prefs.getInt(DHIKR_COUNT_TODAY, 0)
        } else {
            0 // Different day, count is 0
        }
    }
    
    fun getLastDhikrShownTime(): Long {
        return prefs.getLong(LAST_DHIKR_SHOWN, 0)
    }
    
    fun getTimeUntilNextDhikr(): Long {
        val settings = dhikrSettings.value
        val lastShown = getLastDhikrShownTime()
        val intervalMs = settings.intervalMinutes * 60 * 1000L
        val nextTime = lastShown + intervalMs
        val currentTime = System.currentTimeMillis()
        
        return maxOf(0, nextTime - currentTime)
    }
    
    fun getAllDhikr(): List<Dhikr> {
        return DhikrDataSource.getAllDhikr()
    }
    
    fun getDhikrByTime(time: DhikrTime): List<Dhikr> {
        return DhikrDataSource.getDhikrByTime(time)
    }
    
    fun toggleDhikrEnabled() {
        val currentSettings = settingsRepository.settings.value
        settingsRepository.updateSettings(currentSettings.copy(dhikrEnabled = !currentSettings.dhikrEnabled))
    }
    
    fun setDhikrEnabled(enabled: Boolean) {
        val currentSettings = settingsRepository.settings.value
        settingsRepository.updateSettings(currentSettings.copy(dhikrEnabled = enabled))
    }
}