package com.iicytower.wanderlist

import android.app.Application
import com.iicytower.wanderlist.data.local.attractionRepositoryModule
import com.iicytower.wanderlist.data.local.databaseModule
import com.iicytower.wanderlist.data.local.settingsModule
import com.iicytower.wanderlist.data.local.location.locationModule
import com.iicytower.wanderlist.data.remote.openrouter.llmModule
import com.iicytower.wanderlist.data.remote.opentripmap.httpClientModule
import com.iicytower.wanderlist.data.remote.overpass.overpassModule
import com.iicytower.wanderlist.data.remote.tavily.webSearchModule
import com.iicytower.wanderlist.data.remote.wikipedia.wikipediaModule
import com.iicytower.wanderlist.di.useCaseModule
import com.iicytower.wanderlist.feature.assistant.di.assistantModule
import com.iicytower.wanderlist.feature.detail.di.detailModule
import com.iicytower.wanderlist.feature.map.di.mapModule
import com.iicytower.wanderlist.feature.mylist.di.myListModule
import com.iicytower.wanderlist.feature.search.di.searchModule
import com.iicytower.wanderlist.feature.settings.di.settingsViewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class WanderListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@WanderListApp)
            modules(
                databaseModule,
                attractionRepositoryModule,
                settingsModule,
                httpClientModule,
                overpassModule,
                webSearchModule,
                wikipediaModule,
                llmModule,
                locationModule,
                useCaseModule,
                searchModule,
                mapModule,
                myListModule,
                assistantModule,
                settingsViewModelModule,
                detailModule
            )
        }
    }
}
