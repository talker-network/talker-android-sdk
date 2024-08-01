package com.dev.talkersdk.webrtc

import android.util.Log
import com.dev.talkersdk.LOG_TAG
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.RtpReceiver
import java.lang.String
import kotlin.Array
import kotlin.Boolean

/**
 * Listener for Peer connection events. Prints event info to the logs at debug level.
 */
open class KinesisVideoPeerConnection : PeerConnection.Observer {
    /**
     * Triggered when the SignalingState changes.
     */
    override fun onSignalingChange(signalingState: SignalingState) {
        Log.d(LOG_TAG, "onSignalingChange(): signalingState = [$signalingState]")
    }

    /**
     * Triggered when the IceConnectionState changes.
     */
    override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
        Log.d(LOG_TAG, "onIceConnectionChange(): iceConnectionState = [$iceConnectionState]")
    }

    /**
     * Triggered when the ICE connection receiving status changes.
     */
    override fun onIceConnectionReceivingChange(connectionChange: Boolean) {
        Log.d(LOG_TAG, "onIceConnectionReceivingChange(): connectionChange = [$connectionChange]")
    }

    /**
     * Triggered when the IceGatheringState changes.
     */
    override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
        Log.d(LOG_TAG, "iceGatheringState = [$iceGatheringState]")
    }

    /**
     * Triggered when a new ICE candidate has been found.
     */
    override fun onIceCandidate(iceCandidate: IceCandidate) {
        Log.d(LOG_TAG, "iceCandidate = [$iceCandidate]")
    }

    /**
     * Triggered when some ICE candidates have been removed.
     */
    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        Log.d(LOG_TAG, "iceCandidates Length = [" + iceCandidates.size + "]")
    }

    /**
     * Triggered when the ICE candidate pair is changed.
     */
    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
        val eventString = "{" +
                String.join(
                    ", ",
                    "reason: " + event.reason,
                    "remote: " + event.remote,
                    "local: " + event.local,
                    "lastReceivedMs: " + event.lastDataReceivedMs
                ) +
                "}"
        Log.d(LOG_TAG, "event = $eventString")
    }

    /**
     * Triggered when media is received on a new stream from remote peer.
     */
    override fun onAddStream(mediaStream: MediaStream) {
        Log.d(LOG_TAG, "mediaStream = [$mediaStream]")
    }

    /**
     * Triggered when a remote peer close a stream.
     */
    override fun onRemoveStream(mediaStream: MediaStream) {
        Log.d(LOG_TAG, "mediaStream = [$mediaStream]")
    }

    /**
     * Triggered when a remote peer opens a DataChannel.
     */
    override fun onDataChannel(dataChannel: DataChannel) {
        Log.d(LOG_TAG, "dataChannel = [${dataChannel.state()}]")
    }

    /**
     * Triggered when renegotiation is necessary.
     */
    override fun onRenegotiationNeeded() {
        Log.d(LOG_TAG, "onRenegotiationNeeded...")
    }

    /**
     * Triggered when a new track is signaled by the remote peer, as a result of setRemoteDescription.
     */
    override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
        Log.d(
            LOG_TAG, "rtpReceiver = [" + rtpReceiver + "], " +
                    "mediaStreams Length = [" + mediaStreams.size + "]"
        )
    }

    companion object {
        private const val TAG = "KVSPeerConnection"
    }
}
