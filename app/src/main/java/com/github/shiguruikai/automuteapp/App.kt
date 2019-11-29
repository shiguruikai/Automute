package com.github.shiguruikai.automuteapp

import android.app.Application
import androidx.preference.PreferenceManager
import com.github.shiguruikai.automuteapp.util.DefaultSharedPreferences

lateinit var defaultSharedPreferences: DefaultSharedPreferences

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        PreferenceManager.setDefaultValues(this, R.xml.preferences_mute, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_notification, false)

        defaultSharedPreferences = DefaultSharedPreferences(this)
    }

    companion object {
        lateinit var instance: App
    }
}
