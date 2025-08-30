package com.hieltech.haramblur.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the permission setup wizard state and logic
 */
@HiltViewModel
class PermissionWizardViewModel @Inject constructor(
    private val permissionHelper: PermissionHelper,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Data class representing a single wizard step
     */
    data class WizardStep(
        val stepNumber: Int,
        val title: String,
        val description: String,
        val permissionType: String,
        val status: PermissionStatus = PermissionStatus.PENDING,
        val isRequired: Boolean = true,
        val isCompleted: Boolean = false
    )

    /**
     * Permission status for wizard steps
     */
    enum class PermissionStatus {
        PENDING, GRANTED, DENIED, REQUESTING
    }

    /**
     * Overall wizard state
     */
    data class WizardState(
        val currentStepIndex: Int = 0,
        val steps: List<WizardStep> = emptyList(),
        val isLoading: Boolean = true,
        val isComplete: Boolean = false,
        val canProceed: Boolean = false,
        val error: String? = null
    )

    // Initialize wizard steps
    private val initialSteps = listOf(
        WizardStep(
            stepNumber = 1,
            title = "Accessibility Service",
            description = "Enable real-time content detection across all apps",
            permissionType = "ACCESSIBILITY_SERVICE",
            isRequired = true
        ),
        WizardStep(
            stepNumber = 2,
            title = "Usage Stats Access",
            description = "Monitor app usage for enhanced blocking capabilities",
            permissionType = "PACKAGE_USAGE_STATS",
            isRequired = true
        ),
        WizardStep(
            stepNumber = 3,
            title = "Device Admin (Optional)",
            description = "Force-close blocked apps for stronger enforcement",
            permissionType = "DEVICE_ADMIN",
            isRequired = false
        ),
        WizardStep(
            stepNumber = 4,
            title = "Location Permission",
            description = "Enable accurate prayer times and Islamic calendar",
            permissionType = "LOCATION_PERMISSION",
            isRequired = false
        ),
        WizardStep(
            stepNumber = 5,
            title = "Islamic Features",
            description = "Set up prayer times, Islamic calendar, and spiritual features",
            permissionType = "ISLAMIC_FEATURES",
            isRequired = false,
            isCompleted = true
        )
    )

    // Current wizard state
    private val _wizardState = MutableStateFlow(
        WizardState(steps = initialSteps, isLoading = true)
    )
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    init {
        initializeWizardState()
        observePermissionChanges()
    }

    /**
     * Initialize wizard state by checking current permission statuses
     */
    private fun initializeWizardState() {
        viewModelScope.launch {
            try {
                permissionHelper.updatePermissionStatuses()

                val updatedSteps = initialSteps.map { step ->
                    val permissionResult = permissionHelper.permissionStatusFlow.value[step.permissionType]
                    step.copy(
                        status = when (permissionResult) {
                            is PermissionResult.Granted -> PermissionStatus.GRANTED
                            is PermissionResult.Denied -> PermissionStatus.DENIED
                            else -> PermissionStatus.PENDING
                        },
                        isCompleted = permissionResult is PermissionResult.Granted
                    )
                }

                val currentStepIndex = updatedSteps.indexOfFirst { !it.isCompleted }
                val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }

                _wizardState.value = WizardState(
                    currentStepIndex = if (currentStepIndex == -1) 0 else currentStepIndex,
                    steps = updatedSteps,
                    isLoading = false,
                    isComplete = isComplete,
                    canProceed = updatedSteps.getOrNull(currentStepIndex)?.isCompleted == true ||
                               !(updatedSteps.getOrNull(currentStepIndex)?.isRequired ?: true)
                )
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize wizard: ${e.message}"
                )
            }
        }
    }

    /**
     * Observe permission status changes and update wizard state accordingly
     */
    private fun observePermissionChanges() {
        viewModelScope.launch {
            permissionHelper.permissionStatusFlow
                .combine(settingsRepository.settings) { permissions, settings ->
                    permissions to settings
                }
                .collect { (permissions, settings) ->
                    val updatedSteps = _wizardState.value.steps.map { step ->
                        val permissionResult = permissions[step.permissionType]
                        step.copy(
                            status = when (permissionResult) {
                                is PermissionResult.Granted -> PermissionStatus.GRANTED
                                is PermissionResult.Denied -> PermissionStatus.DENIED
                                else -> PermissionStatus.PENDING
                            },
                            isCompleted = permissionResult is PermissionResult.Granted
                        )
                    }

                    val currentStepIndex = _wizardState.value.currentStepIndex
                    val currentStep = updatedSteps.getOrNull(currentStepIndex)

                    // Auto-advance if current step is completed
                    val newStepIndex = if (currentStep?.isCompleted == true &&
                                          currentStepIndex < updatedSteps.size - 1) {
                        currentStepIndex + 1
                    } else {
                        currentStepIndex
                    }

                    val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }
                    val canProceed = currentStep?.isCompleted == true ||
                                   !(currentStep?.isRequired ?: true)

                    _wizardState.value = _wizardState.value.copy(
                        currentStepIndex = newStepIndex,
                        steps = updatedSteps,
                        isComplete = isComplete,
                        canProceed = canProceed,
                        error = null
                    )
                }
        }
    }

    /**
     * Get the current wizard step
     */
    fun getCurrentStep(): WizardStep? {
        return _wizardState.value.steps.getOrNull(_wizardState.value.currentStepIndex)
    }

    /**
     * Proceed to the next step
     */
    fun proceedToNextStep() {
        val currentState = _wizardState.value
        if (currentState.currentStepIndex < currentState.steps.size - 1) {
            _wizardState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex + 1
            )
        }
    }

    /**
     * Go to the previous step
     */
    fun goToPreviousStep() {
        val currentState = _wizardState.value
        if (currentState.currentStepIndex > 0) {
            _wizardState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex - 1
            )
        }
    }

    /**
     * Request permission for the current step
     */
    fun requestCurrentPermission(activity: Activity) {
        val currentStep = getCurrentStep() ?: return

        // Update step status to requesting
        updateStepStatus(currentStep.permissionType, PermissionStatus.REQUESTING)

        when (currentStep.permissionType) {
            "ACCESSIBILITY_SERVICE" -> {
                permissionHelper.requestAccessibilityService(activity)
            }
            "PACKAGE_USAGE_STATS" -> {
                permissionHelper.requestUsageStatsPermission(activity)
            }
            "DEVICE_ADMIN" -> {
                permissionHelper.requestDeviceAdminPermission(activity)
            }
            "LOCATION_PERMISSION" -> {
                // Open location settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Complete the wizard and mark onboarding as completed
     */
    fun completeWizard() {
        viewModelScope.launch {
            try {
                settingsRepository.markOnboardingCompleted()
                _wizardState.value = _wizardState.value.copy(
                    isComplete = true,
                    error = null
                )
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    error = "Failed to complete wizard: ${e.message}"
                )
            }
        }
    }

    /**
     * Skip optional permissions
     */
    fun skipOptionalPermissions() {
        viewModelScope.launch {
            try {
                settingsRepository.updateSettings(
                    settingsRepository.getCurrentSettings().copy(
                        skipOptionalPermissions = true
                    )
                )
                _wizardState.value = _wizardState.value.copy(
                    isComplete = true,
                    error = null
                )
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    error = "Failed to skip optional permissions: ${e.message}"
                )
            }
        }
    }



    /**
     * Update step status
     */
    private fun updateStepStatus(permissionType: String, status: PermissionStatus) {
        val updatedSteps = _wizardState.value.steps.map { step ->
            if (step.permissionType == permissionType) {
                step.copy(status = status)
            } else {
                step
            }
        }
        _wizardState.value = _wizardState.value.copy(steps = updatedSteps)
    }

    /**
     * Check if wizard should be shown
     */
    fun shouldShowWizard(): Flow<Boolean> {
        return settingsRepository.settings.map { settings ->
            !settings.onboardingCompleted ||
            !permissionHelper.getEnhancedBlockingPermissionStatus().isComplete
        }
    }

    /**
     * Refresh permission statuses
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            try {
                permissionHelper.updatePermissionStatuses()
                initializeWizardState()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    /**
     * Reset wizard state (for testing/debugging)
     */
    fun resetWizard() {
        viewModelScope.launch {
            try {
                settingsRepository.updateSettings(
                    settingsRepository.getCurrentSettings().copy(
                        onboardingCompleted = false,
                        skipOptionalPermissions = false
                    )
                )
                initializeWizardState()
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    error = "Failed to reset wizard: ${e.message}"
                )
            }
        }
    }
}
