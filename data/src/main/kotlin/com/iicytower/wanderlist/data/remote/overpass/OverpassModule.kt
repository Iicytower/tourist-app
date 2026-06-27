package com.iicytower.wanderlist.data.remote.overpass

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import org.koin.dsl.module

val overpassModule = module {
    single<RemoteAttractionSource> { OverpassApiClient(get()) }
}
