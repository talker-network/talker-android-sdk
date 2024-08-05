package network.talker.app.dev.webrtc

import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import network.talker.app.dev.KinesisTalkerApp

internal fun getAwsKinesisVideoClient(region: String): AWSKinesisVideoClient {
    val awsKinesisVideoClient = AWSKinesisVideoClient(
        KinesisTalkerApp.credentialsProvider.credentials
    )
    awsKinesisVideoClient.setRegion(Region.getRegion(region))
    awsKinesisVideoClient.signerRegionOverride = region
    awsKinesisVideoClient.setServiceNameIntern("kinesisvideo")
    return awsKinesisVideoClient
}