package com.hieltech.haramblur.di

import android.content.Context
import androidx.work.WorkManager
import com.hieltech.haramblur.detection.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for performance monitoring and optimization components
 * Provides dependency injection configuration for performance-critical components
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun provideDevicePerformanceAnalyzer(
        @ApplicationContext context: Context
    ): DevicePerformanceAnalyzer {
        return DevicePerformanceAnalyzer(context)
    }
    
    @Provides
    @Singleton
    fun provideBatteryOptimizationManager(
        @ApplicationContext context: Context
    ): BatteryOptimizationManager {
        return BatteryOptimizationManager(context)
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        devicePerformanceAnalyzer: DevicePerformanceAnalyzer,
        memoryManager: MemoryManager,
        batteryOptimizationManager: BatteryOptimizationManager
    ): PerformanceMonitor {
        return PerformanceMonitor(devicePerformanceAnalyzer, memoryManager, batteryOptimizationManager)
    }
    
    @Provides
    @Singleton
    fun provideErrorReportingManager(
        @ApplicationContext context: Context
    ): ErrorReportingManager {
        return ErrorReportingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideComprehensiveErrorHandler(
        memoryManager: MemoryManager,
        performanceMonitor: PerformanceMonitor
    ): ComprehensiveErrorHandler {
        return ComprehensiveErrorHandler(memoryManager, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun provideServiceLifecycleManager(
        mlModelManager: com.hieltech.haramblur.ml.MLModelManager,
        databaseInitializer: com.hieltech.haramblur.data.database.DatabaseInitializer,
        performanceMonitor: PerformanceMonitor,
        memoryManager: MemoryManager,
        gpuAccelerationManager: GPUAccelerationManager,
        errorReportingManager: ErrorReportingManager
    ): ServiceLifecycleManager {
        return ServiceLifecycleManager(
            mlModelManager,
            databaseInitializer,
            performanceMonitor,
            memoryManager,
            gpuAccelerationManager,
            errorReportingManager
        )
    }

    @Provides
    @Singleton
    fun provideAppBlockingManager(
        database: com.hieltech.haramblur.data.database.SiteBlockingDatabase,
        @ApplicationContext context: Context,
        logRepository: com.hieltech.haramblur.data.LogRepository
    ): AppBlockingManager {
        return AppBlockingManagerImpl(database, context, context.packageManager, logRepository)
    }

    @Provides
    @Singleton
    fun provideScheduleManager(
        database: com.hieltech.haramblur.data.database.SiteBlockingDatabase,
        @ApplicationContext context: Context
    ): ScheduleManager {
        return ScheduleManagerImpl(database, context, null)
    }

    @Provides
    @Singleton
    fun provideEnhancedSiteBlockingManager(
        database: com.hieltech.haramblur.data.database.SiteBlockingDatabase,
        @ApplicationContext context: Context,
        originalManager: SiteBlockingManager
    ): EnhancedSiteBlockingManager {
        return EnhancedSiteBlockingManager(database, context, originalManager)
    }


}