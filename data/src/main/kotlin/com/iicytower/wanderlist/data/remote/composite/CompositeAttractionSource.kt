package com.iicytower.wanderlist.data.remote.composite

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

class CompositeAttractionSource(
    private val sources: List<RemoteAttractionSource>,
    private val sourceNames: List<String>
) : RemoteAttractionSource {

    @Volatile
    var lastStats: Map<String, Int> = emptyMap()
        private set

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        coroutineScope {
            val jobs = sources.map { source -> async { source.searchAttractions(params).getOrElse { emptyList() } } }
            val results = jobs.map { it.await() }

            lastStats = sourceNames.zip(results).associate { (name, list) -> name to list.size }

            results.flatten().let(::deduplicate).sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        }
    }

    private fun deduplicate(attractions: List<Attraction>): List<Attraction> =
        attractions
            .groupBy { coordKey(it.latitude, it.longitude) }
            .values
            .map { group -> group.maxByOrNull { it.name.length }!! }

    private fun coordKey(lat: Double, lon: Double): String {
        val latR = (lat * 1000).roundToInt()
        val lonR = (lon * 1000).roundToInt()
        return "$latR,$lonR"
    }
}
