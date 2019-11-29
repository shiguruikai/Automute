package com.github.shiguruikai.automuteapp.service

import android.app.IntentService
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.getSystemService
import com.github.shiguruikai.automuteapp.util.setMasterMute

class MasterMuteIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null || !intent.hasExtra(EXTRA_MUTE)) return

        val audioManager = getSystemService<AudioManager>() ?: return

        val mute = intent.getBooleanExtra(EXTRA_MUTE, false)

        audioManager.setMasterMute(mute)
    }

    companion object {
        private val TAG = MasterMuteIntentService::class.java.simpleName

        private const val EXTRA_MUTE = "mute"
    }
}
