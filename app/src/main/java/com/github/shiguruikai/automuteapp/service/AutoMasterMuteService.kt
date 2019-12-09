package com.github.shiguruikai.automuteapp.service

import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.shiguruikai.automuteapp.MainActivity
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.defaultSharedPreferences
import com.github.shiguruikai.automuteapp.receiver.ScreenStateReceiver
import com.github.shiguruikai.automuteapp.secretSharedPreferences
import com.github.shiguruikai.automuteapp.util.NOTIFICATION_CHANNEL_ID
import com.github.shiguruikai.automuteapp.util.createNotificationChannel
import com.github.shiguruikai.automuteapp.util.isMasterMute
import com.github.shiguruikai.automuteapp.util.newIntent
import com.github.shiguruikai.automuteapp.util.setMasterMute
import com.github.shiguruikai.automuteapp.util.toastMuteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoMasterMuteService :
    LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val audioManager by lazy { getSystemService<AudioManager>()!! }

    private var selectedPackageNames = emptySet<String>()
    private var selectedActivityNames = emptySet<String>()

    private var autoMuteJob: Job? = null

    private val screenStateReceiver = ScreenStateReceiver().apply {
        onScreenOn = {
            autoMuteJob?.cancel()
            autoMuteJob = startAutoMuteJob()
        }
        onScreenOff = {
            autoMuteJob?.cancel()
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter)
        secretSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        selectedPackageNames = secretSharedPreferences.selectedPackageNames
        selectedActivityNames = secretSharedPreferences.selectedActivityNames

        startNotification()

        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        autoMuteJob?.cancel()
        autoMuteJob = startAutoMuteJob()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(screenStateReceiver)
        secretSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        // サービスが終了したら、マスターミュートを解除する
        audioManager.setMasterMute(false)

        isRunning = false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            secretSharedPreferences::selectedPackageNames.name  -> {
                selectedPackageNames = secretSharedPreferences.selectedPackageNames
            }
            secretSharedPreferences::selectedActivityNames.name -> {
                selectedActivityNames = secretSharedPreferences.selectedActivityNames
            }
        }
    }

    private fun startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notifyIntent = newIntent<MainActivity>().also {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.auto_mute_service_running))
            .setSmallIcon(R.drawable.ic_stat_automute)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startAutoMuteJob(): Job = lifecycleScope.launch(Dispatchers.Default) {
        val usageStatsManager = getSystemService<UsageStatsManager>()!!

        while (isActive) {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - QUERY_EVENTS_TIME_RANGE

            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)

            if (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                var lastResumedPackageName: String? = null
                var lastResumedClassName: String? = null

                while (usageEvents.getNextEvent(event)) {
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastResumedPackageName = event.packageName
                        lastResumedClassName = event.className
                    }
                }

                if (lastResumedPackageName != null && lastResumedClassName != null) {
                    val isMuted = audioManager.isMasterMute()
                    val shouldMute = (lastResumedPackageName in selectedPackageNames
                            || lastResumedClassName in selectedActivityNames)

                    if (isMuted xor shouldMute) {
                        audioManager.setMasterMute(shouldMute)

                        if (defaultSharedPreferences.toastMuteChange) {
                            toastMuteState(audioManager)
                        }
                    }
                }
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(USAGE_MONITORING_INTERVAL)
        }
    }

    companion object {
        private val TAG = AutoMasterMuteService::class.java.simpleName

        /** アプリの前面化をモニタリングする間隔(ミリ秒) */
        private const val USAGE_MONITORING_INTERVAL = 600L

        /** [UsageStatsManager.queryEvents] で照会する時間(ミリ秒)の範囲 */
        private const val QUERY_EVENTS_TIME_RANGE = 1000L

        /** [AutoMasterMuteService] が起動しているかどうか。 */
        var isRunning = false; private set
    }
}
