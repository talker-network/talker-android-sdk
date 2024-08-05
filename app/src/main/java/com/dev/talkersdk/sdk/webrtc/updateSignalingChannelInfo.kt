package com.dev.talkersdk.sdk.webrtc

import android.os.AsyncTask
import android.util.Log
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import com.amazonaws.services.kinesisvideo.model.ChannelRole
import com.amazonaws.services.kinesisvideo.model.CreateSignalingChannelRequest
import com.amazonaws.services.kinesisvideo.model.DescribeSignalingChannelRequest
import com.amazonaws.services.kinesisvideo.model.GetSignalingChannelEndpointRequest
import com.amazonaws.services.kinesisvideo.model.ResourceEndpointListItem
import com.amazonaws.services.kinesisvideo.model.ResourceNotFoundException
import com.amazonaws.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration
import com.amazonaws.services.kinesisvideosignaling.AWSKinesisVideoSignalingClient
import com.amazonaws.services.kinesisvideosignaling.model.GetIceServerConfigRequest
import com.amazonaws.services.kinesisvideosignaling.model.IceServer
import com.dev.talkersdk.sdk.LOG_TAG


class UpdateSignalingChannelInfoTask(
    private val onMChannelArn : (String) -> Unit,
    private val onMEndpointList : (List<ResourceEndpointListItem>) -> Unit,
    private val onMIceServerList : (ArrayList<IceServer>) -> Unit
) : AsyncTask<Any?, String?, String?>() {
    val TAG = "UpdateSignalingChannelInfoTask"
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg objects: Any?): String? {
        val region = objects[0] as String
        val channelName = objects[1] as String
        val role = objects[2] as ChannelRole
        var mChannelArn = ""
        val mEndpointList : ArrayList<ResourceEndpointListItem> = java.util.ArrayList()
        val mIceServerList: ArrayList<IceServer> = java.util.ArrayList()
        // Step 1. Create Kinesis Video Client
        val awsKinesisVideoClient: AWSKinesisVideoClient
        try {
            Log.d(LOG_TAG, "Region : $region")
            awsKinesisVideoClient = getAwsKinesisVideoClient(region)
        } catch (e: java.lang.Exception) {
            return "Create client failed with " + e.localizedMessage
        }
        try {
            val describeSignalingChannelResult = awsKinesisVideoClient.describeSignalingChannel(
                DescribeSignalingChannelRequest().withChannelName(channelName)
            )
            Log.i(
                TAG, "Channel ARN is " + describeSignalingChannelResult.channelInfo.channelARN
            )
            mChannelArn = describeSignalingChannelResult.channelInfo.channelARN
            onMChannelArn(mChannelArn)
        } catch (e: ResourceNotFoundException) {
            if (role == ChannelRole.MASTER) {
                try {
                    val createSignalingChannelResult =
                        awsKinesisVideoClient.createSignalingChannel(
                            CreateSignalingChannelRequest().withChannelName(channelName)
                        )
                    mChannelArn = createSignalingChannelResult.channelARN
                    onMChannelArn(mChannelArn)
                } catch (ex: java.lang.Exception) {
                    return "Create Signaling Channel failed with Exception " + ex.localizedMessage
                }
            } else {
                return "Signaling Channel $channelName doesn't exist!"
            }
        } catch (ex: java.lang.Exception) {
            return "Describe Signaling Channel failed with Exception " + ex.localizedMessage
        }

        val protocols = arrayOf("WSS", "HTTPS")

        try {
            val getSignalingChannelEndpointResult =
                awsKinesisVideoClient.getSignalingChannelEndpoint(
                    GetSignalingChannelEndpointRequest().withChannelARN(mChannelArn)
                        .withSingleMasterChannelEndpointConfiguration(
                            SingleMasterChannelEndpointConfiguration().withProtocols(*protocols)
                                .withRole(role)
                        )
                )

            Log.i(
                TAG, "Endpoints $getSignalingChannelEndpointResult"
            )
            mEndpointList.addAll(getSignalingChannelEndpointResult.resourceEndpointList)
            onMEndpointList(getSignalingChannelEndpointResult.resourceEndpointList)
        } catch (e: java.lang.Exception) {
            return "Get Signaling Endpoint failed with Exception " + e.localizedMessage
        }

        var dataEndpoint = ""
        for (endpoint in mEndpointList) {
            if (endpoint.protocol == "HTTPS") {
                dataEndpoint = endpoint.resourceEndpoint
            }
        }
        try {
            val awsKinesisVideoSignalingClient: AWSKinesisVideoSignalingClient =
                getAwsKinesisVideoSignalingClient(region, dataEndpoint)
            val getIceServerConfigResult = awsKinesisVideoSignalingClient.getIceServerConfig(
                GetIceServerConfigRequest().withChannelARN(mChannelArn).withClientId(role.name)
            )
            mIceServerList.addAll(getIceServerConfigResult.iceServerList)
            onMIceServerList(mIceServerList)
        } catch (e: java.lang.Exception) {
            return "Get Ice Server Config failed with Exception " + e.localizedMessage
        }
        return null
    }


    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: String?) {
        if (result != null) {
            Log.d(TAG, result)
        }
    }
}