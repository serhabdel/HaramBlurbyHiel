package com.hieltech.haramblur.ui.hadith

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.R
import com.hieltech.haramblur.data.api.Hadith

// Islamic-themed color palette
private val HadithGreen = Color(0xFF1B5E20)
private val HadithGreenLight = Color(0xFF4CAF50)
private val HadithGold = Color(0xFFFFD700)
private val HadithGoldDark = Color(0xFFB8860B)
private val SahihGreen = Color(0xFF2E7D32)
private val HasanBlue = Color(0xFF1565C0)
private val DaifRed = Color(0xFFD32F2F)

@Composable
fun HadithScreen(
    viewModel: HadithViewModel = hiltViewModel(),
    onHadithClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            HadithHeaderCard(total = uiState.total)
        }

        // Hadith of the Day
        if (uiState.hadithOfDay != null) {
            item {
                HadithOfDayCard(
                    hadith = uiState.hadithOfDay!!,
                    onClick = {
                        // Find index in list, or navigate with -1 for standalone
                        val idx = uiState.hadiths.indexOfFirst {
                            it.hadithNumber == uiState.hadithOfDay!!.hadithNumber &&
                            it.bookSlug == uiState.hadithOfDay!!.bookSlug
                        }
                        onHadithClick(if (idx >= 0) idx else 0)
                    }
                )
            }
        } else if (uiState.isLoadingHadithOfDay) {
            item {
                HadithOfDayShimmer()
            }
        }

        // API Key Required State
        if (!uiState.hasApiKey) {
            item {
                ApiKeyRequiredCard()
            }
        } else {
            // Book Selector
            item {
                BookSelectorSection(
                    context = context,
                    selectedBook = uiState.selectedBook,
                    availableBooks = viewModel.availableBooks,
                    onBookSelected = { viewModel.selectBook(it) }
                )
            }

            // Loading State
            if (uiState.isLoading && uiState.hadiths.isEmpty()) {
                items(3) { index ->
                    HadithShimmerCard(index)
                }
            }

            // Error State
            uiState.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onRetry = { viewModel.retry() }
                    )
                }
            }

            // Hadiths List
            if (uiState.hadiths.isNotEmpty()) {
                itemsIndexed(uiState.hadiths) { index, hadith ->
                    AnimatedHadithCard(
                        hadith = hadith,
                        index = index,
                        onClick = { onHadithClick(index) }
                    )
                }

                // Load More
                if (uiState.currentPage < uiState.lastPage) {
                    item {
                        LoadMoreButton(
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.loadNextPage() }
                        )
                    }
                }
            }

            // Empty State
            if (uiState.hadiths.isEmpty() && !uiState.isLoading && uiState.error == null) {
                item {
                    EmptyHadithCard()
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HadithHeaderCard(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HadithGreen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "📖", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hadith_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.hadith_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stringResource(R.string.hadith_total_hadiths, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithOfDayCard(hadith: Hadith, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = HadithGoldDark.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HadithGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✨", fontSize = 24.sp)
                    Text(
                        text = stringResource(R.string.hadith_of_the_day),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HadithGoldDark
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HadithGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = hadith.bookName,
                        style = MaterialTheme.typography.labelSmall,
                        color = HadithGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Arabic text preview
            if (hadith.arabicText.isNotEmpty()) {
                Text(
                    text = hadith.arabicText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 30.sp
                    ),
                    color = HadithGreen,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // English text preview
            if (hadith.englishText.isNotEmpty()) {
                Text(
                    text = hadith.englishText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Read more hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hadith_read_more),
                    style = MaterialTheme.typography.labelMedium,
                    color = HadithGoldDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = HadithGoldDark
                )
            }
        }
    }
}

@Composable
private fun HadithOfDayShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "hodShimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hodShimmerAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HadithGoldDark.copy(alpha = alpha * 0.06f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.1f))
            )
        }
    }
}


@Composable
private fun ApiKeyRequiredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "🔑", fontSize = 48.sp)
            Text(
                text = stringResource(R.string.hadith_api_key_required),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.hadith_api_key_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookSelectorSection(
    context: Context,
    selectedBook: String?,
    availableBooks: List<HadithBook>,
    onBookSelected: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.hadith_select_book),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Books" chip
                item {
                    FilterChip(
                        selected = selectedBook == null,
                        onClick = { onBookSelected(null) },
                        label = {
                            Text(
                                stringResource(R.string.hadith_all_books),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            if (selectedBook == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HadithGreen.copy(alpha = 0.2f),
                            selectedLabelColor = HadithGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedBook == null,
                            selectedBorderColor = HadithGreen
                        )
                    )
                }

                items(availableBooks) { book ->
                    FilterChip(
                        selected = selectedBook == book.slug,
                        onClick = { onBookSelected(book.slug) },
                        label = {
                            Text(
                                book.getDisplayName(context),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            if (selectedBook == book.slug) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HadithGreen.copy(alpha = 0.2f),
                            selectedLabelColor = HadithGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedBook == book.slug,
                            selectedBorderColor = HadithGreen
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedHadithCard(hadith: Hadith, index: Int, onClick: () -> Unit = {}) {
    // Staggered entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.toLong() * 40)
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "offsetY"
    )

    val gradeColor = when {
        hadith.grade.contains("Sahih", ignoreCase = true) -> SahihGreen
        hadith.grade.contains("Hasan", ignoreCase = true) -> HasanBlue
        hadith.grade.contains("Da", ignoreCase = true) -> DaifRed
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha, translationY = offsetY)
            .animateContentSize()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: Hadith # + Book + Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hadith number badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HadithGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "#${hadith.hadithNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HadithGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Book name
                    if (hadith.bookName.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = hadith.bookName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Grade pill
                    if (hadith.grade.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = gradeColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = gradeColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = hadith.grade,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Arabic text
            if (hadith.arabicText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HadithGreen.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = hadith.arabicText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 32.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        color = HadithGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            // English text
            if (hadith.englishText.isNotEmpty()) {
                Text(
                    text = hadith.englishText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Narrator
            if (hadith.narrator.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hadith.narrator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithShimmerCard(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header region
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f))
                )
            }
            // Arabic region
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f))
            )
            // English region
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.1f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f))
            )
        }
    }
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "⚠️", fontSize = 40.sp)
            Text(
                text = stringResource(R.string.hadith_error),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = HadithGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hadith_retry))
            }
        }
    }
}

@Composable
private fun LoadMoreButton(isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = HadithGreen,
                strokeWidth = 3.dp
            )
        } else {
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HadithGreen.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = HadithGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.hadith_load_more),
                    color = HadithGreen
                )
            }
        }
    }
}

@Composable
private fun EmptyHadithCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "📖", fontSize = 48.sp)
            Text(
                text = stringResource(R.string.hadith_no_results),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
