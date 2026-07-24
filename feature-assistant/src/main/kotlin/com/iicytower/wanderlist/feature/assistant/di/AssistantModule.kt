package com.iicytower.wanderlist.feature.assistant.di

import com.iicytower.wanderlist.domain.repository.LlmService
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.domain.repository.WebSearchService
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore
import com.iicytower.wanderlist.feature.assistant.viewmodel.AssistantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val assistantModule = module {
    viewModel {
        AssistantViewModel(
            llmService = get<LlmService>(),
            searchAttractionsUseCase = get<SearchAttractionsUseCase>(),
            getTripListsUseCase = get<GetTripListsUseCase>(),
            getAttractionsForListUseCase = get<GetAttractionsForListUseCase>(),
            addToTripListUseCase = get<AddToTripListUseCase>(),
            removeFromTripListUseCase = get<RemoveFromTripListUseCase>(),
            createTripListUseCase = get<CreateTripListUseCase>(),
            webSearchService = get<WebSearchService>(),
            settingsRepository = get<SettingsRepository>(),
            getTripPlanUseCase = get<GetTripPlanUseCase>(),
            generateTripPlanUseCase = get<GenerateTripPlanUseCase>(),
            tripPlanRevertStore = get<TripPlanRevertStore>()
        )
    }
}
