package com.iicytower.wanderlist.domain.usecase

import com.iicytower.wanderlist.core.util.calculateDistanceKm
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.RoutePoint
import com.iicytower.wanderlist.domain.repository.AttractionRepository
import kotlinx.coroutines.flow.first

/**
 * Trasa przez atrakcje z Mojej Listy — greedy nearest-neighbor po stronie
 * klienta (bez API routingu; realne trasy drogowe to osobny temat).
 */
class PlanRouteUseCase(
    private val attractionRepository: AttractionRepository
) {
    suspend operator fun invoke(): Result<List<RoutePoint>> = runCatching {
        val attractions = attractionRepository.getMyList().first()
        if (attractions.size < 2) error("Dodaj co najmniej 2 miejsca do listy, aby zaplanować trasę")
        greedyOrder(attractions).mapIndexed { index, attraction -> RoutePoint(attraction, index + 1) }
    }

    private fun greedyOrder(attractions: List<Attraction>): List<Attraction> {
        val remaining = attractions.toMutableList()
        val route = mutableListOf(remaining.removeAt(0))
        while (remaining.isNotEmpty()) {
            val last = route.last()
            val nearest = remaining.minByOrNull {
                calculateDistanceKm(last.latitude, last.longitude, it.latitude, it.longitude)
            }!!
            remaining.remove(nearest)
            route.add(nearest)
        }
        return route
    }
}
