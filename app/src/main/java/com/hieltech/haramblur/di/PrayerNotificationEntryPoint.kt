package com.hieltech.haramblur.di

import com.hieltech.haramblur.services.PrayerTimeNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing Prayer notification dependencies from non-Hilt components
 * like BroadcastReceiver
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrayerNotificationEntryPoint {
    fun prayerTimeNotificationManager(): PrayerTimeNotificationManager
}