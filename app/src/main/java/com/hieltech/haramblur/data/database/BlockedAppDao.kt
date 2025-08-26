package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blocked apps
 */
@Dao
interface BlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: BlockedAppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<BlockedAppEntity>)

    @Update
    suspend fun updateApp(app: BlockedAppEntity)

    @Delete
    suspend fun deleteApp(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps WHERE package_name = :packageName")
    suspend fun getAppByPackageName(packageName: String): BlockedAppEntity?

    @Query("SELECT * FROM blocked_apps WHERE is_blocked = 1")
    suspend fun getAllBlockedApps(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE is_blocked = 0")
    suspend fun getAllUnblockedApps(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps ORDER BY app_name ASC")
    suspend fun getAllApps(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE category = :category")
    suspend fun getAppsByCategory(category: String): List<BlockedAppEntity>

    @Query("SELECT DISTINCT category FROM blocked_apps WHERE category IS NOT NULL ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT * FROM blocked_apps WHERE is_blocked = 1 ORDER BY last_blocked_at DESC")
    suspend fun getRecentlyBlockedApps(): List<BlockedAppEntity>

    @Query("UPDATE blocked_apps SET is_blocked = :isBlocked, updated_at = :updatedAt WHERE package_name = :packageName")
    suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean, updatedAt: Long)

    @Query("UPDATE blocked_apps SET last_blocked_at = :blockedAt, block_count = block_count + 1 WHERE package_name = :packageName")
    suspend fun incrementBlockCount(packageName: String, blockedAt: Long)

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE is_blocked = 1")
    fun getBlockedAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_apps")
    suspend fun getTotalAppsCount(): Int

    @Query("DELETE FROM blocked_apps WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT * FROM blocked_apps WHERE app_name LIKE '%' || :query || '%' OR package_name LIKE '%' || :query || '%'")
    suspend fun searchApps(query: String): List<BlockedAppEntity>

    // Flow versions for reactive UI updates
    @Query("SELECT * FROM blocked_apps WHERE is_blocked = 1 ORDER BY app_name ASC")
    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps ORDER BY app_name ASC")
    fun getAllAppsFlow(): Flow<List<BlockedAppEntity>>
}
