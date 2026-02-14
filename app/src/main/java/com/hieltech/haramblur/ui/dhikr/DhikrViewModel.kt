package com.hieltech.haramblur.ui.dhikr

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI State for Dhikr Screen
 */
data class DhikrUiState(
    val selectedCategory: DhikrCategory = DhikrCategory.AFTER_PRAYER,
    val currentDhikrList: List<Dhikr> = emptyList(),
    val tasbihCounters: Map<String, TasbihCounter> = emptyMap(),
    val dailyProgress: DailyDhikrProgress = DailyDhikrProgress(date = ""),
    val isLoading: Boolean = false,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val hapticEnabled: Boolean = true,
    val totalDhikrToday: Int = 0,
    val currentTasbihIndex: Int = 0 // 0 = SubhanAllah, 1 = Alhamdulillah, 2 = AllahuAkbar
)

@HiltViewModel
class DhikrViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dhikrRepository: DhikrRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DhikrUiState())
    val uiState: StateFlow<DhikrUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("dhikr_counter_prefs", Context.MODE_PRIVATE)
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        loadInitialData()
        observeSettings()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load tasbih counters from preferences
            val counters = loadTasbihCounters()
            val dailyProgress = loadDailyProgress()
            val dhikrList = getDhikrForCategory(DhikrCategory.AFTER_PRAYER)
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    tasbihCounters = counters,
                    dailyProgress = dailyProgress,
                    currentDhikrList = dhikrList,
                    totalDhikrToday = dailyProgress.totalDhikrCount
                )
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        showTransliteration = settings.dhikrShowTransliteration,
                        showTranslation = settings.dhikrShowTranslation
                    )
                }
            }
        }
    }

    fun selectCategory(category: DhikrCategory) {
        val dhikrList = getDhikrForCategory(category)
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                currentDhikrList = dhikrList
            )
        }
    }

    private fun getDhikrForCategory(category: DhikrCategory): List<Dhikr> {
        return when (category) {
            DhikrCategory.MORNING_REMEMBRANCE -> DhikrDataSource.morningDhikr
            DhikrCategory.EVENING_REMEMBRANCE -> DhikrDataSource.eveningDhikr
            DhikrCategory.AFTER_PRAYER -> DhikrDataSource.afterPrayerDhikr
            DhikrCategory.TASBIH -> DhikrDataSource.tasbihDhikr
            DhikrCategory.GENERAL -> DhikrDataSource.anytimeDhikr
            DhikrCategory.ISTIGHFAR -> DhikrDataSource.anytimeDhikr.filter { it.category == DhikrCategory.ISTIGHFAR }
            DhikrCategory.SALAWAT -> emptyList() // Can be extended
            DhikrCategory.DUA -> DhikrDataSource.anytimeDhikr.filter { it.category == DhikrCategory.DUA }
        }
    }

    /**
     * Increment tasbih counter with haptic feedback
     */
    fun incrementTasbih(dhikrId: String) {
        viewModelScope.launch {
            val currentCounters = _uiState.value.tasbihCounters.toMutableMap()
            val counter = currentCounters[dhikrId] ?: TasbihCounter(dhikrId = dhikrId)
            
            val newCount = counter.currentCount + 1
            val isComplete = newCount >= counter.targetCount
            
            val updatedCounter = counter.copy(
                currentCount = if (isComplete) 0 else newCount,
                totalCompleted = if (isComplete) counter.totalCompleted + 1 else counter.totalCompleted
            )
            
            currentCounters[dhikrId] = updatedCounter
            
            // Update daily progress
            val dailyProgress = _uiState.value.dailyProgress
            val newTotalCount = dailyProgress.totalDhikrCount + 1
            val newTasbihSets = if (isComplete) dailyProgress.tasbihSets + 1 else dailyProgress.tasbihSets
            
            val updatedProgress = dailyProgress.copy(
                totalDhikrCount = newTotalCount,
                tasbihSets = newTasbihSets
            )
            
            _uiState.update { state ->
                state.copy(
                    tasbihCounters = currentCounters,
                    dailyProgress = updatedProgress,
                    totalDhikrToday = newTotalCount
                )
            }
            
            // Provide haptic feedback
            if (_uiState.value.hapticEnabled) {
                provideHapticFeedback(isComplete)
            }
            
            // Auto-advance to next tasbih when complete
            if (isComplete) {
                advanceToNextTasbih()
            }
            
            // Persist counters
            saveTasbihCounters(currentCounters)
            saveDailyProgress(updatedProgress)
        }
    }

    /**
     * Advance to next tasbih in sequence (SubhanAllah -> Alhamdulillah -> AllahuAkbar)
     */
    private fun advanceToNextTasbih() {
        val currentIndex = _uiState.value.currentTasbihIndex
        val nextIndex = (currentIndex + 1) % 3
        _uiState.update { it.copy(currentTasbihIndex = nextIndex) }
    }

    fun setCurrentTasbihIndex(index: Int) {
        _uiState.update { it.copy(currentTasbihIndex = index.coerceIn(0, 2)) }
    }

    /**
     * Reset tasbih counter for a specific dhikr
     */
    fun resetTasbih(dhikrId: String) {
        viewModelScope.launch {
            val currentCounters = _uiState.value.tasbihCounters.toMutableMap()
            val counter = currentCounters[dhikrId] ?: TasbihCounter(dhikrId = dhikrId)
            currentCounters[dhikrId] = counter.copy(currentCount = 0)
            
            _uiState.update { state ->
                state.copy(tasbihCounters = currentCounters)
            }
            
            saveTasbihCounters(currentCounters)
        }
    }

    /**
     * Reset all tasbih counters
     */
    fun resetAllTasbih() {
        viewModelScope.launch {
            val resetCounters = _uiState.value.tasbihCounters.mapValues { (_, counter) ->
                counter.copy(currentCount = 0)
            }
            
            _uiState.update { state ->
                state.copy(
                    tasbihCounters = resetCounters,
                    currentTasbihIndex = 0
                )
            }
            
            saveTasbihCounters(resetCounters)
        }
    }

    fun toggleHaptic() {
        _uiState.update { it.copy(hapticEnabled = !it.hapticEnabled) }
    }

    private fun provideHapticFeedback(isComplete: Boolean) {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (isComplete) {
                    // Stronger feedback for completion
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    // Light tap for each count
                    VibrationEffect.createOneShot(30, 50)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (isComplete) 100 else 30)
            }
        }
    }

    private fun getCurrentDateKey(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun loadTasbihCounters(): Map<String, TasbihCounter> {
        val dateKey = getCurrentDateKey()
        val savedDate = prefs.getString("tasbih_date", "")
        
        // Reset if it's a new day
        if (savedDate != dateKey) {
            prefs.edit().clear().putString("tasbih_date", dateKey).apply()
            return DhikrDataSource.tasbihDhikr.associate { dhikr ->
                dhikr.id to TasbihCounter(dhikrId = dhikr.id)
            }
        }
        
        return DhikrDataSource.tasbihDhikr.associate { dhikr ->
            val count = prefs.getInt("${dhikr.id}_count", 0)
            val completed = prefs.getInt("${dhikr.id}_completed", 0)
            dhikr.id to TasbihCounter(
                dhikrId = dhikr.id,
                currentCount = count,
                totalCompleted = completed
            )
        }
    }

    private fun saveTasbihCounters(counters: Map<String, TasbihCounter>) {
        prefs.edit().apply {
            putString("tasbih_date", getCurrentDateKey())
            counters.forEach { (id, counter) ->
                putInt("${id}_count", counter.currentCount)
                putInt("${id}_completed", counter.totalCompleted)
            }
            apply()
        }
    }

    private fun loadDailyProgress(): DailyDhikrProgress {
        val dateKey = getCurrentDateKey()
        val savedDate = prefs.getString("progress_date", "")
        
        if (savedDate != dateKey) {
            return DailyDhikrProgress(date = dateKey)
        }
        
        return DailyDhikrProgress(
            date = dateKey,
            morningCompleted = prefs.getBoolean("morning_completed", false),
            eveningCompleted = prefs.getBoolean("evening_completed", false),
            afterPrayerCount = prefs.getInt("after_prayer_count", 0),
            tasbihSets = prefs.getInt("tasbih_sets", 0),
            totalDhikrCount = prefs.getInt("total_dhikr_count", 0)
        )
    }

    private fun saveDailyProgress(progress: DailyDhikrProgress) {
        prefs.edit().apply {
            putString("progress_date", progress.date)
            putBoolean("morning_completed", progress.morningCompleted)
            putBoolean("evening_completed", progress.eveningCompleted)
            putInt("after_prayer_count", progress.afterPrayerCount)
            putInt("tasbih_sets", progress.tasbihSets)
            putInt("total_dhikr_count", progress.totalDhikrCount)
            apply()
        }
    }

    fun markMorningCompleted() {
        viewModelScope.launch {
            val progress = _uiState.value.dailyProgress.copy(morningCompleted = true)
            _uiState.update { it.copy(dailyProgress = progress) }
            saveDailyProgress(progress)
        }
    }

    fun markEveningCompleted() {
        viewModelScope.launch {
            val progress = _uiState.value.dailyProgress.copy(eveningCompleted = true)
            _uiState.update { it.copy(dailyProgress = progress) }
            saveDailyProgress(progress)
        }
    }
}
