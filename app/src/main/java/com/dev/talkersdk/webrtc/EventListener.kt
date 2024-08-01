package com.dev.talkersdk.webrtc

interface EventListener {
    abstract fun onConnectionSuccess()
    abstract fun onConnectionFailure(errorMessage : String)
    abstract fun onConnectionClosed()
}