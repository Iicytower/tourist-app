package com.iicytower.wanderlist.data.remote.wikipedia

import com.iicytower.wanderlist.domain.repository.WikipediaService
import org.koin.dsl.module

val wikipediaModule = module {
    single<WikipediaService> { WikipediaServiceImpl(get()) }
}
