package network.talker.app.dev.localDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.Converters
import network.talker.app.dev.networking.data.GetAllUserModelData

@Database(entities = [GetAllUserModelData::class, Channel::class], version = 1)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun roomDao(): RoomDao
}