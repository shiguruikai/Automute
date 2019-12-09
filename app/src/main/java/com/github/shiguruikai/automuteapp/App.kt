package com.github.shiguruikai.automuteapp

import android.app.Application
import androidx.preference.PreferenceManager
import com.github.shiguruikai.automuteapp.util.DefaultSharedPreferences
import com.github.shiguruikai.automuteapp.util.SecretSharedPreferences

lateinit var defaultSharedPreferences: DefaultSharedPreferences
lateinit var secretSharedPreferences: SecretSharedPreferences

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        PreferenceManager.setDefaultValues(this, R.xml.preferences_mute, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_notification, false)

        defaultSharedPreferences = DefaultSharedPreferences(this)
        secretSharedPreferences = SecretSharedPreferences(this)
    }

    companion object {
        lateinit var instance: App
    }
}
