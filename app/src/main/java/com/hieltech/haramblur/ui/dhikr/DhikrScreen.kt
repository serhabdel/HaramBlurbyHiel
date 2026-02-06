package com.hieltech.haramblur.ui.dhikr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.*

// Islamic green colors
private val IslamicGreen = Color(0xFF1B5E20)
private val IslamicGreenLight = Color(0xFF4CAF50)
private val IslamicGold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrScreen(
    viewModel: DhikrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.dhikr_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                DhikrHeader(totalToday = uiState.totalDhikrToday)
            }

            // Tasbih Counter Section
            item {
                TasbihCounterSection(
                    counters = uiState.tasbihCounters,
                    currentIndex = uiState.currentTasbihIndex,
                    onIncrement = viewModel::incrementTasbih,
                    onReset = viewModel::resetTasbih,
                    onResetAll = viewModel::resetAllTasbih,
                    onSelectIndex = viewModel::setCurrentTasbihIndex,
                    hapticEnabled = uiState.hapticEnabled,
                    onToggleHaptic = viewModel::toggleHaptic
                )
            }

            // Category Selector
            item {
                CategorySelector(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory
                )
            }

            // Daily Progress Card
            item {
                DailyProgressCard(
                    progress = uiState.dailyProgress,
                    onMarkMorning = viewModel::markMorningCompleted,
                    onMarkEvening = viewModel::markEveningCompleted
                )
            }

            // Dhikr List
            item {
                Text(
                    text = stringResource(R.string.dhikr_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.currentDhikrList) { dhikr ->
                DhikrCard(
                    dhikr = dhikr,
                    showTransliteration = uiState.showTransliteration,
                    showTranslation = uiState.showTranslation
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DhikrHeader(totalToday: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = IslamicGreen
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🕌",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dhikr_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.dhikr_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = IslamicGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dhikr_today_count_full, totalToday),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TasbihCounterSection(
    counters: Map<String, TasbihCounter>,
    currentIndex: Int,
    onIncrement: (String) -> Unit,
    onReset: (String) -> Unit,
    onResetAll: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    hapticEnabled: Boolean,
    onToggleHaptic: () -> Unit
) {
    val tasbihList = DhikrDataSource.tasbihDhikr
    val currentTasbih = tasbihList.getOrNull(currentIndex) ?: return
    val counter = counters[currentTasbih.id] ?: TasbihCounter(dhikrId = currentTasbih.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with haptic toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tasbih_counter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggleHaptic) {
                    Icon(
                        imageVector = if (hapticEnabled) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = stringResource(R.string.toggle_haptic),
                        tint = if (hapticEnabled) IslamicGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tasbih selector tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tasbihList.forEachIndexed { index, dhikr ->
                    val isSelected = index == currentIndex
                    val tabCounter = counters[dhikr.id] ?: TasbihCounter(dhikrId = dhikr.id)
                    
                    TasbihTab(
                        arabicText = dhikr.arabicText,
                        count = tabCounter.currentCount,
                        isSelected = isSelected,
                        onClick = { onSelectIndex(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main counter button
            TasbihCounterButton(
                count = counter.currentCount,
                target = counter.targetCount,
                arabicText = currentTasbih.arabicText,
                transliteration = currentTasbih.transliteration,
                onClick = { onIncrement(currentTasbih.id) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { counter.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = IslamicGreen,
                trackColor = IslamicGreen.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.tasbih_progress, counter.currentCount, counter.targetCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasbih_sets_completed, counter.totalCompleted),
                    style = MaterialTheme.typography.bodySmall,
                    color = IslamicGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reset buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onReset(currentTasbih.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reset_current))
                }
                OutlinedButton(
                    onClick = onResetAll,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.reset_all))
                }
            }
        }
    }
}

@Composable
private fun TasbihTab(
    arabicText: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) IslamicGreen else Color.Transparent,
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = arabicText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count/33",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TasbihCounterButton(
    count: Int,
    target: Int,
    arabicText: String,
    transliteration: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IslamicGreenLight,
                        IslamicGreen
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = Color.White),
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = arabicText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transliteration,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = IslamicGold
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: DhikrCategory,
    onCategorySelected: (DhikrCategory) -> Unit
) {
    val categories = listOf(
        DhikrCategory.AFTER_PRAYER,
        DhikrCategory.TASBIH,
        DhikrCategory.MORNING_REMEMBRANCE,
        DhikrCategory.EVENING_REMEMBRANCE,
        DhikrCategory.GENERAL,
        DhikrCategory.DUA
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayNameEn) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IslamicGreen,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun DailyProgressCard(
    progress: DailyDhikrProgress,
    onMarkMorning: () -> Unit,
    onMarkEvening: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.daily_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressItem(
                    icon = "🌅",
                    label = stringResource(R.string.morning_adhkar),
                    isCompleted = progress.morningCompleted,
                    onClick = onMarkMorning
                )
                ProgressItem(
                    icon = "🌙",
                    label = stringResource(R.string.evening_adhkar),
                    isCompleted = progress.eveningCompleted,
                    onClick = onMarkEvening
                )
                ProgressItem(
                    icon = "📿",
                    label = stringResource(R.string.tasbih_sets_label),
                    count = progress.tasbihSets,
                    isCompleted = progress.tasbihSets >= 5
                )
            }
        }
    }
}

@Composable
private fun ProgressItem(
    icon: String,
    label: String,
    isCompleted: Boolean = false,
    count: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) IslamicGreen.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (count != null) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) IslamicGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = icon, fontSize = 24.sp)
            }
            if (isCompleted && count == null) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = IslamicGreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DhikrCard(
    dhikr: Dhikr,
    showTransliteration: Boolean,
    showTranslation: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Arabic text
            Text(
                text = dhikr.arabicText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = IslamicGreen,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 36.sp
            )

            if (showTransliteration) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dhikr.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            if (showTranslation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dhikr.englishTranslation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Category chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IslamicGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    text = dhikr.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = IslamicGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
