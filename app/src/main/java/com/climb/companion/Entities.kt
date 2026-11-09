package com.climb.companion

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "Login",
        indices = [Index(value = ["Username"], unique = true)])
data class Login(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val Username: String,
    val Password: String,
)

@Entity(tableName = "ClimbLogs")
data class ClimbLogs(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userID: Long,
    val date: Long = System.currentTimeMillis(),
    val notes: String?,
    val tops: Int?,
    val attempts: Int?,
    val distance: Double?,
    val sessionTime: Long?
)

@Entity(
    tableName = "Favorites",
    indices = [Index(value = ["userID", "boardID", "angle"], unique = true)]
)
data class Favorites(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userID: Int,
    val boardID: String,
    val angle: Int?
)

@Entity(tableName = "PhotoGallery")
data class PhotoGallery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId:  Long,
    val imagePath: String
)
