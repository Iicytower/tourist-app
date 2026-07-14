package com.iicytower.wanderlist.data.remote.composite

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import timber.log.Timber

class CompositeAttractionSource(
    private val sources: List<RemoteAttractionSource>,
    private val sourceNames: List<String>
) : RemoteAttractionSource {

    @Volatile
    var lastStats: Map<String, Int> = emptyMap()
        private set

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        coroutineScope {
            val jobs = sources.mapIndexed { index, source ->
                async {
                    source.searchAttractions(params).getOrElse { e ->
                        Timber.tag("CompositeSource").w(e, "Źródło '%s' zwróciło błąd", sourceNames.getOrElse(index) { "?" })
                        emptyList()
                    }
                }
            }
            val results = jobs.map { it.await() }

            lastStats = sourceNames.zip(results).associate { (name, list) -> name to list.size }

            results.flatten().let(::deduplicate).sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        }
    }

    private fun deduplicate(attractions: List<Attraction>): List<Attraction> =
        attractions
            .groupBy { coordKey(it.latitude, it.longitude) }
            .values
            .map { group ->
                val best = group.maxByOrNull { it.name.length }!!
                best.copy(
                    countryCode = best.countryCode ?: group.firstNotNullOfOrNull { it.countryCode },
                    openingHours = best.openingHours ?: group.firstNotNullOfOrNull { it.openingHours }
                )
            }

    private fun coordKey(lat: Double, lon: Double): String {
        val latR = (lat * 1000).roundToInt()
        val lonR = (lon * 1000).roundToInt()
        return "$latR,$lonR"
    }
}
