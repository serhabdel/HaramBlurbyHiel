package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.ui.StatsViewModel



/**
 * Status card component with different status types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    status: StatusType,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    val backgroundColor = when (status) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        StatusType.ERROR -> MaterialTheme.colorScheme.errorContainer
        StatusType.INFO -> MaterialTheme.colorScheme.surfaceVariant
        StatusType.NORMAL -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (status) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        StatusType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        StatusType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusType.NORMAL -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(modifier = Modifier.size(48.dp)) {
                icon()
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}





/**
 * Metric chip for displaying key metrics
 */
@Composable
fun MetricChip(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}









/**
 * Individual stat card
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

