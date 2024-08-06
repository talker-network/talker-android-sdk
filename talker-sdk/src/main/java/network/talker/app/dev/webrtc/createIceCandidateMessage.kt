package network.talker.app.dev.webrtc

import android.util.Log
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.model.Message
import io.socket.engineio.parser.Base64
import org.webrtc.IceCandidate

internal data class MessagePayload(
    val candidate : String,
    val sdpMid : String,
    val sdpMLineIndex : Int,
)

internal fun createIceCandidateMessage(
    iceCandidate: IceCandidate,
    master: Boolean = true,
    mClientId: String?,
    recipientClientId: String,
): Message {
    val sdpMid = iceCandidate.sdpMid
    val sdpMLineIndex = iceCandidate.sdpMLineIndex
    val sdp = iceCandidate.sdp

    val messagePayload = ("{\"candidate\":\"" + sdp + "\",\"sdpMid\":\""
            + sdpMid
            + "\",\"sdpMLineIndex\":"
            + sdpMLineIndex
            + "}")

    Log.d(
        LOG_TAG, "message_payload : $messagePayload"
    )

    val senderClientId = if ((master)) "" else mClientId!!

    return Message(
        "ICE_CANDIDATE", recipientClientId, senderClientId, String(
            Base64.encode(
                messagePayload.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP
            )
        )
    )
}