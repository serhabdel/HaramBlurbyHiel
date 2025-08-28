package com.hieltech.haramblur.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.detection.AppInfo
import com.hieltech.haramblur.utils.SocialMediaDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaAppCard(
    appInfo: AppInfo,
    isBlocked: Boolean,
    subcategory: SocialMediaDetector.SocialMediaSubcategory?,
    onBlockToggle: (Boolean) -> Unit,
    onTimeBasedBlock: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimeOptions by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon placeholder with category emoji
                Text(
                    text = getCategoryEmoji(subcategory),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appInfo.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = subcategory?.displayName ?: "Social Media",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBlocked) "BLOCKED" else "ALLOWED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Show social media features if available
                    val features = SocialMediaDetector.detectSocialMediaFeatures(appInfo.packageName)
                    if (features.hasMessaging || features.hasVideoSharing || features.hasStories) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (features.hasMessaging) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Messaging",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (features.hasVideoSharing) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Video Sharing",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (features.hasStories) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Stories",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Details button
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Details",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Block toggle
                    Switch(
                        checked = isBlocked,
                        onCheckedChange = onBlockToggle
                    )
                }
            }

            // Expandable details section
            AnimatedVisibility(visible = showDetails) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // App details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = appInfo.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Type",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (appInfo.isSystemApp) "System App" else "User App",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Social media specific features
                    val features = SocialMediaDetector.detectSocialMediaFeatures(appInfo.packageName)
                    if (features.hasMessaging || features.hasVideoSharing || features.hasStories ||
                        features.hasLiveStreaming || features.isProfessional || features.isGamingRelated) {

                        Text(
                            text = "Features:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (features.hasMessaging) {
                                FeatureChip("💬 Messaging")
                            }
                            if (features.hasVideoSharing) {
                                FeatureChip("🎬 Video Sharing")
                            }
                            if (features.hasStories) {
                                FeatureChip("📖 Stories")
                            }
                            if (features.hasLiveStreaming) {
                                FeatureChip("🔴 Live Streaming")
                            }
                            if (features.isProfessional) {
                                FeatureChip("💼 Professional")
                            }
                            if (features.isGamingRelated) {
                                FeatureChip("🎮 Gaming")
                            }
                        }
                    }

                    // Quick actions
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick Actions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(15) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("15 min")
                        }
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(120) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2 hours")
                        }
                        OutlinedButton(
                            onClick = { showTimeOptions = !showTimeOptions },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Custom")
                        }
                    }
                }
            }

            // Time-based blocking options (expanded)
            if (showTimeOptions) {
                AnimatedVisibility(visible = showTimeOptions) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Time-based blocking:",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTimeBasedBlock(5) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("5 min")
                            }
                            OutlinedButton(
                                onClick = { onTimeBasedBlock(30) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("30 min")
                            }
                            OutlinedButton(
                                onClick = { onTimeBasedBlock(60) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("1 hour")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTimeBasedBlock(240) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("4 hours")
                            }
                            OutlinedButton(
                                onClick = { onTimeBasedBlock(480) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("8 hours")
                            }
                            OutlinedButton(
                                onClick = { showTimeOptions = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Custom")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(feature: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun getCategoryEmoji(subcategory: SocialMediaDetector.SocialMediaSubcategory?): String {
    return when (subcategory) {
        SocialMediaDetector.SocialMediaSubcategory.PHOTO_SHARING -> "📸"
        SocialMediaDetector.SocialMediaSubcategory.VIDEO_PLATFORMS -> "🎬"
        SocialMediaDetector.SocialMediaSubcategory.MESSAGING -> "💬"
        SocialMediaDetector.SocialMediaSubcategory.PROFESSIONAL_NETWORKS -> "💼"
        SocialMediaDetector.SocialMediaSubcategory.FORUMS_DISCUSSIONS -> "📱"
        SocialMediaDetector.SocialMediaSubcategory.NEWS_FEEDS -> "📰"
        SocialMediaDetector.SocialMediaSubcategory.DATING -> "💕"
        SocialMediaDetector.SocialMediaSubcategory.GAMING_SOCIAL -> "🎮"
        null -> "📱"
    }
}
