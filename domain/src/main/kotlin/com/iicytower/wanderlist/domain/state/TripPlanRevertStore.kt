package com.iicytower.wanderlist.domain.state

import com.iicytower.wanderlist.domain.model.TripPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Poprzednie wersje planów wycieczek (per lista) nadpisanych przez asystenta.
 * Żyje przez czas trwania sesji aplikacji — świadome uproszczenie zamiast trwałej historii wersji.
 */
class TripPlanRevertStore {

    private val _previousPlans = MutableStateFlow<Map<Long, TripPlan>>(emptyMap())
    val previousPlans: StateFlow<Map<Long, TripPlan>> = _previousPlans.asStateFlow()

    fun store(listId: Long, plan: TripPlan) {
        _previousPlans.value = _previousPlans.value + (listId to plan)
    }

    fun consume(listId: Long): TripPlan? {
        val plan = _previousPlans.value[listId] ?: return null
        _previousPlans.value = _previousPlans.value - listId
        return plan
    }
}
