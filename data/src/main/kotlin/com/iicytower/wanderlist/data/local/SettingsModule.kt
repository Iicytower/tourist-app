package com.iicytower.wanderlist.data.local

import com.iicytower.wanderlist.data.repository.DataStoreSettingsRepository
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    single { SecureKeyStorage(androidContext()) }
    single { androidContext().dataStore }
    single<SettingsRepository> { DataStoreSettingsRepository(get(), get()) }
}
