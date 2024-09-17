package network.talker.sdk.networking.data

import java.io.Serializable

data class CreateChannelModel(
    val data: CreateGroupModelData = CreateGroupModelData(),
    val success: Boolean = false
)

data class CreateGroupModelData(
    val channel_id: String = "",
    val channel_type: String = "",
    val group_name: String = "",
    val participants: List<Participant> = emptyList()
) : Serializable