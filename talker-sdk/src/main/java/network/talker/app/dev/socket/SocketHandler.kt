package network.talker.app.dev.socket

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.Talker
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.TalkerSdkBackgroundService
import network.talker.app.dev.model.AudioModel
import network.talker.app.dev.networking.data.AddNewAdminModelData
import network.talker.app.dev.networking.data.AddNewParticipantModelData
import network.talker.app.dev.networking.data.AdminRemoveModelData
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.GetAllUserModelData
import network.talker.app.dev.networking.data.MessageObject
import network.talker.app.dev.networking.data.RemoveParticipantModelData
import network.talker.app.dev.networking.data.UpdateChannelNameModelData
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
                        reconnection = true
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
                        putExtra("message", "Error from websocket : " + e.localizedMessage)
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
//            context.sendBroadcast(
//                Intent()
//                    .setPackage(context.packageName)
//                    .setAction("com.talker.sdk")
//                    .apply {
//                        putExtra("action", "CONNECTION_FAILURE")
//                        putExtra("message", "Websocket connection error")
//                        putExtra(
//                            "failure_from", "SOCKET"
//                        )
//                    }
//            )
            Log.d(LOG_TAG, "Socket EVENT_CONNECT_ERROR")
        }
        socket?.on(
            "broadcast_start"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        val url = JsonParser.parseString(arg).asJsonObject["media_link"].asString
                        val channelId =
                            JsonParser.parseString(arg).asJsonObject["channel_id"].asString
                        val senderId =
                            JsonParser.parseString(arg).asJsonObject["sender_id"].asString
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "audio obj : $${JsonParser.parseString(arg).asJsonObject}"
                            )
                            Log.d(
                                LOG_TAG,
                                "userId aksjdlkjsdl: ${userData.user_id}"
                            )
                        }
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
                                        putExtra(
                                            "channel_obj",
                                            Gson().fromJson(
                                                arg,
                                                AudioModel::class.java
                                            ).copy(
                                                group_name = TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.group_name,
                                                sender_name = TalkerSdkBackgroundService.database.roomDao().getUserById(senderId)?.name
                                            )
                                        )
                                    }
                            )
                        }

                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(LOG_TAG, "Socket broadcast_start channel object : $arg")
                        }
                    }
                } else {
                    acknowledgeAck(arg)
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
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "new_channel"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket new_channel object : ${
                                    JsonParser.parseString(
                                        arg
                                    ).asJsonObject
                                }"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "NEW_CHANNEL")
                                        putExtra("message", "New channel added")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                Channel::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket new_channel error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "room_name_update"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket room_name_update object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "ROOM_NAME_UPDATE")
                                        putExtra("message", "Channel name updated")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                UpdateChannelNameModelData::class.java
                                            )
                                        )
                                    }
                            )

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket room_name_update error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "room_participant_removed"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket room_participant_removed object : ${
                                    JsonParser.parseString(
                                        arg
                                    ).asJsonObject
                                }"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "ROOM_PARTICIPANT_REMOVED")
                                        putExtra("message", "Participant Removed")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                RemoveParticipantModelData::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket room_name_update error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "room_participant_added"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket room_participant_added object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "ROOM_PARTICIPANT_ADDED")
                                        putExtra("message", "Participant added")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                AddNewParticipantModelData::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket room_participant_added error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "new_sdk_user"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket new_sdk_user object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "NEW_SDK_USER")
                                        putExtra("message", "New user created")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                GetAllUserModelData::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket new_sdk_user error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "room_admin_added"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket room_admin_added object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "ROOM_ADMIN_ADDED")
                                        putExtra("message", "New room admin added")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                AddNewAdminModelData::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket room_admin_added error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "room_admin_removed"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket room_admin_removed object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "ROOM_ADMIN_REMOVED")
                                        putExtra("message", "New room admin added")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                AdminRemoveModelData::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket room_admin_removed error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
        socket?.on(
            "message"
        ) { args ->
            args.forEach { arg ->
                if (arg is String) {
                    runOnUiThread {
                        try {
                            Log.d(
                                LOG_TAG,
                                "Socket message object : ${JsonParser.parseString(arg).asJsonObject}"
                            )
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "MESSAGE")
                                        putExtra("message", "New message received")
                                        putExtra(
                                            "channel_obj", Gson().fromJson(
                                                arg,
                                                MessageObject::class.java
                                            )
                                        )
                                    }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(
                                LOG_TAG,
                                "Socket message error : ${e.message}"
                            )
                        }
                    }
                } else {
                    acknowledgeAck(arg)
                }
            }
        }
    }

    private fun acknowledgeAck(arg: Any?) {
        if (arg is Ack) {
            arg.call()
            if (TalkerGlobalVariables.printLogs) {
                Log.d(
                    LOG_TAG,
                    "Sending acknowledgment back... broadcast_start"
                )
            }
        }
    }

    fun closeConnection() {
        if (!Talker.isUserLoggedIn()) {
            System.err.println(
                "Kindly initialize Talker with SDK key or Api Key"
            )
        } else {
            socket?.disconnect()
            socket?.off()
            socket?.close()
        }

    }

    fun broadCastStart(channelID: String, onAck: (Array<Any>) -> Unit) {
        if (!Talker.isUserLoggedIn()) {
            System.err.println(
                "Kindly initialize Talker with SDK key or Api Key"
            )
        } else {
            val payload = JSONObject().apply {
                put("channel_id", channelID)
            }
            if (socket?.connected() == true || socket?.isActive == true) {
                socket?.emit("broadcast_start", payload, Ack { args ->
                    onAck(args)
                })
            } else {
                System.err.println(
                    "Socket connect : ${socket?.connected()} isActive : ${socket?.isActive}"
                )
            }
        }
    }

    fun broadCastStop(channelID: String) {
        if (!Talker.isUserLoggedIn()) {
            System.err.println("Kindly initialize Talker with SDK key or Api Key")
        } else {
            val payload = JSONObject().apply {
                put("channel_id", channelID)
            }
            if (socket?.connected() == true || socket?.isActive == true) {
                socket?.emit("broadcast_end", payload)
            }
        }
    }
}