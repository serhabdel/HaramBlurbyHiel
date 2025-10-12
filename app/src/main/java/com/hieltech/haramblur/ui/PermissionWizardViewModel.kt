package com.hieltech.haramblur.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.QualityMode
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

    // Define the setup steps in order - language first, then permissions
    private fun createInitialSteps(): List<WizardStep> {
        return listOf(
            WizardStep(
                stepNumber = 0,
                title = context.getString(R.string.wizard_step_language_title),
                description = context.getString(R.string.wizard_step_language_description),
                permissionType = "LANGUAGE_SELECTION",
                isRequired = true
            ),
            WizardStep(
                stepNumber = 1,
                title = context.getString(R.string.wizard_step_gender_title),
                description = context.getString(R.string.wizard_step_gender_description),
                permissionType = "GENDER_SELECTION",
                isRequired = true
            ),
            WizardStep(
                stepNumber = 2,
                title = context.getString(R.string.wizard_step_accessibility_title),
                description = context.getString(R.string.wizard_step_accessibility_description),
                permissionType = "ACCESSIBILITY_SERVICE",
                isRequired = true
            ),
            WizardStep(
                stepNumber = 3,
                title = context.getString(R.string.wizard_step_usage_stats_title),
                description = context.getString(R.string.wizard_step_usage_stats_description),
                permissionType = "PACKAGE_USAGE_STATS",
                isRequired = true
            ),
            WizardStep(
                stepNumber = 4,
                title = context.getString(R.string.wizard_step_overlay_title),
                description = context.getString(R.string.wizard_step_overlay_description),
                permissionType = "OVERLAY_PERMISSION",
                isRequired = true
            ),
            WizardStep(
                stepNumber = 5,
                title = context.getString(R.string.wizard_step_location_title),
                description = context.getString(R.string.wizard_step_location_description),
                permissionType = "LOCATION_PERMISSION",
                isRequired = false
            ),
            WizardStep(
                stepNumber = 6,
                title = context.getString(R.string.wizard_step_notification_title),
                description = context.getString(R.string.wizard_step_notification_description),
                permissionType = "NOTIFICATION_PERMISSION",
                isRequired = false
            ),
            WizardStep(
                stepNumber = 7,
                title = context.getString(R.string.wizard_step_islamic_features_title),
                description = context.getString(R.string.wizard_step_islamic_features_description),
                permissionType = "ISLAMIC_FEATURES",
                isRequired = false // This is a configuration step, not a permission step
            )
        )
    }

    // Current wizard state
    private val _wizardState = MutableStateFlow(
        WizardState(steps = createInitialSteps(), isLoading = true)
    )
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    init {
        initializeWizardState()
        observePermissionChanges()
    }

    /**
     * Initialize wizard state by checking current permission statuses with retry logic
     */
    private fun initializeWizardState() {
        viewModelScope.launch {
            try {
                _wizardState.value = _wizardState.value.copy(isLoading = true, error = null)

                // Update permissions with retry for critical permissions
                permissionHelper.updatePermissionStatuses()

                val updatedSteps = createInitialSteps().map { step ->
                    // Handle configuration steps differently from permission steps
                    if (step.permissionType == "LANGUAGE_SELECTION") {
                        // Language selection is a configuration step
                        step.copy(
                            status = PermissionStatus.PENDING,
                            isCompleted = false // Will be marked complete when user selects language
                        )
                    } else if (step.permissionType == "GENDER_SELECTION") {
                        // Gender selection is a configuration step
                        step.copy(
                            status = PermissionStatus.PENDING,
                            isCompleted = false // Will be marked complete when user selects gender
                        )
                    } else if (step.permissionType == "ISLAMIC_FEATURES") {
                        // Islamic features is a configuration step, always show as pending until user completes it
                        step.copy(
                            status = PermissionStatus.PENDING,
                            isCompleted = false // Will be marked complete when user finishes configuration
                        )
                    } else {
                        // Use retry logic for critical permissions
                        val permissionResult = if (step.isRequired && step.permissionType == "ACCESSIBILITY_SERVICE") {
                            permissionHelper.retryPermissionCheck(step.permissionType, maxRetries = 3, delayMs = 1500)
                        } else {
                            permissionHelper.permissionStatusFlow.value[step.permissionType] ?: PermissionResult.Denied(step.permissionType)
                        }

                        step.copy(
                            status = when (permissionResult) {
                                is PermissionResult.Granted -> PermissionStatus.GRANTED
                                is PermissionResult.Denied -> PermissionStatus.DENIED
                                else -> PermissionStatus.PENDING
                            },
                            isCompleted = permissionResult is PermissionResult.Granted
                        )
                    }
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
            "OVERLAY_PERMISSION" -> {
                // Request overlay permission using PermissionHelper
                permissionHelper.requestOverlayPermission(activity)
            }
            "DEVICE_ADMIN" -> {
                permissionHelper.requestDeviceAdminPermission(activity)
            }
            "LOCATION_PERMISSION" -> {
                // Request location permission using PermissionHelper
                permissionHelper.requestLocationPermission(activity)
            }
            "NOTIFICATION_PERMISSION" -> {
                // Request notification permission using PermissionHelper
                permissionHelper.requestNotificationPermission(activity)
            }
        }
    }

    /**
     * Complete the wizard and mark onboarding as completed
     */
    fun completeWizard() {
        viewModelScope.launch {
            try {
                // Mark onboarding as completed
                settingsRepository.markOnboardingCompleted()
                
                // CRUCIAL: Ensure content detection is enabled after wizard completion with High Quality mode
                val currentSettings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(currentSettings.copy(
                    // Ensure detection is enabled with High Quality defaults
                    enableFaceDetection = true,
                    enableNSFWDetection = true,
                    enableRealTimeProcessing = true,
                    isServicePaused = false, // Make sure service is not paused
                    // Apply High Quality mode settings automatically using the enum values
                    qualityMode = QualityMode.HIGH_QUALITY,
                    detectionSensitivity = QualityMode.HIGH_QUALITY.detectionSensitivity,
                    processingSpeed = QualityMode.HIGH_QUALITY.processingSpeed,
                    blurIntensity = QualityMode.HIGH_QUALITY.blurIntensity,
                    maxProcessingTimeMs = QualityMode.HIGH_QUALITY.maxProcessingTimeMs,
                    frameSkipThreshold = QualityMode.HIGH_QUALITY.frameSkipThreshold,
                    imageDownscaleRatio = QualityMode.HIGH_QUALITY.imageDownscaleRatio,
                    enableGPUAcceleration = QualityMode.HIGH_QUALITY.enableGPUAcceleration,
                    // Use the optimized confidence thresholds from AppSettings defaults
                    nsfwConfidenceThreshold = 0.5f,
                    genderConfidenceThreshold = 0.4f
                ))
                
                // Update permission statuses one final time
                permissionHelper.updatePermissionStatuses()
                
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
     * Refresh permission statuses with enhanced error handling
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            try {
                _wizardState.value = _wizardState.value.copy(isLoading = true, error = null)

                // Force update all permissions
                permissionHelper.updatePermissionStatuses()

                // Re-initialize wizard state with fresh data
                val updatedSteps = _wizardState.value.steps.map { step ->
                    val permissionResult = if (step.isRequired && step.permissionType == "ACCESSIBILITY_SERVICE") {
                        // Extra retry for accessibility service
                        permissionHelper.retryPermissionCheck(step.permissionType, maxRetries = 2, delayMs = 1000)
                    } else {
                        permissionHelper.permissionStatusFlow.value[step.permissionType] ?: PermissionResult.Denied(step.permissionType)
                    }

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
                val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }
                val canProceed = currentStep?.isCompleted == true || !(currentStep?.isRequired ?: true)

                _wizardState.value = _wizardState.value.copy(
                    steps = updatedSteps,
                    isLoading = false,
                    isComplete = isComplete,
                    canProceed = canProceed,
                    error = null
                )
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh permissions: ${e.message}"
                )
            }
        }
    }

    /**
     * Force refresh current step permission status
     */
    fun refreshCurrentStep() {
        val currentStep = getCurrentStep() ?: return

        viewModelScope.launch {
            try {
                val permissionResult = permissionHelper.retryPermissionCheck(
                    currentStep.permissionType,
                    maxRetries = 3,
                    delayMs = 1500
                )

                val updatedStatus = when (permissionResult) {
                    is PermissionResult.Granted -> PermissionStatus.GRANTED
                    is PermissionResult.Denied -> PermissionStatus.DENIED
                    else -> PermissionStatus.PENDING
                }

                updateStepStatus(currentStep.permissionType, updatedStatus)

                // Update completion status
                val updatedSteps = _wizardState.value.steps.map { step ->
                    if (step.permissionType == currentStep.permissionType) {
                        step.copy(
                            status = updatedStatus,
                            isCompleted = permissionResult is PermissionResult.Granted
                        )
                    } else {
                        step
                    }
                }

                _wizardState.value = _wizardState.value.copy(steps = updatedSteps)
            } catch (e: Exception) {
                _wizardState.value = _wizardState.value.copy(
                    error = "Failed to refresh current step: ${e.message}"
                )
            }
        }
    }

    /**
     * Complete language selection step
     */
    fun completeLanguageSelection() {
        val currentState = _wizardState.value
        val updatedSteps = currentState.steps.map { step ->
            if (step.permissionType == "LANGUAGE_SELECTION") {
                step.copy(
                    status = PermissionStatus.GRANTED,
                    isCompleted = true
                )
            } else {
                step
            }
        }

        val currentStepIndex = updatedSteps.indexOfFirst { !it.isCompleted }
        val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }

        _wizardState.value = currentState.copy(
            currentStepIndex = if (currentStepIndex == -1) 0 else currentStepIndex,
            steps = updatedSteps,
            isComplete = isComplete,
            canProceed = true
        )

        android.util.Log.d("PermissionWizardViewModel", "✅ Language selection step completed successfully")
    }

    /**
     * Complete gender selection step
     */
    fun completeGenderSelection() {
        android.util.Log.d("PermissionWizardViewModel", "completeGenderSelection() called")

        // TRUST the persistence result instead of immediately re-reading from StateFlow
        // The gender has been successfully saved via persistGenderSyncWithResult()
        // StateFlow may lag behind SharedPreferences due to async updates

        android.util.Log.d("PermissionWizardViewModel", "✅ Trusting gender persistence success, marking step complete")

        val currentState = _wizardState.value
        val updatedSteps = currentState.steps.map { step ->
            if (step.permissionType == "GENDER_SELECTION") {
                step.copy(
                    status = PermissionStatus.GRANTED,
                    isCompleted = true
                )
            } else {
                step
            }
        }

        val currentStepIndex = updatedSteps.indexOfFirst { !it.isCompleted }
        val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }

        _wizardState.value = currentState.copy(
            currentStepIndex = if (currentStepIndex == -1) 0 else currentStepIndex,
            steps = updatedSteps,
            isComplete = isComplete,
            canProceed = true
        )

        android.util.Log.d("PermissionWizardViewModel", "✅ Gender selection step marked as complete")
        android.util.Log.d("PermissionWizardViewModel", "   Next step index: ${if (currentStepIndex == -1) 0 else currentStepIndex}")
        android.util.Log.d("PermissionWizardViewModel", "   Wizard complete: $isComplete")
    }

    /**
     * Complete Islamic features configuration step
     */
    fun completeIslamicFeaturesConfiguration() {
        val currentState = _wizardState.value
        val updatedSteps = currentState.steps.map { step ->
            if (step.permissionType == "ISLAMIC_FEATURES") {
                step.copy(
                    status = PermissionStatus.GRANTED,
                    isCompleted = true
                )
            } else {
                step
            }
        }

        val currentStepIndex = updatedSteps.indexOfFirst { !it.isCompleted }
        val isComplete = updatedSteps.all { !it.isRequired || it.isCompleted }

        _wizardState.value = currentState.copy(
            currentStepIndex = if (currentStepIndex == -1) 0 else currentStepIndex,
            steps = updatedSteps,
            isComplete = isComplete,
            canProceed = true
        )
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
