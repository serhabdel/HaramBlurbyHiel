package com.hieltech.haramblur.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for site blocking, app blocking, and Quranic verses
 */
@Database(
    entities = [
        BlockedSiteEntity::class,
        BlockedAppEntity::class,
        BlockingScheduleEntity::class,
        BlockingPreferencesEntity::class,
        QuranicVerseEntity::class,
        FalsePositiveEntity::class,
        LogEntity::class,
        PrayerTimesEntity::class,
        com.hieltech.haramblur.data.database.entities.StatisticsEntity::class,
        com.hieltech.haramblur.data.database.entities.AppUsageStatsEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class SiteBlockingDatabase : RoomDatabase() {
    
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockingScheduleDao(): BlockingScheduleDao
    abstract fun blockingPreferencesDao(): BlockingPreferencesDao
    abstract fun quranicVerseDao(): QuranicVerseDao
    abstract fun falsePositiveDao(): FalsePositiveDao
    abstract fun logDao(): LogDao
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun appUsageStatsDao(): AppUsageStatsDao
    
    companion object {
        private const val DATABASE_NAME = "site_blocking_database"

        // Migration from version 1 to 2: Add app blocking tables
        private val MIGRATION_1_2 = Migration(1, 2) { database ->
            // Create blocked_apps table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS blocked_apps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    package_name TEXT NOT NULL UNIQUE,
                    app_name TEXT NOT NULL,
                    is_blocked INTEGER DEFAULT 0,
                    block_type TEXT DEFAULT 'simple',
                    icon_path TEXT,
                    category TEXT,
                    is_system_app INTEGER DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    last_blocked_at INTEGER,
                    block_count INTEGER DEFAULT 0
                )
            """)

            // Create blocking_schedules table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS blocking_schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app_package_name TEXT,
                    site_domain TEXT,
                    schedule_type TEXT NOT NULL,
                    duration_minutes INTEGER,
                    start_hour INTEGER,
                    start_minute INTEGER,
                    end_hour INTEGER,
                    end_minute INTEGER,
                    days_of_week TEXT,
                    is_active INTEGER DEFAULT 1,
                    schedule_name TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    last_applied_at INTEGER,
                    next_scheduled_at INTEGER
                )
            """)

            // Create blocking_preferences table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS blocking_preferences (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    strict_blocking INTEGER DEFAULT 0,
                    allow_temporary_unblock INTEGER DEFAULT 1,
                    temporary_unblock_duration INTEGER DEFAULT 5,
                    show_quranic_verses INTEGER DEFAULT 1,
                    vibration_on_block INTEGER DEFAULT 1,
                    sound_on_block INTEGER DEFAULT 0,
                    show_blocking_notifications INTEGER DEFAULT 1,
                    auto_close_blocked_browser_tabs INTEGER DEFAULT 1,
                    block_during_prayer_times INTEGER DEFAULT 0,
                    prayer_time_blocking_minutes INTEGER DEFAULT 30,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)

            // Add new columns to existing blocked_sites table
            // Use PRAGMA to check if columns exist before adding them
            val cursor = database.query("PRAGMA table_info(blocked_sites)")
            val existingColumns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            cursor.close()

            // Add columns only if they don't exist
            if (!existingColumns.contains("is_custom")) {
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN is_custom INTEGER DEFAULT 0")
            }
            if (!existingColumns.contains("added_by_user")) {
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN added_by_user INTEGER DEFAULT 0")
            }
            if (!existingColumns.contains("custom_category")) {
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN custom_category TEXT")
            }
            if (!existingColumns.contains("date_added")) {
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN date_added INTEGER")
            }
            if (!existingColumns.contains("block_count")) {
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN block_count INTEGER DEFAULT 0")
            }

            // Update existing data to have proper default values
            database.execSQL("UPDATE blocked_sites SET is_custom = 0 WHERE is_custom IS NULL")
            database.execSQL("UPDATE blocked_sites SET added_by_user = 0 WHERE added_by_user IS NULL")
            database.execSQL("UPDATE blocked_sites SET block_count = 0 WHERE block_count IS NULL")

            // Create indexes for performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_apps_package_name ON blocked_apps(package_name)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_apps_is_blocked ON blocked_apps(is_blocked)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_apps_block_type ON blocked_apps(block_type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocking_schedules_app_package_name ON blocking_schedules(app_package_name)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocking_schedules_site_domain ON blocking_schedules(site_domain)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocking_schedules_schedule_type ON blocking_schedules(schedule_type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocking_schedules_is_active ON blocking_schedules(is_active)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_sites_added_by_user ON blocked_sites(added_by_user)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_sites_is_custom ON blocked_sites(is_custom)")
        }

        // Migration from version 2 to 3: Add logging table
        private val MIGRATION_2_3 = Migration(2, 3) { database ->
            try {
                // Check if logs table already exists
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='logs'")
                val tableExists = cursor.moveToFirst()
                cursor.close()

                if (!tableExists) {
                    // Create logs table only if it doesn't exist
                    database.execSQL("""
                        CREATE TABLE logs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            timestamp INTEGER NOT NULL,
                            tag TEXT NOT NULL,
                            message TEXT NOT NULL,
                            level TEXT NOT NULL DEFAULT 'DEBUG',
                            category TEXT DEFAULT 'GENERAL',
                            stack_trace TEXT,
                            user_action TEXT,
                            device_info TEXT,
                            app_version TEXT,
                            session_id TEXT,
                            created_at INTEGER NOT NULL
                        )
                    """)

                    // Create indexes for performance
                    database.execSQL("CREATE INDEX index_logs_timestamp ON logs(timestamp)")
                    database.execSQL("CREATE INDEX index_logs_level ON logs(level)")
                    database.execSQL("CREATE INDEX index_logs_category ON logs(category)")
                    database.execSQL("CREATE INDEX index_logs_tag ON logs(tag)")
                } else {
                    // If table exists, ensure all required columns are present
                    val tableInfoCursor = database.query("PRAGMA table_info(logs)")
                    val existingColumns = mutableSetOf<String>()
                    while (tableInfoCursor.moveToNext()) {
                        existingColumns.add(tableInfoCursor.getString(tableInfoCursor.getColumnIndexOrThrow("name")))
                    }
                    tableInfoCursor.close()

                    // Add any missing columns
                    val requiredColumns = listOf(
                        "id" to "INTEGER PRIMARY KEY AUTOINCREMENT",
                        "timestamp" to "INTEGER NOT NULL",
                        "tag" to "TEXT NOT NULL",
                        "message" to "TEXT NOT NULL",
                        "level" to "TEXT NOT NULL DEFAULT 'DEBUG'",
                        "category" to "TEXT DEFAULT 'GENERAL'",
                        "stack_trace" to "TEXT",
                        "user_action" to "TEXT",
                        "device_info" to "TEXT",
                        "app_version" to "TEXT",
                        "session_id" to "TEXT",
                        "created_at" to "INTEGER NOT NULL"
                    )

                    for ((columnName, columnDef) in requiredColumns) {
                        if (!existingColumns.contains(columnName)) {
                            database.execSQL("ALTER TABLE logs ADD COLUMN $columnName $columnDef")
                        }
                    }
                }
            } catch (e: Exception) {
                // If migration fails, log the error and continue
                android.util.Log.e("SiteBlockingDatabase", "Migration 2->3 failed", e)
                throw e // Re-throw to let Room handle it
            }
        }

        // Migration from version 3 to 4: Add prayer_times table
        private val MIGRATION_3_4 = Migration(3, 4) { database ->
            // Create prayer_times table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS prayer_times (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date_millis INTEGER NOT NULL,
                    lat REAL NOT NULL,
                    lon REAL NOT NULL,
                    tz_id TEXT NOT NULL,
                    method TEXT NOT NULL,
                    asr_madhab TEXT,
                    fajr INTEGER NOT NULL,
                    sunrise INTEGER NOT NULL,
                    dhuhr INTEGER NOT NULL,
                    asr INTEGER NOT NULL,
                    maghrib INTEGER NOT NULL,
                    isha INTEGER NOT NULL,
                    fajr_offset_min INTEGER NOT NULL DEFAULT 0,
                    sunrise_offset_min INTEGER NOT NULL DEFAULT 0,
                    dhuhr_offset_min INTEGER NOT NULL DEFAULT 0,
                    asr_offset_min INTEGER NOT NULL DEFAULT 0,
                    maghrib_offset_min INTEGER NOT NULL DEFAULT 0,
                    isha_offset_min INTEGER NOT NULL DEFAULT 0,
                    gps_accuracy_m REAL,
                    location_source TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """
            )

            // Indexes
            database.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_times_date_millis ON prayer_times(date_millis)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_times_lat_lon ON prayer_times(lat, lon)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_times_method ON prayer_times(method)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_prayer_times_unique ON prayer_times(date_millis, lat, lon, method)")
        }

        // Migration from version 4 to 5: Add daily_statistics table
        private val MIGRATION_4_5 = Migration(4, 5) { database ->
            try {
                // First, drop any existing statistics table to avoid conflicts
                database.execSQL("DROP TABLE IF EXISTS statistics")
                
                // Create daily_statistics table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_statistics (
                        date INTEGER NOT NULL,
                        facesDetected INTEGER NOT NULL DEFAULT 0,
                        sitesBlocked INTEGER NOT NULL DEFAULT 0,
                        detectionAccuracy REAL NOT NULL DEFAULT 0.0,
                        averageProcessingTime INTEGER NOT NULL DEFAULT 0,
                        totalScreenTime INTEGER NOT NULL DEFAULT 0,
                        protectionActiveTime INTEGER NOT NULL DEFAULT 0,
                        batteryUsage REAL NOT NULL DEFAULT 0.0,
                        memoryPeakUsage REAL NOT NULL DEFAULT 0.0,
                        networkRequests INTEGER NOT NULL DEFAULT 0,
                        errorsEncountered INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(date)
                    )
                    """
                )

                // Create index for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_statistics_date ON daily_statistics(date)")
            } catch (e: Exception) {
                android.util.Log.e("SiteBlockingDatabase", "Migration 4->5 failed", e)
                throw e
            }
        }

        // Migration from version 5 to 6: Clean up any remaining database issues
        private val MIGRATION_5_6 = Migration(5, 6) { database ->
            try {
                // Drop any problematic tables and recreate them cleanly
                database.execSQL("DROP TABLE IF EXISTS statistics")
                database.execSQL("DROP TABLE IF EXISTS daily_statistics")

                // Create daily_statistics table fresh
                database.execSQL(
                    """
                    CREATE TABLE daily_statistics (
                        date INTEGER NOT NULL,
                        facesDetected INTEGER NOT NULL DEFAULT 0,
                        sitesBlocked INTEGER NOT NULL DEFAULT 0,
                        detectionAccuracy REAL NOT NULL DEFAULT 0.0,
                        averageProcessingTime INTEGER NOT NULL DEFAULT 0,
                        totalScreenTime INTEGER NOT NULL DEFAULT 0,
                        protectionActiveTime INTEGER NOT NULL DEFAULT 0,
                        batteryUsage REAL NOT NULL DEFAULT 0.0,
                        memoryPeakUsage REAL NOT NULL DEFAULT 0.0,
                        networkRequests INTEGER NOT NULL DEFAULT 0,
                        errorsEncountered INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(date)
                    )
                    """
                )

                // Create index for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_statistics_date ON daily_statistics(date)")
            } catch (e: Exception) {
                android.util.Log.e("SiteBlockingDatabase", "Migration 5->6 failed", e)
                throw e
            }
        }

        // Migration from version 6 to 7: Add app usage statistics table
        private val MIGRATION_6_7 = Migration(6, 7) { database ->
            try {
                // Create app_usage_stats table
                database.execSQL("""
                    CREATE TABLE app_usage_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        packageName TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        totalMinutes INTEGER NOT NULL DEFAULT 0,
                        lastUsed INTEGER NOT NULL,
                        limitExceededTime INTEGER,
                        notificationCount INTEGER NOT NULL DEFAULT 0,
                        lastNotificationTime INTEGER,
                        appCategory TEXT,
                        timeLimitMinutes INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // Create indexes for performance
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_app_usage_stats_package_date ON app_usage_stats(packageName, date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_stats_date ON app_usage_stats(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_stats_package ON app_usage_stats(packageName)")

                android.util.Log.i("SiteBlockingDatabase", "Migration 6->7 completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("SiteBlockingDatabase", "Migration 6->7 failed", e)
                throw e
            }
        }

        @Volatile
        private var INSTANCE: SiteBlockingDatabase? = null

        fun getDatabase(context: Context): SiteBlockingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteBlockingDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database initialization will be handled by repository
            }
        }
    }
}