package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.services.DhikrManager
import com.hieltech.haramblur.services.DhikrSystemStatus
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R

/**
 * ViewModel for DhikrStatusCard to properly handle DhikrManager injection
 */
@HiltViewModel
class DhikrStatusCardViewModel @Inject constructor(
    val dhikrManager: DhikrManager
) : ViewModel()

/**
 * Status card showing dhikr system information and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrStatusCard(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    viewModel: DhikrStatusCardViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var systemStatus by remember { mutableStateOf<DhikrSystemStatus?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Load status when card is shown
    LaunchedEffect(Unit) {
        isLoading = true
        systemStatus = viewModel.dhikrManager.getSystemStatus()
        isLoading = false
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dhikr_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.dhikr_today_count,
                            systemStatus?.dailyDhikrCount ?: 0
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status information
            systemStatus?.let { status ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Current status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.status_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val (statusText, statusColor) = when {
                            status.isOverlayVisible -> stringResource(R.string.status_displaying) to MaterialTheme.colorScheme.primary
                            status.timeUntilNextDhikr <= 0 -> stringResource(R.string.status_ready) to MaterialTheme.colorScheme.primary
                            else -> {
                                val minutes = status.timeUntilNextDhikr / 1000 / 60
                                stringResource(R.string.status_next_in_minutes, minutes) to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Time window
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.time_window_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(status.currentTimeWindow.displayNameResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Display method
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.display_method_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val methodText = when (status.recommendedDisplayMethod) {
                            com.hieltech.haramblur.utils.DhikrDisplayMethod.OVERLAY -> stringResource(R.string.display_method_overlay)
                            com.hieltech.haramblur.utils.DhikrDisplayMethod.NOTIFICATION -> stringResource(R.string.display_method_notification)
                            com.hieltech.haramblur.utils.DhikrDisplayMethod.NONE -> stringResource(R.string.display_method_none)
                        }
                        Text(
                            text = methodText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (status.recommendedDisplayMethod ==
                                       com.hieltech.haramblur.utils.DhikrDisplayMethod.NONE)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick action button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel.dhikrManager.forceShowDhikrNow()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = status.canShowDhikr
                ) {
                    Text(stringResource(R.string.show_dhikr_now))
                }

                // Status description
                Text(
                    text = status.statusDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}