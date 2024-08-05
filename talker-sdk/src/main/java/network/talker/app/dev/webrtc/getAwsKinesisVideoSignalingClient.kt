package network.talker.app.dev.webrtc

import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideosignaling.AWSKinesisVideoSignalingClient
import network.talker.app.dev.KinesisTalkerApp

internal fun getAwsKinesisVideoSignalingClient(
        region: String, endpoint: String
    ): AWSKinesisVideoSignalingClient {
        val client = AWSKinesisVideoSignalingClient(
            KinesisTalkerApp.credentialsProvider.credentials
        )
        client.setRegion(Region.getRegion(region))
        client.signerRegionOverride = region
        client.setServiceNameIntern("kinesisvideo")
        client.endpoint = endpoint
        return client
    }