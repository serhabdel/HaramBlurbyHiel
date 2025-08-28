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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import com.hieltech.haramblur.ui.components.*
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
                        text = "Checking permissions...",
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
            // Main wizard interface
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(responsiveContentPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 16.dp, medium = 20.dp, expanded = 24.dp))
            ) {
                // Welcome header
                AnimatedFadeIn(visible = true, durationMillis = 600) {
                    ModernCard(
                        modifier = responsiveMaxContentWidth(),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                        ),
                        contentPadding = responsiveCardPadding()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 8.dp, medium = 10.dp, expanded = 12.dp))
                        ) {
                            Text(
                                text = "🔐",
                                fontSize = responsiveEmojiSize()
                            )
                            Text(
                                text = "Welcome to HaramBlur Setup",
                                fontSize = responsiveHeadlineSize(),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Let's set up the permissions needed for optimal content protection",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Progress indicator
                AnimatedFadeIn(visible = true, durationMillis = 800) {
                    WizardProgressIndicator(
                        currentStepIndex = wizardState.currentStepIndex,
                        totalSteps = wizardState.steps.size
                    )
                }

                // Step pager
                AnimatedFadeIn(visible = true, durationMillis = 1000) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .then(responsiveMaxContentWidth()),
                        userScrollEnabled = false // Disable manual scrolling
                    ) { page ->
                        val step = wizardState.steps.getOrNull(page)
                        step?.let {
                            StepPage(
                                step = it,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Navigation buttons
                AnimatedFadeIn(visible = true, durationMillis = 1200) {
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
                        }
                    )
                }

                // Error display
                wizardState.error?.let { error ->
                    AnimatedFadeIn(visible = true, durationMillis = 400) {
                        ModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            gradientColors = listOf(
                                Color(0xFFF44336).copy(alpha = 0.1f),
                                Color(0xFFB71C1C).copy(alpha = 0.05f)
                            ),
                            contentPadding = responsiveCardPadding()
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
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual step page composable
 */
@Composable
private fun StepPage(
    step: PermissionWizardViewModel.WizardStep,
    viewModel: PermissionWizardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionHelper = remember { PermissionHelper(context) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 16.dp, medium = 20.dp, expanded = 24.dp))
    ) {
        // Step card
        WizardStepCard(
            step = step,
            permissionHelper = permissionHelper,
            onGrantClick = {
                viewModel.requestCurrentPermission(context as android.app.Activity)
            },
            onSkipClick = if (!step.isRequired) {
                { viewModel.skipOptionalPermissions() }
            } else null
        )

        // Permission explanation
        PermissionExplanationSection(
            permissionType = step.permissionType,
            permissionHelper = permissionHelper
        )

        // Help section for denied permissions
        if (step.status == PermissionWizardViewModel.PermissionStatus.DENIED) {
            PermissionDeniedHelp(
                permissionType = step.permissionType
            )
        }

        // Status-specific instructions
        when (step.permissionType) {
            "ACCESSIBILITY_SERVICE" -> {
                AccessibilityServiceInstructions(step.status)
            }
            "PACKAGE_USAGE_STATS" -> {
                UsageStatsInstructions(step.status)
            }
            "DEVICE_ADMIN" -> {
                DeviceAdminInstructions(step.status)
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
            text = "Setup Complete!",
            fontSize = responsiveHeadlineSize() * 1.1f,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(responsiveSpacing(compact = 8.dp, medium = 12.dp, expanded = 16.dp)))

        Text(
            text = "HaramBlur is now ready to protect your digital space",
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
                    text = "✅ Granted Permissions",
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
                Text("Continue to HaramBlur")
                Icon(
                    Icons.Default.ArrowForward,
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
                text = "📱 How it works:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "The Accessibility Service allows HaramBlur to monitor content across all apps in real-time, enabling automatic blur detection and application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please complete the setup in Settings to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
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

            if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                Text(
                    text = "⚠️ Please grant the permission in Settings to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
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
