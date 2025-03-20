package network.talker.sdk.webrtc

import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideosignaling.AWSKinesisVideoSignalingClient

internal fun getAwsKinesisVideoSignalingClient(
        region: String, endpoint: String
    ): AWSKinesisVideoSignalingClient {
        val client = AWSKinesisVideoSignalingClient(
            Kinesis.credentialsProvider.credentials
        )
        client.setRegion(Region.getRegion(region))
        client.signerRegionOverride = region
        client.setServiceNameIntern("kinesisvideo")
        client.endpoint = endpoint
        return client
    }