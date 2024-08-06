package network.talker.app.dev.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.model.Message
import network.talker.app.dev.okhttp.SignalingServiceWebSocketClient
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription

// when mobile sdk is viewer
internal fun createSdpOffer(
    peerIceServers: ArrayList<PeerConnection.IceServer>,
    peerConnectionFactory: PeerConnectionFactory,
    onLocalPeer: (PeerConnection?) -> Unit = {},
    applicationContext: Context,
    client: SignalingServiceWebSocketClient,
    CHANNEL_ID: String,
    mNotificationId: () -> Int,
    master: Boolean = true,
    mClientId: String?,
    recipientClientId: String,
    onRemoteAudioTrack: (AudioTrack?) -> Unit = {},
    audioManager: AudioManager?,
    onFinish: () -> Unit,
    localPeer1: PeerConnection?,
    mChannelName: String,
    onDataChannel: (DataChannel?) -> Unit
) {
    var localPeer: PeerConnection? = localPeer1
    // specifying that we want to offer sharing audio source
    val sdpMediaConstraints = MediaConstraints()
    sdpMediaConstraints.mandatory.add(
        MediaConstraints.KeyValuePair(
            "OfferToReceiveAudio", "true"
        )
    )


    // we will first check if the local peer is null or not.
    // if the local peer is not created meaning that no master has yet joined the session then we will
    // create tha local peer first.
    if (localPeer == null) {
        CreateLocalPeerConnection(
            peerIceServers,
            peerConnectionFactory,
            onLocalPeer = {
                localPeer = it
                onLocalPeer(it)
            },
            applicationContext,
            client,
//            printStatsExecutor,
            CHANNEL_ID,
            mNotificationId,
            master,
            mClientId,
            recipientClientId,
            onRemoteAudioTrack,
            audioManager,
            mChannelName,
            onDataChannel
        )
    }



    // create offer and sending it to the master....
    localPeer?.createOffer(object : KinesisVideoSdpObserver() {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            super.onCreateSuccess(sessionDescription)
            if (TalkerGlobalVariables.printLogs){
                Log.d(
                    LOG_TAG,
                    "SDP offer created successfully"
                )
            }
            localPeer?.setLocalDescription(KinesisVideoSdpObserver(), sessionDescription)
            val sdpOfferMessage = Message.createOfferMessage(
                sessionDescription, mClientId!!
            )
            if (client.isOpen) {
                client.sendSdpOffer(
                    sdpOfferMessage
                )
            } else {
                // notify that the sdp offer could not be sent.
                notifySignalingConnectionFailed(
                    applicationContext,
                    onFinish
                )
            }
        }
    }, sdpMediaConstraints)
}