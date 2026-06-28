package com.iicytower.wanderlist.feature.mylist.di

import com.iicytower.wanderlist.feature.mylist.viewmodel.TripListDetailViewModel
import com.iicytower.wanderlist.feature.mylist.viewmodel.TripListsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val myListModule = module {
    viewModel { TripListsViewModel(get(), get(), get()) }
    viewModel { TripListDetailViewModel(get(), get(), get()) }
}
