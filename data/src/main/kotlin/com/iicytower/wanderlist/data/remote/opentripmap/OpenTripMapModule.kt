package com.iicytower.wanderlist.data.remote.opentripmap

import com.iicytower.wanderlist.data.remote.createHttpClient
import org.koin.dsl.module

val httpClientModule = module {
    single { createHttpClient() }
}

val openTripMapModule = module {
    single<OpenTripMapClient> { FakeOpenTripMapClient() }
}
