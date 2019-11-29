package com.github.shiguruikai.automuteapp.util

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.os.postDelayed
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import kotlinx.android.synthetic.main.toast_mute_state.view.*

private val mainHandler = Handler(Looper.getMainLooper())

private var toast: Toast? = null

private const val SHORT_DURATION = 1000L
private const val NORMAL_DURATION = 2000L

fun Context.singleToast(@StringRes resId: Int) {
    mainHandler.post {
        toast?.cancel()
        toast = Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).apply { show() }
    }
}

fun Context.singleToast(text: CharSequence) {
    mainHandler.post {
        toast?.cancel()
        toast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).apply { show() }
    }
}

fun Context.toastMuteState(audioManager: AudioManager) {
    val gravity = defaultSharedPreferences.toastGravity.toInt()
    val duration = if (defaultSharedPreferences.isShortDurationToast) SHORT_DURATION else NORMAL_DURATION
    val toastView = View.inflate(applicationContext, R.layout.toast_mute_state, null)

    if (audioManager.isMasterMute()) {
        toastView.toast_mute_state_textView.setText(R.string.mute)
        toastView.toast_mute_state_imageView.setImageResource(R.drawable.ic_volume_off_black_24dp)
    } else {
        toastView.toast_mute_state_textView.setText(R.string.un_mute)
        toastView.toast_mute_state_imageView.setImageResource(R.drawable.ic_volume_up_black_24dp)
    }

    mainHandler.post {
        toast?.cancel()
        toast = Toast(applicationContext).also {
            it.setGravity(gravity, 0, 0)
            it.duration = Toast.LENGTH_LONG
            it.view = toastView
            it.show()

            mainHandler.postDelayed(duration) {
                it.cancel()
            }
        }
    }
}
