package com.dev.talkersdk.sdk.webrtc

enum class PeerConnectionState {
    Success,
    Failure,
    Closed
}

enum class RegistrationState {
    Success,
    Failure
}

enum class AudioStatus {
    Connecting,
    Busy,
    Sending,
    Stopped
}

interface EventListener {
    abstract fun onRegistrationStateChange(registrationState: com.dev.talkersdk.sdk.webrtc.RegistrationState, message: String)
    abstract fun onPeerConnectionStateChange(peerConnectionState: com.dev.talkersdk.sdk.webrtc.PeerConnectionState, message : String)
    abstract fun onAudioStatusChange(audioStatus: com.dev.talkersdk.sdk.webrtc.AudioStatus)
}