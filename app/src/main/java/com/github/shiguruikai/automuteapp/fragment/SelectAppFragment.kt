package com.github.shiguruikai.automuteapp.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.adapter.AppInfoListAdapter
import com.github.shiguruikai.automuteapp.data.AppInfo
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import com.github.shiguruikai.automuteapp.util.afterMeasured
import com.github.shiguruikai.automuteapp.util.iin
import com.github.shiguruikai.automuteapp.util.queryTextAsFlow
import kotlinx.android.synthetic.main.fragment_select_app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectAppFragment : Fragment() {

    private var appInfoList: ArrayList<AppInfo> = ArrayList(0)
    private var adapterLayoutState: Parcelable? = null
    private var adapterLayoutManager: RecyclerView.LayoutManager? = null
    private var searchView: SearchView? = null
    private var searchViewQuery: String? = null

    private val appInfoListAdapter by lazy { AppInfoListAdapter(requireActivity(), lifecycleScope) }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(::appInfoList.name, appInfoList)
        outState.putString(::searchViewQuery.name, searchView?.query?.toString())
        adapterLayoutManager?.let {
            outState.putParcelable(::adapterLayoutState.name, it.onSaveInstanceState())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            appInfoList = savedInstanceState.getParcelableArrayList(::appInfoList.name) ?: ArrayList(0)
            searchViewQuery = savedInstanceState.getString(::searchViewQuery.name)
            adapterLayoutState = savedInstanceState.getParcelable(::adapterLayoutState.name)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_app, container, false)
    }

    private fun showLoadingProgress() {
        progressBar.visibility = View.VISIBLE
        progress_textView.visibility = View.VISIBLE
    }

    private fun hideLoadingProgress() {
        progressBar.visibility = View.GONE
        progress_textView.visibility = View.GONE
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            // RecycleView にリストアダプターをセット
            app_info_recyclerView.adapter = appInfoListAdapter

            // リストアダプターの submitList() を実行する処理
            // 1. リストが未取得の場合、取得した後に実行
            // 2. リストが取得済みで、検索クエリがない場合、即時実行
            // 3. リストが取得済みで、検索クエリがある場合、検索して実行
            if (appInfoList.isEmpty()) {
                launch {
                    showLoadingProgress()
                    appInfoList = getInstalledAppInfoList()
                    hideLoadingProgress()
                    appInfoListAdapter.submitList(appInfoList)
                }
            } else {
                val query = searchViewQuery
                if (query.isNullOrEmpty()) {
                    appInfoListAdapter.submitList(appInfoList)
                } else {
                    launch {
                        val list = searchAppInfoList(query)
                        appInfoListAdapter.submitList(list)
                    }
                }
            }

            /**
             * RecycleView のレイアウト完了後にレイアウトマネージャをセットする。
             * このエラーログは無視して問題ない "E/RecyclerView: No layout manager attached; skipping layout"
             */
            app_info_recyclerView.afterMeasured { rv ->
                // ビューの横幅の大きさに応じて列の数を計算
                val dm = DisplayMetrics().also { requireActivity().windowManager.defaultDisplay.getMetrics(it) }
                val columnCount = (rv.measuredWidth / dm.scaledDensity / COLUMN_DP).toInt() + 1

                // 行の区切り線を追加
                rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                // 列数が2つ以上の場合、列の区切り線を追加
                if (columnCount >= 2) {
                    rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL))
                }

                // レイアウトマネージャを作成
                adapterLayoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else             -> GridLayoutManager(context, columnCount)
                }.apply {
                    // 状態をリストア
                    onRestoreInstanceState(adapterLayoutState)
                }
                // レイアウトマネージャをセット
                rv.layoutManager = adapterLayoutManager
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.search_option_menu, menu)

        val searchMenuItem = menu.findItem(R.id.search_action_item)

        searchView = (searchMenuItem.actionView as SearchView).also { sv ->
            // 画面回転後も横幅が最大になるようにする
            sv.maxWidth = Int.MAX_VALUE

            // 前回の検索クエリをセットする
            if (!searchViewQuery.isNullOrEmpty()) {
                searchMenuItem.expandActionView()
                sv.setQuery(searchViewQuery, false)
                sv.clearFocus()
            }

            lifecycleScope.launch {
                // 検索クエリに応じて、リストをサブミットする
                // submitList() は実行済みのため、最初の検索クエリを無視する
                sv.queryTextAsFlow()
                    .drop(1)
                    .collectLatest { newText ->
                        if (newText.isEmpty()) {
                            appInfoListAdapter.submitList(appInfoList)
                        } else {
                            val list = searchAppInfoList(newText)
                            appInfoListAdapter.submitList(list)
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // アプリリストが取得できていない場合を除く
        if (appInfoList.isNotEmpty()) {
            // チェックが付いたアプリのパッケージ名を保存する
            defaultSharedPreferences.selectedPackageNames = appInfoList.asSequence()
                .filter { it.isChecked }
                .map { it.packageName }
                .toSet()
        }
    }

    private suspend fun searchAppInfoList(query: String): List<AppInfo> = withContext(Dispatchers.Default) {
        appInfoList.asFlow()
            .filter { query iin it.label || query iin it.packageName }
            .toList()
    }

    /**
     * インストール済みのアプリの中から、有効かつアクティビティとラベルが存在するアプリの情報を取得する。
     * インストール済みのアプリの数にもよるが、取得に数秒かかる。
     */
    private suspend fun getInstalledAppInfoList(): ArrayList<AppInfo> = withContext(Dispatchers.Default) {
        val pm = requireContext().packageManager
        val selectedPackageNames = defaultSharedPreferences.selectedPackageNames

        // ソートの優先順は、
        // チェック済み > ラベルの昇順 > パッケージ名の昇順
        val comparator = compareByDescending(AppInfo::isChecked)
            .thenComparator { a, b -> a.label.compareTo(b.label, true) }
            .thenComparator { a, b -> a.packageName.compareTo(b.packageName, true) }

        val flag = PackageManager.GET_ACTIVITIES

        pm.getInstalledPackages(flag)
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
                            packageName = it.applicationInfo.packageName
                        ).apply {
                            // ユーザーが選択済みのアプリかどうか
                            isChecked = it.applicationInfo.packageName in selectedPackageNames
                        }
                    } else {
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNullTo(ArrayList())
            .apply {
                sortWith(comparator)
            }
    }

    companion object {
        private val TAG = SelectAppFragment::class.java.simpleName

        /** RecyclerView の1列当たりの横幅(dp) */
        private const val COLUMN_DP = 592
    }
}
