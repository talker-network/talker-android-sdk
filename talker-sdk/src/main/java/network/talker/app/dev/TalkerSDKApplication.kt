package network.talker.app.dev

import android.app.Application
import android.content.Intent
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.app.dev.player.AudioPlayerService
import network.talker.app.dev.webrtc.initializeMobileClient

internal class TalkerSDKApplication : Application() {
    val auth : AWSMobileClient
        get() = AWSMobileClient.getInstance()

    override fun onCreate() {
        super.onCreate()
        initializeMobileClient(auth, applicationContext)
        val serviceIntent = Intent(this, AudioPlayerService::class.java)
        startForegroundService(serviceIntent)
    }
}