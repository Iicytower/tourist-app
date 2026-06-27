package com.iicytower.wanderlist.feature.mylist.di

import com.iicytower.wanderlist.feature.mylist.viewmodel.MyListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val myListModule = module {
    viewModel { MyListViewModel(get(), get()) }
}
