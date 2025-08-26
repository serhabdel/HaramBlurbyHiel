package com.hieltech.haramblur.di

import android.content.Context
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.database.DatabaseInitializer
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
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
}