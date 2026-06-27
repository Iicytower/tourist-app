package com.iicytower.wanderlist.data.local

import androidx.room.Room
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
    single<AttractionRepository> { RoomAttractionRepository(get(), get()) }
}
