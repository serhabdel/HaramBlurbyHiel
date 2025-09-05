package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {

    @Query("""
        SELECT * FROM prayer_times 
        WHERE date_millis = :dateMillis 
          AND lat = :latitude AND lon = :longitude 
          AND method = :method
        LIMIT 1
    """)
    suspend fun getPrayerTimes(
        dateMillis: Long,
        latitude: Double,
        longitude: Double,
        method: String
    ): PrayerTimesEntity?

    @Query("""
        SELECT * FROM prayer_times 
        WHERE date_millis BETWEEN :start AND :end 
          AND lat = :latitude AND lon = :longitude 
          AND method = :method
        ORDER BY date_millis ASC
    """)
    suspend fun getPrayerTimesRange(
        start: Long,
        end: Long,
        latitude: Double,
        longitude: Double,
        method: String
    ): List<PrayerTimesEntity>

    @Query("""
        SELECT * FROM prayer_times 
        WHERE date_millis BETWEEN :start AND :end 
          AND lat = :latitude AND lon = :longitude 
          AND method = :method
        ORDER BY date_millis ASC
    """)
    fun getPrayerTimesRangeFlow(
        start: Long,
        end: Long,
        latitude: Double,
        longitude: Double,
        method: String
    ): Flow<List<PrayerTimesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrayerTimesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PrayerTimesEntity>): List<Long>

    @Query("DELETE FROM prayer_times WHERE date_millis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int

    @Query("DELETE FROM prayer_times")
    suspend fun clearAll(): Int

    @Update
    suspend fun update(entity: PrayerTimesEntity)

    @Delete
    suspend fun delete(entity: PrayerTimesEntity)
}
