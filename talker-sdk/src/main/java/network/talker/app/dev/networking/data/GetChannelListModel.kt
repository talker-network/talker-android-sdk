package network.talker.app.dev.networking.data

import java.io.Serializable

data class GetChannelListModel(
    val data: GetChannelListModelData = GetChannelListModelData(),
    val success: Boolean = false
)

data class GetChannelListModelData(
    val channels: List<Channel> = emptyList()
) : Serializable

data class Channel(
    val channel_id: String = "",
    val channel_type: String = "",
    var group_name: String = "",
    var participants: List<Participant> = emptyList()
) : Serializable

data class Participant(
    var admin: Boolean = false,
    val name: String = "",
    val user_id: String = ""
) : Serializable