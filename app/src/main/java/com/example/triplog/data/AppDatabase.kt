package com.example.triplog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.triplog.data.dao.TripDao
import com.example.triplog.data.dao.UserDao
import com.example.triplog.data.entities.TripEntity
import com.example.triplog.data.entities.UserEntity

@Database(
    entities = [UserEntity::class, TripEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_log_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

