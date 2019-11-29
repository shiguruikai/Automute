package com.github.shiguruikai.automuteapp.util

import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.BoolRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferencesDelegate<T>(
    private val key: String? = null,
    private val getter: (k: String) -> T,
    private val setter: SharedPreferences.Editor.(k: String, v: T) -> Unit
) : ReadWriteProperty<SharedPreferences, T> {

    override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): T {
        return getter(key ?: property.name)
    }

    override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: T) {
        thisRef.edit { setter(key ?: property.name, value) }
    }
}

fun SharedPreferences.prefInt(defaultValue: Int, key: String? = null): PreferencesDelegate<Int> =
    PreferencesDelegate(
        key,
        { k -> getInt(k, defaultValue) },
        { k, v -> putInt(k, v) })

fun SharedPreferences.prefInt(
    resources: Resources, @IntegerRes defaultValue: Int, @StringRes key: Int
): PreferencesDelegate<Int> = prefInt(resources.getInteger(defaultValue), resources.getString(key))

fun SharedPreferences.prefBoolean(defaultValue: Boolean, key: String? = null): PreferencesDelegate<Boolean> =
    PreferencesDelegate(
        key,
        { k -> getBoolean(k, defaultValue) },
        { k, v -> putBoolean(k, v) })

fun SharedPreferences.prefBoolean(
    resources: Resources, @BoolRes defaultValue: Int, @StringRes key: Int
): PreferencesDelegate<Boolean> = prefBoolean(resources.getBoolean(defaultValue), resources.getString(key))

fun SharedPreferences.prefString(defaultValue: String, key: String? = null): PreferencesDelegate<String> =
    PreferencesDelegate(
        key,
        { k -> getString(k, defaultValue) },
        { k, v -> putString(k, v) })

fun SharedPreferences.prefString(
    resources: Resources, @StringRes defaultValue: Int, @StringRes key: Int
): PreferencesDelegate<String> = prefString(resources.getString(defaultValue), resources.getString(key))

fun SharedPreferences.prefStringSet(defaultValue: Set<String>, key: String? = null): PreferencesDelegate<Set<String>> =
    PreferencesDelegate(
        key,
        { k -> getStringSet(k, defaultValue) },
        { k, v -> putStringSet(k, v) })
