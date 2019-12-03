package com.github.shiguruikai.automuteapp.data

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppInfo(
    /** @see ApplicationInfo.uid */
    val uid: Int,
    /** @see ApplicationInfo.loadLabel */
    val label: String,
    /** @see ApplicationInfo.packageName */
    val packageName: String,
    /** [ActivityInfo.name] のクラス名 */
    val activityName: String?,
    /**
     * @see [ApplicationInfo.packageName]
     * @see [ActivityInfo.name]
     */
    val name: String,
    /** 選択されているかどうか。 */
    var isChecked: Boolean = false
) : Parcelable
