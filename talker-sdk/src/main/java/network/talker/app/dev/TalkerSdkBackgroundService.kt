package network.talker.app.dev

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.app.dev.localDatabase.Database
import network.talker.app.dev.networking.data.Cognito
import network.talker.app.dev.player.AudioPlayerService
import network.talker.app.dev.sharedPreference.SharedPreference
import network.talker.app.dev.webrtc.initializeMobileClient

internal class TalkerSdkBackgroundService : Application() {

    companion object {
        lateinit var auth : AWSMobileClient
        lateinit var database : Database
        lateinit var talkerApplicationContext : Context
        fun initAWS(config: Cognito) {
            initializeMobileClient(auth, this.talkerApplicationContext, config)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        talkerApplicationContext = applicationContext
        auth = AWSMobileClient.getInstance()
        database = Room.databaseBuilder(
            applicationContext,
            Database::class.java,
            "talker_database"
        ).allowMainThreadQueries().build()
        val serviceIntent = Intent(this, AudioPlayerService::class.java)
        startForegroundService(serviceIntent)
        val sharedPreference = SharedPreference(this)
        println("Prev User Id : ${sharedPreference.getPrevUserId()}")
    }
}