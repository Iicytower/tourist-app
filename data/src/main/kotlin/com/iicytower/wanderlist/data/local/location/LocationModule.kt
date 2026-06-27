package com.iicytower.wanderlist.data.local.location

import com.iicytower.wanderlist.domain.repository.LocationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single<LocationService> { AndroidLocationService(androidContext()) }
}
