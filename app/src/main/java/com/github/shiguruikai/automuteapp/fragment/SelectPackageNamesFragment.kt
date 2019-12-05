package com.github.shiguruikai.automuteapp.fragment

import android.content.pm.PackageManager
import com.github.shiguruikai.automuteapp.data.AppInfo
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class SelectPackageNamesFragment : SelectAppFragment() {

    override fun saveSelectedAppNames(selectedAppNames: Set<String>) {
        defaultSharedPreferences.selectedPackageNames = selectedAppNames
    }

    override suspend fun getInstalledAppInfoList(): ArrayList<AppInfo> = withContext(Dispatchers.Default) {
        val pm = requireContext().packageManager

        // ソートの優先順は、
        // チェック済み > ラベルの昇順 > パッケージ名の昇順
        val comparator = compareByDescending(AppInfo::isChecked)
            .thenComparator { a, b -> a.label.compareTo(b.label, true) }
            .thenComparator { a, b -> a.packageName.compareTo(b.packageName, true) }

        val flag = PackageManager.GET_ACTIVITIES

        pm.getInstalledPackages(flag)
            .asFlow()
            .map {
                async {
                    if (it.activities != null
                        && it.applicationInfo != null
                        && it.applicationInfo.enabled
                        && it.applicationInfo.labelRes != 0
                    ) {
                        AppInfo(
                            uid = it.applicationInfo.uid,
                            // ラベルがない場合、パッケージ名と同じインスタンス
                            label = it.applicationInfo.loadLabel(pm).toString(),
                            packageName = it.packageName,
                            activityName = null,
                            name = it.packageName
                        ).apply {
                            // ユーザーが選択済みのアプリかどうか
                            isChecked = name in defaultSharedPreferences.selectedPackageNames
                        }
                    } else {
                        null
                    }
                }
            }
            .toList()
            .awaitAll()
            .filterNotNullTo(ArrayList())
            .apply {
                sortWith(comparator)
            }
    }
}
