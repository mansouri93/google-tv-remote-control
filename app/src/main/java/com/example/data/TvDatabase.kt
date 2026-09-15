package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_tv_devices")
data class SavedTvDevice(
    @PrimaryKey val ipAddress: String,
    val name: String,
    val port: Int = 5555,
    val model: String = "Google TV",
    val isFavorite: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)

@Dao
interface TvDeviceDao {
    @Query("SELECT * FROM saved_tv_devices ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<SavedTvDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: SavedTvDevice)

    @Delete
    suspend fun deleteDevice(device: SavedTvDevice)

    @Query("UPDATE saved_tv_devices SET isFavorite = :isFav WHERE ipAddress = :ip")
    suspend fun updateFavorite(ip: String, isFav: Boolean)

    @Query("UPDATE saved_tv_devices SET lastConnected = :time WHERE ipAddress = :ip")
    suspend fun updateLastConnected(ip: String, time: Long = System.currentTimeMillis())
}

@Database(entities = [SavedTvDevice::class], version = 1, exportSchema = false)
abstract class AppTvDatabase : RoomDatabase() {
    abstract fun tvDeviceDao(): TvDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppTvDatabase? = null

        fun getDatabase(context: Context): AppTvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppTvDatabase::class.java,
                    "google_tv_remote_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
