package com.github.shiguruikai.automuteapp.fragment

import android.Manifest
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
import com.github.shiguruikai.automuteapp.util.allPermissionsGranted
import com.github.shiguruikai.automuteapp.util.isMasterMute
import com.github.shiguruikai.automuteapp.util.isUsageStatsAllowed
import com.github.shiguruikai.automuteapp.util.newIntent
import com.github.shiguruikai.automuteapp.util.setMasterMute
import com.github.shiguruikai.automuteapp.util.setOnPreferenceChangeListener
import com.github.shiguruikai.automuteapp.util.singleToast
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

    /**
     * 着信時にミュートを解除するためには、電話の権限が必要。
     * 電話の権限がない場合、チェックが付かないようにする。
     */
    private val unmuteOnIncomingCall by lazy {
        findPreference<CheckBoxPreference>(getString(R.string.unmuteOnIncomingCall))!!
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

        unmuteOnIncomingCall.setOnPreferenceChangeListener { newValue ->
            if (newValue && !isPhonePermissionGranted()) {
                // 電話の権限がない場合、権限をリクエスト
                requestPermissions(
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    REQUEST_CODE_UNMUTE_ON_INCOMING_CALL
                )
                false
            } else {
                true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_USAGE_ACCESS_SETTINGS_REQUEST_CODE && requireActivity().isUsageStatsAllowed()) {
            startAutoMuteService()
        }
    }

    private fun updatePreferencesState() {
        manualMute.isChecked = audioManager.isMasterMute()

        AutoMasterMuteService.isRunning.also {
            startAutoMuteService.isChecked = it
            startAfterReboot.isEnabled = it
            startAfterUpdate.isEnabled = it
        }

        // 電話の権限がない場合、チェックを外す
        if (unmuteOnIncomingCall.isChecked && !isPhonePermissionGranted()) {
            unmuteOnIncomingCall.isChecked = false
        }
    }

    private fun isPhonePermissionGranted(): Boolean {
        return requireActivity().allPermissionsGranted(Manifest.permission.READ_PHONE_STATE)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // 電話の権限が付与された場合、チェックを付ける
        if (requestCode == REQUEST_CODE_UNMUTE_ON_INCOMING_CALL) {
            if (isPhonePermissionGranted()) {
                unmuteOnIncomingCall.isChecked = true
            } else {
                requireActivity().singleToast("Permissions not granted.")
            }
        }
    }

    companion object {
        private val TAG = MuteSettingsFragment::class.java.simpleName

        private const val ACTION_USAGE_ACCESS_SETTINGS_REQUEST_CODE = 1

        private const val REQUEST_CODE_UNMUTE_ON_INCOMING_CALL = 11

        private const val CHECK_STATE_INTERVAL = 200L
    }
}
