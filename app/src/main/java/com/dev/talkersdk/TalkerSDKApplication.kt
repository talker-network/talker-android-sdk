package com.dev.talkersdk

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import com.amazonaws.mobile.client.AWSMobileClient
import com.dev.talkersdk.player.AudioPlayerService
import com.dev.talkersdk.webrtc.initializeMobileClient

class TalkerSDKApplication : Application() {
    val auth : AWSMobileClient
        get() = AWSMobileClient.getInstance()

    override fun onCreate() {
        super.onCreate()
        initializeMobileClient(auth, applicationContext)
        val serviceIntent = Intent(this, AudioPlayerService::class.java)
        startForegroundService(serviceIntent)
    }
}