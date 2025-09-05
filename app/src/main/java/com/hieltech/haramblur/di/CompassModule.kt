package com.hieltech.haramblur.di

import android.content.Context
import com.hieltech.haramblur.utils.CompassSensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for compass-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CompassModule {

    @Provides
    @Singleton
    fun provideCompassSensorManager(
        @ApplicationContext context: Context
    ): CompassSensorManager {
        return CompassSensorManager(context)
    }
}
