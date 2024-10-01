package network.talker.app.dev.model

import java.io.Serializable

data class AudioModel(
    val attachments: Any?,
    val channel_id: String?,
    val description: Any?,
    val duration: Any?,
    val group_icon: Any?,
    val group_name: String?,
    val id: String?,
    val live_broadcast: Boolean?,
    val media_link: String?,
    val sender_id: String?,
    val sender_name: String?,
    val sent_at: String?,
    val start_time: Any?
) : Serializable

data class AudioData(
    val senderId : String,
    val channelId : String,
    val channelName : String,
    val SenderName : String
) : Serializable