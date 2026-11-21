package com.climb.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.climb.companion.data.entity.Attempts
import com.climb.companion.data.entity.ClimbLogs
import com.climb.companion.data.entity.Favorites
import com.climb.companion.data.entity.Login
import com.climb.companion.data.entity.PhotoGallery
import com.climb.companion.data.entity.Sessions

@Database(
    entities = [Login::class, ClimbLogs::class, Favorites::class, PhotoGallery::class, Sessions::class, Attempts::class],
    version = 7
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun appDao(): AppDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "app-db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}