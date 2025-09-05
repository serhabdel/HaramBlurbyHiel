package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.cities.CitySearchState
import com.hieltech.haramblur.data.cities.CitySelection
import com.hieltech.haramblur.data.cities.CitySearchResult
import com.hieltech.haramblur.data.cities.toSelection
import com.hieltech.haramblur.ui.cities.CitySelectorViewModel

/**
 * City selector dialog for manual location entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectorDialog(
    currentCity: String? = null,
    currentCountry: String? = null,
    onCitySelected: (selection: CitySelection) -> Unit,
    onDismiss: () -> Unit,
    viewModel: CitySelectorViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedResult by remember { mutableStateOf<CitySearchResult?>(null) }
    val state by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Select Your City",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onQueryChange(it)
                    },
                    label = { Text("Search cities") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    is CitySearchState.Empty -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Type at least ${com.hieltech.haramblur.data.cities.CitySearchConfig.MIN_QUERY_LENGTH} characters to search")
                        }
                    }
                    is CitySearchState.Loading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is CitySearchState.Error -> {
                        val msg = (state as CitySearchState.Error).message
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(msg)
                        }
                    }
                    is CitySearchState.Success -> {
                        val results = (state as CitySearchState.Success).results
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { item ->
                                val city = item.name
                                val country = item.country ?: ""
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedResult = item
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedResult?.name == city && selectedResult?.country == country)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = city,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (country.isNotBlank()) country else item.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (selectedResult?.name == city && selectedResult?.country == country) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            selectedResult?.let { result ->
                                onCitySelected(result.toSelection())
                                onDismiss()
                            }
                        },
                        enabled = selectedResult != null
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

/**
 * Simple city selector for settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelector(
    selectedCity: String? = null,
    selectedCountry: String? = null,
    onCitySelected: (selection: CitySelection) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Selected Location",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedCity ?: "No city selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = selectedCountry ?: "Tap to select location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedCountry != null)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "📍",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }

    if (showDialog) {
        CitySelectorDialog(
            currentCity = selectedCity,
            onCitySelected = onCitySelected,
            onDismiss = { showDialog = false }
        )
    }
}