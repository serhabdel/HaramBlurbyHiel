package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.data.prayer.CalendarDay
import java.util.*

/**
 * Compact month grid showing the current Hijri month aligned to Gregorian weekdays.
 */
@Composable
fun IslamicCalendarMonthGrid(
    monthDays: List<CalendarDay>,
    modifier: Modifier = Modifier
) {
    if (monthDays.isEmpty()) return

    val first = monthDays.first()
    val firstWeekdayIdx = weekdayIndex(first.date.gregorian.weekday.en)
    val today = Calendar.getInstance()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    Column(modifier = modifier) {
        // Header
        Text(
            text = "${first.date.hijri.month.en} ${first.date.hijri.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { wd ->
                Text(
                    text = wd,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val placeholders = List(firstWeekdayIdx) { null }
        val cells: List<CalendarDay?> = placeholders + monthDays

        // Build a non-scrollable grid to avoid nested scroll measurement issues
        val rows = cells.chunked(7)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(min = 160.dp)) {
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowItems.forEach { item ->
                        if (item == null) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {}
                        } else {
                            val gDay = item.date.gregorian.day.toIntOrNull()
                            val hDay = item.date.hijri.day
                            val isToday = gDay == todayDay && isSameMonth(item.date.gregorian, today)
                            Box(modifier = Modifier.weight(1f)) {
                                CalendarCell(gDay ?: 0, hDay, isToday, item.date.hijri.holidays?.isNotEmpty() == true)
                            }
                        }
                    }
                    // pad if last row shorter
                    repeat(7 - rowItems.size) { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {} }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(gDay: Int, hDay: String, isToday: Boolean, hasHoliday: Boolean) {
    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = gDay.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = hDay,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasHoliday) {
                        Box(
                            modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.tertiary, shape = MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

private fun weekdayIndex(name: String): Int {
    return when (name.lowercase(Locale.ROOT)) {
        "sunday" -> 0
        "monday" -> 1
        "tuesday" -> 2
        "wednesday" -> 3
        "thursday" -> 4
        "friday" -> 5
        "saturday" -> 6
        else -> 0
    }
}

private fun isSameMonth(gregorian: com.hieltech.haramblur.data.prayer.GregorianCalendar, cal: Calendar): Boolean {
    val month = gregorian.month.number
    val year = gregorian.year.toIntOrNull() ?: 0
    return year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH) + 1
}
