package com.iicytower.wanderlist.data.remote.openrouter

import com.iicytower.wanderlist.domain.repository.LlmService
import org.koin.dsl.module

val llmModule = module {
    single<LlmService> { OpenRouterLlmService(get(), get()) }
}
