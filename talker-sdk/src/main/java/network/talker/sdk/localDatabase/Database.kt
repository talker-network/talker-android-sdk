package network.talker.sdk.localDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import network.talker.sdk.networking.data.Channel
import network.talker.sdk.networking.data.Converters
import network.talker.sdk.networking.data.UserModel

@Database(entities = [UserModel::class, Channel::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun roomDao(): Dao
}