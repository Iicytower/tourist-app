package com.iicytower.wanderlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attractions")
data class AttractionEntity(
    @PrimaryKey val xid: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isInMyList: Boolean,
    val dateAddedToList: Long?,
    val description: String?,
    val descriptionSources: String?,
    val isFromLastSearch: Boolean
)
