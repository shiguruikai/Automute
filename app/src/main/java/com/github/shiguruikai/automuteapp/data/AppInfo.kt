package com.github.shiguruikai.automuteapp.data

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppInfo(
    /** @see ApplicationInfo.uid */
    val uid: Int,
    /** @see ApplicationInfo.loadLabel */
    val label: String,
    /** @see ApplicationInfo.processName */
    val packageName: String,
    /** 選択されているかどうか。 */
    var isChecked: Boolean = false
) : Parcelable
