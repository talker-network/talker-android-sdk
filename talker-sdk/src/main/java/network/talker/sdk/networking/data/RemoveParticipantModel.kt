package network.talker.sdk.networking.data

import java.io.Serializable

data class RemoveParticipantModelRequest(
    val channel_id : String = "",
    val delete_participant : String = "",
)

data class RemoveParticipantModel(
    val data: RemoveParticipantModelData = RemoveParticipantModelData(),
    val success: Boolean = false
)

data class RemoveParticipantModelData(
    val channel_id: String = "",
    val removed_participant: String = ""
) : Serializable