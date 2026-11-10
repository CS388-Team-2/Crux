package com.climb.companion

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
