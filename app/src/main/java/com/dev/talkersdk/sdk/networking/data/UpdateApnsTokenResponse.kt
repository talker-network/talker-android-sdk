package com.dev.talkersdk.sdk.networking.data

data class UpdateApnsTokenRequest(
    val apns_ptt_token : String = ""
)

data class UpdateApnsTokenResponse(
    val success: Boolean = false
)
