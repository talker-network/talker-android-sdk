package network.talker.app.dev.networking.data

import java.io.Serializable

data class AddNewAdminModelRequest(
    val channel_id : String = "",
    val new_admin : String = ""
)

data class AddNewAdminModel(
    val data: AddNewAdminModelData = AddNewAdminModelData(),
    val success: Boolean = false
)

data class AddNewAdminModelData(
    val channel_id: String = "",
    val new_admin: String = ""
) : Serializable