package network.talker.app.dev.webrtc

import android.util.Log
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.model.Message
import network.talker.app.dev.okhttp.SignalingServiceWebSocketClient
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.util.Queue

internal fun createSdpAnswer(
    localPeer: PeerConnection?,
    master: Boolean = true,
    recipientClientId: String,
    client: SignalingServiceWebSocketClient?,
    peerConnectionFoundMap: HashMap<String, PeerConnection>,
    pendingIceCandidatesMap: HashMap<String, Queue<IceCandidate>>
) {
    // Create SDP answer
    val sdpMediaConstraints = MediaConstraints()

    // we specify that we only want to receive audio
    sdpMediaConstraints.mandatory.add(
        MediaConstraints.KeyValuePair(
            "OfferToReceiveAudio", "true"
        )
    )


    if (localPeer != null) {

        // create answer and attack listeners which will tell us if the creating answer was successfully created or not
        localPeer.createAnswer(object : KinesisVideoSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(
                    LOG_TAG, "Sdp Answer created successfully."
                )
                super.onCreateSuccess(sessionDescription)
                // set the local peer's local description to the answer
                localPeer.setLocalDescription(KinesisVideoSdpObserver(), sessionDescription)

                // creating answer message data which will be sending to the client server.
                val answer = Message.createAnswerMessage(
                    sessionDescription, master, recipientClientId
                )

                //sending sdp answer
                client?.sendSdpAnswer(
                    answer
                )

                // adding the local peer to the map of peer connections.
                // this map basically stores the current connections that the user has right now.
                peerConnectionFoundMap[recipientClientId] = localPeer


                // handle pending ice candidates
                handlePendingIceCandidates(
                    recipientClientId,
                    pendingIceCandidatesMap,
                    peerConnectionFoundMap
                )
            }

            override fun onCreateFailure(error: String) {
                super.onCreateFailure(error)
                Log.d(
                    LOG_TAG, "Sdp Answer creation failed with error : $error"
                )
            }
        }, sdpMediaConstraints)
    } else {
        throw Exception().initCause(Throwable("local peer is null"))
    }
}