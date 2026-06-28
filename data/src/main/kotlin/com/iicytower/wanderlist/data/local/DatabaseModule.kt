package com.iicytower.wanderlist.data.local

import androidx.room.Room
import com.iicytower.wanderlist.data.remote.composite.CompositeAttractionSource
import com.iicytower.wanderlist.data.repository.RoomAttractionRepository
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "wanderlist.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().attractionDao() }
}

val attractionRepositoryModule = module {
    single<AttractionRepository> {
        val composite = getOrNull<CompositeAttractionSource>()
        RoomAttractionRepository(get(), get(), statsProvider = { composite?.lastStats ?: emptyMap() })
    }
}
