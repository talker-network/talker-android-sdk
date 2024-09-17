package network.talker.sdk.networking.data

import java.io.Serializable

data class AddNewParticipantModelRequest(
    val channel_id : String = "",
    val new_participants : String = ""
)


data class AddNewParticipantModel(
    val data: network.talker.sdk.networking.data.AddNewParticipantModelData = network.talker.sdk.networking.data.AddNewParticipantModelData(),
    val success: Boolean
)

data class AddNewParticipantModelData(
    val channel_id: String = "",
    val new_participants: List<network.talker.sdk.networking.data.Participant> = emptyList()
) : Serializable