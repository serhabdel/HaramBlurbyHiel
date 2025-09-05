package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.R

/**
 * Error boundary for Islamic features area.
 * Keeps a simple, compile-safe implementation with retry and settings actions.
 */
sealed class IslamicErrorState {
    data object NoError : IslamicErrorState()
    data class LocationError(val message: String? = null) : IslamicErrorState()
    data class NetworkError(val message: String? = null) : IslamicErrorState()
    data class PermissionError(val message: String? = null) : IslamicErrorState()
    data class SensorError(val message: String? = null) : IslamicErrorState()
    data class ApiError(val message: String? = null) : IslamicErrorState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicFeaturesErrorBoundary(
    modifier: Modifier = Modifier,
    errorState: IslamicErrorState = IslamicErrorState.NoError,
    onRetry: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    headerIcon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    when (errorState) {
        is IslamicErrorState.NoError -> content()
        else -> {
            // Simple friendly error card
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Issue with Islamic features",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    val friendly = when (errorState) {
                        is IslamicErrorState.LocationError -> "Location issue detected. Please allow or refresh location."
                        is IslamicErrorState.NetworkError -> "Network issue detected. Check your connection and try again."
                        is IslamicErrorState.PermissionError -> "Required permissions are missing. Please grant them in settings."
                        is IslamicErrorState.SensorError -> "Device sensors unavailable or unreliable. Try recalibrating."
                        is IslamicErrorState.ApiError -> "Service temporarily unavailable. Please try again."
                        else -> "Something went wrong. Please try again."
                    }
                    Text(
                        text = friendly,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        onRetry?.let { action ->
                            Button(onClick = action) { Text("Retry") }
                        }
                        onOpenSettings?.let { action ->
                            Button(onClick = action) { Text("Settings") }
                        }
                        Button(onClick = { showDetails = !showDetails }) {
                            Text(if (showDetails) "Hide details" else "Show details")
                        }
                    }

                    if (showDetails) {
                        val details = when (errorState) {
                            is IslamicErrorState.LocationError -> errorState.message
                            is IslamicErrorState.NetworkError -> errorState.message
                            is IslamicErrorState.PermissionError -> errorState.message
                            is IslamicErrorState.SensorError -> errorState.message
                            is IslamicErrorState.ApiError -> errorState.message
                            else -> null
                        }
                        if (!details.isNullOrBlank()) {
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
