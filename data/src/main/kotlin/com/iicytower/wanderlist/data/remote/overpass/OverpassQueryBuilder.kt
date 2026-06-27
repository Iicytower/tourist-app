package com.iicytower.wanderlist.data.remote.overpass

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.domain.model.SearchParams

internal object OverpassQueryBuilder {

    private val CATEGORY_TAG_FILTERS: Map<AttractionCategory, List<Pair<String, String>>> = mapOf(
        AttractionCategory.CASTLES_AND_FORTIFICATIONS to listOf(
            "historic" to "castle|fort|fortification|palace|city_gate|tower"
        ),
        AttractionCategory.CHURCHES_AND_SACRED to listOf(
            "amenity" to "place_of_worship",
            "historic" to "monastery|abbey|chapel|church"
        ),
        AttractionCategory.MUSEUMS_AND_GALLERIES to listOf(
            "tourism" to "museum|gallery",
            "amenity" to "arts_centre"
        ),
        AttractionCategory.RUINS_AND_ARCHAEOLOGICAL to listOf(
            "historic" to "ruins|archaeological_site|roman_road"
        ),
        AttractionCategory.NATURE_AND_PARKS to listOf(
            "leisure" to "park|garden|nature_reserve",
            "natural" to "wood"
        ),
        AttractionCategory.VIEWPOINTS to listOf(
            "tourism" to "viewpoint"
        ),
        AttractionCategory.MILITARY to listOf(
            "historic" to "battlefield"
        ),
        AttractionCategory.MILLS_AND_TECH to listOf(
            "man_made" to "windmill|watermill",
            "historic" to "industrial"
        ),
        AttractionCategory.MEMORIALS_AND_CEMETERIES to listOf(
            "historic" to "memorial|monument|tomb",
            "amenity" to "grave_yard"
        ),
        AttractionCategory.CAVES_AND_GEOLOGY to listOf(
            "natural" to "cave_entrance"
        )
    )

    fun build(params: SearchParams): String {
        val radiusMeters = params.radiusKm * 1000
        val lat = params.latitude
        val lon = params.longitude
        val categories = params.categories.ifEmpty { AttractionCategory.entries.toSet() }

        val tagFilters = categories
            .flatMap { CATEGORY_TAG_FILTERS[it] ?: emptyList() }
            .distinct()

        val nodeLines = tagFilters.joinToString("\n  ") { (tag, valueRegex) ->
            """node["$tag"~"$valueRegex"](around:$radiusMeters,$lat,$lon);"""
        }
        val wayLines = tagFilters.joinToString("\n  ") { (tag, valueRegex) ->
            """way["$tag"~"$valueRegex"](around:$radiusMeters,$lat,$lon);"""
        }

        return """
[out:json][timeout:30];
(
  $nodeLines
  $wayLines
);
out center 200;
""".trimIndent()
    }
}
