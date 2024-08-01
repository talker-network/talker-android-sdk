package com.dev.talkersdk.webrtc

import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import com.amazonaws.services.kinesisvideo.model.DescribeSignalingChannelRequest
import com.dev.talkersdk.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun isChannelAvailable(channelName: String): Boolean {
    val TAG = "isChannelAvailable"
    var isChannelAvailable = false
    val auth = AWSMobileClient.getInstance()
    if (!auth.isSignedIn) {
        println(
            "$LOG_TAG : User is not signed in"
        )
        return false
    }
    CoroutineScope(Dispatchers.IO).launch {
        val client = AWSKinesisVideoClient(
            auth.awsCredentials
        )
        client.setRegion(Region.getRegion("us-east-2"))
        client.signerRegionOverride = "us-east-2"
        try {
            val describeSignalingChannelResult = client.describeSignalingChannel(
                DescribeSignalingChannelRequest().withChannelName(channelName)
            )
            Log.i(TAG, "Channel ID " + describeSignalingChannelResult.channelInfo.channelARN)
            isChannelAvailable = true
        } catch (e: Exception) {
            Log.i(TAG, "Error : ${e.localizedMessage}")
        }
    }
    return isChannelAvailable
}