package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Streak counter card showing consecutive days of protection
 */
@Composable
fun StreakCard(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    UnifiedCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            Color(0xFFFF6B35).copy(alpha = 0.15f),
            Color(0xFFFF8C42).copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Current streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (currentStreak > 0) flameScale else 1f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF6B35).copy(alpha = 0.3f),
                                    Color(0xFFFF6B35).copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (currentStreak > 0) Color(0xFFFF6B35) else Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentStreak > 0) Color(0xFFFF6B35) else Color.Gray
                )
                Text(
                    text = "Day Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Best streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$bestStreak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = "Best Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Achievement badge component
 */
@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    isNew: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isNew) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badge_scale"
    )

    UnifiedCard(
        modifier = modifier.scale(scale),
        backgroundColor = if (achievement.isUnlocked) {
            achievement.rarity.color.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        contentPadding = 16.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (achievement.isUnlocked) {
                            achievement.rarity.color.copy(alpha = 0.2f)
                        } else Color.Gray.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.emoji,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    color = if (achievement.isUnlocked) {
                        Color.Unspecified
                    } else Color.Gray.copy(alpha = 0.5f)
                )
            }

            // Badge name
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (achievement.isUnlocked) {
                    achievement.rarity.color
                } else Color.Gray,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Progress bar if not unlocked
            if (!achievement.isUnlocked && achievement.progress > 0) {
                LinearProgressIndicator(
                    progress = { achievement.progress.toFloat() / achievement.maxProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = achievement.rarity.color,
                    trackColor = achievement.rarity.color.copy(alpha = 0.2f)
                )
                Text(
                    text = "${achievement.progress}/${achievement.maxProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rarity indicator
            if (achievement.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = achievement.rarity.color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = achievement.rarity.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = achievement.rarity.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Daily progress summary card
 */
@Composable
fun DailyProgressCard(
    facesBlocked: Int,
    sitesBlocked: Int,
    dailyGoal: Int,
    modifier: Modifier = Modifier
) {
    val progress = (facesBlocked + sitesBlocked).coerceAtMost(dailyGoal).toFloat() / dailyGoal

    UnifiedCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Protection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (progress >= 1f) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🎯 Goal Reached!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${facesBlocked + sitesBlocked}/$dailyGoal protected today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(emoji = "👤", value = "$facesBlocked", label = "Faces")
                MiniStat(emoji = "🔞", value = "$sitesBlocked", label = "Sites")
                MiniStat(emoji = "⭐", value = "${(progress * 100).toInt()}%", label = "Progress")
            }
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Achievement data classes
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val rarity: AchievementRarity,
    val isUnlocked: Boolean,
    val progress: Int = 0,
    val maxProgress: Int = 1
)

enum class AchievementRarity(
    val displayName: String,
    val color: Color
) {
    COMMON("Common", Color(0xFF9E9E9E)),
    UNCOMMON("Uncommon", Color(0xFF4CAF50)),
    RARE("Rare", Color(0xFF2196F3)),
    EPIC("Epic", Color(0xFF9C27B0)),
    LEGENDARY("Legendary", Color(0xFFFFD700))
}

/**
 * Predefined achievements for HaramBlur
 */
object DefaultAchievements {
    val all = listOf(
        Achievement(
            id = "first_day",
            name = "First Step",
            description = "Complete your first day with HaramBlur",
            emoji = "🌟",
            rarity = AchievementRarity.COMMON,
            isUnlocked = false
        ),
        Achievement(
            id = "week_warrior",
            name = "Week Warrior",
            description = "Maintain protection for 7 days straight",
            emoji = "🔥",
            rarity = AchievementRarity.UNCOMMON,
            isUnlocked = false
        ),
        Achievement(
            id = "month_master",
            name = "Month Master",
            description = "Incredible! 30 days of protection",
            emoji = "🛡️",
            rarity = AchievementRarity.RARE,
            isUnlocked = false
        ),
        Achievement(
            id = "hundred_hero",
            name = "Centurion",
            description = "100 days of consistent protection",
            emoji = "👑",
            rarity = AchievementRarity.EPIC,
            isUnlocked = false
        ),
        Achievement(
            id = "block_master",
            name = "Guardian",
            description = "Block 1000 inappropriate items",
            emoji = "🚫",
            rarity = AchievementRarity.RARE,
            isUnlocked = false,
            progress = 0,
            maxProgress = 1000
        ),
        Achievement(
            id = "dhikr_devotee",
            name = "Dhikr Devotee",
            description = "Complete 100 tasbih sets",
            emoji = "📿",
            rarity = AchievementRarity.UNCOMMON,
            isUnlocked = false,
            progress = 0,
            maxProgress = 100
        ),
        Achievement(
            id = "prayer_punctual",
            name = "Salah Star",
            description = "Never miss a prayer time notification for a week",
            emoji = "🕌",
            rarity = AchievementRarity.RARE,
            isUnlocked = false
        )
    )
}
