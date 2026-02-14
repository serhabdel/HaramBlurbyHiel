package com.hieltech.haramblur.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R

/**
 * Glance Widget UI for Islamic Dashboard
 */
class IslamicDashboardGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    
    // Use stateDefinition for reactive updates
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("DashboardWidget", "provideGlance called for id=$id")
        provideContent {
            WidgetContent(context)
        }
    }
    
    @Composable
    private fun WidgetContent(context: Context) {
        // Read tasbih from Glance state for reactive updates
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val tasbihCount = prefs[intPreferencesKey("tasbih_count")] ?: WidgetDataProvider.getTasbihCount(context)
        val tasbihIndex = prefs[intPreferencesKey("tasbih_index")] ?: WidgetDataProvider.getTasbihIndex(context)
        
        Log.d("DashboardWidget", "Rendering with tasbih count=$tasbihCount")
        
        // Get other data from provider
        val prayerTimes = WidgetDataProvider.getPrayerTimes(context)
        val nextPrayer = WidgetDataProvider.getNextPrayer(context)
        val hijriDate = WidgetDataProvider.getHijriDate(context)
        val currentTasbih = WidgetDataProvider.tasbihTexts.getOrNull(tasbihIndex)
            ?: WidgetDataProvider.tasbihTexts[0]
        
        // Check if we have data - if not, show a helpful message
        val hasData = prayerTimes != null || hijriDate.isNotEmpty()
        
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1B5E20)))
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                if (!hasData) {
                    // No data yet - show tap to load message
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = context.getString(R.string.widget_mosque_icon),
                            style = TextStyle(fontSize = 32.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.widget_tap_to_open_app),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = context.getString(R.string.widget_prayer_times_sync),
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Header - Next Prayer calculated dynamically
                        val dynamicNextPrayer = WidgetDataProvider.getDynamicNextPrayer(context)
                        NextPrayerSectionDynamic(context, dynamicNextPrayer)
                        
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        // Hijri Date
                        if (hijriDate.isNotEmpty()) {
                            Text(
                                text = context.getString(R.string.widget_date_prefix) + " " + hijriDate,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                        
                        // Main content row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Tasbih Counter
                            TasbihCounterSection(
                                context = context,
                                currentTasbih = currentTasbih,
                                count = tasbihCount,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            
                            // Prayer Times List
                            if (prayerTimes != null) {
                                PrayerTimesSection(
                                    context = context,
                                    prayerTimes = prayerTimes,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun NextPrayerSectionDynamic(
        context: Context,
        dynamicNextPrayer: WidgetDataProvider.DynamicNextPrayer?
    ) {
        val displayName = dynamicNextPrayer?.name ?: context.getString(R.string.widget_prayer_times_fallback)
        val countdown = dynamicNextPrayer?.countdown ?: ""
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_mosque_icon),
                style = TextStyle(fontSize = 24.sp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = displayName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (countdown.isNotEmpty()) {
                    Text(
                        text = countdown,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFD700)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
    
    @Composable
    private fun NextPrayerSection(
        context: Context,
        nextPrayer: com.hieltech.haramblur.data.prayer.NextPrayerInfo?,
        prayerTimes: com.hieltech.haramblur.data.prayer.PrayerTimings?,
        dynamicCountdown: String = ""
    ) {
        val displayName = nextPrayer?.name ?: getNextPrayerName(prayerTimes)
        // Use dynamic countdown if available, otherwise fall back to stored value
        val timeUntil = dynamicCountdown.ifEmpty { nextPrayer?.timeUntil ?: "" }
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_mosque_icon),
                style = TextStyle(fontSize = 24.sp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = displayName.ifEmpty { "Prayer Times" },
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (timeUntil.isNotEmpty()) {
                    Text(
                        text = timeUntil,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFD700)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
    
    @Composable
    private fun TasbihCounterSection(
        context: Context,
        currentTasbih: WidgetDataProvider.TasbihData,
        count: Int,
        modifier: GlanceModifier
    ) {
        Box(
            modifier = modifier
                .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
                .cornerRadius(12.dp)
                .padding(8.dp)
                .clickable(actionSendBroadcast(
                    Intent(context, DhikrCounterWidgetReceiver::class.java).apply {
                        action = DhikrCounterWidgetReceiver.ACTION_INCREMENT
                    }
                )),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTasbih.arabic,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_counter_display, count),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFD700)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = context.getString(R.string.widget_tap_to_count),
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
    
    @Composable
    private fun PrayerTimesSection(
        context: Context,
        prayerTimes: com.hieltech.haramblur.data.prayer.PrayerTimings,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .background(ColorProvider(Color.White.copy(alpha = 0.1f)))
                .cornerRadius(12.dp)
                .padding(8.dp)
        ) {
            PrayerTimeRow(context.getString(R.string.widget_prayer_fajr), prayerTimes.Fajr)
            PrayerTimeRow(context.getString(R.string.widget_prayer_dhuhr), prayerTimes.Dhuhr)
            PrayerTimeRow(context.getString(R.string.widget_prayer_asr), prayerTimes.Asr)
            PrayerTimeRow(context.getString(R.string.widget_prayer_maghrib), prayerTimes.Maghrib)
            PrayerTimeRow(context.getString(R.string.widget_prayer_isha), prayerTimes.Isha)
        }
    }
    
    @Composable
    private fun PrayerTimeRow(name: String, time: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = time.take(5),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
    
    private fun getNextPrayerName(prayerTimes: com.hieltech.haramblur.data.prayer.PrayerTimings?): String {
        if (prayerTimes == null) return ""
        
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
        
        return "Fajr" // Next day
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

/**
 * Action callback for incrementing tasbih counter
 */
class IncrementTasbihAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        android.util.Log.d("TasbihAction", "Increment tasbih clicked")
        WidgetDataProvider.incrementTasbih(context)
        
        // Update the current widget immediately
        IslamicDashboardGlanceWidget().update(context, glanceId)
        
        // Update all Dhikr widgets using GlanceAppWidgetManager
        try {
            val manager = GlanceAppWidgetManager(context)
            val dhikrWidget = DhikrCounterGlanceWidget()
            val dhikrIds = manager.getGlanceIds(DhikrCounterGlanceWidget::class.java)
            android.util.Log.d("TasbihAction", "Updating ${dhikrIds.size} Dhikr widgets")
            dhikrIds.forEach { id ->
                dhikrWidget.update(context, id)
            }
        } catch (e: Exception) {
            android.util.Log.e("TasbihAction", "Error updating Dhikr widgets", e)
        }
    }
}

/**
 * Action callback for resetting tasbih counter
 */
class ResetTasbihAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        android.util.Log.d("TasbihAction", "Reset tasbih clicked")
        WidgetDataProvider.resetTasbih(context)
        
        // Update the current widget immediately
        IslamicDashboardGlanceWidget().update(context, glanceId)
        
        // Update all Dhikr widgets using GlanceAppWidgetManager
        try {
            val manager = GlanceAppWidgetManager(context)
            val dhikrWidget = DhikrCounterGlanceWidget()
            val dhikrIds = manager.getGlanceIds(DhikrCounterGlanceWidget::class.java)
            android.util.Log.d("TasbihAction", "Resetting ${dhikrIds.size} Dhikr widgets")
            dhikrIds.forEach { id ->
                dhikrWidget.update(context, id)
            }
        } catch (e: Exception) {
            android.util.Log.e("TasbihAction", "Error updating Dhikr widgets", e)
        }
    }
}
