package com.iicytower.wanderlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iicytower.wanderlist.data.local.dao.AttractionDao
import com.iicytower.wanderlist.data.local.dao.TripListDao
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import com.iicytower.wanderlist.data.local.entity.AttractionListCrossRefEntity
import com.iicytower.wanderlist.data.local.entity.TripListEntity

@Database(
    entities = [AttractionEntity::class, TripListEntity::class, AttractionListCrossRefEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attractionDao(): AttractionDao
    abstract fun tripListDao(): TripListDao
}
