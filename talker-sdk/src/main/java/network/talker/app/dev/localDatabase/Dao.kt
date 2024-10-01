package network.talker.app.dev.localDatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.GetAllUserModelData

@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Channel::class)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE channel_id = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Update(entity = Channel::class)
    suspend fun updateChannel(channel: Channel)

    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE channel_id = :channelId")
    fun getChannelById(channelId: String) : Channel?

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = GetAllUserModelData::class)
    suspend fun insertUsers(users: List<GetAllUserModelData>)

    @Query("SELECT * FROM user_table WHERE user_id != :excludedUserId ORDER BY name ASC")
    fun getAllUsersExcept(excludedUserId: String): Flow<List<GetAllUserModelData>>

    @Query("SELECT * FROM user_table WHERE user_id = :userId")
    fun getUserById(userId: String) : GetAllUserModelData?

    @Query("DELETE FROM user_table")
    suspend fun clearUsers()
}