package network.talker.sdk.networking.data

internal data class SetUserModelRequest(
//    val prev_user_id : String = "",
    val fcm_token : String = "",
    val user_id : String = "",
    val platform : String = "ANDROID",
    val apns_ptt_token : String = ""
)

internal data class SetUserModelRequestWithPrevId(
    val prev_user_id : String = "",
    val fcm_token : String = "",
    val user_id : String = "",
    val platform : String = "ANDROID",
    val apns_ptt_token : String = ""
)
