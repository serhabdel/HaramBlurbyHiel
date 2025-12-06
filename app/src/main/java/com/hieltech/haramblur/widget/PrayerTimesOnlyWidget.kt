package com.hieltech.haramblur.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hieltech.haramblur.MainActivity

/**
 * Prayer Times Only Widget (4x2)
 * Shows all 5 prayer times with next prayer highlighted
 */
class PrayerTimesOnlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesOnlyGlanceWidget()
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("PrayerTimesWidget", "onUpdate called for ${appWidgetIds.size} widgets")
    }
}

class PrayerTimesOnlyGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("PrayerTimesWidget", "provideGlance called for id: $id")
        provideContent {
            WidgetContent(context)
        }
    }
    
    @Composable
    private fun WidgetContent(context: Context) {
        val prayerTimes = WidgetDataProvider.getPrayerTimes(context)
        val nextPrayer = WidgetDataProvider.getNextPrayer(context)
        val hijriDate = WidgetDataProvider.getHijriDate(context)
        val locationName = WidgetDataProvider.getLocationName(context)
        
        val hasData = prayerTimes != null
        
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1B5E20)))
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                if (!hasData) {
                    // No data - show helpful message
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🕌",
                            style = TextStyle(fontSize = 28.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = "Tap to load prayer times",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "🕌",
                            style = TextStyle(fontSize = 20.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Prayer Times",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (locationName.isNotEmpty() && locationName != "Unknown") {
                                Text(
                                    text = "📍 $locationName",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        if (hijriDate.isNotEmpty()) {
                            Text(
                                text = hijriDate,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    // Prayer times grid - use dynamic next prayer calculation
                        val dynamicNextPrayer = WidgetDataProvider.getDynamicNextPrayer(context)
                        val nextPrayerName = dynamicNextPrayer?.name ?: nextPrayer?.name ?: getNextPrayerName(prayerTimes)
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Left column
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                PrayerTimeItem("Fajr", prayerTimes.Fajr, nextPrayerName == "Fajr")
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                PrayerTimeItem("Dhuhr", prayerTimes.Dhuhr, nextPrayerName == "Dhuhr")
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                PrayerTimeItem("Asr", prayerTimes.Asr, nextPrayerName == "Asr")
                            }
                            
                            Spacer(modifier = GlanceModifier.width(16.dp))
                            
                            // Right column
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                PrayerTimeItem("Maghrib", prayerTimes.Maghrib, nextPrayerName == "Maghrib")
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                PrayerTimeItem("Isha", prayerTimes.Isha, nextPrayerName == "Isha")
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                // Sunrise as bonus
                                PrayerTimeItem("Sunrise", prayerTimes.Sunrise, false, isSecondary = true)
                            }
                        }
                        
                        // Next prayer countdown - use dynamic calculation
                        if (dynamicNextPrayer != null) {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(Color(0xFFFFD700).copy(alpha = 0.2f)))
                                    .cornerRadius(8.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⏰ ${dynamicNextPrayer.name} ${dynamicNextPrayer.countdown}",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFFFD700)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun PrayerTimeItem(
        name: String,
        time: String,
        isNext: Boolean,
        isSecondary: Boolean = false
    ) {
        val bgColor = when {
            isNext -> Color(0xFFFFD700).copy(alpha = 0.3f)
            else -> Color.Transparent
        }
        val textColor = when {
            isSecondary -> Color.White.copy(alpha = 0.5f)
            isNext -> Color(0xFFFFD700)
            else -> Color.White
        }
        
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(bgColor))
                .cornerRadius(6.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 12.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = time.take(5),
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 12.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
    
    private fun getNextPrayerName(prayerTimes: com.hieltech.haramblur.data.prayer.PrayerTimings): String {
        val now = java.util.Calendar.getInstance()
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        
        val prayers = listOf(
            "Fajr" to parseTimeToMinutes(prayerTimes.Fajr),
            "Dhuhr" to parseTimeToMinutes(prayerTimes.Dhuhr),
            "Asr" to parseTimeToMinutes(prayerTimes.Asr),
            "Maghrib" to parseTimeToMinutes(prayerTimes.Maghrib),
            "Isha" to parseTimeToMinutes(prayerTimes.Isha)
        )
        
        for ((name, minutes) in prayers) {
            if (minutes > currentMinutes) return name
        }
        
        return "Fajr"
    }
    
    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.take(5).split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }
}
