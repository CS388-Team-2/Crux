package com.climb.companion.data

import androidx.room.Dao
import com.climb.companion.data.entity.CatalogClimb
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
