package network.talker.sdk.webrtc

import android.util.Log
import network.talker.sdk.LOG_TAG
import network.talker.sdk.model.Event
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.util.LinkedList
import java.util.Queue


internal fun checkAndAddIceCandidate(
    message: Event,
    iceCandidate: IceCandidate,
    peerConnectionFoundMap : HashMap<String, PeerConnection>,
    pendingIceCandidatesMap : HashMap<String, Queue<IceCandidate>>,
) {
    if (!peerConnectionFoundMap.containsKey(message.senderClientId)) {
        Log.d(
            LOG_TAG,
            "SDP exchange is not complete. Ice candidate added to pending queue"
        )
        val pendingIceCandidatesQueueByClientId =
            if (pendingIceCandidatesMap.containsKey(message.senderClientId)) {
                pendingIceCandidatesMap[message.senderClientId]
            } else {
                LinkedList<IceCandidate>()
            }
        if (pendingIceCandidatesQueueByClientId != null) {
            pendingIceCandidatesQueueByClientId.add(iceCandidate)
            pendingIceCandidatesMap[message.senderClientId] =
                pendingIceCandidatesQueueByClientId
        }
    } else {
        Log.d(
            LOG_TAG, "Peer connection found already"
        )
        // Remote sent us ICE candidates, add to local peer connection
        val peer = peerConnectionFoundMap[message.senderClientId]
        if (peer != null) {
            val addIce = peer.addIceCandidate(iceCandidate)
            Log.d(
                LOG_TAG,
                "Added ice candidate " + iceCandidate + " " + (if (addIce) "Successfully" else "Failed")
            )
        }
    }
}