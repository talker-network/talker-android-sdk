package com.dev.talkersdk.networking.data

data class UpdateApnsTokenRequest(
    val apns_ptt_token : String = ""
)

data class UpdateApnsTokenResponse(
    val success: Boolean = false
)
