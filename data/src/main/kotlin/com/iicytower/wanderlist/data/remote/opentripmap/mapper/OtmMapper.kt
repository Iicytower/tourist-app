package com.iicytower.wanderlist.data.remote.opentripmap.mapper

import com.iicytower.wanderlist.core.constant.CATEGORY_KINDS_MAP
import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDto
import com.iicytower.wanderlist.domain.model.Attraction

fun OtmAttractionDto.toDomain(): Attraction {
    val category = resolveCategory(kinds)
    return Attraction(
        xid = xid,
        name = name.ifBlank { xid },
        latitude = point.lat,
        longitude = point.lon,
        category = category,
        isInMyList = false,
        dateAddedToList = null,
        description = null,
        descriptionSources = emptyList(),
        isFromLastSearch = true,
        distanceKm = dist / 1000.0
    )
}

private fun resolveCategory(kinds: String): AttractionCategory {
    val kindList = kinds.split(",").map { it.trim() }
    var bestCategory = AttractionCategory.MUSEUMS_AND_GALLERIES
    var bestCount = 0
    for ((category, categoryKinds) in CATEGORY_KINDS_MAP) {
        val count = kindList.count { it in categoryKinds }
        if (count > bestCount) {
            bestCount = count
            bestCategory = category
        }
    }
    return bestCategory
}
