package com.dev.talkersdk.sdk.webrtc

import android.app.NotificationChannel
import android.app.NotificationManager

private fun createNotificationChannel(
    channel_id : String,
    notificationManager: NotificationManager
) {
        val name: CharSequence = "KVS WebRTC Data Channel Notification Channel"
        val description = "This channel shows all messages coming from WebRTC."
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(
            channel_id, name, importance
        )
        channel.description = description
//        val notificationManager = getSystemService(
//            NotificationManager::class.java
//        )
        notificationManager.createNotificationChannel(channel)
    }