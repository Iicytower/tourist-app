package com.iicytower.wanderlist

import android.app.Application
import com.iicytower.wanderlist.data.local.attractionRepositoryModule
import com.iicytower.wanderlist.data.local.databaseModule
import com.iicytower.wanderlist.data.local.settingsModule
import com.iicytower.wanderlist.data.local.location.locationModule
import com.iicytower.wanderlist.data.remote.openrouter.llmModule
import com.iicytower.wanderlist.data.remote.opentripmap.httpClientModule
import com.iicytower.wanderlist.data.remote.opentripmap.openTripMapModule
import com.iicytower.wanderlist.data.remote.tavily.webSearchModule
import com.iicytower.wanderlist.data.remote.wikipedia.wikipediaModule
import com.iicytower.wanderlist.di.useCaseModule
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
                openTripMapModule,
                webSearchModule,
                wikipediaModule,
                llmModule,
                locationModule,
                useCaseModule
            )
        }
    }
}
