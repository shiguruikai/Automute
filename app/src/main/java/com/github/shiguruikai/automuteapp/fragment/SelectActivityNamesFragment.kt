package com.github.shiguruikai.automuteapp.fragment

import android.content.pm.PackageManager
import com.github.shiguruikai.automuteapp.data.AppInfo
import com.github.shiguruikai.automuteapp.secretSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class SelectActivityNamesFragment : SelectAppFragment() {

    override fun saveSelectedAppNames(selectedAppNames: Set<String>) {
        secretSharedPreferences.selectedActivityNames = selectedAppNames
    }

    @FlowPreview
    override suspend fun getInstalledAppInfoList(): ArrayList<AppInfo> = withContext(Dispatchers.Default) {
        val pm = requireContext().packageManager

        val selectedActivityNames = secretSharedPreferences.selectedActivityNames

        val comparator = compareByDescending(AppInfo::isChecked)
            .thenComparator { a, b -> a.packageName.compareTo(b.packageName, true) }
            .thenComparator { a, b ->
                if (a.activityName != null && b.activityName != null) {
                    a.activityName.compareTo(b.activityName, true)
                } else {
                    0
                }
            }

        val flag = PackageManager.GET_ACTIVITIES

        pm.getInstalledPackages(flag)
            .asFlow()
            .mapNotNull { it.activities }
            .flatMapConcat { it.asFlow() }
            .map {
                async {
                    if (it.applicationInfo != null && it.isEnabled && it.name.isNotEmpty()) {
                        AppInfo(
                            uid = it.applicationInfo.uid + it.name.hashCode(),
                            label = it.loadLabel(pm).toString(),
                            packageName = it.packageName,
                            activityName = it.name.substringAfterLast('.'),
                            name = it.name
                        ).apply {
                            isChecked = name in selectedActivityNames
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
