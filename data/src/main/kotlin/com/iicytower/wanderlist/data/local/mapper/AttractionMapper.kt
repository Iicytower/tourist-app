package com.iicytower.wanderlist.data.local.mapper

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.local.entity.AttractionEntity
import com.iicytower.wanderlist.domain.model.Attraction
import com.iicytower.wanderlist.domain.model.DescriptionSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class DescriptionSourceDto(val name: String, val url: String)

private val json = Json { ignoreUnknownKeys = true }

fun AttractionEntity.toDomain(distanceKm: Double? = null): Attraction {
    val sources = descriptionSources?.let {
        runCatching { json.decodeFromString<List<DescriptionSourceDto>>(it) }.getOrNull()
            ?.map { dto -> DescriptionSource(dto.name, dto.url) }
    } ?: emptyList()

    return Attraction(
        xid = xid,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = runCatching { AttractionCategory.valueOf(category) }.getOrDefault(AttractionCategory.MUSEUMS_AND_GALLERIES),
        isInMyList = isInMyList,
        dateAddedToList = dateAddedToList,
        description = description,
        descriptionSources = sources,
        isFromLastSearch = isFromLastSearch,
        distanceKm = distanceKm
    )
}

fun Attraction.toEntity(): AttractionEntity {
    val sourcesJson = if (descriptionSources.isEmpty()) null else {
        json.encodeToString(descriptionSources.map { DescriptionSourceDto(it.name, it.url) })
    }
    return AttractionEntity(
        xid = xid,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = category.name,
        isInMyList = isInMyList,
        dateAddedToList = dateAddedToList,
        description = description,
        descriptionSources = sourcesJson,
        isFromLastSearch = isFromLastSearch
    )
}
