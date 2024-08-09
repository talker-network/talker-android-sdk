package network.talker.app.dev.webrtc

import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient

internal fun getAwsKinesisVideoClient(region: String): AWSKinesisVideoClient {
    val awsKinesisVideoClient = AWSKinesisVideoClient(
        Kinesis.credentialsProvider.credentials
    )
    awsKinesisVideoClient.setRegion(Region.getRegion(region))
    awsKinesisVideoClient.signerRegionOverride = region
    awsKinesisVideoClient.setServiceNameIntern("kinesisvideo")
    return awsKinesisVideoClient
}