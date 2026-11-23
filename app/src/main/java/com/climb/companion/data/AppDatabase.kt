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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_7)
                .fallbackToDestructiveMigrationFrom(1, 2)
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

        // Version 5 ClimbLogs has no title, and tops/attempts/distance/sessionTime allow null.
        // SQLite cannot change those in place, and PhotoGallery references ClimbLogs, so both
        // tables are rebuilt. A database already on version 5 uses this path instead of a wipe.
        // Versions 3 and 4 already have migrations, so they cannot also be destructive fallbacks.
        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ClimbLogs_new` (
                        `logId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT,
                        `date` TEXT NOT NULL,
                        `notes` TEXT,
                        `tops` INTEGER NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `distance` REAL NOT NULL,
                        `sessionTime` INTEGER NOT NULL,
                        `userId` INTEGER NOT NULL,
                        FOREIGN KEY(`userId`) REFERENCES `Login`(`uId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO ClimbLogs_new (
                        logId, title, date, notes, tops, attempts, distance, sessionTime, userId
                    )
                    SELECT logId, NULL, date, notes,
                        IFNULL(tops, 0), IFNULL(attempts, 0), IFNULL(distance, 0), IFNULL(sessionTime, 0),
                        userId
                    FROM ClimbLogs
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PhotoGallery_backup` (
                        `photoId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `lId` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO PhotoGallery_backup (photoId, imagePath, lId)
                    SELECT photoId, imagePath, lId FROM PhotoGallery
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE PhotoGallery")
                db.execSQL("DROP TABLE ClimbLogs")
                db.execSQL("ALTER TABLE ClimbLogs_new RENAME TO ClimbLogs")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ClimbLogs_userId_date` ON `ClimbLogs` (`userId`, `date`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PhotoGallery` (
                        `photoId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `lId` INTEGER NOT NULL,
                        FOREIGN KEY(`lId`) REFERENCES `ClimbLogs`(`logId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO PhotoGallery (photoId, imagePath, lId)
                    SELECT photoId, imagePath, lId FROM PhotoGallery_backup
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE PhotoGallery_backup")
            }
        }
    }
}