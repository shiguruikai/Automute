package com.github.shiguruikai.automuteapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

const val NOTIFICATION_CHANNEL_ID = "automute_service_channel"
const val NOTIFICATION_CHANNEL_NAME = "Automute Service"

@RequiresApi(Build.VERSION_CODES.O)
fun Context.createNotificationChannel(): NotificationChannel {
    val notificationManager = NotificationManagerCompat.from(this)
    var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
    if (channel == null) {
        channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
    return channel
}
