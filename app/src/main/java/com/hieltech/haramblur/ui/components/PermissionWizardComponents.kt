package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.ui.PermissionHelper
import com.hieltech.haramblur.ui.PermissionWizardViewModel

/**
 * Simplified wizard step card component - much cleaner and less overwhelming
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedWizardStepCard(
    step: PermissionWizardViewModel.WizardStep,
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    showGrantButton: Boolean = true
) {

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = when (step.status) {
            PermissionWizardViewModel.PermissionStatus.GRANTED -> listOf(
                Color(0xFF4CAF50).copy(alpha = 0.1f),
                Color(0xFF2E7D32).copy(alpha = 0.05f)
            )
            PermissionWizardViewModel.PermissionStatus.DENIED -> listOf(
                Color(0xFFF44336).copy(alpha = 0.1f),
                Color(0xFFC62828).copy(alpha = 0.05f)
            )
            else -> null
        },
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simple header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status icon
                val (icon, iconColor) = when (step.status) {
                    PermissionWizardViewModel.PermissionStatus.GRANTED -> 
                        Icons.Default.CheckCircle to Color(0xFF4CAF50)
                    PermissionWizardViewModel.PermissionStatus.DENIED -> 
                        Icons.Default.Warning to Color(0xFFF44336)
                    else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Simple action button
            if (showGrantButton && step.status != PermissionWizardViewModel.PermissionStatus.GRANTED) {
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = step.status != PermissionWizardViewModel.PermissionStatus.REQUESTING
                ) {
                    if (step.status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Requesting...")
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Permission")
                    }
                }
            }
            
            // Optional skip button
            if (!step.isRequired && onSkipClick != null && 
                step.status != PermissionWizardViewModel.PermissionStatus.GRANTED) {
                TextButton(
                    onClick = onSkipClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}

/**
 * Wizard progress indicator showing current step and overall progress
 */
@Composable
fun WizardProgressIndicator(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 6.dp, medium = 8.dp, expanded = 10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i == currentStepIndex
            val isCompleted = i < currentStepIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF4CAF50)
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .animateContentSize()
            )
        }
    }

    // Step counter
    Text(
        text = "Step ${currentStepIndex + 1} of $totalSteps",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = responsiveSpacing(compact = 6.dp, medium = 7.dp, expanded = 8.dp))
    )
}

/**
 * Permission status indicator with animated icon
 */
@Composable
fun PermissionStatusIndicator(
    status: PermissionWizardViewModel.PermissionStatus,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        PermissionWizardViewModel.PermissionStatus.GRANTED -> Icons.Default.CheckCircle
        PermissionWizardViewModel.PermissionStatus.DENIED -> Icons.Default.Warning
        PermissionWizardViewModel.PermissionStatus.REQUESTING -> null // Show progress indicator instead
        else -> Icons.Default.Info
    }

    val color = when (status) {
        PermissionWizardViewModel.PermissionStatus.GRANTED -> Color(0xFF4CAF50)
        PermissionWizardViewModel.PermissionStatus.DENIED -> Color(0xFFF44336)
        PermissionWizardViewModel.PermissionStatus.REQUESTING -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(responsiveIconSize() * 1.5f)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (status == PermissionWizardViewModel.PermissionStatus.REQUESTING) {
            CircularProgressIndicator(
                modifier = Modifier.size(responsiveIconSize() * 0.75f),
                strokeWidth = 2.dp,
                color = color
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(responsiveIconSize() * 0.75f)
                )
            }
        }
    }
}

/**
 * Wizard navigation buttons component
 */
@Composable
fun WizardNavigationButtons(
    currentStepIndex: Int,
    totalSteps: Int,
    canProceed: Boolean,
    isOptionalStep: Boolean,
    isLoading: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.then(responsiveMaxContentWidth()),
        verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
    ) {
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            // Previous button
            if (currentStepIndex > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Next/Complete button
            if (currentStepIndex == totalSteps - 1) {
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Complete Setup")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Complete"
                    )
                }
            } else {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.weight(1f),
                    enabled = canProceed && !isLoading
                ) {
                    Text("Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }

        // Skip button for optional steps
        if (isOptionalStep && onSkipClick != null && !canProceed) {
            TextButton(
                onClick = onSkipClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !isLoading
            ) {
                Text("Skip Optional Permission")
            }
        }
    }
}

/**
 * Permission denied help component
 */
@Composable
fun PermissionDeniedHelp(
    permissionType: String,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier.then(responsiveMaxContentWidth()),
        gradientColors = listOf(
            Color(0xFFFFF3E0),
            Color(0xFFFFE0B2).copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 8.dp, medium = 10.dp, expanded = 12.dp))
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Help",
                    tint = Color(0xFFFF9800)
                )
                Text(
                    text = "Need Help?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
            }

            when (permissionType) {
                "ACCESSIBILITY_SERVICE" -> {
                    Text(
                        text = "To enable Accessibility Service:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 3.dp, medium = 4.dp, expanded = 5.dp)),
                        modifier = Modifier.padding(start = responsiveSpacing())
                    ) {
                        SetupStep(number = "1", text = "Go to Settings > Accessibility")
                        SetupStep(number = "2", text = "Find 'HaramBlur' in the list")
                        SetupStep(number = "3", text = "Tap to open and toggle ON")
                        SetupStep(number = "4", text = "Confirm by tapping 'Allow'")
                    }
                }
                "PACKAGE_USAGE_STATS" -> {
                    Text(
                        text = "To grant Usage Access:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 3.dp, medium = 4.dp, expanded = 5.dp)),
                        modifier = Modifier.padding(start = responsiveSpacing())
                    ) {
                        SetupStep(number = "1", text = "Go to Settings > Apps > Special access")
                        SetupStep(number = "2", text = "Tap 'Usage access'")
                        SetupStep(number = "3", text = "Find 'HaramBlur' and tap it")
                        SetupStep(number = "4", text = "Toggle ON and confirm")
                    }
                }
                "DEVICE_ADMIN" -> {
                    Text(
                        text = "To enable Device Admin:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 3.dp, medium = 4.dp, expanded = 5.dp)),
                        modifier = Modifier.padding(start = responsiveSpacing())
                    ) {
                        SetupStep(number = "1", text = "Find 'Device admin apps' in Settings")
                        SetupStep(number = "2", text = "Tap 'HaramBlur' to activate")
                        SetupStep(number = "3", text = "Review permissions and activate")
                    }
                }
            }
        }
    }
}

/**
 * Permission explanation section with expandable details
 */
@Composable
fun PermissionExplanationSection(
    permissionType: String,
    permissionHelper: PermissionHelper,
    modifier: Modifier = Modifier
) {
    val explanation = permissionHelper.getPermissionExplanation(permissionType)
    var expanded by remember { mutableStateOf(false) }

    ModernCard(
        modifier = modifier.then(responsiveMaxContentWidth()),
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            // Header with expand/collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = explanation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = explanation.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expandable benefits section
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 6.dp, medium = 7.dp, expanded = 8.dp))
                ) {
                    Text(
                        text = "Benefits:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    explanation.benefits.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing(compact = 8.dp, medium = 10.dp, expanded = 12.dp))
                        ) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Location permission instructions
 */
@Composable
fun LocationPermissionInstructions(status: PermissionWizardViewModel.PermissionStatus) {
    when (status) {
        PermissionWizardViewModel.PermissionStatus.PENDING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Location access enables accurate prayer times and Islamic calendar for your specific city.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "🕌 Prayer times vary by longitude - different cities have different prayer times",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "📅 Islamic calendar dates may differ by country due to moon sighting methods",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "🔒 Your location data stays on your device and is never shared",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        PermissionWizardViewModel.PermissionStatus.GRANTED -> {
            Text(
                text = "✅ Location access granted. Prayer times and Islamic calendar will be accurate for your location.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50)
            )
        }
        PermissionWizardViewModel.PermissionStatus.DENIED -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "❌ Location access denied. Using default location (Mecca) for prayer times.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF44336)
                )
                Text(
                    text = "You can grant location permission later in Settings > Islamic for accurate local prayer times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PermissionWizardViewModel.PermissionStatus.REQUESTING -> {
            Text(
                text = "Requesting location permission...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
