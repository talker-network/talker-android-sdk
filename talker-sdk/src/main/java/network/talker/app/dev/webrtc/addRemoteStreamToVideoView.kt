package network.talker.app.dev.webrtc

import android.media.AudioManager
import android.util.Log
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.LOG_TAG
import org.webrtc.AudioTrack
import org.webrtc.MediaStream


internal fun addRemoteStreamToVideoView(
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
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = true
    }
    onRemoteAudioTrack(remoteAudioTrack)
}