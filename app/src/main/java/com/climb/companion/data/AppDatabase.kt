package com.climb.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Favorites ADD COLUMN climbName TEXT")
                db.execSQL("ALTER TABLE Favorites ADD COLUMN grade TEXT")
                db.execSQL("ALTER TABLE Favorites ADD COLUMN photoUrl TEXT")
                // Version 3 stored fallHeight and accelSamples. The current table stores path instead.
                // SQLite on older phones cannot drop a column, so the table is rebuilt.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `Attempts_new` (
                        `attemptId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `result` TEXT NOT NULL,
                        `route` TEXT,
                        `note` TEXT,
                        `sId` INTEGER NOT NULL,
                        `time` TEXT NOT NULL,
                        `isAuto` INTEGER NOT NULL,
                        `maxHeight` REAL NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `path` TEXT,
                        FOREIGN KEY(`sId`) REFERENCES `Sessions`(`sessionId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO Attempts_new (
                        attemptId, result, route, note, sId, time, isAuto, maxHeight, durationSeconds, path
                    )
                    SELECT attemptId, result, route, note, sId, time, isAuto, maxHeight, durationSeconds, NULL
                    FROM Attempts
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE Attempts")
                db.execSQL("ALTER TABLE Attempts_new RENAME TO Attempts")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Login ADD COLUMN FirstName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Login ADD COLUMN LastName TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}