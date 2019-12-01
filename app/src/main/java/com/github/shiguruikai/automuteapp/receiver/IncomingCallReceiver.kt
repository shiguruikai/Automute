package com.github.shiguruikai.automuteapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import com.github.shiguruikai.automuteapp.util.setMasterMute
import com.github.shiguruikai.automuteapp.util.toastMuteState

class IncomingCallReceiver(
    var onIdle: (phoneNumber: String) -> Unit = {},
    var onOffHook: (phoneNumber: String) -> Unit = {},
    var onRinging: (phoneNumber: String) -> Unit = {}
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PHONE_STATE) {
            val telephonyManager = context.getSystemService<TelephonyManager>()!!

            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String) {
                    when (state) {
                        TelephonyManager.CALL_STATE_IDLE    -> onIdle(phoneNumber)
                        TelephonyManager.CALL_STATE_OFFHOOK -> onOffHook(phoneNumber)
                        TelephonyManager.CALL_STATE_RINGING -> {
                            onRinging(phoneNumber)

                            // 着信でミュートを解除
                            if (defaultSharedPreferences.unmuteOnIncomingCall) {
                                val audioManager = context.getSystemService<AudioManager>()!!
                                audioManager.setMasterMute(false)

                                // ミュートの状態を通知
                                if (defaultSharedPreferences.toastMuteChange) {
                                    context.toastMuteState(audioManager)
                                }
                            }
                        }
                    }
                }
            }

            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    companion object {
        private val TAG = IncomingCallReceiver::class.java.simpleName

        private const val ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE"

        val intentFilter: IntentFilter = IntentFilter(ACTION_PHONE_STATE)
    }
}
