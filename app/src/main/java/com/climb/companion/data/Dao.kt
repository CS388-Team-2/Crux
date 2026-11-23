package com.climb.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.climb.companion.data.entity.Attempts
import com.climb.companion.data.entity.ClimbLogs
import com.climb.companion.data.entity.Favorites
import com.climb.companion.data.entity.Login
import com.climb.companion.data.entity.PhotoGallery
import com.climb.companion.data.entity.Sessions
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao{
    // Logins
    @Insert
    suspend fun insertLogin(user: Login)

    @Query("SELECT * FROM Login WHERE Username = :username")
    suspend fun getLogin(username: String): List<Login>

    @Query("SELECT * FROM Login WHERE uId = :id LIMIT 1")
    suspend fun getLoginById(id: Long): Login?

    @Query("UPDATE Login SET FirstName = :firstName, LastName = :lastName WHERE uId = :id")
    suspend fun updateNames(id: Long, firstName: String, lastName: String)

    @Query("UPDATE Login SET Password = :password WHERE uId = :id")
    suspend fun updatePassword(id: Long, password: String)

    @Query("DELETE FROM Login WHERE uId = :id")
    suspend fun deleteLogin(id: Long)

    //Climb logs
    @Insert
    suspend fun insertLog(log: ClimbLogs)

    @Query("SELECT * FROM ClimbLogs WHERE userID = :id")
    fun getLogs(id: Long): Flow<List<ClimbLogs>>

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

    @Query("SELECT * FROM Favorites WHERE userId = :userId AND boardID = :boardId AND angle = :angle LIMIT 1")
    suspend fun findFavorite(userId: Long, boardId: String, angle: Int): Favorites?

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