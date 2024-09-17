package network.talker.sdk.okhttp

import android.util.Log
import network.talker.sdk.TalkerGlobalVariables
import network.talker.sdk.LOG_TAG
import network.talker.sdk.model.Message
import com.google.gson.Gson
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

internal class SignalingServiceWebSocketClient(
    uri: String,
    signalingListener: SignalingListener,
    executorService: ExecutorService
) {
    private val websocketClient: WebSocketClient

    private val executorService: ExecutorService

    private val gson = Gson()

    init {
        Log.d(LOG_TAG, "Connecting to web socket client...")
        this.executorService = executorService
        websocketClient = WebSocketClient(uri, signalingListener)
    }

    val isOpen: Boolean
        get() = websocketClient.isOpen

    fun sendSdpOffer(offer: Message) {
        executorService.submit {
            if (offer.action.equals("SDP_OFFER", true)) {
                Log.d(LOG_TAG, "Sending Offer")
                send(offer)
            }
        }
    }

    fun sendSdpAnswer(answer: Message) {
        executorService.submit {
            if (answer.action.equals("SDP_ANSWER", true)) {
                if (TalkerGlobalVariables.printLogs){
                    Log.d(
                        LOG_TAG, "Sending Sdp Answer"
                    )
                }
                send(answer)
            }
        }
    }

    fun sendIceCandidate(candidate: Message) {
        executorService.submit {
            if (candidate.action.equals("ICE_CANDIDATE", true)) {
                send(candidate)
            }
        }
    }



    fun sendPermission(answer: Message) {
        executorService.submit {
            if (answer.action.equals("PERMISSION_CHANGED", true)) {
                send(answer)
            }
        }
    }

    fun disconnect() {
        try {
            executorService.submit(websocketClient::disconnect)
            executorService.shutdown()
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            Log.e(LOG_TAG, "Error in disconnect")
        }
    }

    private fun send(message: Message) {
        val jsonMessage = gson.toJson(message)
        websocketClient.send(jsonMessage)
        Log.d(LOG_TAG, "Sent successfully : $jsonMessage")
    }


    // The function which we have to call when connection is established and also when the user presses the push to talk button.
    // This will notify the back-end that the user has started the push to talk.
    fun send(channelId : String) {
        val message = "{\"type\": \"stream_start\",\"channel_id\": \"${channelId}\",\"message_id\": \"${UUID.randomUUID()}\",\"bit_depth\": \"8\",\"sample_rate\": \"16000\",\"receiver_count\": \"2\"}"
        websocketClient.send(message)
        Log.d(LOG_TAG, "Sent JSON Message= $message")
    }

    companion object {
        private const val TAG = "SignalingServiceWebSocketClient"
    }
}
