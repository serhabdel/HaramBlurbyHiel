package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blocking preferences
 */
@Dao
interface BlockingPreferencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: BlockingPreferencesEntity): Long

    @Update
    suspend fun updatePreferences(preferences: BlockingPreferencesEntity)

    @Query("SELECT * FROM blocking_preferences LIMIT 1")
    suspend fun getPreferences(): BlockingPreferencesEntity?

    @Query("SELECT * FROM blocking_preferences LIMIT 1")
    fun getPreferencesFlow(): Flow<BlockingPreferencesEntity?>

    @Query("UPDATE blocking_preferences SET strict_blocking = :strict WHERE id = :id")
    suspend fun updateStrictBlocking(id: Long, strict: Boolean)

    @Query("UPDATE blocking_preferences SET allow_temporary_unblock = :allow WHERE id = :id")
    suspend fun updateTemporaryUnblock(id: Long, allow: Boolean)

    @Query("UPDATE blocking_preferences SET temporary_unblock_duration = :duration WHERE id = :id")
    suspend fun updateTemporaryUnblockDuration(id: Long, duration: Int)

    @Query("UPDATE blocking_preferences SET show_quranic_verses = :show WHERE id = :id")
    suspend fun updateQuranicVerses(id: Long, show: Boolean)

    @Query("UPDATE blocking_preferences SET vibration_on_block = :vibrate WHERE id = :id")
    suspend fun updateVibrationOnBlock(id: Long, vibrate: Boolean)

    @Query("UPDATE blocking_preferences SET sound_on_block = :sound WHERE id = :id")
    suspend fun updateSoundOnBlock(id: Long, sound: Boolean)

    @Query("UPDATE blocking_preferences SET show_blocking_notifications = :show WHERE id = :id")
    suspend fun updateBlockingNotifications(id: Long, show: Boolean)

    @Query("UPDATE blocking_preferences SET auto_close_blocked_browser_tabs = :autoClose WHERE id = :id")
    suspend fun updateAutoCloseBrowserTabs(id: Long, autoClose: Boolean)

    @Query("UPDATE blocking_preferences SET block_during_prayer_times = :block WHERE id = :id")
    suspend fun updateBlockDuringPrayerTimes(id: Long, block: Boolean)

    @Query("UPDATE blocking_preferences SET prayer_time_blocking_minutes = :minutes WHERE id = :id")
    suspend fun updatePrayerTimeBlockingMinutes(id: Long, minutes: Int)

    @Query("DELETE FROM blocking_preferences")
    suspend fun deleteAllPreferences()
}
