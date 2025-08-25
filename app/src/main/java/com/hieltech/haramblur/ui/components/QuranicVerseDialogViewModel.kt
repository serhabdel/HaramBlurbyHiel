package com.hieltech.haramblur.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.IslamicGuidance
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.data.WarningDialogResult
import com.hieltech.haramblur.data.WarningDialogState
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Quranic verse dialog state and interactions
 */
@HiltViewModel
class QuranicVerseDialogViewModel @Inject constructor(
    private val quranicRepository: QuranicRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _dialogState = MutableStateFlow(WarningDialogState())
    val dialogState: StateFlow<WarningDialogState> = _dialogState.asStateFlow()
    
    private val _currentVerse = MutableStateFlow<QuranicVerse?>(null)
    val currentVerse: StateFlow<QuranicVerse?> = _currentVerse.asStateFlow()
    
    private val _currentGuidance = MutableStateFlow<IslamicGuidance?>(null)
    val currentGuidance: StateFlow<IslamicGuidance?> = _currentGuidance.asStateFlow()
    
    private val _dialogResult = MutableStateFlow<WarningDialogResult?>(null)
    val dialogResult: StateFlow<WarningDialogResult?> = _dialogResult.asStateFlow()
    
    // Combine settings with dialog state for reactive updates
    val combinedState = combine(
        dialogState,
        settingsRepository.settings,
        currentVerse,
        currentGuidance
    ) { state, settings, verse, guidance ->
        CombinedDialogState(
            dialogState = state,
            verse = verse,
            guidance = guidance,
            selectedLanguage = settings.preferredLanguage,
            enableArabicText = settings.enableArabicText,
            reflectionTime = settings.mandatoryReflectionTime
        )
    }
    
    /**
     * Show dialog for a specific blocking category
     */
    fun showDialog(
        category: BlockingCategory,
        title: String = "",
        message: String = "",
        showCloseOption: Boolean = true,
        showContinueOption: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                // Load verse and guidance for the category
                val verse = quranicRepository.getVerseForCategory(category)
                val guidance = quranicRepository.getGuidanceForCategory(category)
                val settings = settingsRepository.getCurrentSettings()
                
                _currentVerse.value = verse
                _currentGuidance.value = guidance
                
                _dialogState.value = WarningDialogState(
                    isVisible = true,
                    title = title.ifBlank { getCategoryTitle(category) },
                    message = message,
                    quranicVerse = verse,
                    reflectionTimeSeconds = settings.mandatoryReflectionTime,
                    remainingTimeSeconds = settings.mandatoryReflectionTime,
                    canContinue = false,
                    showCloseOption = showCloseOption,
                    showContinueOption = showContinueOption,
                    category = category,
                    language = settings.preferredLanguage
                )
            } catch (e: Exception) {
                // Handle error by showing basic dialog
                showBasicDialog(category, title, message, showCloseOption, showContinueOption)
            }
        }
    }
    
    /**
     * Show dialog with contextual guidance
     */
    fun showContextualDialog(
        category: BlockingCategory,
        isRepeatedOffense: Boolean = false,
        timeOfDay: String? = null,
        customReflectionTime: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                val guidance = quranicRepository.generateContextualGuidance(
                    category = category,
                    userLanguage = settings.preferredLanguage,
                    timeOfDay = timeOfDay,
                    isRepeatedOffense = isRepeatedOffense
                )
                
                _currentVerse.value = guidance?.verse
                _currentGuidance.value = guidance
                
                val reflectionTime = customReflectionTime ?: 
                    if (isRepeatedOffense) settings.mandatoryReflectionTime + 10 
                    else settings.mandatoryReflectionTime
                
                _dialogState.value = WarningDialogState(
                    isVisible = true,
                    title = getCategoryTitle(category),
                    message = if (isRepeatedOffense) "We notice this is happening frequently. Let's take extra time to reflect." else "",
                    quranicVerse = guidance?.verse,
                    reflectionTimeSeconds = reflectionTime,
                    remainingTimeSeconds = reflectionTime,
                    canContinue = false,
                    showCloseOption = true,
                    showContinueOption = true,
                    category = category,
                    language = settings.preferredLanguage
                )
            } catch (e: Exception) {
                showBasicDialog(category)
            }
        }
    }
    
    /**
     * Handle dialog actions
     */
    fun handleAction(action: WarningDialogAction) {
        val currentState = _dialogState.value
        
        when (action) {
            is WarningDialogAction.Close -> {
                _dialogResult.value = WarningDialogResult(
                    action = action,
                    reflectionTimeCompleted = currentState.canContinue,
                    verseRead = true // Assume user read the verse if they took action
                )
                hideDialog()
            }
            
            is WarningDialogAction.Continue -> {
                if (currentState.canContinue) {
                    _dialogResult.value = WarningDialogResult(
                        action = action,
                        reflectionTimeCompleted = true,
                        verseRead = true
                    )
                    hideDialog()
                }
            }
            
            is WarningDialogAction.ChangeLanguage -> {
                updateLanguage(action.language)
            }
            
            is WarningDialogAction.Dismiss -> {
                _dialogResult.value = WarningDialogResult(
                    action = action,
                    reflectionTimeCompleted = currentState.canContinue,
                    verseRead = false
                )
                hideDialog()
            }
        }
    }
    
    /**
     * Update reflection timer
     */
    fun updateReflectionTimer(remainingSeconds: Int) {
        _dialogState.value = _dialogState.value.copy(
            remainingTimeSeconds = remainingSeconds,
            canContinue = remainingSeconds <= 0
        )
    }
    
    /**
     * Change language preference
     */
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getCurrentSettings()
            settingsRepository.updateSettings(
                currentSettings.copy(preferredLanguage = language)
            )
            
            _dialogState.value = _dialogState.value.copy(language = language)
            
            // Reload guidance with new language if needed
            _dialogState.value.category?.let { category ->
                val guidance = quranicRepository.generateContextualGuidance(
                    category = category,
                    userLanguage = language
                )
                _currentGuidance.value = guidance
            }
        }
    }
    
    /**
     * Hide dialog
     */
    fun hideDialog() {
        _dialogState.value = WarningDialogState()
        _currentVerse.value = null
        _currentGuidance.value = null
    }
    
    /**
     * Clear dialog result after handling
     */
    fun clearDialogResult() {
        _dialogResult.value = null
    }
    
    /**
     * Get random verse for inspiration
     */
    fun loadRandomVerse() {
        viewModelScope.launch {
            try {
                val verse = quranicRepository.getRandomVerse()
                _currentVerse.value = verse
                
                val guidance = quranicRepository.getGuidanceForCategory(verse.category)
                _currentGuidance.value = guidance
            } catch (e: Exception) {
                // Handle error silently or show fallback content
            }
        }
    }
    
    /**
     * Search verses by text
     */
    fun searchVerses(query: String, onResult: (List<QuranicVerse>) -> Unit) {
        viewModelScope.launch {
            try {
                val results = quranicRepository.searchVerses(query)
                onResult(results)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Get verses by category
     */
    fun getVersesByCategory(category: BlockingCategory, onResult: (List<QuranicVerse>) -> Unit) {
        viewModelScope.launch {
            try {
                val verses = quranicRepository.getAllVersesForCategory(category)
                onResult(verses)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    private fun showBasicDialog(
        category: BlockingCategory,
        title: String = "",
        message: String = "",
        showCloseOption: Boolean = true,
        showContinueOption: Boolean = true
    ) {
        val settings = settingsRepository.getCurrentSettings()
        
        _dialogState.value = WarningDialogState(
            isVisible = true,
            title = title.ifBlank { getCategoryTitle(category) },
            message = message.ifBlank { getDefaultMessage(category) },
            quranicVerse = null,
            reflectionTimeSeconds = settings.mandatoryReflectionTime,
            remainingTimeSeconds = settings.mandatoryReflectionTime,
            canContinue = false,
            showCloseOption = showCloseOption,
            showContinueOption = showContinueOption,
            category = category,
            language = settings.preferredLanguage
        )
    }
    
    private fun getCategoryTitle(category: BlockingCategory): String {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT -> "Content Warning"
            BlockingCategory.ADULT_ENTERTAINMENT -> "Adult Content Blocked"
            BlockingCategory.INAPPROPRIATE_IMAGERY -> "Inappropriate Content"
            BlockingCategory.GAMBLING -> "Gambling Site Blocked"
            BlockingCategory.DATING_SITES -> "Dating Site Notice"
            BlockingCategory.SUSPICIOUS_CONTENT -> "Content Advisory"
            BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> "Social Media Warning"
            BlockingCategory.VIOLENCE -> "Violent Content Warning"
            BlockingCategory.HATE_SPEECH -> "Harmful Content Blocked"
            BlockingCategory.SUBSTANCE_ABUSE -> "Substance-Related Content"
        }
    }
    
    private fun getDefaultMessage(category: BlockingCategory): String {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT -> "This content may not align with Islamic values. Take a moment to reflect."
            BlockingCategory.ADULT_ENTERTAINMENT -> "This site contains adult content. Consider choosing more beneficial activities."
            BlockingCategory.GAMBLING -> "Gambling is prohibited in Islam. Choose activities that bring barakah instead."
            BlockingCategory.DATING_SITES -> "Consider Islamic approaches to finding a spouse through family and community."
            else -> "This content may not be beneficial. Take time to consider your choices."
        }
    }
}

/**
 * Combined state for the dialog UI
 */
data class CombinedDialogState(
    val dialogState: WarningDialogState,
    val verse: QuranicVerse?,
    val guidance: IslamicGuidance?,
    val selectedLanguage: Language,
    val enableArabicText: Boolean,
    val reflectionTime: Int
)