package com.hieltech.haramblur.di

import android.app.usage.UsageStatsManager
import android.content.Context
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.UsageStatsHelper
import com.hieltech.haramblur.data.AppUsageTracker
import com.hieltech.haramblur.data.database.DatabaseInitializer
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import com.hieltech.haramblur.data.database.AppUsageStatsDao
import com.hieltech.haramblur.utils.LocalPrayerCalculator
import com.hieltech.haramblur.utils.MoroccanLocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for core data components
 * Provides dependency injection configuration for data layer components
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    // Core Data Components
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideQuranicRepository(
        database: SiteBlockingDatabase,
        databaseInitializer: DatabaseInitializer
    ): QuranicRepository {
        return QuranicRepository(database, databaseInitializer)
    }

    @Provides
    @Singleton
    fun provideLogRepository(
        @ApplicationContext context: Context,
        database: SiteBlockingDatabase
    ): LogRepository {
        return LogRepository(context, database)
    }
    
    // Database Components
    @Provides
    @Singleton
    fun provideSiteBlockingDatabase(
        @ApplicationContext context: Context
    ): SiteBlockingDatabase {
        return SiteBlockingDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseInitializer(
        database: SiteBlockingDatabase
    ): DatabaseInitializer {
        return DatabaseInitializer(database)
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(
        database: SiteBlockingDatabase
    ): com.hieltech.haramblur.data.database.StatisticsDao {
        return database.statisticsDao()
    }

    // Usage Stats Components

    @Provides
    @Singleton
    fun provideUsageStatsManager(
        @ApplicationContext context: Context
    ): UsageStatsManager {
        return context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    @Provides
    @Singleton
    fun provideUsageStatsHelper(
        @ApplicationContext context: Context,
        usageStatsManager: UsageStatsManager
    ): UsageStatsHelper {
        return UsageStatsHelper(context, usageStatsManager)
    }

    @Provides
    @Singleton
    fun provideAppUsageStatsDao(
        database: SiteBlockingDatabase
    ): AppUsageStatsDao {
        return database.appUsageStatsDao()
    }

    @Provides
    @Singleton
    fun provideAppUsageTracker(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        usageStatsHelper: UsageStatsHelper,
        appUsageStatsDao: AppUsageStatsDao
    ): AppUsageTracker {
        return AppUsageTracker(
            context,
            settingsRepository,
            usageStatsHelper,
            appUsageStatsDao
        )
    }

    // Prayer Calculation Components
    @Provides
    @Singleton
    fun provideLocalPrayerCalculator(
        moroccanLocationHelper: MoroccanLocationHelper
    ): LocalPrayerCalculator {
        return LocalPrayerCalculator(moroccanLocationHelper)
    }

    @Provides
    @Singleton
    fun provideMoroccanLocationHelper(): MoroccanLocationHelper {
        return MoroccanLocationHelper()
    }
}
