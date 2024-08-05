package com.dev.talkersdk.sdk.networking.data

data class CreateUserModelRequest(
    val name : String = "",
    val fcm_token : String = "",
    val platform : String = "ANDROID"
)

data class CreateUserModel(
    val data: CreateUserModelData = CreateUserModelData(),
    val success: Boolean = false
)

data class CreateUserModelData(
    val name: String = "",
    val a_username: String = "",
    val a_pass: String = "",
    val user_auth_token: String = "",
    val user_id: String = ""
)



