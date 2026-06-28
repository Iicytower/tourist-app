package com.iicytower.wanderlist.data.remote.composite

import com.iicytower.wanderlist.data.remote.RemoteAttractionSource
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.SearchParams
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

class CompositeAttractionSource(
    private val sources: List<RemoteAttractionSource>
) : RemoteAttractionSource {

    override suspend fun searchAttractions(params: SearchParams): Result<List<Attraction>> = runCatching {
        coroutineScope {
            sources
                .map { source -> async { source.searchAttractions(params).getOrElse { emptyList() } } }
                .map { it.await() }
                .flatten()
                .let(::deduplicate)
                .sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        }
    }

    // Deduplikacja po zaokrąglonych koordynatach (3 miejsca dziesiętne ≈ 111m).
    // Jeśli kilka źródeł zwróci to samo miejsce, zachowujemy wpis z najdłuższą nazwą
    // (preferujemy bogatsze metadane).
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
