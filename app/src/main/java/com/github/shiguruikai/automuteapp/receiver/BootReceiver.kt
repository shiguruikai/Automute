package com.github.shiguruikai.automuteapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import com.github.shiguruikai.automuteapp.service.AutoMasterMuteService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref = defaultSharedPreferences

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED      -> when {
                pref.startAutoMuteService && pref.startAfterReboot -> startAutoMuteService(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> when {
                pref.startAutoMuteService && pref.startAfterUpdate -> startAutoMuteService(context)
            }
        }
    }

    private fun startAutoMuteService(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, AutoMasterMuteService::class.java))
    }

    companion object {
        private val TAG = BootReceiver::class.java.simpleName
    }
}
