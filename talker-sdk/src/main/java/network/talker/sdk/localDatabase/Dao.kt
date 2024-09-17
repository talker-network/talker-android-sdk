package network.talker.sdk.localDatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import network.talker.sdk.networking.data.Channel
import network.talker.sdk.networking.data.UserModel

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

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = UserModel::class)
    suspend fun insertUsers(users: List<UserModel>)

    @Query("SELECT * FROM user_table WHERE user_id != :excludedUserId ORDER BY name ASC")
    fun getAllUsersExcept(excludedUserId: String): Flow<List<UserModel>>

    @Query("SELECT * FROM user_table WHERE user_id = :userId")
    fun getUserById(userId: String) : UserModel?

    @Query("DELETE FROM user_table")
    suspend fun clearUsers()
}