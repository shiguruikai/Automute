package com.github.shiguruikai.automuteapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ScreenStateReceiver(
    var onScreenOn: () -> Unit = {},
    var onScreenOff: () -> Unit = {}
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON  -> onScreenOn()
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
        }
    }

    companion object {
        val intentFilter: IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
    }
}
