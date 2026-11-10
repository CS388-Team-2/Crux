package com.climb.companion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao{
    // Logins
    @Insert
    suspend fun insertLogin(user: Login)

    @Query("SELECT * FROM Login WHERE Username = :username")
    suspend fun getLogin(username: String): List<Login>

    //Climb logs
    @Insert
    suspend fun insertLog(log: ClimbLogs)

    @Query("SELECT * FROM ClimbLogs WHERE userID = :id")
    fun getLogs(id: Long): Flow<List<ClimbLogs>>

    //Favorites
    @Insert
    suspend fun insertFavorite(favorite: Favorites)

    @Query("SELECT * FROM Favorites WHERE userID = :id")
    fun getFavorites(id: Long): Flow<List<Favorites>>

    @Query("Delete from Favorites where id = :fid")
    suspend fun deleteFavorites(fid: Long)

    //Photo Gallery
    @Insert
    suspend fun insertPhoto(photo: PhotoGallery)

    @Query("Select * from PhotoGallery where logId = :id")
    fun getPhotos(id: Long): Flow<List<PhotoGallery>>

    @Query("SELECT imagePath FROM PhotoGallery WHERE logId = :id")
    suspend fun getPhotoPaths(id: Long): List<String>
}