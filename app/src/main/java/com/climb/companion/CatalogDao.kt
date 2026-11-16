package com.climb.companion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(climbs: List<CatalogClimb>)

    @Query("SELECT COUNT(*) FROM CatalogClimb")
    suspend fun count(): Int

    @Query("SELECT * FROM CatalogClimb ORDER BY position")
    suspend fun getAll(): List<CatalogClimb>

    @Query("SELECT * FROM CatalogClimb WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): CatalogClimb?
}
