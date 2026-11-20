package com.climb.companion.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(tableName = "Login",
        indices = [Index(value = ["Username"], unique = true)])
data class Login(
    @PrimaryKey(autoGenerate = true) val uId: Long = 0,
    val Username: String,
    val Password: String,
)

@Entity(tableName = "ClimbLogs",
        foreignKeys = [
        ForeignKey(
        entity = Login::class,
        parentColumns = ["uId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class ClimbLogs(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val date: String, //yyyy-mm-dd
    val notes: String?,
    val tops: Int?,
    val attempts: Int?,
    val distance: Double?,
    val sessionTime: Long?,

    @ColumnInfo(name = "userId")
    val uId: Long,
)

@Entity(tableName = "Sessions",
    foreignKeys = [
        ForeignKey(
            entity = Login::class,
            parentColumns = ["uId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )],
)
data class Sessions(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val date: String, //yyyy-mm-dd
    val startTime: String?,
    val route: String?,
    val distance: Double?,
    val sessionTime: Long?,
    val endTime: String? = null,
    val startMillis: Long = 0, // start time in milliseconds, so the timer can count up from it

    @ColumnInfo(name = "userId")
    val uId: Long,
)

@Entity(tableName = "Attempts",
        foreignKeys = [
        ForeignKey(
        entity = Sessions::class,
        parentColumns = ["sessionId"],
        childColumns = ["sId"],
        onDelete = ForeignKey.CASCADE
    )])
data class Attempts(
    @PrimaryKey(autoGenerate = true) val attemptId: Long = 0,
    val result: String,
    val route: String?,
    val note: String?,

    @ColumnInfo(name = "sId")
    val sessionId: Long,

    val time: String = "",                // shown on the row, e.g. "7:41 PM"
    val isAuto: Boolean = false,          // caught by the sensors instead of logged by hand
    val maxHeight: Double = 0.0,          // meters, from the barometer
    val fallHeight: Double? = null,       // meters, where the fall happened (null if no fall)
    val durationSeconds: Int = 0,
    val accelSamples: String? = null,     // g-force readings for the graph, comma separated
)

@Entity(
    tableName = "Favorites",
    foreignKeys = [
        ForeignKey(
            entity = Login::class,
            parentColumns = ["uId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["userId", "boardID", "angle"], unique = true)]
)
data class Favorites(
    @PrimaryKey(autoGenerate = true) val favoriteId: Long = 0,
    val boardID: String,
    val angle: Int?,
    val Attempted: Boolean = false,
    val climbName: String? = null,
    val grade: String? = null,
    val photoUrl: String? = null,

    @ColumnInfo(name = "userId")
    val uId: Long,
)

@Entity(tableName = "PhotoGallery",
    foreignKeys = [
        ForeignKey(
            entity = ClimbLogs::class,
            parentColumns = ["logId"],
            childColumns = ["lId"],
            onDelete = ForeignKey.CASCADE
        )])
data class PhotoGallery(
    @PrimaryKey(autoGenerate = true) val photoId: Long = 0,
    val imagePath: String,

    @ColumnInfo(name="lId")
    val logId:  Long
)
