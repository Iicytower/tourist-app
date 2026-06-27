package com.iicytower.wanderlist.feature.map.di

import com.iicytower.wanderlist.feature.map.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mapModule = module {
    viewModel { MapViewModel(get(), get()) }
}
