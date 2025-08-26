package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a blocking schedule for apps or websites
 */
@Entity(
    tableName = "blocking_schedules",
    indices = [
        Index(value = ["app_package_name"]),
        Index(value = ["site_domain"]),
        Index(value = ["schedule_type"]),
        Index(value = ["is_active"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BlockedAppEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["app_package_name"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BlockedSiteEntity::class,
            parentColumns = ["domain_hash"],
            childColumns = ["site_domain"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BlockingScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "app_package_name")
    val appPackageName: String? = null, // null if this is for a website

    @ColumnInfo(name = "site_domain")
    val siteDomain: String? = null, // null if this is for an app

    @ColumnInfo(name = "schedule_type")
    val scheduleType: String, // 'duration', 'time_range', 'recurring'

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null, // for duration-based blocking

    @ColumnInfo(name = "start_hour")
    val startHour: Int? = null, // for time-range blocking (0-23)

    @ColumnInfo(name = "start_minute")
    val startMinute: Int? = null, // for time-range blocking (0-59)

    @ColumnInfo(name = "end_hour")
    val endHour: Int? = null, // for time-range blocking (0-23)

    @ColumnInfo(name = "end_minute")
    val endMinute: Int? = null, // for time-range blocking (0-59)

    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: String? = null, // JSON array of days (0-6, Sunday=0)

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "schedule_name")
    val scheduleName: String? = null, // user-friendly name for the schedule

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_applied_at")
    val lastAppliedAt: Long? = null,

    @ColumnInfo(name = "next_scheduled_at")
    val nextScheduledAt: Long? = null
)
