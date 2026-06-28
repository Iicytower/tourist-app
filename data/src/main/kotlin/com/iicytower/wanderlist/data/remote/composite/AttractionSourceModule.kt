package com.iicytower.wanderlist.data.remote.composite

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.data.remote.overpass.OverpassApiClient
import com.iicytower.wanderlist.data.remote.wikidata.WikidataSparqlSource
import com.iicytower.wanderlist.data.remote.wikipedia.WikipediaGeoSearchSource
import org.koin.dsl.module

val attractionSourceModule = module {
    single { OverpassApiClient(get()) }
    single { WikipediaGeoSearchSource(get()) }
    single { WikidataSparqlSource(get()) }
    single {
        CompositeAttractionSource(
            sources = listOf(get<OverpassApiClient>(), get<WikipediaGeoSearchSource>(), get<WikidataSparqlSource>()),
            sourceNames = listOf("Overpass/OSM", "Wikipedia GeoSearch", "Wikidata SPARQL")
        )
    }
    single<RemoteAttractionSource> { get<CompositeAttractionSource>() }
}
