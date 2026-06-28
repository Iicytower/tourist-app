package com.iicytower.wanderlist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "attraction_list_crossref",
    primaryKeys = ["attractionXid", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = AttractionEntity::class,
            parentColumns = ["xid"],
            childColumns = ["attractionXid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TripListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("attractionXid")]
)
data class AttractionListCrossRefEntity(
    val attractionXid: String,
    val listId: Long,
    val addedAt: Long
)
