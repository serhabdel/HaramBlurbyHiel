package com.hieltech.haramblur.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import com.hieltech.haramblur.ui.components.UnifiedCard
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * Streamlined onboarding with only 3 essential steps:
 * 1. Welcome + Language
 * 2. Essential Permissions (Accessibility, Usage Stats)
 * 3. Islamic Features Setup
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StreamlinedOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: PermissionWizardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val wizardState by viewModel.wizardState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3
    val scope = rememberCoroutineScope()

    // Prevent back button on first step
    BackHandler(enabled = currentStep > 0) {
        if (currentStep > 0) currentStep--
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            OnboardingProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Step content with animations
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(
                        viewModel = viewModel,
                        onContinue = { currentStep++ }
                    )
                    1 -> EssentialPermissionsStep(
                        viewModel = viewModel,
                        onContinue = { currentStep++ },
                        onBack = { currentStep-- }
                    )
                    2 -> IslamicFeaturesStep(
                        viewModel = viewModel,
                        onComplete = onComplete,
                        onBack = { currentStep-- }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = when {
                            isCompleted || isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Step ${currentStep + 1} of $totalSteps",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${((currentStep + 1) * 100 / totalSteps)}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Step 1: Welcome + Language Selection
 */
@Composable
private fun WelcomeStep(
    viewModel: PermissionWizardViewModel,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated logo
        val infiniteTransition = rememberInfiniteTransition(label = "logo")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_scale"
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2E7D32).copy(alpha = 0.2f),
                            Color(0xFF2E7D32).copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛡️",
                fontSize = MaterialTheme.typography.displayLarge.fontSize
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to HaramBlur",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your Islamic content filtering companion\nfor a safer digital experience",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Language selector (simplified)
        UnifiedCard(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select your language",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageChip(
                        emoji = "🇺🇸",
                        name = "English",
                        selected = true,
                        onClick = { /* Set English */ }
                    )
                    LanguageChip(
                        emoji = "🇸🇦",
                        name = "العربية",
                        selected = false,
                        onClick = { /* Set Arabic */ }
                    )
                    LanguageChip(
                        emoji = "🇫🇷",
                        name = "Français",
                        selected = false,
                        onClick = { /* Set French */ }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}

@Composable
private fun LanguageChip(
    emoji: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji)
                Text(name)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

/**
 * Step 2: Essential Permissions (combined into one visual screen)
 */
@Composable
private fun EssentialPermissionsStep(
    viewModel: PermissionWizardViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    var accessibilityGranted by remember { mutableStateOf(false) }
    var usageStatsGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enable Protection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HaramBlur needs these permissions to protect you",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Accessibility permission card
        PermissionCard(
            icon = Icons.Default.Settings,
            title = "Accessibility Service",
            description = "Required to detect and blur inappropriate content across all apps",
            isGranted = accessibilityGranted,
            onGrant = {
                viewModel.openAccessibilitySettings(context)
                accessibilityGranted = true // In real app, check actual permission
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Usage stats permission card
        PermissionCard(
            icon = Icons.Default.Warning,
            title = "Usage Access",
            description = "Helps detect when blocked apps are opened for faster response",
            isGranted = usageStatsGranted,
            isOptional = true,
            onGrant = {
                viewModel.openUsageAccessSettings(context)
                usageStatsGranted = true
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.weight(2f),
                enabled = accessibilityGranted // Only accessibility is required
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isOptional: Boolean = false,
    onGrant: () -> Unit
) {
    UnifiedCard(
        backgroundColor = if (isGranted) {
            Color(0xFF4CAF50).copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isGranted) {
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        } else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isOptional) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Optional",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Grant button
            if (!isGranted) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Enable")
                }
            }
        }
    }
}

/**
 * Step 3: Islamic Features Setup
 */
@Composable
private fun IslamicFeaturesStep(
    viewModel: PermissionWizardViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    var enableIslamicFeatures by remember { mutableStateOf(true) }
    var enableNotifications by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🕌 Islamic Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enhance your spiritual journey with these features",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Islamic features toggle card
        UnifiedCard(
            onClick = { enableIslamicFeatures = !enableIslamicFeatures }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🕌",
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }

                    Column {
                        Text(
                            text = "Enable Islamic Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Prayer times, Qibla, Dhikr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enableIslamicFeatures,
                    onCheckedChange = { enableIslamicFeatures = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feature preview cards (only show if enabled)
        AnimatedVisibility(visible = enableIslamicFeatures) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeaturePreviewItem(
                    emoji = "🕐",
                    title = "Accurate Prayer Times",
                    description = "Based on your location with multiple calculation methods"
                )
                FeaturePreviewItem(
                    emoji = "🧭",
                    title = "Qibla Direction",
                    description = "Find the direction to Mecca from anywhere"
                )
                FeaturePreviewItem(
                    emoji = "📿",
                    title = "Dhikr Reminders",
                    description = "Gentle reminders for daily remembrance"
                )
                
                // Notifications toggle
                UnifiedCard(
                    onClick = { enableNotifications = !enableNotifications }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Allow Notifications",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = enableNotifications,
                            onCheckedChange = { enableNotifications = it }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(2f)
            ) {
                Text("Complete Setup")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@Composable
private fun FeaturePreviewItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = emoji,
            fontSize = MaterialTheme.typography.titleLarge.fontSize
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
