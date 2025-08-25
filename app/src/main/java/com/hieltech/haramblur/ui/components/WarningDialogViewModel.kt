package com.hieltech.haramblur.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing warning dialog state and countdown timer
 */
@HiltViewModel
class WarningDialogViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _dialogState = MutableStateFlow(WarningDialogState())
    val dialogState: StateFlow<WarningDialogState> = _dialogState.asStateFlow()
    
    private var countdownJob: Job? = null
    
    /**
     * Show warning dialog with Islamic guidance
     */
    fun showWarningDialog(
        title: String,
        message: String,
        quranicVerse: QuranicVerse? = null,
        category: BlockingCategory? = null,
        reflectionTimeSeconds: Int? = null
    ) {
        val settings = settingsRepository.getCurrentSettings()
        val actualReflectionTime = reflectionTimeSeconds 
            ?: category?.defaultReflectionTime 
            ?: settings.mandatoryReflectionTime
        
        _dialogState.value = WarningDialogState(
            isVisible = true,
            title = title,
            message = message,
            quranicVerse = quranicVerse,
            reflectionTimeSeconds = actualReflectionTime,
            remainingTimeSeconds = actualReflectionTime,
            canContinue = false,
            showCloseOption = true,
            showContinueOption = true,
            category = category,
            language = settings.preferredLanguage
        )
        
        startCountdown()
    }
    
    /**
     * Handle dialog actions
     */
    fun handleAction(action: WarningDialogAction): WarningDialogResult {
        val currentState = _dialogState.value
        
        return when (action) {
            is WarningDialogAction.Close -> {
                hideDialog()
                WarningDialogResult(
                    action = action,
                    reflectionTimeCompleted = currentState.canContinue,
                    verseRead = currentState.quranicVerse != null
                )
            }
            
            is WarningDialogAction.Continue -> {
                if (currentState.canContinue) {
                    hideDialog()
                    WarningDialogResult(
                        action = action,
                        reflectionTimeCompleted = true,
                        verseRead = currentState.quranicVerse != null
                    )
                } else {
                    // Cannot continue yet, return current state
                    WarningDialogResult(
                        action = action,
                        reflectionTimeCompleted = false,
                        verseRead = currentState.quranicVerse != null
                    )
                }
            }
            
            is WarningDialogAction.Dismiss -> {
                if (currentState.canContinue) {
                    hideDialog()
                }
                WarningDialogResult(
                    action = action,
                    reflectionTimeCompleted = currentState.canContinue,
                    verseRead = currentState.quranicVerse != null
                )
            }
            
            is WarningDialogAction.ChangeLanguage -> {
                _dialogState.value = currentState.copy(language = action.language)
                WarningDialogResult(
                    action = action,
                    reflectionTimeCompleted = currentState.canContinue,
                    verseRead = currentState.quranicVerse != null
                )
            }
        }
    }
    
    /**
     * Hide the dialog
     */
    fun hideDialog() {
        countdownJob?.cancel()
        _dialogState.value = WarningDialogState()
    }
    
    /**
     * Start the reflection countdown timer
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val totalTime = _dialogState.value.reflectionTimeSeconds
            
            for (remaining in totalTime downTo 0) {
                _dialogState.value = _dialogState.value.copy(
                    remainingTimeSeconds = remaining,
                    canContinue = remaining == 0
                )
                
                if (remaining > 0) {
                    delay(1000) // Wait 1 second
                }
            }
        }
    }
    
    /**
     * Update reflection time (for settings changes)
     */
    fun updateReflectionTime(newTime: Int) {
        val currentState = _dialogState.value
        if (currentState.isVisible && !currentState.canContinue) {
            _dialogState.value = currentState.copy(
                reflectionTimeSeconds = newTime,
                remainingTimeSeconds = newTime
            )
            startCountdown() // Restart countdown with new time
        }
    }
    
    /**
     * Check if dialog is currently visible
     */
    fun isDialogVisible(): Boolean = _dialogState.value.isVisible
    
    /**
     * Get current reflection progress (0.0 to 1.0)
     */
    fun getReflectionProgress(): Float {
        val state = _dialogState.value
        return if (state.reflectionTimeSeconds > 0) {
            (state.reflectionTimeSeconds - state.remainingTimeSeconds).toFloat() / state.reflectionTimeSeconds
        } else {
            1.0f
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}