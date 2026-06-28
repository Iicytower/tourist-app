package com.iicytower.wanderlist.feature.assistant.di

import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.feature.assistant.viewmodel.AssistantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val assistantModule = module {
    viewModel {
        AssistantViewModel(
            llmService = get<LlmService>(),
            searchAttractionsUseCase = get<SearchAttractionsUseCase>(),
            getMyListUseCase = get<GetMyListUseCase>(),
            webSearchService = get<WebSearchService>(),
            settingsRepository = get<SettingsRepository>()
        )
    }
}
