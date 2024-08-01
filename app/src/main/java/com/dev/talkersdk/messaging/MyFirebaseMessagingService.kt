package com.dev.talkersdk.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import network.talker.app.dev.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonParser
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val CHANNEL_NAME = "General"
    private val CHANNEL_ID = "channel 1"
    private val IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
    private val channelDescription = "Channel for important notifications"
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(
            "onMessageReceived : ",
            "${remoteMessage.notification?.title}"
        )
//        handleIntent(remoteMessage.toIntent())
    }

    @OptIn(UnstableApi::class)
    override fun handleIntent(intent: Intent?) {
        Log.d(
            "onMessageReceived : ",
            intent?.extras?.getString("type").toString()
        )
        // Handle the received message
        val notificationTitle = "Audio message"
        val notificationBody = "Playing remote audio message..."
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, IMPORTANCE
        )
        channel.description = channelDescription
        // create channel...
        notificationManager.createNotificationChannel(channel)

        // build notification...
        val notificationBuilder = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // notify the user
        notificationManager.notify(
            1,
            notificationBuilder.build()
        )

        // extract the music's link form intent and play it using exoplayer
        intent?.extras?.get("data")?.toString()?.let {
            Handler(Looper.getMainLooper()).post {
                val url = JsonParser.parseString(intent?.extras?.get("data")?.toString()).asJsonObject["media_link"].asString
                sendBroadcast(
                    Intent()
                        .setPackage(this.packageName)
                        .setAction("audio_player.sdk")
                        .apply {
                            putExtra("media_link", url)
                        }
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(
            "FCM",
            "FCM TOKEN : $token"
        )
    }
}