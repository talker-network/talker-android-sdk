package com.dev.talkersdk.webrtc

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.dev.talkersdk.LOG_TAG

fun notifySignalingConnectionFailed(
    context: Context,
    onFinish : () -> Unit
) {
    onFinish()
    Log.d(
        LOG_TAG,
        "notifySignalingConnectionFailed"
    )
}