package network.talker.app.dev.webrtc

import android.content.Context
import android.util.Log
import network.talker.app.dev.LOG_TAG

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