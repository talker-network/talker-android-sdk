package network.talker.app.dev.utils

import com.amazonaws.kinesisvideo.BuildConfig

internal object Constants {
    /**
     * SDK identifier
     */
    const val APP_NAME: String = "aws-kvs-webrtc-android-client"

    /**
     * SDK version identifier
     */
    val VERSION: String = BuildConfig.VERSION_NAME

    /**
     * Query parameter for Channel ARN. Used for calling Kinesis Video Websocket APIs.
     */
    const val CHANNEL_ARN_QUERY_PARAM: String = "X-Amz-ChannelARN"

    /**
     * Query parameter for Client Id. Only used for viewers. Used for calling Kinesis Video Websocket APIs.
     */
    const val CLIENT_ID_QUERY_PARAM: String = "X-Amz-ClientId"
}
