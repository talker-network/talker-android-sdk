package network.talker.app.dev.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.JsonParser
import network.talker.app.dev.R
import network.talker.app.dev.Talker

fun Talker.handleIntent(intent: Intent?, context: Context) {
    val CHANNEL_NAME = "General"
    val CHANNEL_ID = "channel 1"
    val IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
    val channelDescription = "Channel for important notifications"
    // extract the music's link form intent and play it using exoplayer
    intent?.extras?.get("data")?.toString()?.let {
        Handler(Looper.getMainLooper()).post {
            Log.d(
                "onMessageReceived : ",
                intent.extras?.getString("data").toString()
            )
            // Handle the received message
            val notificationTitle = "Audio message"
            val notificationBody = "Playing remote audio message..."
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, IMPORTANCE
            )
            channel.description = channelDescription
            // create channel...
            notificationManager.createNotificationChannel(channel)

            // build notification...
            val notificationBuilder = NotificationCompat.Builder(
                context,
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

            try {
                val url = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["media_link"].asString
                val channelId = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["channel_id"].asString
                context.sendBroadcast(
                    Intent()
                        .setPackage(context.packageName)
                        .setAction("audio_player.sdk")
                        .apply {
                            putExtra("media_link", url)
                            putExtra("channel_id", channelId)
                        }
                )
            } catch (e : Exception){
                e.printStackTrace()
            }
        }
    }
}