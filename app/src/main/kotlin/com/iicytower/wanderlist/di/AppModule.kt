package com.iicytower.wanderlist.di

import com.iicytower.wanderlist.domain.usecase.AddToMyListUseCase
import com.iicytower.wanderlist.domain.usecase.GenerateDescriptionUseCase
import com.iicytower.wanderlist.domain.usecase.GetAttractionDetailUseCase
import com.iicytower.wanderlist.domain.usecase.GetMyListUseCase
import com.iicytower.wanderlist.domain.usecase.GetSettingsUseCase
import com.iicytower.wanderlist.domain.usecase.RemoveFromMyListUseCase
import com.iicytower.wanderlist.domain.usecase.SearchAttractionsUseCase
import com.iicytower.wanderlist.domain.usecase.SendChatMessageUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { SearchAttractionsUseCase(get()) }
    factory { GetMyListUseCase(get()) }
    factory { AddToMyListUseCase(get()) }
    factory { RemoveFromMyListUseCase(get()) }
    factory { GenerateDescriptionUseCase(get(), get(), get(), get(), get()) }
    factory { GetSettingsUseCase(get()) }
    factory { GetAttractionDetailUseCase(get()) }
    factory { SendChatMessageUseCase(get()) }
}
