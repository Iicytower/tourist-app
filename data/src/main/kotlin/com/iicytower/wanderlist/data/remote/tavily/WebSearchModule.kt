package com.iicytower.wanderlist.data.remote.tavily

import com.iicytower.wanderlist.domain.repository.WebSearchService
import org.koin.dsl.module

val webSearchModule = module {
    single<WebSearchService> { TavilyWebSearchService(get(), get()) }
}
