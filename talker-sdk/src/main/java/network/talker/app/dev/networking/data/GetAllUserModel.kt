package network.talker.app.dev.networking.data

import java.io.Serializable

data class GetAllUserModel(
    val data: List<GetAllUserModelData> = emptyList(),
    val success: Boolean = false
)

data class GetAllUserModelData(
    val name: String = "",
    val user_id: String = ""
) : Serializable