package network.talker.app.dev.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import network.talker.app.dev.R
import kotlin.random.Random

class AudioPlayerService : Service() {
    private var player: ExoPlayer? = null
    private val mediaLinkList = mutableListOf<String>()
    // first string is for channel id
    // second Array<String> is for storing list of media_links that we are going to play
    private val liveMsgQue : MutableMap<String, Array<String>> = mutableMapOf()
    // this contains the list of channel id's whose media_links are currently in the que for playing
    private val liveMsgPrty : MutableList<String> = mutableListOf()
    // this will store the current playing media link
    private var currentLiveMessage : String? = null
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
                Log.d(
                    "media_link",
                    "media_link : $audioUrl"
                )
                Log.d(
                    "channel_id",
                    "channel_id : $channelId"
                )
                liveMsgQue[channelId] = liveMsgQue.getOrDefault(channelId, arrayOf()) + audioUrl
                liveMsgPrty.add(channelId)
                nextLiveMsg()
            }
        }
        super.onCreate()
        registerReceiver(broadCastReceiver, IntentFilter("audio_player.sdk"), RECEIVER_NOT_EXPORTED)
    }


    @OptIn(UnstableApi::class)
    private fun nextLiveMsg(){
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
//            https://talker-dev.s3.us-east-2.amazonaws.com/9cd12929-ebec-4140-9d0b-39371668ce6c.m3u8
            val notificationManager = getSystemService(NotificationManager::class.java)
            player = ExoPlayer.Builder(this@AudioPlayerService).build()
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED){
                        if (liveMsgQue.isEmpty()){
                            notificationManager.cancel(1)
                        }
                        currentLiveMessage = null
                        nextLiveMsg()
                    }
                }
            })
            val hlsUri = Uri.parse(currentLiveMessage)
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
            .setContentText("Foreground service running...")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(Random.nextInt(from = 2, until = Int.MAX_VALUE), notification)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
//        super.onDestroy()
    }

}