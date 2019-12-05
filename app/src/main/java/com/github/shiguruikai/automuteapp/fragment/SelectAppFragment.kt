package com.github.shiguruikai.automuteapp.fragment

import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
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
import com.github.shiguruikai.automuteapp.util.afterMeasured
import com.github.shiguruikai.automuteapp.util.iin
import com.github.shiguruikai.automuteapp.util.queryTextAsFlow
import kotlinx.android.synthetic.main.fragment_select_app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SelectAppFragment : Fragment() {

    private val dataFragment by lazy {
        val tag = "data"
        val fm = childFragmentManager
        fm.findFragmentByTag(tag) as DataFragment? ?: run {
            DataFragment().also { fm.beginTransaction().add(it, tag).commit() }
        }
    }

    private var appInfoList: ArrayList<AppInfo>
        get() = dataFragment.appInfoList
        set(value) {
            dataFragment.appInfoList = value
        }

    private val appInfoListAdapter by lazy { AppInfoListAdapter(requireActivity(), lifecycleScope) }

    private var adapterLayoutState: Parcelable? = null
    private var adapterLayoutManager: RecyclerView.LayoutManager? = null
    private var searchView: SearchView? = null
    private var lastSearchQueryText: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(::lastSearchQueryText.name, searchView?.query?.toString())
        adapterLayoutManager?.let {
            outState.putParcelable(::adapterLayoutState.name, it.onSaveInstanceState())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            lastSearchQueryText = savedInstanceState.getString(::lastSearchQueryText.name)
            adapterLayoutState = savedInstanceState.getParcelable(::adapterLayoutState.name)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_app, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            // RecycleView にリストアダプターをセット
            app_info_recyclerView.adapter = appInfoListAdapter

            // RecycleView のレイアウト完了後にレイアウトマネージャをセットする。
            // このエラーログは無視して問題ない "E/RecyclerView: No layout manager attached; skipping layout"
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

            // 前回の検索クエリがあればセットする
            if (!lastSearchQueryText.isNullOrEmpty()) {
                searchMenuItem.expandActionView()
                sv.setQuery(lastSearchQueryText, false)
                sv.clearFocus()
            }

            // アプリリストの表示と検索処理
            lifecycleScope.launch {
                // アプリリストが未取得の場合、取得する
                if (appInfoList.isEmpty()) {
                    showLoadingProgress()
                    appInfoList = getInstalledAppInfoList()
                    hideLoadingProgress()
                }

                sv.queryTextAsFlow().collectLatest { newText ->
                    // 検索クエリに応じて、アプリリストを検索して表示する
                    appInfoListAdapter.submitList(searchAppInfoList(newText))

                    // 前回の検索クエリと現在の検索クエリが等しい場合、前回のリストの状態を復元する
                    if (lastSearchQueryText != null && lastSearchQueryText == newText) {
                        adapterLayoutManager?.let { layoutManager ->
                            layoutManager.onRestoreInstanceState(adapterLayoutState)
                            lastSearchQueryText = null
                        }
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
            val selectedAppNames = appInfoList.asSequence().filter { it.isChecked }.map { it.name }.toSet()
            saveSelectedAppNames(selectedAppNames)
        }
    }

    private fun showLoadingProgress() {
        progressBar.visibility = View.VISIBLE
        progress_textView.visibility = View.VISIBLE
    }

    private fun hideLoadingProgress() {
        progressBar.visibility = View.GONE
        progress_textView.visibility = View.GONE
    }

    private suspend fun searchAppInfoList(query: String): List<AppInfo> = withContext(Dispatchers.Default) {
        return@withContext if (query.isBlank()) {
            appInfoList
        } else {
            val words = query.split(' ', '　')

            appInfoList.asFlow()
                .filter { words.all { w -> w iin it.label || w iin it.name } }
                .toList()
        }
    }

    abstract fun saveSelectedAppNames(selectedAppNames: Set<String>)

    abstract suspend fun getInstalledAppInfoList(): ArrayList<AppInfo>

    class DataFragment : Fragment() {

        var appInfoList: ArrayList<AppInfo> = ArrayList(0)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }
    }

    companion object {
        private val TAG = SelectAppFragment::class.java.simpleName

        /** RecyclerView の1列当たりの横幅(dp) */
        private const val COLUMN_DP = 592
    }
}
