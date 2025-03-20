package network.talker.sdk.localDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import network.talker.sdk.networking.data.AttachmentConverters
import network.talker.sdk.networking.data.Channel
import network.talker.sdk.networking.data.Converters
import network.talker.sdk.networking.data.GetAllUserModelData
import network.talker.sdk.networking.data.MessageObjectForLocalDB

@Database(entities = [GetAllUserModelData::class, Channel::class, MessageObjectForLocalDB::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class, AttachmentConverters::class)
abstract class Database : RoomDatabase() {
    abstract fun roomDao(): Dao
}