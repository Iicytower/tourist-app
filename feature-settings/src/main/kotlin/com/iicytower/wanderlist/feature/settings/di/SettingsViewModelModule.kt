package com.iicytower.wanderlist.feature.settings.di

import com.iicytower.wanderlist.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val settingsViewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
