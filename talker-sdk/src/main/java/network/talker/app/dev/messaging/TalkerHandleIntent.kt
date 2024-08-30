package network.talker.app.dev.messaging

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParser
import network.talker.app.dev.R
import network.talker.app.dev.model.AudioModel
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.player.AudioPlayerService

fun handleIntent(intent: Intent?, context: Context) {
    val CHANNEL_NAME = "General"
    val CHANNEL_ID = "channel 1"
    val IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
    val channelDescription = "Channel for important notifications"
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
                class StartForegroundWorkManager(val context2: Context, params: WorkerParameters) : Worker(context2, params) {
                    override fun doWork(): Result {
                        try {
                            applicationContext.startForegroundService(Intent(context2, AudioPlayerService::class.java))
                            return Result.success()
                        }catch (e : Exception) {
                            e.printStackTrace()
                            return Result.failure()
                        }
                    }
                }
                val workRequestBuilder = OneTimeWorkRequestBuilder<StartForegroundWorkManager>()
                    .build()
                context.let {
                    WorkManager.getInstance(it).enqueue(workRequestBuilder)
                }
            }
            Log.d(
                "onMessageReceived : ",
                Gson().fromJson(
                    intent.extras?.getString("data") ?: "",
                    AudioModel::class.java
                ).toString()
//                Gson().fromJson(
//                    intent.extras?.getString("data") ?: "",
//                    Channel::class.java
//                ).toString()
            )

            try {
                val url = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["media_link"].asString
                val channelId = JsonParser.parseString(intent.extras?.getString("data")).asJsonObject["channel_id"].asString
//                val channel = NotificationChannel(
//                    CHANNEL_ID, CHANNEL_NAME, IMPORTANCE
//                )
//                channel.description = channelDescription
//                // create channel...
//
//                val notificationManager = context.getSystemService(NotificationManager::class.java)
//                notificationManager.createNotificationChannel(channel)
//                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//                    .setSmallIcon(R.drawable.ic_launcher_foreground)
//                    .setContentTitle("")
//                    .setContentText("Foreground service running...")
//                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
//                    .setOngoing(true)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH)
//                    .setWhen(System.currentTimeMillis())
//                    .setShowWhen(true)
//                    .setAutoCancel(false)
//                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
//                    .build()
//
//                // notify the user
//                notificationManager.notify(
//                    1,
//                    notification
//                )
                context.sendBroadcast(
                    Intent()
                        .setPackage(context.packageName)
                        .setAction("audio_player.sdk")
                        .apply {
                            putExtra("media_link", url)
                            putExtra("channel_id", channelId)
                            putExtra("channel_obj",
                            Gson().fromJson(
                                intent.extras?.getString("data") ?: "",
                                AudioModel::class.java
                            ))
                        }
                )
            } catch (e : Exception){
                e.printStackTrace()
            }
        }
    }
}