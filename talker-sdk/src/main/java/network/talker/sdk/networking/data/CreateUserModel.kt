package network.talker.sdk.networking.data

internal data class CreateUserModelRequest(
    val name : String = "",
    val fcm_token : String = "",
    val platform : String = "ANDROID"
)

internal data class CreateUserModelRequestWithPrevId(
    val name : String = "",
    val fcm_token : String = "",
    val platform : String = "ANDROID",
    val prev_user_id : String = ""
)

internal data class CreateUserModel(
    val data: CreateUserModelData = CreateUserModelData(),
    val success: Boolean = false
)

internal data class CreateUserModelData(
    val name: String = "",
    val a_username: String = "",
    val a_pass: String = "",
    val user_auth_token: String = "",
    val user_id: String = ""
)