package com.iicytower.wanderlist.data.remote.overpass.mapper

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.core.util.calculateDistanceKm
import com.iicytower.wanderlist.data.local.OsmOpeningHoursParser
import com.iicytower.wanderlist.data.remote.overpass.dto.OverpassElement
import com.iicytower.wanderlist.domain.model.Attraction

fun OverpassElement.toAttraction(
    searchLat: Double,
    searchLon: Double,
    language: String = "pl"
): Attraction? {
    val lat = effectiveLat() ?: return null
    val lon = effectiveLon() ?: return null
    val rawName = tags["name"] ?: tags["name:pl"] ?: tags["name:en"] ?: return null
    // Nazwa w języku aplikacji z tagu OSM name:<lang> (FEAT-11) — jeśli różni się od surowej
    // nazwy, ta ostatnia jest pokazywana użytkownikowi jako nazwa oryginalna.
    val translatedName = tags["name:$language"]?.takeIf { it.isNotBlank() }
    val displayName = translatedName ?: rawName
    val originalName = translatedName?.takeIf { it != rawName }?.let { rawName }
    val xid = "${type[0]}$id"
    val distanceKm = calculateDistanceKm(searchLat, searchLon, lat, lon)
    return Attraction(
        xid = xid,
        name = displayName,
        latitude = lat,
        longitude = lon,
        category = resolveCategory(tags),
        isInMyList = false,
        dateAddedToList = null,
        description = null,
        descriptionSources = emptyList(),
        isFromLastSearch = true,
        distanceKm = distanceKm,
        countryCode = tags["addr:country"]?.uppercase()?.takeIf { it.length == 2 },
        openingHours = tags["opening_hours"]?.let { OsmOpeningHoursParser.parse(it) },
        imageUrl = resolveImageUrl(tags),
        originalName = originalName
    )
}

private fun resolveImageUrl(tags: Map<String, String>): String? {
    tags["image"]?.takeIf { it.startsWith("http") }?.let { return it }
    val commons = tags["wikimedia_commons"] ?: return null
    if (!commons.startsWith("File:")) return null
    val fileName = java.net.URLEncoder.encode(commons.removePrefix("File:"), "UTF-8").replace("+", "%20")
    return "https://commons.wikimedia.org/wiki/Special:FilePath/$fileName?width=640"
}

fun resolveCategory(tags: Map<String, String>): AttractionCategory {
    val historic = tags["historic"]
    val tourism = tags["tourism"]
    val amenity = tags["amenity"]
    val leisure = tags["leisure"]
    val natural = tags["natural"]
    val manMade = tags["man_made"]

    return when {
        historic in listOf("castle", "fort", "fortification", "palace", "city_gate", "tower") ->
            AttractionCategory.CASTLES_AND_FORTIFICATIONS
        tourism == "museum" || amenity == "arts_centre" || tourism == "gallery" ->
            AttractionCategory.MUSEUMS_AND_GALLERIES
        amenity == "place_of_worship" || historic in listOf("monastery", "abbey", "chapel", "church") ->
            AttractionCategory.CHURCHES_AND_SACRED
        historic in listOf("ruins", "archaeological_site", "roman_road") ->
            AttractionCategory.RUINS_AND_ARCHAEOLOGICAL
        tourism == "viewpoint" ->
            AttractionCategory.VIEWPOINTS
        leisure in listOf("park", "garden", "nature_reserve") || natural == "wood" ->
            AttractionCategory.NATURE_AND_PARKS
        historic == "battlefield" || tags["landuse"] == "military" ->
            AttractionCategory.MILITARY
        manMade in listOf("windmill", "watermill") || historic == "industrial" ->
            AttractionCategory.MILLS_AND_TECH
        historic in listOf("memorial", "monument", "tomb") || amenity == "grave_yard" || tags["landuse"] == "cemetery" ->
            AttractionCategory.MEMORIALS_AND_CEMETERIES
        natural == "cave_entrance" || tags["geological"] != null ->
            AttractionCategory.CAVES_AND_GEOLOGY
        else -> AttractionCategory.MUSEUMS_AND_GALLERIES
    }
}
