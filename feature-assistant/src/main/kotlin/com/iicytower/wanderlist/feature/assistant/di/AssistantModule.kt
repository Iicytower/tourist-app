package com.iicytower.wanderlist.feature.assistant.di

import com.iicytower.wanderlist.feature.assistant.viewmodel.AssistantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val assistantModule = module {
    viewModel { AssistantViewModel(get(), get(), get(), get(), get()) }
}
