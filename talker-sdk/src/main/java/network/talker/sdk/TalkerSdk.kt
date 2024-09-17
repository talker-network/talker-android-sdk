package network.talker.sdk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.sdk.localDatabase.Database
import network.talker.sdk.player.AudioPlayerService
import network.talker.sdk.webrtc.initializeMobileClient

internal class TalkerSdk : Application() {
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
        ).allowMainThreadQueries().build()
        initializeMobileClient(auth, applicationContext)
        val serviceIntent = Intent(this, AudioPlayerService::class.java)
        startForegroundService(serviceIntent)
    }
}