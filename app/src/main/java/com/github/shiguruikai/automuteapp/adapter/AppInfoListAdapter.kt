package com.github.shiguruikai.automuteapp.adapter

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.lruCache
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.data.AppInfo
import kotlinx.android.synthetic.main.list_item_app_info.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AppInfoListAdapter(
    context: Context,
    private val scope: CoroutineScope,
    private val onItemClickListener: OnItemClickListener? = null
) : ListAdapter<AppInfo, AppInfoListAdapter.ViewHolder>(DiffCallback) {

    interface OnItemClickListener {
        fun onItemClick(appInfo: AppInfo)
    }

    /**
     * itemView のクリックで checkBox の状態が切り替わるようにする。
     * checkBox は android:clickable="false" にするなどして、タッチイベントを下のビューに伝播させろ。
     */
    private val itemViewOnClickListener = View.OnClickListener {
        it.checkBox.toggle()

        onItemClickListener?.onItemClick(it.tag as AppInfo)
    }

    private val packageManager = context.packageManager

    private val iconLoaderCache = lruCache<String, Deferred<Bitmap?>>(
        maxSize = APP_ICON_CACHE_SIZE,
        onEntryRemoved = { _, _, oldValue, _ ->
            oldValue.cancel()
        }
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_app_info, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = getItem(position)
        holder.bind(appInfo)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.loadIconJob?.cancel()
    }

    private fun getApplicationIconBitmap(packageName: String): Bitmap? {
        return try {
            packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load app icon from package name, was $packageName")
            null
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = itemView.icon_imageView
        private val label: TextView = itemView.label_textView
        private val name: TextView = itemView.name
        private val checkBox: CheckBox = itemView.checkBox

        var loadIconJob: Job? = null

        fun bind(appInfo: AppInfo) {
            val packageName = appInfo.packageName

            // アプリアイコンをクリア
            icon.setImageDrawable(null)

            // キャッシュからアイコンローダーを取得
            var iconLoader = iconLoaderCache.get(packageName)

            // アイコンローダーが無い、または、キャンセルされた場合、
            // 新たにアイコンローダーをキャッシュする
            if (iconLoader == null || iconLoader.isCancelled) {
                iconLoader = scope.async(Dispatchers.IO) {
                    getApplicationIconBitmap(packageName)
                }
                @Suppress("DeferredResultUnused") // 前回キャッシュした値は使わないため
                iconLoaderCache.put(packageName, iconLoader)
            }

            // アプリアイコンを取得して表示させる
            loadIconJob = scope.launch(Dispatchers.Main) {
                val bitmap = iconLoader.await()
                if (bitmap != null) {
                    icon.setImageBitmap(bitmap)
                }
            }

            label.text = appInfo.label
            name.text = appInfo.activityName ?: appInfo.name

            // チェックボックスの状態をデータに反映させる
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = appInfo.isChecked
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                appInfo.isChecked = isChecked
            }

            itemView.tag = appInfo
            itemView.setOnClickListener(itemViewOnClickListener)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean = oldItem == newItem
    }

    companion object {
        private val TAG = AppInfoListAdapter::class.java.simpleName

        private const val APP_ICON_CACHE_SIZE = 128
    }
}
