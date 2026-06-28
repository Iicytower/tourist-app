package com.iicytower.wanderlist.feature.detail.di

import com.iicytower.wanderlist.feature.detail.viewmodel.AttractionDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val detailModule = module {
    viewModel { AttractionDetailViewModel(get(), get(), get(), get()) }
}
