package com.github.shiguruikai.automuteapp.util

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.shiguruikai.automuteapp.R

class DefaultSharedPreferences(
    context: Context
) : SharedPreferences by context.getEncryptedSharedPreferences() {
    private val res = context.applicationContext.resources

    var selectedPackageNames by prefStringSet(emptySet())

    var selectedActivityNames by prefStringSet(emptySet())

    val manualMute by prefBoolean(res, R.bool.manualMute_def, R.string.manualMute)

    val startAutoMuteService by prefBoolean(res, R.bool.startAutoMuteService_def, R.string.startAutoMuteService)

    val startAfterReboot by prefBoolean(res, R.bool.startAfterReboot_def, R.string.startAfterReboot)

    val startAfterUpdate by prefBoolean(res, R.bool.startAfterUpdate_def, R.string.startAfterUpdate)

    val toastMuteChange by prefBoolean(res, R.bool.toastMuteChange_def, R.string.toastMuteChange)

    val shortDurationToast by prefBoolean(res, R.bool.shortDurationToast_def, R.string.shortDurationToast)

    val toastGravity by prefString(Gravity.CENTER.toString(), res.getString(R.string.toastGravity))

    val unmuteOnIncomingCall by prefBoolean(res, R.bool.unmuteOnIncomingCall_def, R.string.unmuteOnIncomingCall)
}

private fun Context.getEncryptedSharedPreferences(): SharedPreferences = EncryptedSharedPreferences.create(
    "secret_shared_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    applicationContext,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
