package network.talker.app.dev.networking.data

import java.io.Serializable


data class AdminRemoveModelRequest(
    val channel_id : String = "",
    val admin_removed : String = ""
)

data class AdminRemoveModel(
    val data: AdminRemoveModelData = AdminRemoveModelData(),
    val success: Boolean = false
)

data class AdminRemoveModelData(
    val admin_removed: String = "",
    val channel_id: String = ""
) : Serializable