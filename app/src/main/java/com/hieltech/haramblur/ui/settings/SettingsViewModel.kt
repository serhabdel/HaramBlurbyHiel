package com.hieltech.haramblur.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.models.SystemStatus
import com.hieltech.haramblur.data.models.QuickSetting
import com.hieltech.haramblur.data.models.RecentSetting
import com.hieltech.haramblur.data.models.SystemHealth
import com.hieltech.haramblur.data.models.SettingType
import com.hieltech.haramblur.data.models.StatusIndicator
import com.hieltech.haramblur.data.models.UserActionType
import com.hieltech.haramblur.data.repository.StatisticsRepository
import com.hieltech.haramblur.data.repository.SettingsRepository
import com.hieltech.haramblur.utils.SystemHealthMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the enhanced settings screen with real-time status and statistics
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val settingsRepository: SettingsRepository,
    private val systemHealthMonitor: SystemHealthMonitor
) : ViewModel() {
    
    // System status state
    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    
    // Quick settings state
    private val _quickSettings = MutableStateFlow<List<QuickSetting>>(emptyList())
    val quickSettings: StateFlow<List<QuickSetting>> = _quickSettings.asStateFlow()
    
    // Recent settings state
    private val _recentSettings = MutableStateFlow<List<RecentSetting>>(emptyList())
    val recentSettings: StateFlow<List<RecentSetting>> = _recentSettings.asStateFlow()
    
    // System health state
    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadInitialData()
        observeDataChanges()
    }
    
    /**
     * Load initial data for the settings screen
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load system status
                _systemStatus.value = statisticsRepository.getCurrentSystemStatus()
                
                // Load quick settings
                _quickSettings.value = createQuickSettings()
                
                // Load recent settings
                _recentSettings.value = settingsRepository.getRecentSettings(5)
                
                // Load system health
                _systemHealth.value = systemHealthMonitor.getSystemHealth()
                
            } catch (e: Exception) {
                _error.value = "Failed to load settings data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observe data changes and update UI accordingly
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            // Observe system status flow
            statisticsRepository.getSystemStatusFlow().collect { status ->
                _systemStatus.value = status
            }
        }
        
        viewModelScope.launch {
            // Observe recent settings flow
            settingsRepository.getRecentSettingsFlow(5).collect { recent ->
                _recentSettings.value = recent
            }
        }
        
        viewModelScope.launch {
            // Observe system health
            _systemHealth.value = systemHealthMonitor.getSystemHealth()
        }
    }
    
    /**
     * Create quick settings configuration
     */
    private fun createQuickSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "protection",
                displayName = com.hieltech.haramblur.R.string.protection_active,
                currentValue = _systemStatus.value.protectionEnabled,
                settingType = SettingType.TOGGLE,
                iconRes = android.R.drawable.ic_dialog_alert,
                statusIndicator = if (_systemStatus.value.protectionEnabled) StatusIndicator.ENABLED else StatusIndicator.DISABLED,
                description = "Enable/disable face detection and content blocking"
            ),
            QuickSetting(
                id = "prayer_times",
                displayName = com.hieltech.haramblur.R.string.prayer_times,
                currentValue = true, // Would be loaded from actual settings
                settingType = SettingType.TOGGLE,
                iconRes = android.R.drawable.ic_menu_my_calendar,
                statusIndicator = StatusIndicator.ENABLED,
                description = "Show prayer time notifications"
            ),
            QuickSetting(
                id = "dhikr",
                displayName = com.hieltech.haramblur.R.string.dhikr,
                currentValue = true, // Would be loaded from actual settings
                settingType = SettingType.TOGGLE,
                iconRes = android.R.drawable.ic_menu_edit,
                statusIndicator = StatusIndicator.ENABLED,
                description = "Enable dhikr reminders"
            ),
            QuickSetting(
                id = "site_blocking",
                displayName = com.hieltech.haramblur.R.string.site_blocking,
                currentValue = true, // Would be loaded from actual settings
                settingType = SettingType.TOGGLE,
                iconRes = android.R.drawable.ic_menu_close_clear_cancel,
                statusIndicator = StatusIndicator.ENABLED,
                description = "Block inappropriate websites"
            ),
            QuickSetting(
                id = "nsfw_detection",
                displayName = com.hieltech.haramblur.R.string.nsfw_detection,
                currentValue = true, // Would be loaded from actual settings
                settingType = SettingType.TOGGLE,
                iconRes = android.R.drawable.ic_menu_view,
                statusIndicator = StatusIndicator.ENABLED,
                description = "Detect and blur inappropriate content"
            ),
            QuickSetting(
                id = "blur_intensity",
                displayName = com.hieltech.haramblur.R.string.blur_intensity,
                currentValue = 0.7f, // Would be loaded from actual settings
                settingType = SettingType.SLIDER,
                iconRes = android.R.drawable.ic_menu_manage,
                statusIndicator = StatusIndicator.ENABLED,
                description = "Adjust blur intensity for detected content"
            )
        )
    }
    
    /**
     * Toggle a quick setting
     */
    fun toggleQuickSetting(settingId: String) {
        viewModelScope.launch {
            try {
                val setting = _quickSettings.value.find { it.id == settingId }
                if (setting != null && setting.canToggle()) {
                    val newValue = !(setting.currentValue as Boolean)
                    
                    // Update the setting
                    val updatedSettings = _quickSettings.value.map { s ->
                        if (s.id == settingId) {
                            s.copy(
                                currentValue = newValue,
                                statusIndicator = if (newValue) StatusIndicator.ENABLED else StatusIndicator.DISABLED
                            )
                        } else s
                    }
                    _quickSettings.value = updatedSettings
                    
                    // Record the change
                    settingsRepository.recordToggleChange(
                        settingName = setting.displayName.toString(),
                        category = "Quick Settings",
                        isEnabled = newValue,
                        settingId = settingId
                    )
                    
                    // Update system status if it's the protection setting
                    if (settingId == "protection") {
                        _systemStatus.value = _systemStatus.value.copy(protectionEnabled = newValue)
                    }
                    
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle setting: ${e.message}"
            }
        }
    }
    
    /**
     * Update a quick setting value
     */
    fun updateQuickSetting(settingId: String, newValue: Any) {
        viewModelScope.launch {
            try {
                val setting = _quickSettings.value.find { it.id == settingId }
                if (setting != null) {
                    val oldValue = setting.currentValue
                    
                    // Update the setting
                    val updatedSettings = _quickSettings.value.map { s ->
                        if (s.id == settingId) {
                            s.copy(currentValue = newValue)
                        } else s
                    }
                    _quickSettings.value = updatedSettings
                    
                    // Record the change
                    settingsRepository.recordValueChange(
                        settingName = setting.displayName.toString(),
                        category = "Quick Settings",
                        previousValue = oldValue.toString(),
                        newValue = newValue.toString(),
                        settingId = settingId
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to update setting: ${e.message}"
            }
        }
    }
    
    /**
     * Refresh system status
     */
    fun refreshSystemStatus() {
        viewModelScope.launch {
            try {
                _systemStatus.value = statisticsRepository.getCurrentSystemStatus()
                _systemHealth.value = systemHealthMonitor.getSystemHealth()
            } catch (e: Exception) {
                _error.value = "Failed to refresh system status: ${e.message}"
            }
        }
    }
    
    /**
     * Refresh recent settings
     */
    fun refreshRecentSettings() {
        viewModelScope.launch {
            try {
                _recentSettings.value = settingsRepository.getRecentSettings(5)
            } catch (e: Exception) {
                _error.value = "Failed to refresh recent settings: ${e.message}"
            }
        }
    }
    
    /**
     * Get detailed statistics for a date range
     */
    suspend fun getDetailedStatistics(days: Int = 7): List<com.hieltech.haramblur.data.database.entities.StatisticsEntity> {
        return try {
            statisticsRepository.getRecentStatistics(days)
        } catch (e: Exception) {
            _error.value = "Failed to get detailed statistics: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Get detection trends
     */
    suspend fun getDetectionTrends(days: Int = 7): List<Pair<java.time.LocalDate, Int>> {
        return try {
            statisticsRepository.getDetectionTrends(days)
        } catch (e: Exception) {
            _error.value = "Failed to get detection trends: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Export settings data
     */
    suspend fun exportSettingsData(): String {
        return try {
            val recentSettings = settingsRepository.exportRecentSettings(30)
            val statistics = statisticsRepository.exportStatistics(
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
            )
            
            // Create JSON export (simplified)
            """
            {
                "exportDate": "${java.time.LocalDateTime.now()}",
                "recentSettings": ${recentSettings.size},
                "statistics": ${statistics.size},
                "systemHealth": "${_systemHealth.value.getHealthStatus()}"
            }
            """.trimIndent()
        } catch (e: Exception) {
            _error.value = "Failed to export settings: ${e.message}"
            ""
        }
    }
    
    /**
     * Reset settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                // Reset quick settings to defaults
                _quickSettings.value = createQuickSettings()
                
                // Record reset action
                settingsRepository.recordResetAction(
                    settingName = "All Quick Settings",
                    category = "Quick Settings",
                    previousValue = "Custom",
                    settingId = "all_quick_settings"
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to reset settings: ${e.message}"
            }
        }
    }
}
