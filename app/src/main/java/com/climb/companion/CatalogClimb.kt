package com.climb.companion

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CatalogClimb")
data class CatalogClimb(
    @PrimaryKey val uuid: String,
    val name: String,
    val grade: String,
    val angle: Int,
    val position: Int,
)
