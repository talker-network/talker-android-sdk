package network.talker.app.dev.socket

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.google.gson.JsonParser
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.Talker
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.sharedPreference.SharedPreference
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

internal object SocketHandler {
    private var socket: Socket? = null
    fun setSocket(auth: String, context: Context) {
        try {
            val socketManager = Manager(
                URI("https://test-api.talker.network/"),
                IO.Options()
                    .apply {
                        extraHeaders = mapOf(
                            "Authorization" to listOf(auth)
                        )
                        forceNew = true
                        reconnection = false
                        transports = arrayOf("websocket")

                    }
            )
            socket = socketManager.socket(
                "/sockets"
            )
        } catch (e: URISyntaxException) {
            context.sendBroadcast(
                Intent()
                    .setPackage(context.packageName)
                    .setAction("com.talker.sdk")
                    .apply {
                        putExtra("action", "CONNECTION_FAILURE")
                        putExtra("message", e.localizedMessage)
                        putExtra(
                            "failure_from", "SOCKET"
                        )
                    }
            )
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
            context.sendBroadcast(
                Intent()
                    .setPackage(context.packageName)
                    .setAction("com.talker.sdk")
                    .apply {
                        putExtra("action", "CONNECTION_FAILURE")
                        putExtra("message", "Websocket connection error")
                        putExtra(
                            "failure_from", "SOCKET"
                        )
                    }
            )
            Log.d(LOG_TAG, "Socket EVENT_CONNECT_ERROR")
        }
        socket?.on(
            "broadcast_start"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        val url = JsonParser.parseString(arg).asJsonObject["media_link"].asString
                        val channelId = JsonParser.parseString(arg).asJsonObject["channel_id"].asString
                        val senderId =
                            JsonParser.parseString(arg).asJsonObject["sender_id"].asString
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "media_link : $url"
                            )
                            Log.d(
                                LOG_TAG,
                                "channel_id : $channelId"
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
                                        putExtra("channel_id", channelId)
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
            System.err.println(
                "Kindly initialize Talker with SDK key or Api Key"
            )
        }else{
//            if (socket?.connected() == true || socket?.isActive == true) {
                socket?.disconnect()
                socket?.off()
                socket?.close()
//            }
        }

    }

    fun broadCastStart(channelID: String, onAck: (Array<Any>) -> Unit) {
        if (!Talker.isUserLoggedIn()) {
            System.err.println(
                "Kindly initialize Talker with SDK key or Api Key"
            )
        }else{
            val payload = JSONObject().apply {
                put("channel_id", channelID)
            }
            if (socket?.connected() == true || socket?.isActive == true) {
                socket?.emit("broadcast_start", payload, Ack { args ->
                    onAck(args)
                })
            }else{
                System.err.println(
                    "Socket connect : ${socket?.connected()} isActive : ${socket?.isActive}"
                )
            }
        }
    }

    fun broadCastStop(channelID: String) {
        if (!Talker.isUserLoggedIn()) {
            System.err.println("Kindly initialize Talker with SDK key or Api Key")
        }else{
            val payload = JSONObject().apply {
                put("channel_id", channelID)
            }
            if (socket?.connected() == true || socket?.isActive == true) {
                socket?.emit("broadcast_end", payload)
            }
        }
    }
}