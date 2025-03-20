package network.talker.sdk

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.sdk.localDatabase.Database
import network.talker.sdk.networking.data.Cognito
import network.talker.sdk.player.AudioPlayerService
import network.talker.sdk.sharedPreference.SharedPreference
import network.talker.sdk.webrtc.initializeMobileClient

internal object TalkerSdkBackgroundService {

//    companion object {
//
//    }

    lateinit var auth : AWSMobileClient
    lateinit var database : Database
    lateinit var talkerApplicationContext : Context
    fun initAWS(config: Cognito) {
        initializeMobileClient(auth, this.talkerApplicationContext, config)
    }
    fun init(applicationContext: Context) {
        talkerApplicationContext = applicationContext
        auth = AWSMobileClient.getInstance()
        database = Room.databaseBuilder(
            applicationContext,
            Database::class.java,
            "talker_database"
        ).allowMainThreadQueries().build()
        val serviceIntent = Intent(applicationContext, AudioPlayerService::class.java)
        applicationContext.startForegroundService(serviceIntent)
        val sharedPreference = SharedPreference(applicationContext)
        println("Prev User Id : ${sharedPreference.getPrevUserId()}")
    }
//    override fun onCreate() {
//        super.onCreate()
//
//    }
}