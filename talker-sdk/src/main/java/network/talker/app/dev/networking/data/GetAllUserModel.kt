package network.talker.app.dev.networking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class GetAllUserModel(
    val data: List<GetAllUserModelData> = emptyList(),
    val success: Boolean = false
)

@Entity(tableName = "user_table")
data class GetAllUserModelData(
    val name: String = "",
    @PrimaryKey(autoGenerate = false)
    val user_id: String = ""
) : Serializable