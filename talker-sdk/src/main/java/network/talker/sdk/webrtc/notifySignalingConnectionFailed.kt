package network.talker.sdk.webrtc

import android.content.Context
import android.util.Log
import network.talker.sdk.LOG_TAG

internal fun notifySignalingConnectionFailed(
    context: Context,
    onFinish : () -> Unit
) {
    onFinish()
    Log.d(
        LOG_TAG,
        "notifySignalingConnectionFailed"
    )
}