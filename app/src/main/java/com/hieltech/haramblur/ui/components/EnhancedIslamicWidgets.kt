package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.prayer.HijriCalendar
import com.hieltech.haramblur.data.prayer.NextPrayerInfo
import com.hieltech.haramblur.data.prayer.PrayerData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicFeaturesContainer(
    modifier: Modifier = Modifier,
    title: String? = null,
    error: IslamicErrorState = IslamicErrorState.NoError,
    onRetry: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    IslamicFeaturesErrorBoundary(
        modifier = modifier,
        errorState = error,
        onRetry = onRetry,
        onOpenSettings = onOpenSettings
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun EnhancedPrayerTimesWidget(
    prayerData: PrayerData?,
    nextPrayer: NextPrayerInfo?,
    onLocationSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    error: IslamicErrorState = IslamicErrorState.NoError,
    onRetry: (() -> Unit)? = null
) {
    IslamicFeaturesErrorBoundary(errorState = error, onRetry = onRetry, onOpenSettings = onLocationSettingsClick) {
        if (loading && prayerData == null) {
            PrayerTimesLoadingSkeleton(modifier)
        } else {
            PrayerTimesWidget(
                prayerData = prayerData,
                nextPrayer = nextPrayer,
                onLocationSettingsClick = onLocationSettingsClick,
                modifier = modifier
            )
        }
    }
}

@Composable
fun EnhancedIslamicCalendarWidget(
    hijriDate: HijriCalendar?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    error: IslamicErrorState = IslamicErrorState.NoError,
    onRetry: (() -> Unit)? = null
) {
    IslamicFeaturesErrorBoundary(errorState = error, onRetry = onRetry) {
        if (loading && hijriDate == null) {
            IslamicCalendarLoadingSkeleton(modifier)
        } else {
            IslamicCalendarWidget(hijriDate = hijriDate, modifier = modifier)
        }
    }
}
