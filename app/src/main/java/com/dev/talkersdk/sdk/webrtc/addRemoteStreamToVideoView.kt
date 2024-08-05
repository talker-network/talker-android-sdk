package com.dev.talkersdk.sdk.webrtc

import android.media.AudioManager
import android.util.Log
import com.dev.talkersdk.sdk.TalkerGlobalVariables
import com.dev.talkersdk.sdk.LOG_TAG
import org.webrtc.AudioTrack
import org.webrtc.MediaStream


fun addRemoteStreamToVideoView(
    stream: MediaStream,
    onRemoteAudioTrack: (AudioTrack?) -> Unit = {},
    audioManager: AudioManager?
) {
    var remoteAudioTrack: AudioTrack? = null
    val TAG = "addRemoteStreamToVideoView"

    // getting the first audio track from the media stream which will be of remote user.
    remoteAudioTrack = if (stream.audioTracks != null && stream.audioTracks.isNotEmpty()) stream.audioTracks[0] else null
    if (remoteAudioTrack != null) {
        // enabling this audio track so that it could be played on the speaker.
        remoteAudioTrack.setEnabled(true)
        if (TalkerGlobalVariables.printLogs){
            Log.d(
                LOG_TAG, "remoteAudioTrack received: State=" + remoteAudioTrack.state().name
            )
        }
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
//        audioManager?.isBluetoothScoOn = true
//        audioManager?.isSpeakerphoneOn = true

        // Check if Bluetooth is connected
//        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        val isBluetoothConnected = (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
//                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothAdapter.STATE_CONNECTED)
//
//        if (isBluetoothConnected) {
//            // Route audio to Bluetooth device
//            audioManager?.startBluetoothSco()
//            audioManager?.isBluetoothScoOn = true
//            audioManager?.isSpeakerphoneOn = false // Ensure speakerphone is off
//        } else {
            // Route audio to the main speaker
            audioManager?.stopBluetoothSco()
            audioManager?.isBluetoothScoOn = false
            audioManager?.isSpeakerphoneOn = true
//        }



    }
    onRemoteAudioTrack(remoteAudioTrack)
}