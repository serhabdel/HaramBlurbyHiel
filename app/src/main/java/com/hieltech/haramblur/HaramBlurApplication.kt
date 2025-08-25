package com.hieltech.haramblur

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HaramBlurApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize app-level components here
    }
}