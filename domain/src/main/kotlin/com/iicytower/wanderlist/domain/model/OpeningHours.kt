package com.iicytower.wanderlist.domain.model

/**
 * Godziny otwarcia z tagu OSM `opening_hours`.
 * `slots == null` oznacza format, którego parser nie obsługuje — UI pokazuje wtedy [raw].
 */
data class OpeningHours(
    val raw: String,
    val slots: List<TimeSlot>? = null,
    val isOpenNow: Boolean? = null
)

/** Przedział otwarcia; czasy jako minuty od północy, [dayOfWeek] 1=poniedziałek…7=niedziela. */
data class TimeSlot(
    val dayOfWeek: Int,
    val openMinutes: Int,
    val closeMinutes: Int
)
