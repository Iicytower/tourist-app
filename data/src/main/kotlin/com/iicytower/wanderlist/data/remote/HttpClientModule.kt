package com.iicytower.wanderlist.data.remote

import org.koin.dsl.module

val httpClientModule = module {
    single { createHttpClient() }
}
