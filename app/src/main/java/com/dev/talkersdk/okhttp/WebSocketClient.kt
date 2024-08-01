package com.dev.talkersdk.okhttp

import android.util.Log
import com.dev.talkersdk.LOG_TAG
import com.dev.talkersdk.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

/**
 * An OkHttp based WebSocket client.
 */
internal class WebSocketClient(uri: String, signalingListener: SignalingListener) {
    private val webSocket: WebSocket
    @Volatile
    var isOpen: Boolean = false

    init {
        val client: OkHttpClient = OkHttpClient.Builder().build()

        val userAgent: String =
            ((Constants.APP_NAME + "/" + Constants.VERSION) + " " + System.getProperty("http.agent")).trim { it <= ' ' }

        val request: Request = Request.Builder()
            .url(uri)
            .addHeader("User-Agent", userAgent)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(LOG_TAG, "WebSocket connection opened")
                isOpen = true
            }

            override fun onMessage(webSocket: WebSocket, message: String) {
                Log.d(LOG_TAG, "Websocket received a message: $message")
                signalingListener.websocketListener.onMessage(webSocket, message)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(LOG_TAG, "WebSocket connection closed: $reason")
                isOpen = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(LOG_TAG, "WebSocket connection failed", t)
                isOpen = false
                signalingListener.onException((t as Exception))
            }
        })

        // Await WebSocket connection
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
            .until { this@WebSocketClient.isOpen }
    }

    fun send(message: String) {
        if (isOpen) {
            if (webSocket.send(message)) {
                Log.d(LOG_TAG, "Successfully sent $message")
            } else {
                Log.d(
                    LOG_TAG,
                    "Could not send $message as the connection may have closing, closed, or canceled."
                )
            }
        } else {
            Log.d(LOG_TAG, "Cannot send the websocket message as it is not open.")
        }
    }

    fun disconnect() {
        if (isOpen) {
            if (webSocket.close(1000, "Disconnect requested")) {
                Log.d(LOG_TAG, "Websocket successfully disconnected.")
            } else {
                Log.d(
                    LOG_TAG,
                    "Websocket could not disconnect in a graceful shutdown. Going to cancel it to release resources."
                )
                webSocket.cancel()
            }
        } else {
            Log.d(LOG_TAG, "Cannot close the websocket as it is not open.")
        }
    }

    companion object {
        private const val TAG = "WebSocketClient"
    }
}
