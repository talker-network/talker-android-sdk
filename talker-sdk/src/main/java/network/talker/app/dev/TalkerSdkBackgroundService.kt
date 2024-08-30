package network.talker.app.dev

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.app.dev.localDatabase.Database
import network.talker.app.dev.player.AudioPlayerService
import network.talker.app.dev.webrtc.initializeMobileClient

internal class TalkerSdkBackgroundService : Application() {
    val auth : AWSMobileClient
        get() = AWSMobileClient.getInstance()

    companion object {
        lateinit var database : Database
        lateinit var talkerApplicationContext : Context
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        talkerApplicationContext = applicationContext
        database = Room.databaseBuilder(
            applicationContext,
            Database::class.java,
            "talker_database"
        ).build()
        initializeMobileClient(auth, applicationContext)
        val serviceIntent = Intent(this, AudioPlayerService::class.java)
        startForegroundService(serviceIntent)
    }
}