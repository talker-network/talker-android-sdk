package network.talker.app.dev.messaging

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParser
import network.talker.app.dev.Talker
import network.talker.app.dev.model.AudioModel
import network.talker.app.dev.player.AudioPlayerService

fun Talker.processTalkerFcm(intent: Intent?, context: Context) {
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
                Gson().fromJson(
                    intent.extras?.getString("data") ?: "",
                    AudioModel::class.java
                ).toString()
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