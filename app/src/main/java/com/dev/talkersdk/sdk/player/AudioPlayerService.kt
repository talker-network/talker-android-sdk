package com.dev.talkersdk.sdk.player

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
    val mediaLinkList = mutableListOf<String>()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        val broadCastReceiver = object : BroadcastReceiver() {
            @OptIn(UnstableApi::class)
            override fun onReceive(context: Context?, intent: Intent?) {
                context ?: return
                val audioUrl = intent?.extras?.getString("media_link")
                Log.d(
                    "media_link",
                    "media_link : $audioUrl"
                )
                mediaLinkList.add(audioUrl ?: "")
                Handler(Looper.getMainLooper()).post {
                    if (player == null || mediaLinkList.size == 1){
                        playAudio()
                    }
                }
            }
        }
        super.onCreate()
        registerReceiver(broadCastReceiver, IntentFilter("audio_player.sdk"), RECEIVER_NOT_EXPORTED)
    }

    @OptIn(UnstableApi::class)
    private fun playAudio(){
        Log.d(
            "play_audio",
            "play_audio called..."
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        player = ExoPlayer.Builder(this@AudioPlayerService).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED){
                    mediaLinkList.removeAt(0)
                    if (mediaLinkList.isNotEmpty()){
                        playAudio()
                    }else{
                        notificationManager.cancel(1)
                    }
                }
            }
        })
        val hlsUri = Uri.parse(mediaLinkList[0])
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
            .build()
        startForeground(Random.nextInt(from = 2, until = Int.MAX_VALUE), notification)
        return START_STICKY
    }

    override fun onDestroy() {
//        super.onDestroy()
    }

}