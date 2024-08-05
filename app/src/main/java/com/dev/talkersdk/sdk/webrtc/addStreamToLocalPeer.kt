package com.dev.talkersdk.sdk.webrtc

import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.PeerConnectionFactory

fun addStreamToLocalPeer(
    peerConnectionFactory: PeerConnectionFactory,
    isAudioSent: Boolean,
    localAudioTrack: AudioTrack?,
    onAddTrack : (AudioTrack, List<String>) -> Unit,
    onGetStream : (MediaStream) -> Unit
) {
    val LOCAL_MEDIA_STREAM_LABEL = "KvsLocalMediaStream"
//    val stream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_LABEL)
//    if (isAudioSent) {
//        if (!stream.addTrack(localAudioTrack)) {
//            Log.e(
//                LOG_TAG, "Add audio track failed"
//            )
//        }
//        if (stream.audioTracks.isNotEmpty()) {
//            onAddTrack(
//                stream.audioTracks[0], listOf(stream.id)
//            )
//            if (GlobalVariables.printLogs){
//                Log.d(
//                    LOG_TAG, "Sending local audio track to client"
//                )
//            }
//        }
//    }
//    onGetStream(stream)
}