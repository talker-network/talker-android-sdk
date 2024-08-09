package network.talker.app.dev.webrtc

enum class PeerConnectionState {
    Success,
    Failure,
    Closed
}

//enum class RegistrationState {
//    Success,
//    Failure
//}

enum class AudioStatus {
    Connecting,
    Busy,
    Sending,
    Stopped
}

//interface EventListener {
////    fun onRegistrationStateChange(registrationState: RegistrationState, message: String)
//    fun onPeerConnectionStateChange(peerConnectionState: PeerConnectionState, message : String)
//    fun onAudioStatusChange(audioStatus: AudioStatus)
//}