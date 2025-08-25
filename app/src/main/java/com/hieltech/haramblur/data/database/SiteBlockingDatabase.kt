package com.hieltech.haramblur.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for site blocking and Quranic verses
 */
@Database(
    entities = [
        BlockedSiteEntity::class,
        QuranicVerseEntity::class,
        FalsePositiveEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class SiteBlockingDatabase : RoomDatabase() {
    
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun quranicVerseDao(): QuranicVerseDao
    abstract fun falsePositiveDao(): FalsePositiveDao
    
    companion object {
        private const val DATABASE_NAME = "site_blocking_database"
        
        @Volatile
        private var INSTANCE: SiteBlockingDatabase? = null
        
        fun getDatabase(context: Context): SiteBlockingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteBlockingDatabase::class.java,
                    DATABASE_NAME
                )
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