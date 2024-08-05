package com.dev.talkersdk.sdk.webrtc

import android.content.Context
import android.util.Log
import com.dev.talkersdk.sdk.LOG_TAG

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