package com.iicytower.wanderlist.data.local

import androidx.room.Room
import com.iicytower.wanderlist.data.remote.composite.CompositeAttractionSource
import com.iicytower.wanderlist.data.repository.RoomAttractionRepository
import com.iicytower.wanderlist.data.repository.RoomTripListRepository
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import com.iicytower.wanderlist.domain.repository.TripListRepository
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.DeleteTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "wanderlist.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().attractionDao() }
    single { get<AppDatabase>().tripListDao() }
}

val attractionRepositoryModule = module {
    single<AttractionRepository> {
        val composite = getOrNull<CompositeAttractionSource>()
        RoomAttractionRepository(get(), get(), statsProvider = { composite?.lastStats ?: emptyMap() })
    }
}

val tripListRepositoryModule = module {
    single<TripListRepository> { RoomTripListRepository(get(), get()) }
    single { GetTripListsUseCase(get()) }
    single { CreateTripListUseCase(get()) }
    single { DeleteTripListUseCase(get()) }
    single { AddToTripListUseCase(get()) }
    single { RemoveFromTripListUseCase(get()) }
    single { GetAttractionsForListUseCase(get()) }
}
