package com.iicytower.wanderlist.data.remote.llmfilter

import com.iicytower.wanderlist.domain.repository.AttractionQualityFilter
import org.koin.dsl.module

val llmFilterModule = module {
    single<AttractionQualityFilter> { LlmAttractionQualityFilter(get(), get()) }
}
