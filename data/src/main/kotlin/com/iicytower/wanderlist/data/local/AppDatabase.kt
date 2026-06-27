package com.iicytower.wanderlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iicytower.wanderlist.data.local.dao.AttractionDao
import com.iicytower.wanderlist.data.local.entity.AttractionEntity

@Database(entities = [AttractionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attractionDao(): AttractionDao
}
