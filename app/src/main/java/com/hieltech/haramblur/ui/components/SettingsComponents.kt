package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hieltech.haramblur.data.*
import kotlinx.coroutines.launch

/**
 * Settings mode enum for simple/advanced views
 */
enum class SettingsMode(val displayName: String, val description: String) {
    SIMPLE("Simple", "Essential settings only"),
    ADVANCED("Advanced", "All settings with detailed controls"),
    CUSTOM("Custom", "Current configuration")
}

/**
 * Preset button with gradient background and selection states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetButton(
    name: String,
    description: String,
    icon: String,
    gradientColors: List<Color>,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors),
                    RoundedCornerShape(12.dp)
                )
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Expandable settings section with smooth animation
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpandableSettingsSection(
    title: String,
    description: String? = null,
    icon: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )

                // Title and description
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (badge != null) {
                            Badge(
                                content = { Text(badge) },
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expand/collapse icon
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * Settings mode toggle component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModeToggle(
    currentMode: SettingsMode,
    onModeChange: (SettingsMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = SettingsMode.values()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        modes.forEach { mode ->
            val isSelected = currentMode == mode

            FilterChip(
                selected = isSelected,
                onClick = { onModeChange(mode) },
                label = {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Settings search bar with real-time filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search settings...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        ),
        singleLine = true
    )
}

/**
 * Generic setting item that can render different types
 */
@Composable
fun SettingItem(
    item: SettingItem,
    currentValue: Any?,
    onValueChange: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    when (item.valueType) {
        SettingValueType.BOOLEAN -> {
            SwitchSetting(
                title = item.displayName,
                description = item.description,
                checked = currentValue as? Boolean ?: false,
                onCheckedChange = { onValueChange(it) }
            )
        }

        SettingValueType.FLOAT_SLIDER -> {
            SliderSetting(
                title = item.displayName,
                description = item.description,
                value = currentValue as? Float ?: item.defaultValue as? Float ?: 0f,
                range = (item.minValue as? Float ?: 0f)..(item.maxValue as? Float ?: 1f),
                onValueChange = { onValueChange(it) },
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }

        SettingValueType.ENUM_SINGLE -> {
            RadioButtonGroup(
                title = item.displayName,
                options = item.options?.map { it.first to it.second } ?: emptyList(),
                selectedIndex = item.options?.indexOfFirst {
                    it.first == currentValue?.toString()
                } ?: 0,
                onSelectionChange = { index ->
                    item.options?.get(index)?.let { onValueChange(it.first) }
                }
            )
        }

        else -> {
            Text(
                text = "${item.displayName}: ${currentValue ?: "Not set"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Preset confirmation dialog
 */
@Composable
fun PresetConfirmationDialog(
    preset: PresetData,
    settingsDiff: SettingsDiff,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit = onCancel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🛡️",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Apply Preset",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (settingsDiff.added.isNotEmpty() || settingsDiff.modified.isNotEmpty()) {
                    Text(
                        text = "Changes:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(settingsDiff.modified.entries.toList()) { (key, pair) ->
                            Text(
                                text = "• $key: ${pair.first} → ${pair.second}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Apply Preset")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Import/export card with file operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportCard(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        gradientColors = listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "📁 Import & Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Share your settings configuration or import presets from others",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }

                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import")
                }
            }
        }
    }
}

/**
 * Settings category header with icon and description
 */
@Composable
fun SettingsCategoryHeader(
    category: SettingsCategory,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = category.icon,
            style = MaterialTheme.typography.headlineMedium
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (badge != null) {
                    Badge(
                        content = { Text(badge) },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Quick toggle row for essential settings
 */
@Composable
fun QuickToggleRow(
    toggles: List<Pair<String, Pair<Boolean, (Boolean) -> Unit>>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        toggles.forEach { (label, pair) ->
            val (checked, onCheckedChange) = pair
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Settings diff component for showing changes
 */
@Composable
fun SettingsDiffComponent(
    settingsDiff: SettingsDiff,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (settingsDiff.modified.isNotEmpty()) {
            Text(
                text = "Modified Settings:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            settingsDiff.modified.forEach { (key, pair) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${pair.first} → ${pair.second}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (settingsDiff.added.isNotEmpty()) {
            Text(
                text = "Added Settings:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            settingsDiff.added.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "+ $value",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Reusable switch setting component
 */
@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Reusable slider setting component
 */
@Composable
fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Reusable radio button group component
 */
@Composable
fun RadioButtonGroup(
    title: String,
    options: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        options.forEachIndexed { index, (name, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectionChange(index) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedIndex == index,
                    onClick = { onSelectionChange(index) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
