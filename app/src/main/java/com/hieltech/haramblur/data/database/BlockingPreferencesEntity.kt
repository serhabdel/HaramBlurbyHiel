package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing user preferences for blocking behavior
 */
@Entity(tableName = "blocking_preferences")
data class BlockingPreferencesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "strict_blocking")
    val strictBlocking: Boolean = false, // close app immediately vs show warning

    @ColumnInfo(name = "allow_temporary_unblock")
    val allowTemporaryUnblock: Boolean = true,

    @ColumnInfo(name = "temporary_unblock_duration")
    val temporaryUnblockDuration: Int = 5, // minutes

    @ColumnInfo(name = "show_quranic_verses")
    val showQuranicVerses: Boolean = true,

    @ColumnInfo(name = "vibration_on_block")
    val vibrationOnBlock: Boolean = true,

    @ColumnInfo(name = "sound_on_block")
    val soundOnBlock: Boolean = false,

    @ColumnInfo(name = "show_blocking_notifications")
    val showBlockingNotifications: Boolean = true,

    @ColumnInfo(name = "auto_close_blocked_browser_tabs")
    val autoCloseBlockedBrowserTabs: Boolean = true,

    @ColumnInfo(name = "block_during_prayer_times")
    val blockDuringPrayerTimes: Boolean = false,

    @ColumnInfo(name = "prayer_time_blocking_minutes")
    val prayerTimeBlockingMinutes: Int = 30, // minutes before and after prayer

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
