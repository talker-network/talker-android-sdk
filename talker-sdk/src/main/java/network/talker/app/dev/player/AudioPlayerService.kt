package network.talker.app.dev.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.R
import network.talker.app.dev.Talker.eventListener
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.TalkerSdkBackgroundService
import network.talker.app.dev.model.AudioData
import network.talker.app.dev.model.AudioModel
import network.talker.app.dev.networking.data.Channel
import kotlin.random.Random

class AudioPlayerService : Service() {
    private var player: ExoPlayer? = null
    private val mediaLinkList = mutableListOf<String>()
    // first string is for channel id
    // second Array<String> is for storing list of media_links that we are going to play
    private val liveMsgQue : MutableMap<String, Array<AudioModel>> = mutableMapOf()
    // this contains the list of channel id's whose media_links are currently in the que for playing
    private val liveMsgPrty : MutableList<String> = mutableListOf()
    // this will store the current playing media link
    private var currentLiveMessage : AudioModel? = null
    private var fcmList = mutableSetOf<String>()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onCreate() {
        val broadCastReceiver = object : BroadcastReceiver() {
            @OptIn(UnstableApi::class)
            override fun onReceive(context: Context?, intent: Intent?) {
                context ?: return
                val audioUrl = intent?.extras?.getString("media_link") ?: ""
                val channelId = intent?.extras?.getString("channel_id") ?: ""

                val audioModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.extras?.getSerializable(
                        "channel_obj", AudioModel::class.java
                    )
                } else {
                    intent?.getSerializableExtra(
                        "channel_obj"
                    ) as AudioModel?
                }

                Log.d(
                    "media_link",
                    "media_link : $audioUrl"
                )
                Log.d(
                    "channel_id",
                    "channel_id : $channelId"
                )
                if (audioModel != null) {
                    // if it is coming from fcm then true else false
                    val fromNotification = intent?.extras?.getBoolean("from_notification") ?: false
                    if (fromNotification) {
                        // if it is coming from fcm
                        // then add the id to fcmList of ids
                        audioModel.id?.let {
                            fcmList.add(it)
                        }
                    }else{
                        // if not coming from fcm and the id of the audio
                        // already exists in fcm list and then it means
                        // it was already played or it has already been added in the queue
                        // so simply return
                        if (fcmList.contains(audioModel.id)) {
                            fcmList.remove(audioModel.id)
                            return
                        }
                    }
                    liveMsgQue[channelId] = liveMsgQue.getOrDefault(channelId, arrayOf()) + audioModel
                    liveMsgPrty.add(channelId)
                    nextLiveMsg()
                }
            }
        }
        super.onCreate()
        registerReceiver(broadCastReceiver, IntentFilter("audio_player.sdk"), RECEIVER_NOT_EXPORTED)
    }


    @OptIn(UnstableApi::class)
    private fun nextLiveMsg(){
        val CHANNEL_NAME = "General"
        val CHANNEL_ID = "channel 1"
        val IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
        val channelDescription = "Channel for important notifications"
        // Handle the received message
        val notificationTitle = "Audio message"
        val notificationBody = "Playing remote audio message..."

        if (currentLiveMessage == null){
            if (liveMsgPrty.isNotEmpty()){
                val channelId = liveMsgPrty[0]
                val q = liveMsgQue[channelId]?.toMutableList()
                if (q?.isNotEmpty() == true){
                    currentLiveMessage = q[0]
                    q.removeAt(0)
                    liveMsgQue[channelId] = q.toTypedArray()
                }
                liveMsgPrty.removeAt(0)
            }
        }

        currentLiveMessage?.let {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, IMPORTANCE
            )
            channel.description = channelDescription
            // create channel...
            val notificationManager = getSystemService(NotificationManager::class.java)
            currentLiveMessage!!.group_name?.let {
                notificationManager.createNotificationChannel(channel)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(currentLiveMessage!!.group_name)
                    .setContentText(currentLiveMessage!!.sender_name + " is talking")
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build()

                // notify the user
                notificationManager.notify(
                    1,
                    notification
                )
            }

            player = ExoPlayer.Builder(this@AudioPlayerService).build()
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED){
//                        if (liveMsgQue.isEmpty()){
                        currentLiveMessage?.group_name?.let {
                            val notificationReset = NotificationCompat.Builder(this@AudioPlayerService, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Talker SDK")
                                .setContentText("PTT mode on")
                                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                .setOngoing(true)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setWhen(System.currentTimeMillis())
                                .setShowWhen(true)
                                .setAutoCancel(false)
                                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                                .build()

                            // notify the user
                            notificationManager.notify(
                                1,
                                notificationReset
                            )
                        }
//                        }
                        currentLiveMessage = null
                        nextLiveMsg()
                    }
                }
            })
            CoroutineScope(Dispatchers.IO).launch {
                sendBroadcast(
                    Intent()
                        .setPackage(packageName)
                        .setAction("com.talker.sdk")
                        .apply {
                            putExtra("action", "CURRENT_PTT_AUDIO")
                            putExtra("message", "Current PTT audio playing.")
                            putExtra(
                                "ptt_audio", AudioData(
                                    currentLiveMessage?.sender_id ?: "",
                                    currentLiveMessage?.channel_id ?: "",
                                    (currentLiveMessage?.group_name ?: "").ifEmpty {
                                        TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId = (currentLiveMessage?.channel_id ?: ""))?.group_name ?: ""
                                    },
                                    (currentLiveMessage?.sender_name ?: "").ifEmpty {
                                        TalkerSdkBackgroundService.database.roomDao().getUserById(
                                            currentLiveMessage?.sender_id ?: ""
                                        )?.name ?: ""
                                    }
                                )
                            )
                        }
                )
            }
            val hlsUri = Uri.parse(currentLiveMessage!!.media_link)
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.fromUri(hlsUri)
            )
            player?.addMediaSource(hlsMediaSource)
            player?.prepare()
            player?.apply {
                playWhenReady = true
            }
        }
    }

//    @OptIn(UnstableApi::class)
//    private fun playAudio(){
//        Log.d(
//            "play_audio",
//            "play_audio called..."
//        )
//        val notificationManager = getSystemService(NotificationManager::class.java)
//        player = ExoPlayer.Builder(this@AudioPlayerService).build()
//        player?.addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                if (playbackState == Player.STATE_ENDED){
//                    mediaLinkList.removeAt(0)
//                    if (mediaLinkList.isNotEmpty()){
//                        playAudio()
//                    }else{
//                        notificationManager.cancel(1)
//                    }
//                }
//            }
//        })
//        val hlsUri = Uri.parse(mediaLinkList[0])
//        val dataSourceFactory = DefaultHttpDataSource.Factory()
//        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
//            MediaItem.fromUri(hlsUri)
//        )
//        player?.addMediaSource(hlsMediaSource)
//        player?.prepare()
//        player?.apply {
//            playWhenReady = true
//        }
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelId = "player"
        val channelName = "Audio Player"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Talker SDK")
            .setContentText("PTT mode on")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(1, notification)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
//        super.onDestroy()
    }

}