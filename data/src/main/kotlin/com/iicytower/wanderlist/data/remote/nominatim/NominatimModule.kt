package com.iicytower.wanderlist.data.remote.nominatim

import com.iicytower.wanderlist.domain.repository.GeocoderService
import org.koin.dsl.module

val nominatimModule = module {
    single<GeocoderService> { NominatimGeocoderService(get()) }
}
