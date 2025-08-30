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

/**
 * City selector dialog for manual location entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectorDialog(
    currentCity: String? = null,
    currentCountry: String? = null,
    onCitySelected: (city: String, country: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Popular Islamic cities for quick selection
    val popularCities = listOf(
        "Mecca" to "Saudi Arabia",
        "Medina" to "Saudi Arabia",
        "Casablanca" to "Morocco",
        "Rabat" to "Morocco",
        "Marrakech" to "Morocco",
        "Cairo" to "Egypt",
        "Alexandria" to "Egypt",
        "Istanbul" to "Turkey",
        "Ankara" to "Turkey",
        "Dubai" to "UAE",
        "Abu Dhabi" to "UAE",
        "Kuala Lumpur" to "Malaysia",
        "Jakarta" to "Indonesia",
        "Karachi" to "Pakistan",
        "Lahore" to "Pakistan",
        "Dhaka" to "Bangladesh",
        "Tehran" to "Iran",
        "Baghdad" to "Iraq",
        "Amman" to "Jordan",
        "Beirut" to "Lebanon",
        "Damascus" to "Syria",
        "Tunis" to "Tunisia",
        "Algiers" to "Algeria",
        "Khartoum" to "Sudan",
        "Mogadishu" to "Somalia",
        "Doha" to "Qatar",
        "Kuwait City" to "Kuwait",
        "Muscat" to "Oman",
        "Bahrain" to "Bahrain"
    )

    val filteredCities = popularCities.filter { (city, country) ->
        searchQuery.isEmpty() ||
        city.contains(searchQuery, ignoreCase = true) ||
        country.contains(searchQuery, ignoreCase = true)
    }

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
                    onValueChange = { searchQuery = it },
                    label = { Text("Search cities") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // City list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCities) { (city, country) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCity = city to country
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCity?.first == city && selectedCity?.second == country)
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
                                        text = country,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (selectedCity?.first == city && selectedCity?.second == country) {
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
                            selectedCity?.let { (city, country) ->
                                onCitySelected(city, country)
                                onDismiss()
                            }
                        },
                        enabled = selectedCity != null
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
    onCitySelected: (city: String, country: String) -> Unit,
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
            currentCountry = selectedCountry,
            onCitySelected = onCitySelected,
            onDismiss = { showDialog = false }
        )
    }
}