package com.github.shiguruikai.automuteapp.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.CheckBoxPreference
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate

infix fun CharSequence.iin(other: CharSequence): Boolean = other.contains(this, ignoreCase = true)

inline fun <reified T> Context.newIntent(): Intent = Intent(this, T::class.java)

fun Context.allPermissionsGranted(vararg permissions: String): Boolean {
    return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

fun Context.isUsageStatsAllowed(): Boolean {
    val appOpsManager = getSystemService<AppOpsManager>()!!
    val uid = android.os.Process.myUid()
    val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

@ExperimentalCoroutinesApi
suspend inline fun <T : View> T.afterMeasured(crossinline action: (T) -> Unit) {
    callbackFlow<T> {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (measuredWidth > 0 && measuredHeight > 0) {
                offer(this@afterMeasured)
                close()
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)

        awaitClose {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }.collect {
        action(it)
    }
}

fun AudioManager.isMasterMute(): Boolean {
    val method = this::class.java.getDeclaredMethod("isMasterMute")
    return method.invoke(this) as Boolean
}

fun AudioManager.setMasterMute(mute: Boolean, flags: Int = 0) {
    val method = this::class.java.getDeclaredMethod("setMasterMute", Boolean::class.java, Int::class.java)
    method.invoke(this, mute, flags)
}

fun SwitchPreferenceCompat.setOnPreferenceChangeListener(action: (newValue: Boolean) -> Boolean) {
    setOnPreferenceChangeListener { _, newValue ->
        action(newValue as Boolean)
    }
}

fun CheckBoxPreference.setOnPreferenceChangeListener(action: (newValue: Boolean) -> Boolean) {
    setOnPreferenceChangeListener { _, newValue ->
        action(newValue as Boolean)
    }
}

@ExperimentalCoroutinesApi
fun SearchView.queryTextAsFlow(): Flow<String> = callbackFlow<String> {
    offer(query?.toString().orEmpty())

    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            offer(newText)
            return false
        }

        override fun onQueryTextSubmit(query: String): Boolean = false
    })

    awaitClose {
        setOnQueryTextFocusChangeListener(null)
    }
}.conflate()
