package com.hieltech.haramblur.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.hieltech.haramblur.data.prayer.NextPrayerInfo
import com.hieltech.haramblur.data.prayer.PrayerData
import com.hieltech.haramblur.data.prayer.PrayerTimings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager for updating widget data from the main app
 * Call these methods when prayer times or other data changes
 */
object WidgetUpdateManager {
    
    /**
     * Update widgets with new prayer data
     */
    fun updatePrayerData(
        context: Context,
        prayerData: PrayerData?,
        nextPrayer: NextPrayerInfo?
    ) {
        prayerData?.let { data ->
            // Save prayer times
            WidgetDataProvider.savePrayerTimes(context, data.timings)
            
            // Save Hijri date
            val hijriDate = "${data.date.hijri.day} ${data.date.hijri.month.en} ${data.date.hijri.year}"
            WidgetDataProvider.saveHijriDate(context, hijriDate)
            
            // Save location if available
            data.meta.let { meta ->
                val locationName = meta.timezone.split("/").lastOrNull() ?: "Unknown"
                WidgetDataProvider.saveLocationName(context, locationName.replace("_", " "))
            }
        }
        
        nextPrayer?.let {
            WidgetDataProvider.saveNextPrayer(context, it)
        }
        
        // Update all widgets
        refreshAllWidgets(context)
    }
    
    /**
     * Update widgets with prayer timings only
     */
    fun updatePrayerTimings(context: Context, timings: PrayerTimings) {
        WidgetDataProvider.savePrayerTimes(context, timings)
        refreshAllWidgets(context)
    }
    
    /**
     * Update next prayer info
     */
    fun updateNextPrayer(context: Context, nextPrayer: NextPrayerInfo) {
        WidgetDataProvider.saveNextPrayer(context, nextPrayer)
        refreshAllWidgets(context)
    }
    
    /**
     * Update Hijri date
     */
    fun updateHijriDate(context: Context, hijriDate: String) {
        WidgetDataProvider.saveHijriDate(context, hijriDate)
        refreshAllWidgets(context)
    }
    
    /**
     * Update location name
     */
    fun updateLocationName(context: Context, locationName: String) {
        WidgetDataProvider.saveLocationName(context, locationName)
        refreshAllWidgets(context)
    }
    
    /**
     * Refresh all widgets using GlanceAppWidgetManager
     */
    fun refreshAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                
                // Update Islamic Dashboard widgets
                val dashboardWidget = IslamicDashboardGlanceWidget()
                val dashboardIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
                Log.d(TAG, "Updating ${dashboardIds.size} Islamic Dashboard widgets")
                dashboardIds.forEach { id ->
                    dashboardWidget.update(context, id)
                }
                
                // Update Prayer Times widgets
                val prayerWidget = PrayerTimesOnlyGlanceWidget()
                val prayerIds = manager.getGlanceIds(PrayerTimesOnlyGlanceWidget::class.java)
                Log.d(TAG, "Updating ${prayerIds.size} Prayer Times widgets")
                prayerIds.forEach { id ->
                    prayerWidget.update(context, id)
                }
                
                // Update Dhikr Counter widgets
                val dhikrWidget = DhikrCounterGlanceWidget()
                val dhikrIds = manager.getGlanceIds(DhikrCounterGlanceWidget::class.java)
                Log.d(TAG, "Updating ${dhikrIds.size} Dhikr Counter widgets")
                dhikrIds.forEach { id ->
                    dhikrWidget.update(context, id)
                }
                
                Log.d(TAG, "All widgets refreshed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing widgets", e)
            }
        }
    }
    
    private const val TAG = "WidgetUpdateManager"
}
