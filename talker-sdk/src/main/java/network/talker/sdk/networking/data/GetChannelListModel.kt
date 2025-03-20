package network.talker.sdk.networking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

data class GetChannelListModel(
    val data: GetChannelListModelData = GetChannelListModelData(),
    val success: Boolean = false
)

data class GetChannelListModelData(
    val channels: List<Channel> = emptyList()
) : Serializable

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey
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

class Converters {
    @TypeConverter
    fun fromParticipantsList(participants: List<Participant>): String {
        return Gson().toJson(participants)
    }

    @TypeConverter
    fun toParticipantsList(participantsString: String): List<Participant> {
        val listType = object : TypeToken<List<Participant>>() {}.type
        return Gson().fromJson(participantsString, listType)
    }
}