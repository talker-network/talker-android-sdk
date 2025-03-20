package network.talker.sdk.networking.data

import java.io.Serializable


data class UpdateChannelNameModelRequest(
    val channel_id : String = "",
    val new_name : String = ""
)

data class UpdateChannelNameModel(
    val data: UpdateChannelNameModelData = UpdateChannelNameModelData(),
    val success: Boolean = false
)

data class UpdateChannelNameModelData(
    val channel_id: String = "",
    val new_name: String = ""
) : Serializable