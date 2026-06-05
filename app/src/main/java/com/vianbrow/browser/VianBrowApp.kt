package com.vianbrow.browser

import android.app.Application

class VianBrowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the logger first thing
        VianbrowLogger.init(this)
        
        VianbrowLogger.i("VianBrowApp", "Application started")
    }
}
