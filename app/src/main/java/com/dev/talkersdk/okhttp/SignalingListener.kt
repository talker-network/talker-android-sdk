package com.dev.talkersdk.okhttp

import android.util.Log
import com.dev.talkersdk.LOG_TAG
import com.dev.talkersdk.model.Event
import com.google.gson.Gson
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Locale

abstract class SignalingListener : Signaling {
    private val gson = Gson()

    val websocketListener: WebSocketListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text.isEmpty()) {
                return
            }
            if (!text.contains("messagePayload")) {
                return
            }

            val evt = gson.fromJson(text, Event::class.java)

            if (evt == null || evt.messageType.isEmpty() || evt.messagePayload.isEmpty()) {
                return
            }
            when (evt.messageType.uppercase(Locale.getDefault())) {
                "SDP_OFFER" -> {
                   onSdpOffer(evt)
                }
                "SDP_ANSWER" -> {
                    onSdpAnswer(evt)
                }
                "ICE_CANDIDATE" -> {
                    onIceCandidate(evt)
                }
                "PERMISSION_CHANGED" -> {
                    onPermissionChanged(evt)
                }
                else -> {
                    Log.d(LOG_TAG, "else received: SenderClientId=" + evt.senderClientId)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CustomMessageHandler"
    }
}
