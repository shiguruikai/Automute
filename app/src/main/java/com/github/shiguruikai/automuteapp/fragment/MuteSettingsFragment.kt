package com.github.shiguruikai.automuteapp.fragment

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import com.github.shiguruikai.automuteapp.service.AutoMasterMuteService
import com.github.shiguruikai.automuteapp.util.isMasterMute
import com.github.shiguruikai.automuteapp.util.isUsageStatsAllowed
import com.github.shiguruikai.automuteapp.util.newIntent
import com.github.shiguruikai.automuteapp.util.setMasterMute
import com.github.shiguruikai.automuteapp.util.setOnPreferenceChangeListener
import com.github.shiguruikai.automuteapp.util.toastMuteState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MuteSettingsFragment : PreferenceFragmentCompat() {

    private val audioManager by lazy { requireActivity().getSystemService<AudioManager>()!! }

    private val manualMute by lazy {
        findPreference<SwitchPreferenceCompat>(getString(R.string.manualMute))!!
    }

    private val startAutoMuteService by lazy {
        findPreference<SwitchPreferenceCompat>(getString(R.string.startAutoMuteService))!!
    }

    private val startAfterReboot by lazy {
        findPreference<CheckBoxPreference>(getString(R.string.startAfterReboot))!!
    }

    private val startAfterUpdate by lazy {
        findPreference<CheckBoxPreference>(getString(R.string.startAfterUpdate))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_mute, rootKey)

        lifecycleScope.launch {
            while (isActive) {
                updatePreferencesState()
                delay(CHECK_STATE_INTERVAL)
            }
        }

        // 手動ミュートを実行
        manualMute.setOnPreferenceChangeListener { newValue ->
            audioManager.setMasterMute(newValue)
            if (defaultSharedPreferences.toastMuteChange) {
                requireActivity().toastMuteState(audioManager)
            }
            true
        }

        // 自動ミュートサービスの起動
        startAutoMuteService.setOnPreferenceChangeListener { newValue ->
            if (newValue) {
                if (requireActivity().isUsageStatsAllowed()) {
                    startAutoMuteService()
                } else {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivityForResult(intent, ACTION_USAGE_ACCESS_SETTINGS_REQUEST_CODE)
                    return@setOnPreferenceChangeListener false
                }
            } else {
                stopMasterMuteService()
            }

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_USAGE_ACCESS_SETTINGS_REQUEST_CODE && requireActivity().isUsageStatsAllowed()) {
            startAutoMuteService()
        }
    }

    private fun updatePreferencesState() {
        manualMute.isChecked = audioManager.isMasterMute()
        startAutoMuteService.isChecked = AutoMasterMuteService.isRunning
        startAfterReboot.isEnabled = startAutoMuteService.isChecked
        startAfterUpdate.isEnabled = startAutoMuteService.isChecked
    }

    private fun startAutoMuteService() {
        requireActivity().apply {
            ContextCompat.startForegroundService(this, newIntent<AutoMasterMuteService>())
        }
    }

    private fun stopMasterMuteService() {
        requireActivity().apply {
            stopService(newIntent<AutoMasterMuteService>())
        }
    }

    companion object {
        private val TAG = MuteSettingsFragment::class.java.simpleName

        private const val ACTION_USAGE_ACCESS_SETTINGS_REQUEST_CODE = 1

        private const val CHECK_STATE_INTERVAL = 200L
    }
}
