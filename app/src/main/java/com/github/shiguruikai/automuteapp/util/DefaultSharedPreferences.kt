package com.github.shiguruikai.automuteapp.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.Gravity
import com.github.shiguruikai.automuteapp.R

class DefaultSharedPreferences(
    context: Context
) : SharedPreferences by PreferenceManager.getDefaultSharedPreferences(context.applicationContext) {

    private val res = context.applicationContext.resources

    var selectedPackageNames by prefStringSet(emptySet())

    val isManualMute by prefBoolean(res, R.bool.manualMute_def, R.string.manualMute)

    val isStartAutoMuteService by prefBoolean(res, R.bool.startAutoMuteService_def, R.string.startAutoMuteService)

    val isStartAfterReboot by prefBoolean(res, R.bool.startAfterReboot_def, R.string.startAfterReboot)

    val isStartAfterUpdate by prefBoolean(res, R.bool.startAfterUpdate_def, R.string.startAfterUpdate)

    val isToastMuteChange by prefBoolean(res, R.bool.toastMuteChange_def, R.string.toastMuteChange)

    val isShortDurationToast by prefBoolean(res, R.bool.shortDurationToast_def, R.string.shortDurationToast)

    val toastGravity by prefString(Gravity.CENTER.toString(), res.getString(R.string.toastGravity))
}
