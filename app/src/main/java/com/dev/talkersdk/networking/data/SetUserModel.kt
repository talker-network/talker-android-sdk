package com.dev.talkersdk.networking.data

data class SetUserModelRequest(
    val prev_user_id : String = "",
    val fcm_token : String = "",
    val user_id : String = "",
    val platform : String = "ANDROID",
    val apns_ptt_token : String = ""
)
