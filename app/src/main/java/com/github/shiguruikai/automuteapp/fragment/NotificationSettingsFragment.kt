package com.github.shiguruikai.automuteapp.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.util.createNotificationChannel
import com.github.shiguruikai.automuteapp.util.setOnPreferenceChangeListener

class NotificationSettingsFragment : PreferenceFragmentCompat() {

    private val openNotificationSettings by lazy {
        findPreference<Preference>(getString(R.string.openNotificationSettings))!!
    }

    private val toastMuteChange by lazy {
        findPreference<CheckBoxPreference>(getString(R.string.toastMuteChange))!!
    }

    private val shortDurationToast by lazy {
        findPreference<CheckBoxPreference>(getString(R.string.shortDurationToast))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_notification, rootKey)

        shortDurationToast.isEnabled = toastMuteChange.isChecked

        toastMuteChange.setOnPreferenceChangeListener { newValue ->
            shortDurationToast.isEnabled = newValue
            true
        }

        openNotificationSettings.setOnPreferenceClickListener {
            val intent = Intent()
            val context = requireContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 通知チャンネルの設定画面を開く
                val channel = context.createNotificationChannel()
                intent.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel.id)
            } else {
                val uri = Uri.parse("package:" + context.packageName)
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = uri
            }
            startActivity(intent)

            true
        }
    }

    companion object {
        private val TAG = NotificationSettingsFragment::class.java.simpleName
    }
}
