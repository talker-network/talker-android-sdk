package com.dev.talkersdk.sdk

import android.app.Application
import android.content.Intent
import com.amazonaws.mobile.client.AWSMobileClient
import com.dev.talkersdk.sdk.player.AudioPlayerService
import com.dev.talkersdk.sdk.webrtc.initializeMobileClient

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