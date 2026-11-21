package com.climb.companion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM ClimbLogs WHERE userID = :id Order by logId DESC")
    fun getLogs(id: Long): Flow<List<ClimbLogs>>

    @Query("UPDATE ClimbLogs Set title = :title, notes= :notes WHERE logID = :id")
    suspend fun updateLog(title: String, notes: String, id: Long)

    @Query("SELECT * FROM ClimbLogs WHERE logId = :id")
    suspend fun getSingleLog(id: Long): ClimbLogs?

    @Query("DELETE FROM ClimbLogs WHERE logId = :id")
    suspend fun deleteLog(id: Long)

    //Sessions
    @Insert
    suspend fun insertSession(session: Sessions): Long

    @Query("SELECT * FROM Sessions WHERE sessionId = :id")
    suspend fun getSession(id: Long): List<Sessions>

    @Query("Select * From Sessions where userId = :id and endTime is null")
    fun getRunningSession(id: Long): Flow<List<Sessions>>

    // End session
    @Query("UPDATE Sessions SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: String)

    @Update
    suspend fun updateSession(session: Sessions)

    //Attempts
    @Insert
    suspend fun insertAttempt(attempt: Attempts)

    // All attempts in one session, newest first
    @Query("SELECT * FROM Attempts WHERE sId = :id ORDER BY attemptId DESC")
    fun getAttempt(id: Long): Flow<List<Attempts>>

    // One attempt by its own id (for Edit Attempt)
    @Query("SELECT * FROM Attempts WHERE attemptId = :id")
    suspend fun getAttemptById(id: Long): List<Attempts>

    @Update
    suspend fun updateAttempt(attempt: Attempts)

    @Query("Delete from Attempts where attemptId = :id")
    suspend fun deleteAttempt(id: Long)

    //Favorites
    @Insert
    suspend fun insertFavorite(favorite: Favorites)

    @Query("SELECT * FROM Favorites WHERE userID = :id")
    fun getFavorites(id: Long): Flow<List<Favorites>>

    @Query("Delete from Favorites where favoriteId = :fid")
    suspend fun deleteFavorites(fid: Long)

    @Query("Select * from Favorites WHERE userID = :id and Attempted = 1")
    fun getAttempted(id: Long): Flow<List<Favorites>>

    @Query("Select * from Favorites WHERE userID = :id and Attempted = 0")
    fun getNotAttempted(id: Long): Flow<List<Favorites>>

    //Photo Gallery
    @Insert
    suspend fun insertPhoto(photo: PhotoGallery)

    @Query("Select * from PhotoGallery where lId = :id")
    fun getPhotos(id: Long): Flow<List<PhotoGallery>>

    @Query("SELECT imagePath FROM PhotoGallery WHERE lId = :id")
    suspend fun getPhotoPaths(id: Long): List<String>
}