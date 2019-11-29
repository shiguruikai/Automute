package com.github.shiguruikai.automuteapp.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.lruCache
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.data.AppInfo
import kotlinx.android.synthetic.main.list_item_app_info.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val iconCache = lruCache<String, Drawable>(
        maxSize = APP_ICON_CACHE_SIZE,
        create = {
            try {
                packageManager.getApplicationIcon(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load app icon from package name, was $it")
                null
            }
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = itemView.icon_imageView
        private val label: TextView = itemView.label_textView
        private val packageName: TextView = itemView.package_name
        private val checkBox: CheckBox = itemView.checkBox

        private var loadIconJob: Job? = null

        fun bind(appInfo: AppInfo) {
            // アプリアイコンを非同期で読み込む
            loadIconJob?.cancel()
            loadIconJob = scope.launch(Dispatchers.Main) {
                icon.setImageDrawable(null)
                val drawable = withContext(Dispatchers.IO) {
                    iconCache.get(appInfo.packageName)
                }
                icon.setImageDrawable(drawable)
            }

            label.text = appInfo.label
            packageName.text = appInfo.packageName

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

        private const val APP_ICON_CACHE_SIZE = 256
    }
}
