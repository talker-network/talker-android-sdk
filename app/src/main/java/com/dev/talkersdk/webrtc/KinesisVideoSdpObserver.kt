package com.dev.talkersdk.webrtc

import android.util.Log
import com.dev.talkersdk.LOG_TAG
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class KinesisVideoSdpObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        Log.d(LOG_TAG, "onCreateSuccess...")
    }

    override fun onSetSuccess() {
        Log.d(LOG_TAG, "onSetSuccess...")
    }

    override fun onCreateFailure(error: String) {
        Log.e(LOG_TAG, "onCreateFailure... $error")
    }

    override fun onSetFailure(error: String) {
        Log.e(LOG_TAG, "onSetFailure... $error")
    }

    companion object {
        val TAG: String = KinesisVideoSdpObserver::class.java.simpleName
    }
}
