package network.talker.app.dev.webrtc

import android.content.Context
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import network.talker.app.dev.LOG_TAG
import java.util.concurrent.CountDownLatch

internal fun initializeMobileClient(
    client: AWSMobileClient,
    applicationContext: Context
) {
    val latch = CountDownLatch(1)
    client.initialize(applicationContext, object : Callback<UserStateDetails> {
        override fun onResult(result: UserStateDetails) {
            Log.d(
                LOG_TAG, "user state: " + result.userState
            )
            latch.countDown()
        }

        override fun onError(e: java.lang.Exception) {
            Log.e(
                LOG_TAG, "onError: Initialization error of the mobile client", e
            )
            latch.countDown()
        }
    })
    try {
        latch.await()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}