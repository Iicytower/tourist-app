package com.iicytower.wanderlist.di

import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import com.iicytower.wanderlist.domain.usecase.AskAboutAttractionUseCase
import com.iicytower.wanderlist.domain.usecase.AddToTripListUseCase
import com.iicytower.wanderlist.domain.usecase.CreateTripListUseCase
import com.iicytower.wanderlist.domain.usecase.DeleteTripListUseCase
import com.iicytower.wanderlist.domain.usecase.DeleteTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.FilterAttractionsByQualityUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionsForListUseCase
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.GetSettingsUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripListsUseCase
import com.iicytower.wanderlist.domain.usecase.GetTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.PlanRouteUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromTripListUseCase
import com.iicytower.wanderlist.domain.usecase.SaveTripPlanNotesUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.usecase.RevertTripPlanUseCase
import com.iicytower.wanderlist.domain.usecase.SendChatMessageUseCase
import com.iicytower.wanderlist.domain.state.TripPlanRevertStore
import org.koin.dsl.module

val useCaseModule = module {
    single { TripPlanRevertStore() }
    factory { SearchAttractionsUseCase(get()) }
    factory { FilterAttractionsByQualityUseCase(get()) }
    factory { GetMyListUseCase(get()) }
    factory { AddToMyListUseCase(get()) }
    factory { RemoveFromMyListUseCase(get()) }
    factory { GenerateDescriptionUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetSettingsUseCase(get()) }
    factory { GetAttractionDetailUseCase(get()) }
    factory { SendChatMessageUseCase(get()) }
    factory { GetTripListsUseCase(get()) }
    factory { GetAttractionsForListUseCase(get()) }
    factory { AddToTripListUseCase(get()) }
    factory { RemoveFromTripListUseCase(get()) }
    factory { CreateTripListUseCase(get()) }
    factory { DeleteTripListUseCase(get()) }
    factory { GenerateTripPlanUseCase(get(), get(), get()) }
    factory { GetTripPlanUseCase(get()) }
    factory { SaveTripPlanNotesUseCase(get()) }
    factory { DeleteTripPlanUseCase(get()) }
    factory { RevertTripPlanUseCase(get(), get()) }
    factory { AskAboutAttractionUseCase(get(), get(), get(), get()) }
    factory { PlanRouteUseCase(get()) }
}
