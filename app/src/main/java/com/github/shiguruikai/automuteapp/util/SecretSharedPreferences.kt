package com.github.shiguruikai.automuteapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecretSharedPreferences(
    context: Context
) : SharedPreferences by context.getEncryptedSharedPreferences() {
    var selectedPackageNames by prefStringSet(emptySet())
    var selectedActivityNames by prefStringSet(emptySet())
}

private fun Context.getEncryptedSharedPreferences(): SharedPreferences = EncryptedSharedPreferences.create(
    "secret_shared_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    applicationContext,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
