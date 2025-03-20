package network.talker.sdk.networking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

data class UploadMessageResponse(
    val attachments: Attachments,
    val description: String = "",
    val message_id: String = "",
    val sent_at: String = "",
    val success: Boolean = false
) : Serializable

data class MessageObject(
    val attachments: Attachments,
    val channel_id: String,
    val channel_name : String,
    val description: String,
    val id: String,
    val sent_at: String,
    val sender_id : String,
) : Serializable

@Entity(tableName = "message")
data class MessageObjectForLocalDB(
    val attachments: Attachments,
    val channel_id: String,
    val channel_name : String,
    val text: String,
    @PrimaryKey val id: String,
    val sent_at: String,
    val sender_id : String,
    val sender_name : String,
) : Serializable

data class Attachments(
    val images: List<Media>? = emptyList(),
    val document : List<Media>? = emptyList()
) : Serializable

data class Media(
    val url: String
) : Serializable

class AttachmentConverters {

    @TypeConverter
    fun fromAttachments(attachments: Attachments): String {
        return Gson().toJson(attachments)
    }

    @TypeConverter
    fun toAttachments(json: String): Attachments {
        val type = object : TypeToken<Attachments>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun fromMediaList(mediaList: List<Media>): String {
        return Gson().toJson(mediaList)
    }

    @TypeConverter
    fun toMediaList(json: String): List<Media> {
        val type = object : TypeToken<List<Media>>() {}.type
        return Gson().fromJson(json, type)
    }
}