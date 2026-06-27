package com.iicytower.wanderlist.feature.search.di

import com.iicytower.wanderlist.feature.search.viewmodel.SearchViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val searchModule = module {
    viewModel { SearchViewModel(get(), get(), get()) }
}
