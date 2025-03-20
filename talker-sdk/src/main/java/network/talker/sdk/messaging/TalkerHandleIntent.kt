package network.talker.sdk.messaging

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParser
import network.talker.sdk.R
import network.talker.sdk.Talker
import network.talker.sdk.model.AudioModel
import network.talker.sdk.networking.data.MessageObject
import network.talker.sdk.networking.data.MessageObjectForLocalDB
import network.talker.sdk.player.AudioPlayerService
import kotlin.random.Random

fun Talker.processTalkerFcm(intent: Intent?, context: Context) {
    intent?.extras?.getString("data")?.let {
        Log.d(
            "onMessageReceived : ",
            JsonParser.parseString(it).asJsonObject.toString()
        )
    }
    if (intent?.extras?.getString("type") == "message") {
        Handler(Looper.getMainLooper()).post {
            val channelId = "messages"
            val messageObj =
                Gson().fromJson(intent.extras?.getString("data") ?: "", MessageObject::class.java)
            val messageObjLocalDB =
                Gson().fromJson(intent.extras?.getString("data") ?: "", MessageObjectForLocalDB::class.java)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = "Messages"
            val descriptionText = "Channel for messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("messages", name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Your notification icon
                .setContentTitle(
                    JsonParser.parseString(intent.extras?.getString("data")).asJsonObject.get("group_name").asString.ifEmpty {
                        messageObjLocalDB.channel_name
                    }
                )
                .setContentText(
                    messageObjLocalDB.sender_name + " : " +
                        if (messageObj.attachments.document?.isNotEmpty() == true) "Document"
                    else if (messageObj.attachments.images?.isNotEmpty() == true) "Image"
                    else messageObj.description
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setGroup(messageObj.sender_id) // Group key
                .build()

            // Display the notification
            notificationManager.notify(Random.nextInt(from = 10, Int.MAX_VALUE), notification)

            val summaryNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup(messageObj.sender_id) // Same group key as individual notifications
                .setGroupSummary(true) // Marks this as the summary notification
                .build()

            // Send the summary notification with a fixed ID
            notificationManager.notify(messageObj.sender_id.hashCode(), summaryNotification)
        }
    }else{
        // extract the music's link form intent and play it using exoplayer
        intent?.extras?.get("data")?.toString()?.let {
            Handler(Looper.getMainLooper()).post {
                val serviceManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningServices = serviceManager.getRunningServices(Int.MAX_VALUE)
                runningServices.forEach { service ->
                    println(
                        "Work Manager : ${service.service.className} ${AudioPlayerService::class.java.name}"
                    )
                }
                if (runningServices.none { service ->
                        service.service.className == AudioPlayerService::class.java.name
                    }){
                    context.startForegroundService(Intent(context, AudioPlayerService::class.java))
                }
                Log.d(
                    "onMessageReceived : ",
                    JsonParser.parseString(intent.extras?.getString("data") ?: "").asJsonObject.toString()
                )

                try {
                    val url = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["media_link"].asString
                    val channelId = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["channel_id"].asString
                    context.sendBroadcast(
                        Intent()
                            .setPackage(context.packageName)
                            .setAction("audio_player.sdk")
                            .apply {
                                putExtra("from_notification", true)
                                putExtra("media_link", url)
                                putExtra("channel_id", channelId)
                                putExtra("channel_obj",
                                    Gson().fromJson(
                                        intent.extras?.getString("data") ?: "",
                                        AudioModel::class.java
                                    )
                                )
                            }
                    )
                } catch (e : Exception){
                    e.printStackTrace()
                }
            }
        }
    }
}