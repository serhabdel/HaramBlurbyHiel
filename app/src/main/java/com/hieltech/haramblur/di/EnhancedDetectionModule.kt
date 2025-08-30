package com.hieltech.haramblur.di

import android.app.usage.UsageStatsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import com.hieltech.haramblur.accessibility.AppLaunchInterceptor
import com.hieltech.haramblur.detection.*
import com.hieltech.haramblur.ml.FaceDetectionManager
import com.hieltech.haramblur.ml.MLModelManager
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Hilt module for enhanced detection components
 * Provides dependency injection configuration for the enhanced content detection system
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EnhancedDetectionModule {
    
    // Interface bindings for enhanced detection components
    @Binds
    @Singleton
    abstract fun bindEnhancedGenderDetector(
        enhancedGenderDetectorImpl: EnhancedGenderDetectorImpl
    ): EnhancedGenderDetector
    
    @Binds
    @Singleton
    abstract fun bindFastContentDetector(
        fastContentDetectorImpl: FastContentDetectorImpl
    ): FastContentDetector
    
    @Binds
    @Singleton
    abstract fun bindContentDensityAnalyzer(
        contentDensityAnalyzerImpl: ContentDensityAnalyzerImpl
    ): ContentDensityAnalyzer
    
    @Binds
    @Singleton
    abstract fun bindAppBlockingManager(
        appBlockingManagerImpl: AppBlockingManagerImpl
    ): AppBlockingManager

    @Binds
    @Singleton
    abstract fun bindBlockedAppLaunchCallback(
        appBlockingManagerImpl: AppBlockingManagerImpl
    ): BlockedAppLaunchCallback

    @Binds
    @Singleton
    abstract fun bindSiteBlockingManager(
        siteBlockingManagerImpl: SiteBlockingManagerImpl
    ): SiteBlockingManager
    
    @Binds
    @Singleton
    abstract fun bindErrorRecoveryManager(
        errorRecoveryManagerImpl: ErrorRecoveryManagerImpl
    ): ErrorRecoveryManager
    
    companion object {
        
        // Performance optimization components
        @Provides
        @Singleton
        fun provideGPUAccelerationManager(): GPUAccelerationManager {
            return GPUAccelerationManager()
        }
        
        @Provides
        @Singleton
        fun provideFrameOptimizationManager(): FrameOptimizationManager {
            return FrameOptimizationManager()
        }
        
        @Provides
        @Singleton
        fun provideFullScreenBlurTrigger(): FullScreenBlurTrigger {
            return FullScreenBlurTrigger()
        }
        
        @Provides
        @Singleton
        fun provideMemoryManager(): MemoryManager {
            return MemoryManager()
        }
        
        // Detection engine components with proper lifecycle management
        @Provides
        @Singleton
        fun provideFallbackDetectionEngine(): FallbackDetectionEngine {
            return FallbackDetectionEngine()
        }
        
        @Provides
        @Singleton
        fun provideEmbeddedSiteBlockingList(): EmbeddedSiteBlockingList {
            return EmbeddedSiteBlockingList()
        }
        
        // Enhanced ML model manager with GPU acceleration
        @Provides
        @Singleton
        fun provideMLModelManager(
            gpuAccelerationManager: GPUAccelerationManager,
            performanceMonitor: PerformanceMonitor
        ): MLModelManager {
            return MLModelManager(gpuAccelerationManager, performanceMonitor)
        }
        
        // Enhanced face detection with gender detection
        @Provides
        @Singleton
        fun provideFaceDetectionManager(
            enhancedGenderDetector: EnhancedGenderDetector,
            mlModelManager: MLModelManager
        ): FaceDetectionManager {
            return FaceDetectionManager(enhancedGenderDetector, mlModelManager)
        }
        
        // Main content detection engine with all enhanced features
        @Provides
        @Singleton
        fun provideContentDetectionEngine(
            mlModelManager: MLModelManager,
            faceDetectionManager: FaceDetectionManager,
            fastContentDetector: FastContentDetector,
            frameOptimizationManager: FrameOptimizationManager,
            performanceMonitor: PerformanceMonitor,
            contentDensityAnalyzer: ContentDensityAnalyzer,
            fullScreenBlurTrigger: FullScreenBlurTrigger,
            logRepository: LogRepository
        ): ContentDetectionEngine {
            return ContentDetectionEngine(
                mlModelManager,
                faceDetectionManager,
                fastContentDetector,
                frameOptimizationManager,
                performanceMonitor,
                contentDensityAnalyzer,
                fullScreenBlurTrigger,
                logRepository
            )
        }
        
        // System service providers
        @Provides
        @Singleton
        fun provideUsageStatsManager(
            @ApplicationContext context: Context
        ): UsageStatsManager {
            return context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        }
        
        @Provides
        @Singleton
        fun provideDevicePolicyManager(
            @ApplicationContext context: Context
        ): DevicePolicyManager {
            return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        }

        @Provides
        @Singleton
        fun providePackageManager(
            @ApplicationContext context: Context
        ): PackageManager {
            return context.packageManager
        }
    }
}