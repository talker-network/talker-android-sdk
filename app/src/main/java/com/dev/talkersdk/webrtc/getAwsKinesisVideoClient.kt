package com.dev.talkersdk.webrtc

import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import com.dev.talkersdk.KinesisTalkerApp

fun getAwsKinesisVideoClient(region: String): AWSKinesisVideoClient {
    val awsKinesisVideoClient = AWSKinesisVideoClient(
        KinesisTalkerApp.credentialsProvider.credentials
    )
    awsKinesisVideoClient.setRegion(Region.getRegion(region))
    awsKinesisVideoClient.signerRegionOverride = region
    awsKinesisVideoClient.setServiceNameIntern("kinesisvideo")
    return awsKinesisVideoClient
}