package com.hieltech.haramblur.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import androidx.compose.runtime.DisposableEffect
import com.hieltech.haramblur.ui.components.*
import com.hieltech.haramblur.ui.components.IslamicOnboardingStep
import com.hieltech.haramblur.ui.components.LanguageSelectionStep
import com.hieltech.haramblur.ui.components.GenderSelectionStep
import com.hieltech.haramblur.ui.components.SimpleLocationPermissionStep
import com.hieltech.haramblur.ui.components.SimpleNotificationPermissionStep
import com.hieltech.haramblur.ui.components.SimplifiedWizardStepCard
import android.util.Log
import kotlinx.coroutines.launch

/**
 * Main permission wizard screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionWizardScreen(
    onComplete: () -> Unit = {},
    viewModel: PermissionWizardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val wizardState by viewModel.wizardState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = wizardState.currentStepIndex,
        pageCount = { wizardState.steps.size }
    )
    val scope = rememberCoroutineScope()

    // Handle back navigation - prevent skipping required steps
    BackHandler(enabled = wizardState.currentStepIndex > 0) {
        viewModel.goToPreviousStep()
    }

    // Sync pager with wizard state
    LaunchedEffect(wizardState.currentStepIndex) {
        if (wizardState.currentStepIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(wizardState.currentStepIndex)
        }
    }

    // Sync wizard state with pager
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != wizardState.currentStepIndex) {
            // Only allow forward navigation if current step is completed
            val currentStep = wizardState.steps.getOrNull(wizardState.currentStepIndex)
            if (currentStep?.isCompleted == true || pagerState.currentPage < wizardState.currentStepIndex) {
                // This is a bit complex - for now, let the viewModel handle navigation
            }
        }
    }

    // Check permissions when returning from settings
    DisposableEffect(Unit) {
        onDispose {
            // Refresh permissions when screen is disposed (returning from settings)
            viewModel.refreshPermissions()
        }
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                onOpenDrawer = { /* No drawer in wizard */ },
                onNavigateToSettings = { /* No settings access during wizard */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (wizardState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.checking_permissions),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (wizardState.isComplete) {
            // Completion celebration screen
            CompletionScreen(
                grantedPermissions = wizardState.steps.filter { it.isCompleted },
                onContinue = onComplete,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Main wizard interface - simplified layout without header
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compact header with title and progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.welcome_to_haramblur_setup),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress indicator
                    WizardProgressIndicator(
                        currentStepIndex = wizardState.currentStepIndex,
                        totalSteps = wizardState.steps.size
                    )
                }

                // Step pager - takes all available space
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = false // Disable manual scrolling
                ) { page ->
                    val step = wizardState.steps.getOrNull(page)
                    step?.let {
                        SimplifiedStepPage(
                            step = it,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Navigation buttons at bottom
                WizardNavigationButtons(
                    currentStepIndex = wizardState.currentStepIndex,
                    totalSteps = wizardState.steps.size,
                    canProceed = wizardState.canProceed,
                    isOptionalStep = wizardState.steps.getOrNull(wizardState.currentStepIndex)?.isRequired == false,
                    isLoading = wizardState.isLoading,
                    onPreviousClick = {
                        viewModel.goToPreviousStep()
                    },
                    onNextClick = {
                        viewModel.proceedToNextStep()
                    },
                    onSkipClick = if (wizardState.steps.getOrNull(wizardState.currentStepIndex)?.isRequired == false) {
                        {
                            viewModel.skipOptionalPermissions()
                            onComplete()
                        }
                    } else null,
                    onCompleteClick = {
                        viewModel.completeWizard()
                        onComplete()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                // Error display with refresh option
                if (wizardState.error != null) {
                    AnimatedFadeIn(visible = true, durationMillis = 400) {
                        ModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            gradientColors = listOf(
                                Color(0xFFF44336).copy(alpha = 0.1f),
                                Color(0xFFB71C1C).copy(alpha = 0.05f)
                            ),
                            contentPadding = responsiveCardPadding()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(responsiveSpacing())
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFF44336)
                                    )
                                    Text(
                                        text = wizardState.error ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFC62828),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Add refresh button for errors
                                OutlinedButton(
                                    onClick = { viewModel.refreshPermissions() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simplified individual step page - much cleaner and less overwhelming
 */
@Composable
private fun SimplifiedStepPage(
    step: PermissionWizardViewModel.WizardStep,
    viewModel: PermissionWizardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Handle different step types with clean, focused UI
        when (step.permissionType) {
            "LANGUAGE_SELECTION" -> {
                // Clean language selection - no confusing "grant permission" button
                LanguageSelectionStep(
                    viewModel = viewModel,
                    onLanguageSelected = {
                        // Language change is handled automatically
                    }
                )
            }
            "GENDER_SELECTION" -> {
                // Gender selection for Islamic-compliant content filtering
                GenderSelectionStep(
                    viewModel = viewModel,
                    onGenderSelected = {
                        // Gender-based blur settings are applied automatically
                    }
                )
            }
            "LOCATION_PERMISSION" -> {
                // Simple location permission request
                SimpleLocationPermissionStep(
                    viewModel = viewModel,
                    onPermissionResult = { granted ->
                        if (granted) {
                            viewModel.refreshCurrentStep()
                        }
                    }
                )
            }
            "NOTIFICATION_PERMISSION" -> {
                // Simple notification permission request
                SimpleNotificationPermissionStep(
                    viewModel = viewModel,
                    onPermissionResult = { granted ->
                        if (granted) {
                            viewModel.refreshCurrentStep()
                        }
                    }
                )
            }
            "ISLAMIC_FEATURES" -> {
                // Islamic features configuration step
                IslamicOnboardingStep(
                    onNext = { 
                        viewModel.completeIslamicFeaturesConfiguration()
                    },
                    onSkip = { 
                        viewModel.completeIslamicFeaturesConfiguration()
                    }
                )
            }
            else -> {
                // For complex permissions (Accessibility, Usage Stats, etc.) - use simplified card
                SimplifiedWizardStepCard(
                    step = step,
                    onGrantClick = {
                        viewModel.requestCurrentPermission(context as android.app.Activity)
                    },
                    onSkipClick = if (!step.isRequired) {
                        { viewModel.skipOptionalPermissions() }
                    } else null,
                    showGrantButton = true
                )
            }
        }
    }
}

/**
 * Completion celebration screen
 */
@Composable
private fun CompletionScreen(
    grantedPermissions: List<PermissionWizardViewModel.WizardStep>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .then(responsiveContentPadding().let { padding ->
                    Modifier.padding(padding)
                })
                .then(responsiveMaxContentWidth())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Celebration animation
        AnimatedPulse {
            Text(
                text = "🎉",
                fontSize = responsiveEmojiSize() * 1.2f
            )
        }

        Spacer(modifier = Modifier.height(responsiveSpacing(compact = 12.dp, medium = 16.dp, expanded = 20.dp)))

        Text(
            text = stringResource(R.string.setup_complete_title),
            fontSize = responsiveHeadlineSize() * 1.1f,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(responsiveSpacing(compact = 8.dp, medium = 12.dp, expanded = 16.dp)))

        Text(
            text = stringResource(R.string.haramlur_ready),
            fontSize = responsiveHeadlineSize() * 0.8f,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(responsiveSpacing(compact = 16.dp, medium = 20.dp, expanded = 24.dp)))

        // Granted permissions summary
        ModernCard(
            modifier = responsiveMaxContentWidth(),
            gradientColors = listOf(
                Color(0xFF4CAF50).copy(alpha = 0.1f),
                Color(0xFF2E7D32).copy(alpha = 0.05f)
            ),
            contentPadding = responsiveCardPadding()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 8.dp, medium = 10.dp, expanded = 12.dp))
            ) {
                    Text(
                        text = stringResource(R.string.granted_permissions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )

                grantedPermissions.forEach { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(responsiveSpacing())
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(responsiveSpacing(compact = 16.dp, medium = 20.dp, expanded = 24.dp)))

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = responsiveSpacing(compact = 16.dp, medium = 20.dp, expanded = 24.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 8.dp, medium = 10.dp, expanded = 12.dp))
            ) {
                Text(stringResource(R.string.continue_to_haramblur))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Continue"
                )
            }
        }
        }
    }
}

/**
 * Accessibility Service specific instructions
 */
@Composable
private fun AccessibilityServiceInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    ModernCard(
        modifier = responsiveMaxContentWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 10.dp, medium = 11.dp, expanded = 12.dp))
        ) {
            Text(
                text = stringResource(R.string.how_it_works),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.accessibility_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Step-by-step instructions
            Column(
                verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 6.dp, medium = 8.dp, expanded = 10.dp))
            ) {
                Text(
                    text = "📋 Steps to enable:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Tap 'Grant Permission' below to open Accessibility settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Find 'HaramBlur' in the list of services",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Tap on 'HaramBlur' and toggle the switch to ON",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. Confirm by tapping 'OK' in the dialog",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "5. Return to HaramBlur - the permission will be detected automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please complete the steps above in Settings, then return to HaramBlur",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Usage Stats specific instructions
 */
@Composable
private fun UsageStatsInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    ModernCard(
        modifier = responsiveMaxContentWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 10.dp, medium = 11.dp, expanded = 12.dp))
        ) {
            Text(
                text = "📊 Why needed:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Usage Stats permission enables enhanced app blocking by detecting when blocked applications are launched, allowing for faster response times.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Step-by-step instructions
            Column(
                verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 6.dp, medium = 8.dp, expanded = 10.dp))
            ) {
                Text(
                    text = "📋 Steps to enable:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Tap 'Grant Permission' below to open Usage Access settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Find 'HaramBlur' in the list of apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Tap on 'HaramBlur' and toggle 'Allow usage access' to ON",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. Return to HaramBlur - the permission will be detected automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please complete the steps above in Settings, then return to HaramBlur",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Device Admin specific instructions
 */
@Composable
private fun DeviceAdminInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    ModernCard(
        modifier = responsiveMaxContentWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 10.dp, medium = 11.dp, expanded = 12.dp))
        ) {
            Text(
                text = "🛡️ Enhanced Blocking:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Device Admin access enables stronger app blocking by allowing HaramBlur to force-close blocked applications when they are detected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "This permission is optional but recommended for maximum protection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Location permission specific instructions
 */
@Composable
private fun LocationPermissionInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    ModernCard(
        modifier = responsiveMaxContentWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 10.dp, medium = 11.dp, expanded = 12.dp))
        ) {
            Text(
                text = "📍 Why location access?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Location permission enables accurate Islamic prayer times, Qibla direction, and Islamic calendar calculations for your specific location. Without it, the app will use default settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "You can also manually select your city in the Islamic settings if you prefer not to share your location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please grant location permission in Settings to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

/**
 * Notification permission specific instructions
 */
@Composable
private fun NotificationPermissionInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    ModernCard(
        modifier = responsiveMaxContentWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 10.dp, medium = 11.dp, expanded = 12.dp))
        ) {
            Text(
                text = "🔔 Why notification access?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Notification permission enables the app to show dhikr reminders and Islamic alerts. This is essential for the spiritual features of HaramBlur to work properly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Without this permission, dhikr notifications cannot be displayed, limiting the app's spiritual functionality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please grant notification permission in Settings to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

/**
 * Pulse animation composable
 */
@Composable
fun AnimatedPulse(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
