package network.talker.sdk.networking.data

internal data class UpdateApnsTokenRequest(
    val apns_ptt_token : String = ""
)

internal data class UpdateApnsTokenResponse(
    val success: Boolean = false
)