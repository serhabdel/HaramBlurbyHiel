package com.hieltech.haramblur.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.PrayerTimesRepository
// Using local NextPrayerInfo data class defined below
import com.hieltech.haramblur.data.prayer.PrayerData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Prayer Times screen
 */
@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    private val _prayerTimes = MutableStateFlow<PrayerData?>(null)
    val prayerTimes: StateFlow<PrayerData?> = _prayerTimes.asStateFlow()

    private val _nextPrayer = MutableStateFlow<NextPrayerInfo?>(null)
    val nextPrayer: StateFlow<NextPrayerInfo?> = _nextPrayer.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPrayerTimes()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                prayerTimesRepository.getPrayerTimes().onSuccess { data ->
                    _prayerTimes.value = data
                    calculateNextPrayer(data)
                }.onFailure {
                    // Handle error
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateNextPrayer(prayerData: PrayerData) {
        val currentTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTimeStr = dateFormat.format(currentTime.time)
        
        val prayers = listOf(
            "Fajr" to prayerData.timings.Fajr,
            "Dhuhr" to prayerData.timings.Dhuhr,
            "Asr" to prayerData.timings.Asr,
            "Maghrib" to prayerData.timings.Maghrib,
            "Isha" to prayerData.timings.Isha
        )
        
        for ((name, time) in prayers) {
            if (time > currentTimeStr) {
                val timeUntil = calculateTimeUntil(currentTimeStr, time)
                _nextPrayer.value = NextPrayerInfo(name, time, timeUntil)
                return
            }
        }
        
        // If all prayers passed, next is tomorrow's Fajr
        prayers.firstOrNull()?.let { (name, time) ->
            _nextPrayer.value = NextPrayerInfo(name, time, "Tomorrow")
        }
    }

    private fun calculateTimeUntil(current: String, target: String): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDate = format.parse(current) ?: return ""
        val targetDate = format.parse(target) ?: return ""
        
        val diffMillis = targetDate.time - currentDate.time
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60
        
        return when {
            hours > 0 -> "in ${hours}h ${minutes}m"
            minutes > 0 -> "in ${minutes}m"
            else -> "Now"
        }
    }
}

/**
 * Simple data class for next prayer info
 */
data class NextPrayerInfo(
    val name: String,
    val time: String,
    val timeUntil: String
)
