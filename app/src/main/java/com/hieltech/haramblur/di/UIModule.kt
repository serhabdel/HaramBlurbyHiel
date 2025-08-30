package com.hieltech.haramblur.di

import com.hieltech.haramblur.accessibility.BlurOverlayManager
import com.hieltech.haramblur.accessibility.ScreenCaptureManager
import com.hieltech.haramblur.ui.components.WarningDialogManager
import com.hieltech.haramblur.ui.effects.BlurEffectivenessValidator
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.services.DhikrManager
import com.hieltech.haramblur.services.DhikrNotificationManager
import com.hieltech.haramblur.data.DhikrRepository
import com.hieltech.haramblur.utils.DhikrPermissionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for UI and accessibility components
 * Provides dependency injection configuration for UI-related services
 */
@Module
@InstallIn(SingletonComponent::class)
object UIModule {
    
    @Provides
    @Singleton
    fun provideScreenCaptureManager(): ScreenCaptureManager {
        return ScreenCaptureManager()
    }
    
    @Provides
    @Singleton
    fun provideWarningDialogManager(
        quranicRepository: QuranicRepository,
        settingsRepository: SettingsRepository
    ): WarningDialogManager {
        return WarningDialogManager(quranicRepository, settingsRepository)
    }
    
    @Provides
    @Singleton
    fun provideBlurOverlayManager(
        warningDialogManager: WarningDialogManager,
        settingsRepository: com.hieltech.haramblur.data.SettingsRepository
    ): BlurOverlayManager {
        return BlurOverlayManager(warningDialogManager, settingsRepository)
    }
    
    @Provides
    @Singleton
    fun provideBlurEffectivenessValidator(): BlurEffectivenessValidator {
        return BlurEffectivenessValidator()
    }
    
    @Provides
    @Singleton
    fun provideDhikrPermissionHelper(
        @ApplicationContext context: android.content.Context
    ): DhikrPermissionHelper {
        return DhikrPermissionHelper(context)
    }

    @Provides
    @Singleton
    fun provideDhikrNotificationManager(
        @ApplicationContext context: android.content.Context
    ): DhikrNotificationManager {
        return DhikrNotificationManager(context)
    }

    @Provides
    @Singleton
    fun provideDhikrManager(
        dhikrRepository: DhikrRepository,
        permissionHelper: DhikrPermissionHelper,
        notificationManager: DhikrNotificationManager
    ): DhikrManager {
        return DhikrManager(dhikrRepository, permissionHelper, notificationManager)
    }
}