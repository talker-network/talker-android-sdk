package network.talker.app.dev.socket

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.sharedPreference.SharedPreference
import network.talker.app.dev.Talker
import com.google.gson.JsonParser
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

internal object SocketHandler {
    private var socket: Socket? = null
    fun setSocket(auth: String) {
        try {
            val socketManager = Manager(
                URI("https://test-api.talker.network/"),
                IO.Options()
                    .apply {
                        extraHeaders = mapOf(
                            "Authorization" to listOf(auth)
                        )
                        forceNew = true
                        reconnection = true
                        transports = arrayOf("websocket")

                    }
            )
            socket = socketManager.socket(
                "/sockets"
            )
        } catch (e: URISyntaxException) {
            // Handle the exception more gracefully, e.g., log it or notify the user
            println("$LOG_TAG Error establishing connection: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    fun establishConnection(context: Context) {
        val sharedPreference = SharedPreference(context)
        val userData = sharedPreference.getUserData()
        socket?.connect()
        socket?.on(
            Socket.EVENT_CONNECT
        ) {
            Log.d(LOG_TAG, "Socket connected")
        }
        socket?.on(
            Socket.EVENT_DISCONNECT
        ) {
            Log.d(LOG_TAG, "Socket EVENT_DISCONNECT")
        }
        socket?.on(
            Socket.EVENT_CONNECT_ERROR
        ) {
            Log.d(LOG_TAG, "Socket EVENT_CONNECT_ERROR")
        }
        socket?.on(
            "broadcast_start"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        val url = JsonParser.parseString(arg).asJsonObject["media_link"].asString
                        val senderId =
                            JsonParser.parseString(arg).asJsonObject["sender_id"].asString
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "media_link : $url"
                            )
                        }
                        Log.d(
                            LOG_TAG,
                            "senderId aksjdlkjsdl: $senderId"
                        )
                        Log.d(
                            LOG_TAG,
                            "userId aksjdlkjsdl: ${userData.user_id}"
                        )
                        // if the audio sent from server to us is not the one
                        // which we are currently playing speaking than only play that audio
                        if (senderId != userData.user_id) {
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("audio_player.sdk")
                                    .apply {
                                        putExtra("media_link", url)
                                    }
                            )
                        }

                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(LOG_TAG, "Socket broadcast_start channel object : $arg")
                        }
                    }
                }
            }
        }
        socket?.on(
            "broadcast_end"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket broadcast_end channel object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket broadcast_end error : ${e.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun closeConnection() {
        if (!Talker.isUserLoggedIn()) {
            throw Exception("Kindly initialize Talker with SDK key or Api Key")
        }
        if (socket?.connected() == true || socket?.isActive == true) {
            socket?.disconnect()
            socket?.off()
            socket?.close()
        }
    }

    fun broadCastStart(channelID: String, onAck: (Array<Any>) -> Unit) {
        if (!Talker.isUserLoggedIn()) {
            throw Exception("Kindly initialize Talker with SDK key or Api Key")
        }
        val payload = JSONObject().apply {
            put("channel_id", channelID)
        }
        if (socket?.connected() == true || socket?.isActive == true) {
            socket?.emit("broadcast_start", payload, Ack { args ->
                onAck(args)
            })
        }
    }

    fun broadCastStop(channelID: String) {
        if (!Talker.isUserLoggedIn()) {
            throw Exception("Kindly initialize Talker with SDK key or Api Key")
        }
        val payload = JSONObject().apply {
            put("channel_id", channelID)
        }
        if (socket?.connected() == true || socket?.isActive == true) {
            socket?.emit("broadcast_end", payload)
        }
    }
}