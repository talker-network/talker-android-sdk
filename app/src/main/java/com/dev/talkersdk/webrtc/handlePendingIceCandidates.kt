package com.dev.talkersdk.webrtc

import android.util.Log
import com.dev.talkersdk.GlobalVariables
import com.dev.talkersdk.LOG_TAG
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.util.Queue

fun handlePendingIceCandidates(
    clientId: String?,
    pendingIceCandidatesMap : HashMap<String, Queue<IceCandidate>>,
    peerConnectionFoundMap : HashMap<String, PeerConnection>
) {
    val TAG = "handlePendingIceCandidates"

    // Add any pending ICE candidates from the queue for the client ID
    val pendingIceCandidatesQueueByClientId = pendingIceCandidatesMap[clientId]
    while (!pendingIceCandidatesQueueByClientId.isNullOrEmpty()) {
        val iceCandidate = pendingIceCandidatesQueueByClientId.peek()
        val peer = peerConnectionFoundMap[clientId]
        if (peer != null) {
            val addIce = peer.addIceCandidate(iceCandidate)
            if (GlobalVariables.printLogs){
                Log.d(
                    LOG_TAG,
                    "Adding ice candidate after SDP exchange " + (if (addIce) "Successful" else "Failed")
                )
            }
        }
        pendingIceCandidatesQueueByClientId.remove()
    }

    // this will remove the client id ice candidate as it will be in the map
    // twice for the same client id.
    pendingIceCandidatesMap.remove(clientId)
}